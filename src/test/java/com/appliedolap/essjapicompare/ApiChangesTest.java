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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiClassType;
import japicmp.model.JApiMethod;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.bytecode.DeprecatedAttribute;
import javassist.bytecode.MethodInfo;

/**
 * Tests for {@link ApiChanges}, built on japicmp model objects assembled by hand rather than by
 * comparing real jars.
 *
 * <p>Deprecation detection in particular is worth testing directly. japicmp reports an annotation
 * as a bytecode attribute rather than as a change status of its own, so the rule reads the
 * attribute on both sides of the comparison, and the cases that matter - a method that gained the
 * annotation, one that always had it, one that was removed - are awkward to arrange from real
 * jars and trivial to arrange here.
 */
class ApiChangesTest {

	private static final JarArchiveComparator COMPARATOR =
			new JarArchiveComparator(new JarArchiveComparatorOptions());

	private static int uniqueSuffix;

	private static JApiClass classWithStatus(JApiChangeStatus status) {
		CtClass ctClass = ClassPool.getDefault().makeClass("com.example.Generated" + uniqueSuffix++);
		return new JApiClass(COMPARATOR, ctClass.getName(), Optional.of(ctClass), Optional.of(ctClass),
				status, new JApiClassType(Optional.of(JApiClassType.ClassType.CLASS),
						Optional.of(JApiClassType.ClassType.CLASS), status));
	}

	/** A method on a throwaway class, optionally carrying the bytecode Deprecated attribute. */
	private static CtMethod method(String signature, boolean deprecated) throws Exception {
		CtClass owner = ClassPool.getDefault().makeClass("com.example.Owner" + uniqueSuffix++);
		CtMethod method = CtNewMethod.make(signature, owner);
		owner.addMethod(method);
		if (deprecated) {
			MethodInfo info = method.getMethodInfo();
			info.addAttribute(new DeprecatedAttribute(info.getConstPool()));
		}
		return method;
	}

	private static JApiMethod comparedMethod(CtMethod oldSide, CtMethod newSide) {
		return new JApiMethod(classWithStatus(JApiChangeStatus.MODIFIED), "doIt",
				JApiChangeStatus.MODIFIED, Optional.ofNullable(oldSide), Optional.ofNullable(newSide),
				COMPARATOR);
	}

	@Test
	void selectsClassesByChangeStatus() {
		List<JApiClass> all = List.of(
				classWithStatus(JApiChangeStatus.NEW),
				classWithStatus(JApiChangeStatus.MODIFIED),
				classWithStatus(JApiChangeStatus.NEW),
				classWithStatus(JApiChangeStatus.REMOVED),
				classWithStatus(JApiChangeStatus.UNCHANGED));

		assertEquals(2, ApiChanges.withStatus(all, JApiChangeStatus.NEW).size());
		assertEquals(1, ApiChanges.withStatus(all, JApiChangeStatus.MODIFIED).size());
		assertEquals(1, ApiChanges.withStatus(all, JApiChangeStatus.REMOVED).size());
		assertEquals(1, ApiChanges.withStatus(all, JApiChangeStatus.UNCHANGED).size());
	}

	@Test
	void selectsNothingFromAnEmptyList() {
		assertTrue(ApiChanges.withStatus(List.of(), JApiChangeStatus.NEW).isEmpty());
		assertTrue(ApiChanges.withNewDeprecations(List.of()).isEmpty());
	}

	@Test
	void reportsAMethodThatGainedTheAnnotation() throws Exception {
		JApiMethod method = comparedMethod(method("public void doIt() {}", false),
				method("public void doIt() {}", true));

		assertTrue(ApiChanges.isMethodNowDeprecated(method));
	}

	@Test
	void ignoresAMethodThatWasAlreadyDeprecated() throws Exception {
		JApiMethod method = comparedMethod(method("public void doIt() {}", true),
				method("public void doIt() {}", true));

		assertFalse(ApiChanges.isMethodNowDeprecated(method));
	}

	@Test
	void ignoresAMethodThatIsNotDeprecatedAtAll() throws Exception {
		JApiMethod method = comparedMethod(method("public void doIt() {}", false),
				method("public void doIt() {}", false));

		assertFalse(ApiChanges.isMethodNowDeprecated(method));
	}

	/** A removed method is absent from the new jar, so it cannot have become deprecated in it. */
	@Test
	void ignoresARemovedMethod() throws Exception {
		JApiMethod method = comparedMethod(method("public void doIt() {}", true), null);

		assertFalse(ApiChanges.isMethodNowDeprecated(method));
	}

	/**
	 * Introduced and deprecated in the same release should not happen, but it does when the
	 * comparison skips the version that introduced the method undeprecated - which is common
	 * here, since the archive has gaps.
	 */
	@Test
	void reportsAMethodThatAppearsAlreadyDeprecated() throws Exception {
		JApiMethod method = comparedMethod(null, method("public void doIt() {}", true));

		assertTrue(ApiChanges.isMethodNowDeprecated(method));
	}

}
