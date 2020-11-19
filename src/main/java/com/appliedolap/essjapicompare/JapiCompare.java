package com.appliedolap.essjapicompare;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiMethod;

public class JapiCompare {

	public static void main(String[] args) {
		
		JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
		comparatorOptions.setIgnoreMissingClasses(true);
		JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
		
//		File oldArchives = new File("src/main/resources/ess_japi-11.1.2.4.010.jar");
//		File newArchives = new File("src/main/resources/ess_japi_esscs.jar");

//		File oldArchives = new File("src/main/resources/essbase-japi-11.1.2.3.jar");
//		//File oldArchives = new File("src/main/resources/ess_japi-11.1.2.4.010.jar");
//		File newArchives = new File("src/main/resources/ess_japi-11.1.2.4.014.jar");
		
		String basePath = "/Users/jasonwjones/Development/Applied OLAP/Outline Extractor NG/essbase.versions/lib";
		
		//File oldArchives = new File(basePath, "ess_japi.jar.11.1.1.4");
		File oldArchives = new File(basePath, "ess_japi.jar.11.1.2");
		
		
		//File oldArchives = new File("src/main/resources/ess_japi-11.1.2.4.014.jar");
		//File oldArchives = new File("src/main/resources/ess_japi-11.1.2.4.010.jar");
		File newArchives = new File("/Users/jasonwjones/Desktop/Essbase Cloud Jars/JAPI_Files/japijars/ess_japi.jar");
		
		///Users/jasonwjones/Desktop/Essbase Cloud Jars/JAPI_Files/japijars/ess_japi.jar
		
		//System.out.println("File exists: " + oldArchives.exists() + " / " + newArchives.exists());
		
		List<JApiClass> jApiClasses = jarArchiveComparator.compare(oldArchives, newArchives);
		
		//List<JApiMethod> changes = new ArrayList<JApiMethod>();
		//Map<JApiClass, List<JApiMethod>> changes = new HashMap<JApiClass, List<JApiMethod>>();
		Multimap<JApiClass, JApiMethod> changes = ArrayListMultimap.create();
		
		//System.out.println(jApiClasses.size());
		for (JApiClass apiClass : jApiClasses) {
			//System.out.println(apiClass.getClassType().getNewType());
			if (apiClass.getClassType().getNewType().equals("CLASS")) {
			//if (apiClass.getClassType().getNewType().equals("INTERFACE")) {
				//System.out.println(apiClass.getFullyQualifiedName());
				for (JApiMethod method : apiClass.getMethods()) {
					if (!method.getChangeStatus().equals(JApiChangeStatus.UNCHANGED)) {
						changes.put(apiClass, method);
						//System.out.println("\t" + method.getName() + " --> " + method.getChangeStatus().toString());
					}
				}
			}
		}
		
		new EssJapiChangePrinter().print(changes);
		
	}

}
