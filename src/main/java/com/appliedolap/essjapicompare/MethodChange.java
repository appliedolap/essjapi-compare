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

	private final String signature;

	private final String statusClass;

	MethodChange(String access, String returnType, String signature, String statusClass) {
		this.access = access;
		this.returnType = shorten(returnType);
		this.signature = signature;
		this.statusClass = statusClass;
	}

	/**
	 * Drops the {@code java.lang.} qualifier, so a return type reads {@code String} rather than
	 * {@code java.lang.String}.
	 *
	 * <p>Applied to every section. The added section used to do this and the modified and
	 * deprecated sections did not, so the same type printed two different ways depending on which
	 * heading it happened to fall under.
	 *
	 * <p>Only a direct member of {@code java.lang} is shortened. A nested package keeps its
	 * qualifier, because {@code reflect.Method} names nothing - no such package exists at the top
	 * level. Nothing in the report currently returns such a type, so this costs nothing today and
	 * is simply the rule the shorter version of this check got wrong.
	 */
	private static String shorten(String type) {
		String prefix = "java.lang.";
		if (type == null || !type.startsWith(prefix)) {
			return type;
		}
		String withoutPackage = type.substring(prefix.length());
		return withoutPackage.indexOf('.') < 0 ? withoutPackage : type;
	}

	/** Access modifier, lowercased: {@code public}, {@code protected}. */
	public String getAccess() {
		return access;
	}

	/** Return type, with the java.lang qualifier dropped where it adds nothing. */
	public String getReturnType() {
		return returnType;
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
