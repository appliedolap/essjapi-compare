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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An Essbase version number, and the ordering between two of them.
 *
 * <p>Essbase numbers its releases in a way that defeats most version parsers. The number of
 * components varies from two to six - 7.1 at one end, 21.8.2.0.0.031 at the other - and patch
 * components carry leading zeros that matter when displaying a version and not at all when
 * ordering one. So 11.1.2.4.001 has to print exactly that way, while sorting as though it read 1.
 *
 * <p>A shorter version is treated as though padded with zeros, which makes 11.1.2 and 11.1.2.0 the
 * same version. {@link #equals(Object)} and {@link #hashCode()} agree with that, so the two are
 * interchangeable in a hashed or sorted collection.
 *
 * <p>Instances are immutable.
 */
public class Version implements Comparable<Version> {

	/** Numeric components, most significant first, for comparison. */
	private final int[] components;

	/**
	 * The components exactly as they were parsed, for display. Kept separately because the
	 * numeric form loses leading zeros, and those are how Oracle writes a patch set.
	 */
	private final String[] textComponents;

	/**
	 * Parses a version from text such as {@code 11.1.2.4.048}.
	 *
	 * @param version dot- or underscore-separated numeric components
	 * @throws IllegalArgumentException if any component is not a number
	 */
	public Version(String version) {
		// splits on a period or an underscore
		textComponents = version.split("[\\._]");
		components = new int[textComponents.length];

		for (int index = 0; index < textComponents.length; index++) {
			try {
				components[index] = Integer.valueOf(textComponents[index]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Could not parse version text " + version, e);
			}
		}
	}

	/**
	 * Parses a version from text. Reads better than the constructor at a call site.
	 * 
	 * @param versionText the version to parse
	 * @return a new Version object
	 */
	public static Version of(String versionText) {
		return new Version(versionText);
	}

	/**
	 * Construct a new Version object with the explicit version components.
	 *
	 * @param components the individual version components, most significant first
	 */
	public Version(int... components) {
		this.components = components;

		// toString() reports the original text, so this constructor has to supply it too -
		// without this it threw a NullPointerException for any Version built from ints.
		this.textComponents = new String[components.length];
		for (int index = 0; index < components.length; index++) {
			this.textComponents[index] = Integer.toString(components[index]);
		}
	}

	/** The first component, or zero if there is none. */
	public int getMajor() {
		return getComponent(0);
	}

	/** The second component, or zero if there is none. */
	public int getMinor() {
		return getComponent(1);
	}

	/** The third component, or zero if there is none. */
	public int getRevision() {
		return getComponent(2);
	}

	/** The fourth component, or zero if there is none. */
	public int getBuild() {
		return getComponent(3);
	}

	/**
	 * One component by position, or zero past the end - which is what makes a shorter version
	 * compare as though it were padded with zeros.
	 */
	private int getComponent(int index) {
		if (index < components.length) {
			return components[index];
		} else {
			return 0;
		}
	}

	/**
	 * Checks if this Version object is higher than the Version represented by
	 * the passed in components. If the passed in components are more detailed
	 * (i.e., components.length is greater than the number of components in this
	 * version, the missing components on *this* version are treated like zero).
	 * 
	 * @param components the components to check against
	 * @return true if this version is greater than the version represented by
	 *         the passed in components
	 */
	public boolean isGreaterOrEqual(int... components) {
		for (int index = 0; index < components.length; index++) {
			int thisComponent = getComponent(index);
			int thatComponent = components[index];

			if (thisComponent > thatComponent) {
				return true;
			} else if (thisComponent < thatComponent) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether this version is at least the given one.
	 *
	 * @param otherVersion the version to compare against
	 * @return true if this version is the same or higher
	 */
	public boolean isGreaterOrEqual(Version otherVersion) {
		return isGreaterOrEqual(otherVersion.getComponents());
	}

	/** The numeric components. Internal: the array is the live one, not a copy. */
	private int[] getComponents() {
		return components;
	}

	/**
	 * How many components were actually given, as opposed to how many can be read. A version
	 * built from {@code 9.3} has two, though {@link #getBuild()} will answer zero for it.
	 */
	public int getNumComponents() {
		return components.length;
	}

	/**
	 * Generates a text representation using the original parsed String values.
	 * This is an alternative method to use when printing when you need leading
	 * zeros on a version that would otherwise get lost in the conversion to a
	 * string.
	 *
	 * @return text representation of the version, based on original text values
	 */
	@Override
	public String toString() {
		return Arrays.stream(textComponents).collect(Collectors.joining("."));
	}

	/**
	 * Compares one version to another using standard comparison philosophy.
	 * Checks component by component using the length of this Version of the
	 * comparison version, whichever is greater.
	 */
	@Override
	public int compareTo(Version o) {
		int numComponents = Math.max(this.getNumComponents(), o.getNumComponents());
		for (int index = 0; index < numComponents; index++) {
			if (getComponent(index) < o.getComponent(index)) {
				return -1;
			} else if (getComponent(index) > o.getComponent(index)) {
				return 1;
			}
		}
		return 0;
	}

	/**
	 * Consistent with {@link #compareTo(Version)}, which pads the shorter version with zeros -
	 * so trailing zeros are not significant here either, and 11.1.2 hashes the same as 11.1.2.0.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (int index = 0; index < significantLength(); index++) {
			result = prime * result + components[index];
		}
		return result;
	}

	/**
	 * Defined in terms of {@link #compareTo(Version)} rather than raw component equality. Those
	 * two used to disagree: 11.1.2 and 11.1.2.0 compared equal but were not equal, which breaks
	 * the contract every sorted and hashed collection relies on.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Version)) {
			return false;
		}
		return compareTo((Version) obj) == 0;
	}

	/** Number of leading components excluding any trailing zeros, which carry no meaning. */
	private int significantLength() {
		int length = components.length;
		while (length > 0 && components[length - 1] == 0) {
			length--;
		}
		return length;
	}

}
