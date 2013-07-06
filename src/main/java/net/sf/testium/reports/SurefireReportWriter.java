package net.sf.testium.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import net.sf.testium.configuration.SurefirePluginConfiguration;

import org.testtoolinterfaces.testresult.ResultSummary;
import org.testtoolinterfaces.testresult.TestCaseResult;
import org.testtoolinterfaces.testresult.TestCaseResultLink;
import org.testtoolinterfaces.testresult.TestGroupEntryResult;
import org.testtoolinterfaces.testresult.TestGroupResult;
import org.testtoolinterfaces.testresult.TestResult.VERDICT;
import org.testtoolinterfaces.testresult.TestStepResultBase;
import org.testtoolinterfaces.testresultinterface.TestCaseResultReader;
import org.testtoolinterfaces.testresultinterface.TestGroupResultWriter;
import org.testtoolinterfaces.testsuite.LooseTestInterfaceList;
import org.testtoolinterfaces.utils.Trace;
import org.testtoolinterfaces.utils.Warning;

public class SurefireReportWriter implements TestGroupResultWriter
{
	private final File myReportsDir;
	
	public SurefireReportWriter(SurefirePluginConfiguration config)
	{
		myReportsDir = config.getReportsDir();
        if (!myReportsDir.exists())
        {
        	myReportsDir.mkdir();
        }
	}

	public void notify(TestGroupResult aTgResult)
	{
		write(aTgResult);
	}

	public void write(TestGroupResult aTgResult, File aResultFile)
	{
		write(aTgResult);

		aTgResult.register(this);
	}

	/**
	 * @param aTgResult
	 */
	private void write(TestGroupResult aTgResult)
	{
		File txtFile = new File( myReportsDir, getClassName(aTgResult) + ".txt" );
		this.writeToTxtFile(aTgResult, txtFile);
		
		File xmlFile = new File( myReportsDir, "TEST-" + getClassName(aTgResult) + ".xml" );
		this.writeToXmlFile(aTgResult, xmlFile);
	}

	/**
	 * @param aTgResult
	 * @return some sort of class name that will be used in the surefire-report
	 */
	private String getClassName(TestGroupResult aTgResult)
	{
		return aTgResult.getExecutionPath() + "." + aTgResult.getId();
	}

	/**
	 * @param aRunResult
	 * @param aFile
	 */
	private void writeToTxtFile(TestGroupResult aTgResult, File aFile)
	{
		FileWriter txtFile;
		try
		{
			txtFile = new FileWriter( aFile );
			ResultSummary summary = getSummary( aTgResult );

			txtFile.write("-------------------------------------------------------------------------------\n");
			txtFile.write("Test set: " + getClassName(aTgResult) + "\n");
			txtFile.write("-------------------------------------------------------------------------------\n");
			txtFile.write("Tests run: " + summary.getNrOfTCs() 
					+ ", Failures: " + summary.getNrOfTCsFailed() 
					+ ", Errors: " + summary.getNrOfTCsError()
					+ ", Skipped: 0, Time elapsed: 0 sec ");

//			Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.04 sec <<< FAILURE!
//			initializationError(nl.sdu.erecruiter.integration.inbound.JmsReceiverExceptionTest)  Time elapsed: 0.026 sec  <<< ERROR!
//			java.lang.Exception: No runnable methods
//				at org.junit.runners.BlockJUnit4ClassRunner.validateInstanceMethods(BlockJUnit4ClassRunner.java:166)

			if ( summary.getNrOfTCsFailed() > 0 || summary.getNrOfTCsError() > 0 )
			{
				txtFile.write(" <<< FAILURE!\n");
			}
			
		    Hashtable<Integer,TestGroupEntryResult> teResults = aTgResult.getTestGroupEntryResultsTable();
	    	for (int key = 0; key < teResults.size(); key++)
	    	{
	    		TestGroupEntryResult teResult = teResults.get(key);
	    		if ( teResult instanceof TestCaseResultLink ) {
	    			
		    		TestCaseResultLink tcResult = (TestCaseResultLink) teResult;
					if ( tcResult.getResult().equals( VERDICT.ERROR ) )
					{
						txtFile.write("structuralError(" + getClassName(aTgResult) + ") Time elapsed: 0 sec <<< ERROR!\n");
						txtFile.write("java.lang.exception: Structural error\n");
						txtFile.write( this.getStepTrace(tcResult) );
					}
					else if ( tcResult.getResult().equals( VERDICT.FAILED ) )
					{
						txtFile.write( this.getStepTrace(tcResult) );
					}
					txtFile.write("\n");
	    		}
	    	}

	    	
			txtFile.flush();
		}
		catch (IOException e)
		{
			Trace.print(Trace.SUITE, e);
			Warning.println("Warning writing TXT output failed: " + e.getMessage());
		}
	}

	/**
	 * @param aRunResult
	 * @param aFile
	 */
	private void writeToXmlFile(TestGroupResult aTgResult, File aFile)
	{
		FileWriter xmlFile;
		try
		{
			xmlFile = new FileWriter( aFile );
			ResultSummary summary = getSummary( aTgResult );

			xmlFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
			xmlFile.write("<testsuite failures=\"" + summary.getNrOfTCsFailed()
					+ "\" time=\"0\" errors=\"" + summary.getNrOfTCsError()
					+ "\" skipped=\"0\" tests=\"" + summary.getNrOfTCs()
					+ "\" name=\"" + getClassName(aTgResult) + "\">\n"); // To be added: timestamp, hostname
			xmlFile.write("  <properties/>\n");

		    Hashtable<Integer,TestGroupEntryResult> teResults = aTgResult.getTestGroupEntryResultsTable();
	    	for (int key = 0; key < teResults.size(); key++)
	    	{
	    		TestGroupEntryResult teResult = teResults.get(key);
	    		if ( teResult instanceof TestCaseResultLink ) {	
		    		TestCaseResultLink tcResult = (TestCaseResultLink) teResult;

					xmlFile.write("  <testcase time=\"0\" classname=\"" + getClassName(aTgResult) + "\" name=\"" + tcResult.getId() + "\"");
					if ( tcResult.getResult().equals( VERDICT.ERROR ) )
					{
						xmlFile.write(">\n");
						xmlFile.write("    <error message=\"Structural error\" type=\"error\">");
						xmlFile.write( this.getStepTrace(tcResult) );
						xmlFile.write("    </error>\n");
						xmlFile.write("  </testcase>\n");
					}
					else if ( tcResult.getResult().equals( VERDICT.FAILED ) )
					{
						xmlFile.write(">\n");
						xmlFile.write("    <failure message=\"Test Case failed\" type=\"failed\">");
						xmlFile.write( this.getStepTrace(tcResult) );
						xmlFile.write("    </failure>\n");
						xmlFile.write("  </testcase>\n");
					}
					else
					{
						xmlFile.write("/>\n");
					}
	    		}
	    	}
	    	
			xmlFile.write("</testsuite>\n");

			xmlFile.flush();
		}
		catch (IOException e)
		{
			Trace.print(Trace.SUITE, e);
			Warning.println("Warning writing TXT output failed: " + e.getMessage());
		}
	}

	private ResultSummary getSummary( TestGroupResult aTgResult )
	{
		int totalPassed = 0;
		int totalFailed = 0;
		int totalError = 0;
		int totalUnknown = 0;
		
	    Hashtable<Integer,TestGroupEntryResult> teResults = aTgResult.getTestGroupEntryResultsTable();
    	for (int key = 0; key < teResults.size(); key++)
    	{
    		TestGroupEntryResult teResult = teResults.get(key);
    		if ( teResult instanceof TestCaseResultLink ) {	
	    		TestCaseResultLink tcResult = (TestCaseResultLink) teResult;
	    		if ( tcResult.getResult().equals(VERDICT.PASSED)  ) { totalPassed++;	}
	    		if ( tcResult.getResult().equals(VERDICT.FAILED)  ) { totalFailed++;	}
	    		if ( tcResult.getResult().equals(VERDICT.ERROR)   ) { totalError++;		}
	    		if ( tcResult.getResult().equals(VERDICT.UNKNOWN) ) { totalUnknown++;	}
    		}
    	}

		return new ResultSummary(totalPassed, totalFailed, totalUnknown, totalError);
	}
	
	private String getStepTrace( TestCaseResultLink tcResultLink )
	{
		try 
		{
			TestCaseResult tcResult = getTestCaseResult(tcResultLink);
			String stepTrace = tcResult.getComment() + "\n";
			Hashtable<Integer, TestStepResultBase> tsResults = tcResult.getExecutionResults();
			Iterator<TestStepResultBase> tsResultsItr = tsResults.values().iterator();
			while ( tsResultsItr.hasNext() ) {
				TestStepResultBase tsResult = tsResultsItr.next();
				stepTrace += tsResult.getDisplayName() + ": " + tsResult.getResult() + "\n";
			}

			return stepTrace;
		}
		catch ( IOError err )
		{
			return tcResultLink.getComment();
		}
	}

	/**
	 * @param tcResultLink
	 * @return
	 * 
	 * @throws IOError if reading fails
	 */
	private TestCaseResult getTestCaseResult(TestCaseResultLink tcResultLink)
	{
		TestCaseResultReader tcResultReader = new TestCaseResultReader( new LooseTestInterfaceList() );
		TestCaseResult tcResult = tcResultLink.getTcResult();
		if ( tcResult == null )
		{
			tcResult = tcResultReader.readTcResultFile(tcResultLink);
			tcResultLink.setTcResult(tcResult);
		}
		return tcResult;
	}
}
