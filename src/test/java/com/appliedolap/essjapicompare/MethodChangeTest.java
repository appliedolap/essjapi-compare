package com.appliedolap.essjapicompare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MethodChange}, and in particular for how much of a return type's package it
 * shows.
 */
class MethodChangeTest {

	private static MethodChange returning(String returnType) {
		return new MethodChange("public", returnType, "com.example.Thing.doIt()", "method-new");
	}

	@Test
	void dropsTheJavaLangQualifier() {
		assertEquals("String", returning("java.lang.String").getReturnType());
		assertEquals("Object", returning("java.lang.Object").getReturnType());
	}

	@Test
	void dropsItFromAnArrayTypeToo() {
		assertEquals("String[]", returning("java.lang.String[]").getReturnType());
	}

	/**
	 * The earlier version of this rule cut everything after "java.lang", which would have turned
	 * a nested package into a name that means nothing. No Essbase method returns one of these, so
	 * this guards a rule rather than a reported bug.
	 */
	@Test
	void keepsTheQualifierForNestedJavaLangPackages() {
		assertEquals("java.lang.reflect.Method", returning("java.lang.reflect.Method").getReturnType());
		assertEquals("java.lang.annotation.Annotation",
				returning("java.lang.annotation.Annotation").getReturnType());
	}

	@Test
	void leavesEveryOtherPackageAlone() {
		assertEquals("com.essbase.api.datasource.IEssCube",
				returning("com.essbase.api.datasource.IEssCube").getReturnType());
		assertEquals("java.util.ResourceBundle", returning("java.util.ResourceBundle").getReturnType());
	}

	@Test
	void leavesPrimitivesAndVoidAlone() {
		assertEquals("void", returning("void").getReturnType());
		assertEquals("int", returning("int").getReturnType());
		assertEquals("short", returning("short").getReturnType());
	}

	@Test
	void toleratesAnAbsentReturnType() {
		assertNull(returning(null).getReturnType());
	}

	@Test
	void passesTheOtherValuesThroughUntouched() {
		MethodChange change = new MethodChange("protected", "int", "com.example.Thing.count()",
				"method-removed");

		assertEquals("protected", change.getAccess());
		assertEquals("com.example.Thing.count()", change.getSignature());
		assertEquals("method-removed", change.getStatusClass());
	}

}
