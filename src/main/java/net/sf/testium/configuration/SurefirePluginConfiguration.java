package net.sf.testium.configuration;

import java.io.File;

public class SurefirePluginConfiguration
{
	private final File myReportsDir;
	
	public SurefirePluginConfiguration(File aReportsDir)
	{
		myReportsDir = aReportsDir;
	}

	public File getReportsDir()
	{
		return myReportsDir;
	}
}
