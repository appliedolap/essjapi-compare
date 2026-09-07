package com.appliedolap.essjapicompare;

/**
 * One method as the report renders it: the strings to print, already chosen.
 *
 * <p>Which side of the comparison to read depends on what happened to the method - an added
 * method has only a new signature, a removed one only an old signature - and the template used to
 * make that choice itself, with a pair of conditional spans for each of the three values. Thymeleaf
 * leaves an element's indentation behind when a condition removes it, so every method printed cost
 * three blank lines; those were 7,000 of the 10,000 that remained in the output.
 *
 * <p>Resolving it here leaves the template with no conditionals at all, and puts the rule about
 * which side to read in one place instead of six.
 */
public final class MethodChange {

	private final String access;

	private final String returnType;

	private final String shortReturnType;

	private final String signature;

	private final String statusClass;

	MethodChange(String access, String returnType, String signature, String statusClass) {
		this.access = access;
		this.returnType = returnType;
		this.signature = signature;
		this.statusClass = statusClass;
		this.shortReturnType = shorten(returnType);
	}

	/**
	 * Drops the {@code java.lang.} package qualifier, so a return type reads {@code String} rather
	 * than {@code java.lang.String}.
	 *
	 * <p>Only the added section has ever done this; the modified and deprecated sections print the
	 * qualified name. Both forms are offered here rather than quietly settling that inconsistency,
	 * which is a decision about the report rather than about this refactoring.
	 */
	private static String shorten(String type) {
		return type != null && type.startsWith("java.lang") ? type.substring(10) : type;
	}

	/** Access modifier, lowercased: {@code public}, {@code protected}. */
	public String getAccess() {
		return access;
	}

	/** Return type, fully qualified. */
	public String getReturnType() {
		return returnType;
	}

	/** Return type with {@code java.lang.} removed. */
	public String getShortReturnType() {
		return shortReturnType;
	}

	/** The method signature as japicmp renders it, including parameter types. */
	public String getSignature() {
		return signature;
	}

	/** The CSS class carrying what happened to it: {@code method-new} and so on. */
	public String getStatusClass() {
		return statusClass;
	}

}
