package com.appliedolap.essjapicompare;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class VersionTest {

	private Version textVersion;
	
	private Version intVersion;
	
	private String javaVersion = "1.8.0_25";
	
	private Version v11124 = Version.of("11.1.2.4");
	
	private Version v1112  = Version.of("11.1.2");
	
	@Before
	public void setUp() throws Exception {
		this.textVersion = new Version("11.1.2.3");
		this.intVersion = new Version(9, 3, 1);
	}

	@Test
	public void testVersion() {
		assertEquals(11, textVersion.getMajor());
	}
	
	@Test
	public void testJavaVersionParse() {
		Version java = Version.of(javaVersion);
		assertEquals(java.getMajor(), 1);
		assertEquals(java.getMinor(), 8);
		assertEquals(java.getRevision(), 0);
		assertEquals(java.getBuild(), 25);
	}
	
	@Test
	public void testIntVersion() {
		assertEquals(3, intVersion.getMinor());
	}

	@Test
	public void testVersionCompare() {
		assertTrue(textVersion.isGreaterOrEqual(11, 1, 2, 2, 0, 0, 2));
	}
	
	@Test
	public void testOtherVersionCompare() {
		Version otherVersion = Version.of("11.1.2.4");
		assertTrue(otherVersion.isGreaterOrEqual(this.textVersion));
		assertTrue(otherVersion.isGreaterOrEqual(otherVersion));
		assertFalse(this.textVersion.isGreaterOrEqual(otherVersion));
	}
	
	@Test
	public void testCompareDifferingLengths() {
		assertEquals(1, v11124.compareTo(v1112));
		assertEquals(-1, v1112.compareTo(v11124));
	}
	
	@Test
	public void testToString() {
		assertEquals("9.3.1", intVersion.toString());
	}
	
	@Test
	public void testEquals() {
		Version otherVersionHigher = Version.of("11.1.2.4");
		Version otherVersionSame = Version.of("11.1.2.3");
		Version otherVersionLower = Version.of("11.1.2.2");
		
		assertEquals(otherVersionSame, textVersion);
		assertTrue(textVersion.compareTo(otherVersionLower) > 0);
		assertTrue(textVersion.compareTo(otherVersionHigher) < 0);
		assertTrue(textVersion.compareTo(otherVersionSame) == 0);
	}
	
}
