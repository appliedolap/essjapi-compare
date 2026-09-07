package com.appliedolap.essjapicompare;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;

import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiMethod;
import javassist.CtMethod;
import javassist.bytecode.AttributeInfo;

/**
 * The questions the report asks about a comparison, answered in Java rather than in the template.
 *
 * <p>These used to be asked as {@code th:if} conditions on the template's loops, which meant
 * iterating everything and discarding most of it. Thymeleaf removes the element a false
 * {@code th:if} was on, but leaves behind the indentation and newline that surrounded it, and this
 * report discards far more than it keeps - most methods of a modified class are unchanged. The
 * result was an output file that was 86% blank lines. Handing the template a list that already
 * contains only what it should render avoids generating that negative space at all, and reads
 * better besides.
 */
public final class ApiChanges {

	private ApiChanges() {
	}

	/** Classes whose own status matches, for the report's added/modified/removed sections. */
	public static List<JApiClass> withStatus(List<JApiClass> classes, JApiChangeStatus status) {
		List<JApiClass> matching = new ArrayList<JApiClass>();
		for (JApiClass clazz : classes) {
			if (status.equals(clazz.getChangeStatus())) {
				matching.add(clazz);
			}
		}
		return matching;
	}

	/** Classes that gained at least one deprecation in this comparison. */
	public static List<JApiClass> withNewDeprecations(List<JApiClass> classes) {
		List<JApiClass> matching = new ArrayList<JApiClass>();
		for (JApiClass clazz : classes) {
			if (!newlyDeprecatedMethods(clazz).isEmpty()) {
				matching.add(clazz);
			}
		}
		return matching;
	}

	/** Whether a class gained any deprecation in this comparison. */
	public static boolean classGainedDeprecatedMethods(JApiClass clazz) {
		return !newlyDeprecatedMethods(clazz).isEmpty();
	}

	/** Methods of a class that changed in some way; the unchanged majority is not reported. */
	public static List<JApiMethod> changedMethods(JApiClass clazz) {
		List<JApiMethod> changed = new ArrayList<JApiMethod>();
		for (JApiMethod method : clazz.getMethods()) {
			if (!JApiChangeStatus.UNCHANGED.equals(method.getChangeStatus())) {
				changed.add(method);
			}
		}
		return changed;
	}

	/** Methods of a class that became deprecated in this comparison. */
	public static List<JApiMethod> newlyDeprecatedMethods(JApiClass clazz) {
		List<JApiMethod> deprecated = new ArrayList<JApiMethod>();
		for (JApiMethod method : clazz.getMethods()) {
			if (isMethodNowDeprecated(method)) {
				deprecated.add(method);
			}
		}
		return deprecated;
	}

	/**
	 * Whether a method carries the Deprecated attribute now and did not before.
	 *
	 * <p>japicmp reports annotations as attributes rather than as a change status of their own, so
	 * this reads the bytecode attribute directly on both sides of the comparison.
	 */
	public static boolean isMethodNowDeprecated(JApiMethod method) {
		String attribute = "Deprecated";
		Optional<CtMethod> newMethod = method.getNewMethod();
		if (!newMethod.isPresent()) {
			// The method was removed, so it cannot have been deprecated in this version.
			return false;
		}

		AttributeInfo newMethodAttribute = newMethod.get().getMethodInfo().getAttribute(attribute);
		if (newMethodAttribute == null) {
			return false;
		}

		Optional<CtMethod> oldMethod = method.getOldMethod();
		if (!oldMethod.isPresent()) {
			// Introduced and deprecated in the same release, which should not happen - or, more
			// likely, an intermediate version is missing from the comparison, so the release that
			// introduced the method undeprecated was never seen.
			return true;
		}
		return oldMethod.get().getMethodInfo().getAttribute(attribute) == null;
	}

}
