package com.appliedolap.essjapicompare;

import java.io.File;

import picocli.CommandLine;

@CommandLine.Command(name = "japicompare")
public class JapiCompare implements Runnable {

	@CommandLine.Option(names = "--base-folder", description = "The base folder containing jars of folders of jars", required = true)
	File baseFolder;

	@CommandLine.Option(names = "--jar-name", description = "The name of the jar file to compare, if using version folders", defaultValue = "ess_japi.jar")
	String jarName;

	@CommandLine.Option(names = "--version-folders", description = "Set to true if jars are in folders with a version number")
	boolean baseFolderPerVersion;

	@CommandLine.Option(names = "--prefix", description = "Filename prefix before the version, when jars sit in one folder (default: ${DEFAULT-VALUE})", defaultValue = "ess_japi-")
	String prefix;

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
			} else {
				// Without this the flat-folder mode dereferenced a null prefix and threw. There
				// was no option to supply one, so that mode could never have run at all.
				configuration.setPrefix(prefix);
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
