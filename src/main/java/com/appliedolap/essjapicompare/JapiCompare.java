package com.appliedolap.essjapicompare;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiMethod;
import picocli.CommandLine;

@CommandLine.Command(name = "japicompare")
public class JapiCompare implements Runnable {

	@CommandLine.Option(names = "--base-folder", description = "The base folder containing jars of folders of jars", required = true)
	File baseFolder;

	@CommandLine.Option(names = "--jar-name", description = "The name of the jar file to compare, if using version folders", defaultValue = "ess_japi.jar")
	String jarName;

	@CommandLine.Option(names = "--version-folders", description = "Set to true if jars are in folders with a version number")
	boolean baseFolderPerVersion;

	@CommandLine.Option(names = "--output-file", description = "Set name of output HTML file", defaultValue = "japi-compare.html")
	String outputFile;

	@Override
	public void run() {
		try {
			JapiAnalyzerConfiguration configuration = new JapiAnalyzerConfiguration();
			configuration.setBaseFolder(baseFolder.toPath());
			configuration.setFolderPerVersion(baseFolderPerVersion);
			configuration.setOutputFile(outputFile);
			if (baseFolderPerVersion) {
				configuration.setJarName(jarName);
			}
			JapiAnalyzer japiAnalyzer = new JapiAnalyzer();
			japiAnalyzer.run(configuration);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new JapiCompare()).execute(args);
		System.exit(exitCode);
	}

}
