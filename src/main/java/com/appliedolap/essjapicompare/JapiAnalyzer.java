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

public class JapiAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(JapiAnalyzer.class);

	private static final String JAR_EXTENSION = ".jar";

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
