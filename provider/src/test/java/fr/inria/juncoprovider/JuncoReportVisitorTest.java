package fr.inria.juncoprovider;

import junit.framework.Assert;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;

/**
 * Created by marcel on 22/03/14.
 */
public class JuncoReportVisitorTest {


    private static String ARITHMETIC_CLASS = "fr.inria.juncoprovider.testproject.Arithmetic";
    private static String ARITHMETIC_TEST_CLASS = "fr.inria.testproject.ArithmeticTest.exec";

    private String getResourcePath(String name) throws Exception {
        return getClass().getResource("/" + name).toURI().getPath();
    }

    private IBundleCoverage getCoverageBundle(String exec) throws Exception {
        ExecFileLoader loader = new ExecFileLoader();
        File f = new File(getResourcePath("coverage/" + exec));
        loader.load(f);
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);
        analyzer.analyzeAll(new File(
                getClass().getResource("/resource_classes").toURI().getPath()));
        return coverageBuilder.getBundle(exec);
    }

    @Test
    public void testHandleBundleGoodCoverage() throws Exception {
        JuncoReportVisitor visitor = new JuncoReportVisitor(
                ARITHMETIC_CLASS, 15, "A name");

        visitor.visitBundle(getCoverageBundle(ARITHMETIC_TEST_CLASS), null);
        assertFalse(visitor.getCoverageSum() == 0);
    }

    @Test
    public void testHandleBundleNullCoverage() throws Exception {
        JuncoReportVisitor visitor = new JuncoReportVisitor(
                ARITHMETIC_CLASS, 15, "A name");

        visitor.visitBundle(getCoverageBundle("fr.inria.testproject.TrigonometryTest.exec"), null);
        Assert.assertEquals(visitor.getCoverageSum(), (float)0.0);
    }
}
