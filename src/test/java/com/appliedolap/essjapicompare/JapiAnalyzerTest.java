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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Covers the filename parsing used when every jar sits in one folder rather than in a folder per
 * version. That mode had never worked: no option supplied the prefix, and the extension was left
 * on the version string.
 */
class JapiAnalyzerTest {

	@Test
	void extractsVersionFromPrefixedFilename() {
		assertEquals(Version.of("11.1.2.4.010"),
				JapiAnalyzer.extractVersion("ess_japi-11.1.2.4.010.jar", "ess_japi-"));
	}

	@Test
	void keepsLeadingZerosInThePatchComponent() {
		assertEquals("11.1.2.4.001",
				JapiAnalyzer.extractVersion("ess_japi-11.1.2.4.001.jar", "ess_japi-").toString());
	}

	@Test
	void handlesAShortVersion() {
		assertEquals(Version.of("7.1"), JapiAnalyzer.extractVersion("ess_japi-7.1.jar", "ess_japi-"));
	}

	@Test
	void handlesADifferentPrefix() {
		assertEquals(Version.of("11.1.2.3"),
				JapiAnalyzer.extractVersion("essbase-japi-11.1.2.3.jar", "essbase-japi-"));
	}

	@Test
	void toleratesAFilenameWithNoExtension() {
		assertEquals(Version.of("9.3.1"), JapiAnalyzer.extractVersion("ess_japi-9.3.1", "ess_japi-"));
	}

}
