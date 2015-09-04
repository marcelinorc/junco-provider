package fr.inria.juncoprovider;

import org.apache.maven.surefire.report.ConsoleLogger;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.internal.AbstractGroupVisitor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * A report visitor for the Jacoco exec file in order to extract coverage information. That
 * help us in the ordering of test suites.
 * <p/>
 * The report visitor extract the coverage information of a source position in the following way:
 * 1. Search for the method containing the source position
 * 2. Extract the coverage metrics for that method in the current bundle.
 * 3. Returns a float indicating the class and the coverage percent sum
 * for those method
 * <p/>
 * Created by marcel on 22/03/14.
 */
public class JuncoReportVisitor extends AbstractGroupVisitor implements IReportVisitor {

    /**
     * Class containing the transplantation point
     */
    private String className;
    /**
     * Line number of the transplantation point
     */
    private final int sourcePosition;

    //private List<SessionInfo> sessionInfos;
    //private Collection<ExecutionData> executionDatas;

    private float coverageSum = -1.0f;

    /**
     * Indicate if the mode is verbose
     */
    private boolean verbose;

    public ConsoleLogger getLogger() {
        return logger;
    }

    public void setLogger(ConsoleLogger logger) {
        this.logger = logger;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private ConsoleLogger logger;

    /**
     * The coverage sum for this test case for the transplantation point
     *
     * @return A float with the sum
     */
    public float getCoverageSum() {
        return coverageSum;
    }

    /**
     * Constructor of the report visitor
     *
     * @param className Class containing the transplantation point
     * @param position  Line number of the transplantation point
     * @param name      Name of the visitor (needed for the superclass)
     */
    protected JuncoReportVisitor(String className, int position, String name) {
        super(name);
        //This is in order to get the name in "Jacoco" representation
        this.className = className.replace(".", "/");
        this.sourcePosition = position;
    }


    @Override
    protected void handleBundle(IBundleCoverage bundle,
                                ISourceFileLocator sourceFileLocator) throws IOException {
        coverageSum = 0;
        for (final IPackageCoverage p : bundle.getPackages()) {
            for (final IClassCoverage c : p.getClasses()) {
                if (c.getName().equals(className)) {
                    log("Class " + className + " found in bundle " + bundle.getName());
                    for (final IMethodCoverage m : c.getMethods()) {
                        if (m.getFirstLine() <= sourcePosition &&
                                m.getLastLine() >= sourcePosition) {
                            log("Containing method '" + m.getName() + "' found in bundle ");
                            final ILine line = m.getLine(sourcePosition);
                            log("Instruction counter covered count " + line.getInstructionCounter().getCoveredCount());
                            log("Instruction counter Branch count " + line.getBranchCounter().getCoveredCount());
                            log("Line status " + line.getStatus());
                            if (line.getStatus() > ICounter.NOT_COVERED)
                                coverageSum = 1;

                            /*
                            ICounter e = m.getCounter(ICoverageNode.CounterEntity.INSTRUCTION);
                            coverageSum += (float)e.getCoveredCount() / (float)e.getTotalCount();
                            */
                            //TODO: in the future remove this break to support multiple transplantation points
                            break;
                        }
                    }
                    //TODO: in the future remove this break to support multiple transplantation points
                    break;
                }
            }
        }
    }

    private void log(String s) {
        if (verbose && logger != null) logger.info(s + "\n");
    }

    @Override
    protected AbstractGroupVisitor handleGroup(String s) throws IOException {
        return null;
    }

    @Override
    protected void handleEnd() throws IOException {

    }

    @Override
    public void visitInfo(List<SessionInfo> sessionInfos,
                          Collection<ExecutionData> executionDatas) throws IOException {
        /*
        this.sessionInfos = sessionInfos;
        this.executionDatas = executionDatas;
        */
    }
}
