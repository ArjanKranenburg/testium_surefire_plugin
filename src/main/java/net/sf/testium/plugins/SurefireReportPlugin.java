package net.sf.testium.plugins;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.testium.Testium;
import net.sf.testium.configuration.ConfigurationException;
import net.sf.testium.configuration.SurefirePluginConfiguration;
import net.sf.testium.configuration.SurefireReportConfigurationXmlHandler;
import net.sf.testium.executor.TestrunEnvironment;
import net.sf.testium.plugin.Plugin;
import net.sf.testium.reports.SurefireReportWriter;

import org.testtoolinterfaces.data.RuntimeData;
import org.testtoolinterfaces.utils.Trace;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


/**
 * @author Arjan Kranenburg
 *
 */
public final class SurefireReportPlugin implements Plugin
{
	public SurefireReportPlugin()
	{
		super();
		Trace.println(Trace.CONSTRUCTOR);
	}

	public void loadPlugIn(TestrunEnvironment aPluginCollection,
			RuntimeData anRtData) throws ConfigurationException
	{
		Trace.println(Trace.UTIL, "loadPlugIn( " + aPluginCollection + " )", true );

		SurefirePluginConfiguration config = readConfigFiles( anRtData );
		
		SurefireReportWriter surefireReportWriter = new SurefireReportWriter( config );
		aPluginCollection.addTestGroupResultWriter(surefireReportWriter);
	}

	public SurefirePluginConfiguration readConfigFiles( RuntimeData anRtData ) throws ConfigurationException
	{
		Trace.println(Trace.UTIL);

		File configDir = (File) anRtData.getValue(Testium.CONFIGDIR);
		File configFile = new File( configDir, "surefireReport.xml" );
		SurefirePluginConfiguration globalConfig = readConfigFile( configFile, anRtData );
		
		return globalConfig;
	}

	public SurefirePluginConfiguration readConfigFile( File aConfigFile,
			RuntimeData aRtData ) throws ConfigurationException
	{
		Trace.println(Trace.UTIL, "readConfigFile( " + aConfigFile.getName() + " )", true );
        // create a parser
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(false);
        SAXParser saxParser;
        SurefireReportConfigurationXmlHandler handler = null;
		try
		{
			saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();

	        // create a handler
			handler = new SurefireReportConfigurationXmlHandler(xmlReader, aRtData);

	        // assign the handler to the parser
	        xmlReader.setContentHandler(handler);

	        // parse the document
	        xmlReader.parse( aConfigFile.getAbsolutePath() );
		}
		catch (ParserConfigurationException e)
		{
			Trace.print(Trace.UTIL, e);
			throw new ConfigurationException( e );
		}
		catch (SAXException e)
		{
			Trace.print(Trace.UTIL, e);
			throw new ConfigurationException( e );
		}
		catch (IOException e)
		{
			Trace.print(Trace.UTIL, e);
			throw new ConfigurationException( e );
		}
		
		SurefirePluginConfiguration configuration = handler.getConfiguration();
		
		return configuration;
	}
}
