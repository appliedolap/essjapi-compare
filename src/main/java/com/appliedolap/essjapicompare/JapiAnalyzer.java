package com.appliedolap.essjapicompare;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiAttribute;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiMethod;
import japicmp.model.JApiModifier;
import japicmp.output.stdout.StdoutOutputGenerator;

public class JapiAnalyzer {

	public List<ChangeSet> run(JapiAnalyzerConfiguration configuration) throws IOException {
		List<Path> jars = createOrderedJarList(configuration);
		for (Path jar : jars) {
			System.out.println(jar);
		}
		
		List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
		
		int count = 0;
		for (int index = 0; index < jars.size(); index++) {
			Path currentJar = jars.get(index);
			Path previousJar = index > 0 ? jars.get(index - 1) : null;
						
			System.out.println("JAR: " + currentJar.toAbsolutePath() + ", prev: " + previousJar);
			if (previousJar != null) {
				Version currentVersion = extractVersion(currentJar.getFileName().toString(), configuration.getPrefix());
				Version previousVersion = extractVersion(previousJar.getFileName().toString(), configuration.getPrefix());

				Version nextVersion = null;
				if (index + 1 < jars.size()) {
					Path nextJar = jars.get(index + 1);
					nextVersion = extractVersion(nextJar.getFileName().toString(), configuration.getPrefix());
				}
				
				System.out.println("Comparing " + currentJar + " to previous " + previousJar);
				List<JApiClass> changes = compare(currentJar, previousJar);
				
				ChangeSet changeSet = new ChangeSet(currentVersion, previousVersion, changes);
				changeSet.setNextVersion(nextVersion);
				
				changeSets.add(changeSet);
				if (!(++count < configuration.getMaxVersionsToCheck() || configuration.getMaxVersionsToCheck() <= 0)) break;
			} else {
				System.out.println("Will skip comparison on first JAR");
			}
		}
		
		
		ITemplateResolver resolver = new ClassLoaderTemplateResolver();
		TemplateEngine engine = new TemplateEngine();
		engine.setTemplateResolver(resolver);
		
		Date generationTime = new Date(System.currentTimeMillis());

		Context thContext = new Context();
		thContext.setVariable("changes", changeSets);
		Writer writer = new FileWriter("/Users/jasonwjones/Desktop/japi.html");
		engine.process("templates/index.html", thContext, writer);
		writer.close();
		
		return changeSets;
	}

	public List<JApiClass> compare(Path newJar, Path oldJar) {
		JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
		comparatorOptions.setIncludeSynthetic(true);
		comparatorOptions.setNoAnnotations(true);
		comparatorOptions.setIgnoreMissingClasses(true);
		//comparatorOptions.set
		JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

		List<JApiClass> jApiClasses = jarArchiveComparator.compare(oldJar.toFile(), newJar.toFile());
		
		//Options options = Options.newDefault();
		//StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(options, jApiClasses);
		//String output = stdoutOutputGenerator.generate();
		//System.out.println(output);
		//System.exit(0);
		
		
		//jApiClasses.get(0).getMethods().get(0).getAccessModifier().g
		
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
		return Files.list(configuration.getBaseFolder())
				.filter(e -> e.getFileName().toString().startsWith(configuration.getPrefix()))
				.sorted(comparator(configuration.getPrefix()))
				.collect(Collectors.toList());
	}

	private static Comparator<Path> comparator(String prefix) {
		return new Comparator<Path>() {
			@Override
			public int compare(Path o1, Path o2) {
				String s1 = o1.getFileName().toString().substring(prefix.length());
				String s2 = o2.getFileName().toString().substring(prefix.length());
				Version v1 = Version.of(s1);
				Version v2 = Version.of(s2);
				return v1.compareTo(v2);
			}
		};
	}

}
