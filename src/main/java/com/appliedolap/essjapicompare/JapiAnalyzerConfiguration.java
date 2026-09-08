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

import java.nio.file.Path;

/**
 * What to compare and where to put the result.
 *
 * <p>Two layouts are supported, and {@link #isFolderPerVersion()} chooses between them. Either the
 * base folder holds one subfolder per version, each containing a jar under the same name - which is
 * how the Essbase client jars are archived - or it holds every jar together, named with a common
 * prefix and the version, as in {@code ess_japi-11.1.2.4.048.jar}.
 */
public class JapiAnalyzerConfiguration {

	private Path baseFolder;

	private String prefix;

	private boolean folderPerVersion;

	private String jarName;

	private String outputFile;

	/** The folder holding the jars, or the folders of jars. */
	public Path getBaseFolder() {
		return baseFolder;
	}

	/** @param baseFolder the folder to read jars from */
	public void setBaseFolder(Path baseFolder) {
		this.baseFolder = baseFolder;
	}

	/** Whether each version has its own subfolder, rather than every jar sitting together. */
	public boolean isFolderPerVersion() {
		return folderPerVersion;
	}

	/** @param folderPerVersion true if each version has its own subfolder */
	public void setFolderPerVersion(boolean folderPerVersion) {
		this.folderPerVersion = folderPerVersion;
	}

	/** The jar to read inside each version folder. Only used in the folder-per-version layout. */
	public String getJarName() {
		return jarName;
	}

	/** @param jarName the filename to look for in each version folder */
	public void setJarName(String jarName) {
		this.jarName = jarName;
	}

	/** What comes before the version in a filename. Only used when every jar sits together. */
	public String getPrefix() {
		return prefix;
	}

	/** @param prefix the filename prefix preceding the version */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/** Where to write the report. */
	public String getOutputFile() {
		return outputFile;
	}

	/** @param outputFile the path to write the report to */
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

}
