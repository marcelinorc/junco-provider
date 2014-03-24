package fr.inria.juncoprovider;


import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;

import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.report.*;
import org.apache.maven.surefire.shade.org.apache.maven.shared.utils.io.SelectorUtils;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;
import org.apache.maven.surefire.util.internal.StringUtils;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecFileLoader;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.report.xml.XMLFormatter;

import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

import static java.lang.String.format;

/**
 * Created by marcel on 16/03/14.
 */
public class Junco4Provider
        extends AbstractProvider {

    private static final String CLASSES_DIR = "classes:dir";
    private static final String AGENT_ADDRESS = "jacoco:address";
    private static final String AGENT_PORT = "jacoco:port";
    private static final String REPORT_DIR = "report:dir";
    private static final String TRANSPLANT_FILE = "transplant:file";
    private static final String SOURCES_DIR = "sources:dir";
    private static final String HTML_REPORT = "html:report";

    private final ClassLoader testClassLoader;

    private final List<org.junit.runner.notification.RunListener> customRunListeners;

    private final JUnit4TestChecker jUnit4TestChecker;

    private final String requestedTestMethod;

    private TestsToRun testsToRun;

    private final ProviderParameters providerParameters;

    private RunOrderCalculator runOrderCalculator;

    private final ScanResult scanResult;

    private final String agentAddress;

    private final int agentPort;

    private final ConsoleLogger logger;

    public Junco4Provider( ProviderParameters booterParameters )
    {
        this.logger = booterParameters.getConsoleLogger();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.scanResult = booterParameters.getScanResult();
        this.providerParameters = booterParameters;
        Properties pp = providerParameters.getProviderProperties();
        
        String reportDir = pp.getProperty(REPORT_DIR, "target\\site\\junco\\");
        String classesDir = pp.getProperty(CLASSES_DIR, "target\\classes");
        String transplantFile = pp.getProperty(TRANSPLANT_FILE, "");

        try {
            this.runOrderCalculator = new CoverageRunOrderCalculator(
                    classesDir, reportDir, transplantFile);
        } catch (CoverageRunOrderException e ) {
            this.runOrderCalculator = booterParameters.getRunOrderCalculator();
            logger.info("Not coverage information found when trying to calculate run order or coverage info was corrupt. Default run order assumed. \n");
            logger.info(e.getMessage());
        }

        customRunListeners = JUnit4RunListenerFactory.
                createCustomListeners(booterParameters.getProviderProperties().getProperty("listener"));
        jUnit4TestChecker = new JUnit4TestChecker( testClassLoader );
        requestedTestMethod = booterParameters.getTestRequest().getRequestedTestMethod();

        //Obtain address and port of the coverage agent
        agentAddress = pp.getProperty(AGENT_ADDRESS);
        agentPort = Integer.valueOf(pp.getProperty(AGENT_PORT, "6300"));
    }

    public RunResult invoke( Object forkTestSet )
            throws TestSetFailedException, ReporterException
    {
        if ( testsToRun == null )
        {
            if ( forkTestSet instanceof TestsToRun )
            {
                testsToRun = (TestsToRun) forkTestSet;
            }
            else if ( forkTestSet instanceof Class )
            {
                testsToRun = TestsToRun.fromClass( (Class) forkTestSet );
            }
            else
            {
                testsToRun = scanClassPath();
            }
        }

        upgradeCheck();

        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        final RunListener reporter = reporterFactory.createReporter();

        ConsoleOutputCapture.startCapture((ConsoleOutputReceiver) reporter);

        JUnit4RunListener jUnit4TestSetReporter = new JUnit4RunListener( reporter );

        Result result = new Result();
        RunNotifier runNotifer = getRunNotifier(jUnit4TestSetReporter, result, customRunListeners);

        runNotifer.fireTestRunStarted( null );

        for ( Class aTestsToRun : testsToRun )
        {
            try {
                //Execute the test case
                executeTestSet(aTestsToRun, reporter, runNotifer);
                //Dumps and reset the coverage information
                dumpAndResetCoverageInformation(aTestsToRun.getCanonicalName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        runNotifer.fireTestRunFinished( result );

        JUnit4RunListener.rethrowAnyTestMechanismFailures( result );

        closeRunNotifer( jUnit4TestSetReporter, customRunListeners );

        return reporterFactory.close();
    }

    /*
     * Write the coverage reports to file
     */
    private void writeReports(String testCaseName, ExecFileLoader loader) throws IOException {
        final Properties pp = providerParameters.getProviderProperties();
        final File classesDirectory = new File(pp.getProperty(CLASSES_DIR, "target\\classes"));
        final File sourceDirectory = new File(pp.getProperty(SOURCES_DIR, "src\\main"));
        final File reportDirectory = new File(pp.getProperty(REPORT_DIR, "target\\site\\junco\\"));
        reportDirectory.mkdirs();

        final boolean useHtmlReport = Boolean.valueOf(pp.getProperty(HTML_REPORT));


        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        IBundleCoverage bundle = coverageBuilder.getBundle(testCaseName);

        // Create a concrete html report
        if ( useHtmlReport ) {
            File f = new File(reportDirectory + "/" + testCaseName);
            f.mkdirs();
            final HTMLFormatter htmlFormatter = new HTMLFormatter();
            final IReportVisitor htmlVisitor = htmlFormatter.createVisitor(
                    new FileMultiReportOutput(f));
            htmlVisitor.visitInfo(loader.getSessionInfoStore().getInfos(),
                    loader.getExecutionDataStore().getContents());
            htmlVisitor.visitBundle(bundle, new DirectorySourceFileLocator(
                    sourceDirectory, "utf-8", 4));
            htmlVisitor.visitEnd();
        }

        //Create a xml report
        final XMLFormatter xmlFormatter = new XMLFormatter();
        xmlFormatter.setOutputEncoding("UTF-8");
        File htmlFile = new File(reportDirectory.getAbsolutePath() + "/" + testCaseName + ".xml");
        final IReportVisitor xmlVisitor = xmlFormatter.createVisitor(new FileOutputStream(htmlFile));
        xmlVisitor.visitInfo(loader.getSessionInfoStore().getInfos(),
                loader.getExecutionDataStore().getContents());
        xmlVisitor.visitBundle(bundle, new DirectorySourceFileLocator(
                sourceDirectory, "utf-8", 4));
        xmlVisitor.visitEnd();

        //Write the exec to file
        FileOutputStream fo = new FileOutputStream(reportDirectory.getAbsolutePath() +
                "/" + testCaseName + ".exec");
        final ExecutionDataWriter dataWriter = new ExecutionDataWriter(fo);
        loader.getSessionInfoStore().accept(dataWriter);
        loader.getExecutionDataStore().accept(dataWriter);
    }

    /*
    *   Dumps and resets the coverage information for the test case
    */
    private void dumpAndResetCoverageInformation(String testCaseName) {

        final ExecDumpClient client = new ExecDumpClient() {
            @Override
            protected void onConnecting(final InetAddress address,
                                        final int port) {
                logger.info(format("[INFO] Connecting to %s:%s \n", address,
                                Integer.valueOf(port)));
            }

            @Override
            protected void onConnectionFailure(final IOException exception) {
                logger.info(exception.getMessage() + "\n");
            }
        };
        client.setDump(true);
        client.setReset(true);
        client.setRetryCount(10);

        try {
            final ExecFileLoader loader = client.dump("localhost", agentPort);
            writeReports(testCaseName, loader);
        } catch (final IOException e) {
            logger.info("Unable to dump coverage data. Check that jacoco is properly set and in tcpserver output mode");
            e.printStackTrace();
        }
    }


    private void executeTestSet( Class<?> clazz, RunListener reporter, RunNotifier listeners )
            throws ReporterException, TestSetFailedException
    {
        final ReportEntry report = new SimpleReportEntry( this.getClass().getName(), clazz.getName() );

        reporter.testSetStarting( report );

        try
        {
            if ( !StringUtils.isBlank(this.requestedTestMethod) )
            {
                String actualTestMethod = getMethod( clazz, this.requestedTestMethod );//add by rainLee
                String[] testMethods = StringUtils.split( actualTestMethod, "+" );
                execute( clazz, listeners, testMethods );
            }
            else
            {//the original way
                execute( clazz, listeners, null );
            }
        }
        catch ( TestSetFailedException e )
        {
            throw e;
        }
        catch ( Throwable e )
        {
            reporter.testError( SimpleReportEntry.withException( report.getSourceName(), report.getName(),
                    new PojoStackTraceWriter( report.getSourceName(),
                            report.getName(), e ) ) );
        }
        finally
        {
            reporter.testSetCompleted( report );
        }
    }

    private RunNotifier getRunNotifier(org.junit.runner.notification.RunListener main, Result result,
                                       List<org.junit.runner.notification.RunListener> others)
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.addListener( main );
        fNotifier.addListener( result.createListener() );
        for ( org.junit.runner.notification.RunListener listener : others )
        {
            fNotifier.addListener( listener );
        }
        return fNotifier;
    }

    // I am not entierly sure as to why we do this explicit freeing, it's one of those
    // pieces of code that just seem to linger on in here ;)
    private void closeRunNotifer( org.junit.runner.notification.RunListener main,
                                  List<org.junit.runner.notification.RunListener> others )
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.removeListener( main );
        for ( org.junit.runner.notification.RunListener listener : others )
        {
            fNotifier.removeListener( listener );
        }
    }

    public Iterator<?> getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun.iterator();
    }

    private TestsToRun scanClassPath()
    {
        final TestsToRun scannedClasses = scanResult.applyFilter( jUnit4TestChecker, testClassLoader );
        return runOrderCalculator.orderTestClasses( scannedClasses );
    }

    @SuppressWarnings("unchecked")
    private void upgradeCheck()
            throws TestSetFailedException
    {
        if ( isJunit4UpgradeCheck() )
        {
            List<Class> classesSkippedByValidation =
                    scanResult.getClassesSkippedByValidation( jUnit4TestChecker, testClassLoader );
            if ( !classesSkippedByValidation.isEmpty() )
            {
                StringBuilder reason = new StringBuilder();
                reason.append( "Updated check failed\n" );
                reason.append( "There are tests that would be run with junit4 / surefire 2.6 but not with [2.7,):\n" );
                for ( Class testClass : classesSkippedByValidation )
                {
                    reason.append( "   " );
                    reason.append( testClass.getName() );
                    reason.append( "\n" );
                }
                throw new TestSetFailedException( reason.toString() );
            }
        }
    }

    private boolean isJunit4UpgradeCheck()
    {
        final String property = System.getProperty( "surefire.junit4.upgradecheck" );
        return property != null;
    }


    private static void execute( Class<?> testClass, RunNotifier fNotifier, String[] testMethods )
            throws TestSetFailedException
    {
        if ( null != testMethods )
        {
            Method[] methods = testClass.getMethods();
            for ( Method method : methods )
            {
                for ( String testMethod : testMethods )
                {
                    if ( SelectorUtils.match(testMethod, method.getName()) )
                    {
                        Runner junitTestRunner = Request.method(testClass, method.getName()).getRunner();
                        junitTestRunner.run( fNotifier );
                    }

                }
            }
            return;
        }

        Runner junitTestRunner = Request.aClass(testClass).getRunner();

        junitTestRunner.run( fNotifier );
    }

    /**
     * this method retrive  testMethods from String like "com.xx.ImmutablePairTest#testBasic,com.xx.StopWatchTest#testLang315+testStopWatchSimpleGet"
     * <br>
     * and we need to think about cases that 2 or more method in 1 class. we should choose the correct method
     *
     * @param testClass     the testclass
     * @param testMethodStr the test method string
     * @return a string ;)
     */
    private static String getMethod( Class testClass, String testMethodStr )
    {
        String className = testClass.getName();

        if ( !testMethodStr.contains( "#" ) && !testMethodStr.contains( "," ) )
        {//the original way
            return testMethodStr;
        }
        testMethodStr += ",";//for the bellow  split code
        int beginIndex = testMethodStr.indexOf( className );
        int endIndex = testMethodStr.indexOf( ",", beginIndex );
        String classMethodStr =
                testMethodStr.substring( beginIndex, endIndex );//String like "StopWatchTest#testLang315"

        int index = classMethodStr.indexOf( '#' );
        if ( index >= 0 )
        {
            return classMethodStr.substring( index + 1, classMethodStr.length() );
        }
        return null;
    }
}
