package com.appliedolap.essjapicompare;

import java.nio.file.Path;

public class JapiAnalyzerConfiguration {

	private Path baseFolder;

	private String prefix;

	private int maxVersionsToCheck = 0;
	
	public Path getBaseFolder() {
		return baseFolder;
	}

	public void setBaseFolder(Path baseFolder) {
		this.baseFolder = baseFolder;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public int getMaxVersionsToCheck() {
		return maxVersionsToCheck;
	}

	public void setMaxVersionsToCheck(int maxVersionsToCheck) {
		this.maxVersionsToCheck = maxVersionsToCheck;
	}
		
}
