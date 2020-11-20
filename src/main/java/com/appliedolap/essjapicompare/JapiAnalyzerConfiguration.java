package com.appliedolap.essjapicompare;

import java.nio.file.Path;

public class JapiAnalyzerConfiguration {

	private Path baseFolder;

	private String prefix;

	private boolean folderPerVersion;

	private String jarName;

	private String outputFile;

	public Path getBaseFolder() {
		return baseFolder;
	}

	public void setBaseFolder(Path baseFolder) {
		this.baseFolder = baseFolder;
	}

	public boolean isFolderPerVersion() {
		return folderPerVersion;
	}

	public void setFolderPerVersion(boolean folderPerVersion) {
		this.folderPerVersion = folderPerVersion;
	}

	public String getJarName() {
		return jarName;
	}

	public void setJarName(String jarName) {
		this.jarName = jarName;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

}
