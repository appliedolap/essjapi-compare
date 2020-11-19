package com.appliedolap.essjapicompare;

import java.io.File;
import java.util.List;

import com.google.common.base.Optional;

import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiClass;
import japicmp.model.JApiMethod;
import japicmp.output.stdout.StdoutOutputGenerator;
import javassist.CtMethod;
import javassist.bytecode.AttributeInfo;

public class TempCompare {

	public static void main(String[] args) {

		JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
		// comparatorOptions.setIncludeSynthetic(true);
		// comparatorOptions.setNoAnnotations(true);
		// comparatorOptions.setIgnoreMissingClasses(true);
		JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

		List<JApiClass> jApiClasses = jarArchiveComparator.compare(new File("/Users/jasonwjones/Desktop/temp-essjapi-versions/ess_japi-7.1.3.jar"),
				new File("/Users/jasonwjones/Desktop/temp-essjapi-versions/ess_japi-11.1.2.4.017.jar"));

		for (JApiClass clazz : jApiClasses) {
			for (JApiMethod method : clazz.getMethods()) {
				if (isMethodNowDeprecated(method)) {
					System.out.println(clazz.getFullyQualifiedName() + "." + method.getName() + " is now deprecated");
				}
			}
		}
	}
	
	public static boolean classGainedDeprecatedMethods(JApiClass clazz) {
		for (JApiMethod method : clazz.getMethods()) {
			if (isMethodNowDeprecated(method)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isMethodNowDeprecated(JApiMethod method) {
		String attribute = "Deprecated";
		Optional<CtMethod> newMethod = method.getNewMethod();

		if (newMethod.isPresent()) {
			///method.getAttributes()
			AttributeInfo newMethodAttribute = newMethod.get().getMethodInfo().getAttribute(attribute);
			Optional<CtMethod> oldMethod = method.getOldMethod();
			if (newMethodAttribute != null) {
				if (oldMethod.isPresent()) {
					// there's a new method, an old/existing method, and the new
					// method contains the Deprecated attribute but the old one
					// doesn't
					AttributeInfo oldMethodAttribute = oldMethod.get().getMethodInfo().getAttribute(attribute);
					return oldMethodAttribute == null;
				} else {
					// this can potentially happen if, for some stupid reason,
					// the codebase adds a new method and deprecates it all in
					// the same release. It shouldn't ever happen. But it could.
					// Because stupidity.
					//
					// Or I suppose this could happen when you are comparing and
					// old JAR to a much newer one, where you are missing some
					// of the inbetween JARs, which had originally introduced
					// the new, non-deprecated method, and the newer JAR you are
					// looking at has already deprecated it
					return true;
				}
			}
		}
		// a method was removed, therefore it can't have been deprecated. Also
		// catches fall-through from above where the new method isn't deprecated
		// at all
		return false;
	}

}
