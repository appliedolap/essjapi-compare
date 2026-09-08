/*
 * Copyright 2017-2026 Applied OLAP, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appliedolap.essjapicompare;

import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Compares a sequence of Essbase client jars and writes the report.
 *
 * <p>Each jar is compared against the one before it, so a run over N jars produces N-1 change
 * sets; the oldest jar is the baseline and has nothing to be compared against. The result is
 * rendered through a Thymeleaf template into a single self-contained HTML file.
 */
public class JapiAnalyzer {

	/** Creates an analyzer. What to compare is supplied per run. */
	public JapiAnalyzer() {
	}

	private static final Logger logger = LoggerFactory.getLogger(JapiAnalyzer.class);

	private static final String JAR_EXTENSION = ".jar";

	/**
	 * Runs the comparison and writes the report.
	 *
	 * @param configuration which jars to read and where to write the result
	 * @return one change set per comparison, oldest first
	 * @throws IOException if the jars cannot be listed or the report cannot be written
	 */
	public List<ChangeSet> run(JapiAnalyzerConfiguration configuration) throws IOException {
		List<Path> jars = createOrderedJarList(configuration);
		logger.info("Comparing {} jars", jars.size());
		
		List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
		
		for (int index = 0; index < jars.size(); index++) {
			Path currentJar = jars.get(index);
			Path previousJar = index > 0 ? jars.get(index - 1) : null;
						
			if (previousJar != null) {
				Version currentVersion;
				Version previousVersion;
				if (!configuration.isFolderPerVersion()) {
					currentVersion = extractVersion(currentJar.getFileName().toString(), configuration.getPrefix());
					previousVersion = extractVersion(previousJar.getFileName().toString(), configuration.getPrefix());
				} else {
					currentVersion = Version.of(currentJar.toFile().getParentFile().getName());
					previousVersion = Version.of(previousJar.toFile().getParentFile().getName());
				}

				Version nextVersion = null;
				if (index + 1 < jars.size()) {
					Path nextJar = jars.get(index + 1);
					if (!configuration.isFolderPerVersion()) {
						nextVersion = extractVersion(nextJar.getFileName().toString(), configuration.getPrefix());
					} else {
						nextVersion = Version.of(nextJar.toFile().getParentFile().getName());
					}
				}
				
				List<JApiClass> changes = compare(currentJar, currentVersion, previousJar, previousVersion);
				
				ChangeSet changeSet = new ChangeSet(currentVersion, previousVersion, changes);
				changeSet.setNextVersion(nextVersion);
				changeSets.add(changeSet);
			} else {
				logger.debug("{} is the baseline; nothing to compare it against", currentJar.getFileName());
			}
		}

		ITemplateResolver resolver = new ClassLoaderTemplateResolver();
		TemplateEngine engine = new TemplateEngine();
		engine.setTemplateResolver(resolver);

		Context thContext = new Context();
		thContext.setVariable("changes", changeSets);

		// What the report covers, for the footer. Deliberately no generation timestamp: the
		// report is regenerated occasionally and committed, and a timestamp would make every
		// regeneration a diff even when no jar changed. The commit already records when.
		if (!changeSets.isEmpty()) {
			thContext.setVariable("jarCount", changeSets.size() + 1);
			thContext.setVariable("firstVersion", changeSets.get(0).getPreviousVersion());
			thContext.setVariable("lastVersion", changeSets.get(changeSets.size() - 1).getCurrentVersion());
		}

		// Explicit UTF-8 rather than the platform default, matching the charset the report
		// declares in its own meta tag - otherwise the encoding of the output depends on
		// whichever machine happened to generate it.
		try (Writer writer = new OutputStreamWriter(
				new FileOutputStream(configuration.getOutputFile()), StandardCharsets.UTF_8)) {
			engine.process("templates/index.html", thContext, writer);
		}
		return changeSets;
	}

	/**
	 * Compares two jars and returns every class japicmp has something to say about.
	 *
	 * <p>Annotations are switched off and missing classes ignored: the Essbase client jars
	 * reference plenty they do not ship, and the report is about the public surface rather than
	 * about what japicmp could not resolve. Synthetic members are included because japicmp
	 * classifies some real accessors that way.
	 *
	 * @param newJar the newer jar
	 * @param newVersion the version the newer jar represents
	 * @param oldJar the older jar
	 * @param oldVersion the version the older jar represents
	 * @return every class japicmp reported on
	 */
	public List<JApiClass> compare(Path newJar, Version newVersion, Path oldJar, Version oldVersion) {
		JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
		comparatorOptions.setIncludeSynthetic(true);
		comparatorOptions.setNoAnnotations(true);
		comparatorOptions.getIgnoreMissingClasses().setIgnoreAllMissingClasses(true);

		logger.debug("Comparing {} against {}", newVersion, oldVersion);
		return new JarArchiveComparator(comparatorOptions).compare(
				new JApiCmpArchive(oldJar.toFile(), oldVersion.toString()),
				new JApiCmpArchive(newJar.toFile(), newVersion.toString()));
	}

	/**
	 * Reads the version out of a filename like {@code ess_japi-11.1.2.4.010.jar}, given the part
	 * that comes before it.
	 *
	 * <p>The extension has to come off as well as the prefix. Without that this produced
	 * {@code 11.1.2.4.010.jar} and Version rejected it, so the one folder of many jars mode threw
	 * on the first file it found.
	 *
	 * @param filename the jar's filename, including prefix and extension
	 * @param prefix what precedes the version in that filename
	 * @return the version the filename names
	 * @throws IllegalArgumentException if what remains is not a version
	 */
	public static Version extractVersion(String filename, String prefix) {
		String version = filename.substring(prefix.length());
		// The known extension specifically, not everything after the last dot: a version is
		// itself dotted, so trimming at the last dot turns 9.3.1 into 9.3.
		if (version.toLowerCase().endsWith(JAR_EXTENSION)) {
			version = version.substring(0, version.length() - JAR_EXTENSION.length());
		}
		return Version.of(version);
	}
	
	/**
	 * Finds the jars to compare and puts them in version order.
	 *
	 * <p>Order is everything here: the report describes what changed from one release to the next,
	 * so the jars have to be sorted by version rather than by filename, which would put
	 * 11.1.2.4.010 before 11.1.2.4.9.
	 *
	 * @param configuration the layout to read, and the prefix or jar name it implies
	 * @return the jars, oldest version first
	 * @throws IOException if the folder cannot be read
	 */
	public List<Path> createOrderedJarList(JapiAnalyzerConfiguration configuration) throws IOException {
		if (!configuration.isFolderPerVersion()) {
			return Files.list(configuration.getBaseFolder())
					.filter(e -> e.getFileName().toString().startsWith(configuration.getPrefix()))
					.sorted(comparator(configuration.getPrefix()))
					.collect(Collectors.toList());
		} else {
			File base = configuration.getBaseFolder().toFile();
			File[] subDirectories = base.listFiles(File::isDirectory);
			Arrays.sort(subDirectories, Comparator.comparing(o -> Version.of(o.getName())));
			List<Path> paths = new ArrayList<>();
			for (File subdirectory : subDirectories) {
				File jarFile = new File(subdirectory, configuration.getJarName());
				paths.add(jarFile.toPath());
			}
			return paths;
		}
	}

	/**
	 * Orders jars in a single folder by the version in their filename.
	 *
	 * <p>Delegates to {@link #extractVersion(String, String)} rather than parsing the name again.
	 * It used to do its own parsing and, like the original of that method, left the extension on -
	 * so sorting threw before the comparison could even start.
	 */
	private static Comparator<Path> comparator(String prefix) {
		return Comparator.comparing(jar -> extractVersion(jar.getFileName().toString(), prefix));
	}

}
