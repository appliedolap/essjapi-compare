package com.appliedolap.essjapicompare;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class Version implements Comparable<Version> {

	private int[] components;

	private String[] textComponents;

	public static final Comparator<Version> COMPARATOR = new Comparator<Version>() {
		@Override
		public int compare(Version o1, Version o2) {
			return o1.compareTo(o2);
		}
	};

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
	 * Convenience fluent method or building a new Version object.
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
	 * @param components
	 */
	public Version(int... components) {
		this.components = components;
	}

	public int getMajor() {
		return getComponent(0);
	}

	public int getMinor() {
		return getComponent(1);
	}

	public int getRevision() {
		return getComponent(2);
	}

	public int getBuild() {
		return getComponent(3);
	}

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

	public boolean isGreaterOrEqual(Version otherVersion) {
		return isGreaterOrEqual(otherVersion.getComponents());
	}

	public int[] getComponents() {
		return components;
	}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(components);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Version other = (Version) obj;
		if (!Arrays.equals(components, other.components)) {
			return false;
		}
		return true;
	}

}
