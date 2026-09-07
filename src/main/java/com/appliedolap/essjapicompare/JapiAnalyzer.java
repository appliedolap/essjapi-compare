package com.appliedolap.essjapicompare;

import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiMethod;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

	public List<ChangeSet> run(JapiAnalyzerConfiguration configuration) throws IOException {
		List<Path> jars = createOrderedJarList(configuration);
		for (Path jar : jars) {
			System.out.println(jar);
		}
		
		List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
		
		for (int index = 0; index < jars.size(); index++) {
			Path currentJar = jars.get(index);
			Path previousJar = index > 0 ? jars.get(index - 1) : null;
						
			System.out.println("JAR: " + currentJar.toAbsolutePath() + ", prev: " + previousJar);
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
				
				System.out.println("Comparing " + currentJar + " to previous " + previousJar);
				List<JApiClass> changes = compare(currentJar, previousJar);
				
				ChangeSet changeSet = new ChangeSet(currentVersion, previousVersion, changes);
				changeSet.setNextVersion(nextVersion);
				changeSets.add(changeSet);
			} else {
				System.out.println("Will skip comparison on first JAR");
			}
		}

		ITemplateResolver resolver = new ClassLoaderTemplateResolver();
		TemplateEngine engine = new TemplateEngine();
		engine.setTemplateResolver(resolver);

		Context thContext = new Context();
		thContext.setVariable("changes", changeSets);
		thContext.setVariable("stylesheet", readVendoredStylesheet());

		// Explicit UTF-8 rather than the platform default: the report is a single self-contained
		// file that carries the stylesheet inline, and that stylesheet is UTF-8 with non-ASCII
		// characters in its own license banner.
		try (Writer writer = new OutputStreamWriter(
				new FileOutputStream(configuration.getOutputFile()), StandardCharsets.UTF_8)) {
			engine.process("templates/index.html", thContext, writer);
		}
		return changeSets;
	}

	/**
	 * The vendored Pico CSS, inlined into the report rather than linked.
	 *
	 * <p>The report is meant to stay readable for years with no maintenance, and it used to depend
	 * on a stylesheet from a third-party CDN - one outage or one retired URL away from losing its
	 * formatting silently. Inlining costs about 83 KB in a file already measured in megabytes, and
	 * leaves the output with no external dependencies at all.
	 */
	private String readVendoredStylesheet() throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/pico.min.css")) {
			if (in == null) {
				throw new IOException("pico.min.css is missing from the classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public List<JApiClass> compare(Path newJar, Path oldJar) {
		JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
		comparatorOptions.setIncludeSynthetic(true);
		comparatorOptions.setNoAnnotations(true);
		comparatorOptions.setIgnoreMissingClasses(true);
		JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

		List<JApiClass> jApiClasses = jarArchiveComparator.compare(oldJar.toFile(), newJar.toFile());

		System.out.println("Looking at annotations");
		for (JApiClass apiClass : jApiClasses) {
			//if (apiClass.getFullyQualifiedName().equals("com.essbase.api.domain.IEssDomain")) {
				for (JApiMethod method : apiClass.getMethods()) {
					//if (method.getName().equals("getUser")) {
						if (TempCompare.isMethodNowDeprecated(method)) {
							System.out.println("Deprecated method: " + apiClass.getFullyQualifiedName() + "." + method.getName());							
						}
						if (method.getChangeStatus().equals(JApiChangeStatus.NEW)) {
							System.out.println("New method: " + apiClass.getFullyQualifiedName() + "." + method.getName());
						}
						/*
						System.out.println("Checking " + apiClass.getFullyQualifiedName() + "#" + method.getName());
						for (JApiAttribute<? extends Enum<?>> attr : method.getAttributes()) {
							System.out.println("Attrib: " + attr.getNewValue());
						}
						for (JApiModifier<? extends Enum<? extends Enum<?>>> mod : method.getModifiers()) {
							System.out.println("Modifier: " + mod.getValueNew());
						}
						
						for (JApiAnnotation annotation : method.getAnnotations()) {
							System.out.println("New annotation on class: " + apiClass.getFullyQualifiedName() + ": " + annotation.getNewAnnotation().get());
						}
						*/
					}
				//}
//			}
			
		}
		
		return jApiClasses;
		
//		
//		for (JApiClass apiClass : jApiClasses) {
//			// new classes
//			switch (apiClass.getChangeStatus()) {
//			case MODIFIED:
//				System.out.println("Modified class: " + apiClass.getFullyQualifiedName());
//				break;
//			case NEW:
//				System.out.println("New class: " + apiClass.getFullyQualifiedName());
//				System.out.println("Access: " + apiClass.getAccessModifier().getValueNew());
//				break;
//			case REMOVED:
//				System.out.println("Removed class: " + apiClass.getFullyQualifiedName());
//				break;
//			case UNCHANGED:
//			default:
//				break;
//			
//			}
//			/*
//			if (apiClass.getChangeStatus().equals(JApiChangeStatus.NEW)) {
//				if (apiClass.getAccessModifier().getNewModifier().get().equals(AccessModifier.PUBLIC)) {
//					System.out.println("New class: " + apiClass.getFullyQualifiedName());
//					System.out.println("Access: " + apiClass.getAccessModifier().getValueNew());
//				}
//			}
//			*/
//
//			/*
//			if (apiClass.getChangeStatus().equals(JApiChangeStatus.REMOVED)) {
//				System.out.println("Removed class: " + apiClass.getFullyQualifiedName());
//				System.exit(0);
//			}
//			*/
//			
//			for (JApiMethod method : apiClass.getMethods()) {
//				switch (method.getChangeStatus()) {
//				case MODIFIED:
//					System.out.println("Modified method: " + method.getName());
//					break;
//				case NEW:
//					System.out.println("New method: " + method.getName());
//					break;
//				case REMOVED:
//					System.out.println("Removed method: " + method.getName());
//					break;
//				case UNCHANGED:
//				default:
//					break;
//				}
//			}
//			
//		}
//		System.out.println();
//		
//		// List<JApiMethod> changes = new ArrayList<JApiMethod>();
//		// Map<JApiClass, List<JApiMethod>> changes = new HashMap<JApiClass,
//		// List<JApiMethod>>();
//		Multimap<JApiClass, JApiMethod> changes = ArrayListMultimap.create();
//
//		// System.out.println(jApiClasses.size());
//		/*
//		for (JApiClass apiClass : jApiClasses) {
//			// System.out.println(apiClass.getClassType().getNewType());
//			if (apiClass.getClassType().getNewType().equals("CLASS")) {
//				// if (apiClass.getClassType().getNewType().equals("INTERFACE"))
//				// {
//				// System.out.println(apiClass.getFullyQualifiedName());
//				for (JApiMethod method : apiClass.getMethods()) {
//					if (!method.getChangeStatus().equals(JApiChangeStatus.UNCHANGED)) {
//						changes.put(apiClass, method);
//						// System.out.println("\t" + method.getName() + " --> "
//						// + method.getChangeStatus().toString());
//					}
//				}
//			}
//		}
//		*/
//		//new EssJapiChangePrinter().print(changes);

		
		
	}

	public static Version extractVersion(String filename, String prefix) {
		return Version.of(filename.substring(prefix.length()));
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

	private static Comparator<Path> comparator(String prefix) {
		return (o1, o2) -> {
			String s1 = o1.getFileName().toString().substring(prefix.length());
			String s2 = o2.getFileName().toString().substring(prefix.length());
			Version v1 = Version.of(s1);
			Version v2 = Version.of(s2);
			return v1.compareTo(v2);
		};
	}

}
