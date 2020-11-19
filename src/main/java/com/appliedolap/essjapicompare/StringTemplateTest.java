package com.appliedolap.essjapicompare;

import org.stringtemplate.v4.ST;

public class StringTemplateTest {

	public static void main(String[] args) {
		int[] num = new int[] { 3, 9, 20, 2, 1, 4, 6, 32, 5, 6, 77, 888, 2, 1, 6, 32, 5, 6, 77,
				4, 9, 20, 2, 1, 4, 63, 9, 20, 2, 1, 4, 6, 32, 5, 6, 77, 6, 32, 5, 6, 77,
				3, 9, 20, 2, 1, 4, 6, 32, 5, 6, 77, 888, 1, 6, 32, 5 };
		String t = ST.format(30, "\\<del>int <%1>[] = { <%2; wrap, anchor, separator=\", \"> };", "a", num);
		System.out.println(t);

	}

}
