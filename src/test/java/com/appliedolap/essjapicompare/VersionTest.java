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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Version}, the ordering of Essbase version numbers.
 *
 * <p>Essbase numbers releases in ways that defeat most version parsers: a varying number of
 * components (7.1 through 21.8.2.0.0.031), and patch components whose leading zeros matter for
 * display but not for ordering.
 */
class VersionTest {

	@Test
	void parsesComponentsFromText() {
		Version version = Version.of("11.1.2.3");

		assertEquals(11, version.getMajor());
		assertEquals(1, version.getMinor());
		assertEquals(2, version.getRevision());
		assertEquals(3, version.getBuild());
	}

	@Test
	void parsesAJavaStyleVersionWithAnUnderscore() {
		Version java = Version.of("1.8.0_25");

		assertEquals(1, java.getMajor());
		assertEquals(8, java.getMinor());
		assertEquals(0, java.getRevision());
		assertEquals(25, java.getBuild());
	}

	@Test
	void treatsAbsentComponentsAsZero() {
		Version version = Version.of("9.3");

		assertEquals(9, version.getMajor());
		assertEquals(3, version.getMinor());
		assertEquals(0, version.getRevision());
		assertEquals(0, version.getBuild());
	}

	@Test
	void reportsHowManyComponentsWereGiven() {
		assertEquals(2, Version.of("9.3").getNumComponents());
		assertEquals(6, Version.of("21.8.2.0.0.031").getNumComponents());
	}

	@Test
	void rejectsTextThatIsNotAVersion() {
		assertThrows(IllegalArgumentException.class, () -> Version.of("not-a-version"));
		assertThrows(IllegalArgumentException.class, () -> Version.of("11.1.2.4-beta"));
	}

	@Test
	void comparesAgainstLooseComponents() {
		Version version = Version.of("11.1.2.3");

		assertTrue(version.isGreaterOrEqual(11, 1, 2, 2));
		assertTrue(version.isGreaterOrEqual(11, 1, 2, 3));
		assertFalse(version.isGreaterOrEqual(11, 1, 2, 4));
	}

	@Test
	void comparesAgainstAnotherVersion() {
		Version lower = Version.of("11.1.2.3");
		Version higher = Version.of("11.1.2.4");

		assertTrue(higher.isGreaterOrEqual(lower));
		assertTrue(higher.isGreaterOrEqual(higher));
		assertFalse(lower.isGreaterOrEqual(higher));
	}

	@Test
	void ordersVersionsOfDifferentLengths() {
		Version longer = Version.of("11.1.2.4");
		Version shorter = Version.of("11.1.2");

		assertTrue(longer.compareTo(shorter) > 0);
		assertTrue(shorter.compareTo(longer) < 0);
	}

	@Test
	void ordersTheRealEssbaseSequence() {
		assertTrue(Version.of("9.3.1").compareTo(Version.of("11.1.1")) < 0);
		assertTrue(Version.of("11.1.2.4.048").compareTo(Version.of("11.1.2.4.008")) > 0);
		assertTrue(Version.of("21.8.2.0.0.031").compareTo(Version.of("21.8.0.0.0.414")) > 0);
	}

	/**
	 * Leading zeros are how Oracle writes patch sets, and dropping them would misrepresent the
	 * version. They carry no weight in ordering, though - see
	 * {@link #treatsTrailingZerosAsInsignificantWhenComparing()}.
	 */
	@Test
	void keepsTheTextItWasGivenWhenPrinting() {
		assertEquals("11.1.2.4.001", Version.of("11.1.2.4.001").toString());
		assertEquals("21.8.2.0.0.031", Version.of("21.8.2.0.0.031").toString());
	}

	@Test
	void printsVersionsBuiltFromComponents() {
		assertEquals("11.1.2.4", new Version(11, 1, 2, 4).toString());
		assertEquals("9.3.1", new Version(9, 3, 1).toString());
	}

	/**
	 * compareTo() pads the shorter version with zeros, so equals() and hashCode() have to agree
	 * that trailing zeros carry no meaning. These three used to disagree, which breaks every
	 * sorted and hashed collection.
	 */
	@Test
	void treatsTrailingZerosAsInsignificantWhenComparing() {
		Version shorter = Version.of("11.1.2");
		Version padded = Version.of("11.1.2.0");

		assertEquals(0, shorter.compareTo(padded));
		assertEquals(shorter, padded);
		assertEquals(shorter.hashCode(), padded.hashCode());
	}

	@Test
	void distinguishesVersionsThatDiffer() {
		assertNotEquals(Version.of("11.1.2.3"), Version.of("11.1.2.4"));
		assertNotEquals(Version.of("11.1.2"), Version.of("11.1.2.1"));
		assertNotEquals(Version.of("11.1.2.4.001"), Version.of("11.1.2.4.010"));
	}

	@Test
	void isNotEqualToOtherTypes() {
		assertNotEquals(Version.of("11.1.2"), "11.1.2");
		assertNotEquals(null, Version.of("11.1.2"));
	}

}
