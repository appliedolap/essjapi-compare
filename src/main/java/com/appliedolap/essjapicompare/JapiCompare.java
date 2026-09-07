package com.appliedolap.essjapicompare;

import java.io.File;

import picocli.CommandLine;

@CommandLine.Command(
		name = "essjapi-compare",
		mixinStandardHelpOptions = true,
		versionProvider = JapiCompare.ManifestVersion.class,
		description = "Compares successive versions of the Essbase Java API client jar and reports "
				+ "what changed as a single HTML file.")
/**
 * Command line entry point.
 *
 * <p>Reads a folder of Essbase client jars and writes an HTML report of what changed between
 * successive versions. Two layouts are understood - a folder per version, or every jar together
 * with a common filename prefix - and {@code --version-folders} selects the first.
 */
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

	/** Builds the configuration from the command line and runs the comparison. */
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

	/**
	 * Supplies the version banner from the jar's own manifest, rather than from a constant that
	 * would need updating at every release. Standard help options advertise the flag whether or
	 * not a version is configured, so without this the flag would print nothing.
	 */
	static class ManifestVersion implements CommandLine.IVersionProvider {

		/** @return one line naming the tool and the version recorded in its manifest */
		@Override
		public String[] getVersion() {
			String version = JapiCompare.class.getPackage().getImplementationVersion();
			return new String[]{"essjapi-compare " + (version != null ? version : "(development build)")};
		}

	}

	/**
	 * @param args command line arguments; see {@code --help}
	 */
	public static void main(String[] args) {
		int exitCode = new CommandLine(new JapiCompare()).execute(args);
		System.exit(exitCode);
	}

}
