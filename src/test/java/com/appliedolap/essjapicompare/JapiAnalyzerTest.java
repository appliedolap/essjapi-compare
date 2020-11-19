package com.appliedolap.essjapicompare;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class JapiAnalyzerTest {

	private JapiAnalyzer analyzer;
	
	@Before
	public void setUp() throws Exception {
		analyzer = new JapiAnalyzer();
	}

	@Test
	public void testRun() throws IOException {
		JapiAnalyzerConfiguration configuration = new JapiAnalyzerConfiguration();
		configuration.setMaxVersionsToCheck(0);
		//Users/jasonwjones/Development/Applied OLAP/Outline Extractor NG/essbase.versions/lib
		configuration.setBaseFolder(Paths.get("/Users/jasonwjones/Development/Applied OLAP/Outline Extractor NG/essbase.versions/lib"));
		//configuration.setBaseFolder(Paths.get("/Users/jasonwjones/Development/Applied OLAP/Dodeca/EssbaseServer/lib"));
		configuration.setPrefix("ess_japi.jar.");
		
		analyzer.run(configuration);
	}

}
