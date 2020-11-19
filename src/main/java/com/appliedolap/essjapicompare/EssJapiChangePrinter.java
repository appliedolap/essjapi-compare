package com.appliedolap.essjapicompare;

import org.stringtemplate.v4.ST;

import com.google.common.collect.Multimap;

import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiMethod;
import japicmp.model.JApiParameter;

public class EssJapiChangePrinter {

	public void print(Multimap<JApiClass, JApiMethod> changes) {
		System.out.println("<ul>");
		for (JApiClass clazz : changes.keySet()) {
			System.out.printf("\t<li>%s</li>%n", clazz.getFullyQualifiedName());
			System.out.println("\t<ul>");
			for (JApiMethod method : changes.get(clazz)) {
				Object[] paramTypes = new String[method.getParameters().size()];
				int index = 0;
				for (JApiParameter param : method.getParameters()) {
					paramTypes[index++] = param.getType();
				}
				String paramText = paramTypes.length > 0 ? ST.format("(<%1; separator=\", \">)", paramTypes) : "()";
				String changeStatus = "";
				if (method.getChangeStatus() != JApiChangeStatus.NEW) {
					changeStatus = " - " + method.getChangeStatus().toString();
				}
				System.out.printf("\t\t<li>%s%s%s</li>%n", method.getName(), paramText, changeStatus);
			}
			System.out.println("\t</ul>");
		}
		System.out.println("</ul>");
	}
	
}
