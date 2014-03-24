package fr.inria.juncoprovider;

import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.TestsToRun;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecFileLoader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by marcel on 20/03/14.
 */
public class CoverageRunOrderCalculator implements RunOrderCalculator {

    private File coverageDir;

    //private File transplantFile;

    private File classesDir;

    //Would it would be better to have source positionLine?
    private int positionLine;

    private String positionClass;

    private class ClassOrder {
        Class klass;
        float coverage = 0;

        public ClassOrder(Class c, float coverageSum) {
            klass = c;
            coverage = coverageSum;
        }
    }

   /**
    *  Creates a run order calculator based on the coverage information.
    *
    *  @param coverageDir Directory where to find the coverage information
    *  @param transplantFile Directory where to find the transplant point
    */
    public CoverageRunOrderCalculator(String classesDir, String coverageDir, String transplantFile)
            throws CoverageRunOrderException {
        init(new File(classesDir), new File(coverageDir), new File(transplantFile));
    }

    /**
     *  Creates a run order calculator based on the coverage information.
     *
     *  @param coverageDir Directory where to find the coverage information
     *  @param transplantFile File where to find the transplant point
     */
    public CoverageRunOrderCalculator(File classesDir, File coverageDir, File transplantFile)
            throws CoverageRunOrderException {
        init(classesDir, coverageDir, transplantFile);
    }

    /**
     * Checks that the coverage dir and transplantation file exists, raises a FileNotFoundException otherwise
     *
     * @param coverageDir Directory where to find the coverage information
     * @param transplantFile File where to find the transplant point
     * @throws CoverageRunOrderException
     */
    protected void init(File classesDir, File coverageDir, File transplantFile) throws
            CoverageRunOrderException {

        //Check that the classes and coverages information exists
        if ( classesDir.exists() ) { this.classesDir = classesDir; }
        else {
            throw new CoverageRunOrderException(
                    "Could not find classes dir at " + classesDir.getAbsolutePath());
        }
        if ( coverageDir.exists() ) { this.coverageDir = coverageDir; }
        else {
            throw new CoverageRunOrderException(
                "Could not find coverage dir at " + coverageDir.getAbsolutePath());
        }

        //Obtain the transplantation information from file
        //this.transplantFile = transplantFile;
        try {
            JSONParser parser = new JSONParser();
            JSONObject object = (JSONObject) parser.parse(new FileReader(transplantFile));

            String pos = ((String)object.get("Position"));

            try {
                int index = pos.lastIndexOf(":");
                positionLine = Integer.valueOf(pos.substring(index + 1));
                positionClass = pos.substring(0, index);
            } catch (NumberFormatException e ) {
                throw new ParseException(0, object);
            }
        } catch (IOException e) {
            throw new CoverageRunOrderException("Cannot found transplant file at " +
                    transplantFile.getAbsolutePath());
        } catch (ParseException e) {
            throw new CoverageRunOrderException("Cannot parse transplant file at " +
                    transplantFile.getAbsolutePath());
        }
    }

    /**
     * Inserts the Class order ordered by coverage value from max to min
     * @param order Current order of class
     * @param o New order to insert
     */
    private void insertOrdered(ArrayList<ClassOrder> order, ClassOrder o) {
        int i = 0;
        while ( i < order.size() && o.coverage < order.get(i).coverage ) { i++; }
        order.add(i, o);
    }

    @Override
    public TestsToRun orderTestClasses(TestsToRun classes) {
        ArrayList<Class> result = new ArrayList<Class>();
        ArrayList<ClassOrder> order = new  ArrayList<ClassOrder>();

        for ( Class c : classes ) {

            try {
                //Obtain the coverage bundle
                ExecFileLoader loader = new ExecFileLoader();
                loader.load(new File(coverageDir + "/" + c.getCanonicalName() + ".exec"));
                final CoverageBuilder coverageBuilder = new CoverageBuilder();
                final Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);
                analyzer.analyzeAll(classesDir);
                IBundleCoverage bundle = coverageBuilder.getBundle(c.getCanonicalName());

                //Visit the bundle obtaining the coverage for the transplant point
                JuncoReportVisitor visitor = new JuncoReportVisitor(
                        positionClass, positionLine, c.getSimpleName());
                visitor.visitBundle(bundle, null);
                float coverageSum = visitor.getCoverageSum();

                //Insert the class ordered by its coverage value of the transplant point
                insertOrdered(order, new ClassOrder(c, coverageSum));
            } catch (IOException e) {
                e.printStackTrace();
                //Don't include this class
            }

        }
        //Return the TestToRun
        for ( ClassOrder o : order ) { result.add(o.klass); }
        return new TestsToRun(result);
    }
}
