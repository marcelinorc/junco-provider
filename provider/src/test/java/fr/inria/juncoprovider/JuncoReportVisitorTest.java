package fr.inria.juncoprovider;
import junit.framework.Assert;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecFileLoader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by marcel on 22/03/14.
 */
public class JuncoReportVisitorTest {


    private static String ARITHMETIC_CLASS = "fr.inria.covermath.Arithmetic";

    private IBundleCoverage getCoverageBundle(String exec) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(getClass().getResource("/coverage/" + exec + "Test.exec").openStream());
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);
        analyzer.analyzeAll(new File("example\\covermath\\target\\classes"));
        return coverageBuilder.getBundle(exec);
    }

    @Test
    public void testHandleBundleGoodCoverage() throws Exception {
        JuncoReportVisitor visitor = new JuncoReportVisitor(
                ARITHMETIC_CLASS, 15, "A name");

        visitor.visitBundle(getCoverageBundle(ARITHMETIC_CLASS), null);
        Assert.assertFalse(visitor.getCoverageSum() == 0);
    }

    @Test
    public void testHandleBundleNullCoverage() throws Exception {
        JuncoReportVisitor visitor = new JuncoReportVisitor(
                ARITHMETIC_CLASS, 15, "A name");

        visitor.visitBundle(getCoverageBundle("fr.inria.covermath.Trigonometry"), null);
        Assert.assertEquals(visitor.getCoverageSum(), (float)0.0);
    }
}
