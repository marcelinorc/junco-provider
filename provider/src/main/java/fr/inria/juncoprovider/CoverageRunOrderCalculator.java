package fr.inria.juncoprovider;

import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.TestsToRun;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by marcel on 20/03/14.
 */
public class CoverageRunOrderCalculator implements RunOrderCalculator {

    private boolean coveredOnly;

    public boolean isUseXML() {
        return useXML;
    }

    public void setUseXML(boolean useXML) {
        this.useXML = useXML;
    }

    /**
     * Use the xml coverage analysis. Less accurate. Allows transformed code.
     */
    private boolean useXML;

    private File coverageDir;

    //private File transplantFile;

    private File classesDir;

    //Would it would be better to have source positionLine?
    private int positionLine;

    private String positionClass;

    private ConsoleLogger logger;

    public void setLogger(ConsoleLogger logger) {
        this.logger = logger;
    }

    public ConsoleLogger getLogger() {
        return logger;
    }

    public void setCoveredOnly(boolean coveredOnly) {
        this.coveredOnly = coveredOnly;
    }

    public boolean isCoveredOnly() {
        return coveredOnly;
    }

    private class ClassOrder {
        Class klass;
        float coverage = 0;

        public ClassOrder(Class c, float coverageSum) {
            klass = c;
            coverage = coverageSum;
        }
    }

    /**
     * Creates a run order calculator based on the coverage information.
     *
     * @param coverageDir    Directory where to find the coverage information
     * @param transplantFile Directory where to find the transplant point
     */
    public CoverageRunOrderCalculator(String classesDir, String coverageDir, String transplantFile)
            throws CoverageRunOrderException {
        this(new File(classesDir), new File(coverageDir), new File(transplantFile));
    }

    /**
     * Creates a run order calculator based on the coverage information.
     *
     * @param coverageDir    Directory where to find the coverage information
     * @param transplantFile File where to find the transplant point
     */
    public CoverageRunOrderCalculator(File classesDir, File coverageDir, File transplantFile)
            throws CoverageRunOrderException {
        init(classesDir, coverageDir, transplantFile);
    }

    /**
     * Checks that the coverage dir and transplantation file exists, raises a FileNotFoundException otherwise
     *
     * @param coverageDir    Directory where to find the coverage information
     * @param transplantFile File where to find the transplant point
     * @throws CoverageRunOrderException
     */
    protected void init(File classesDir, File coverageDir, File transplantFile) throws
            CoverageRunOrderException {

        //Check that the classes and coverages information exists
        if (classesDir.exists()) {
            this.classesDir = classesDir;
        } else {
            String err = "Could not find classes dir at " + classesDir.getAbsolutePath() + "\r\n";
            log(err);
            throw new CoverageRunOrderException(err);
        }
        if (coverageDir.exists()) {
            this.coverageDir = coverageDir;
        } else {
            String err = "Could not find coverage dir at " + coverageDir.getAbsolutePath() + "\r\n";
            log(err);
            throw new CoverageRunOrderException(err);
        }

        //Obtain the transplantation information from file
        //this.transplantFile = transplantFile;
        try {
            JSONParser parser = new JSONParser();
            JSONObject object = (JSONObject) parser.parse(new FileReader(transplantFile));
            String pos = ((String) object.get("Position"));

            try {
                int index = pos.lastIndexOf(":");
                positionLine = Integer.valueOf(pos.substring(index + 1));
                positionClass = pos.substring(0, index);
                log("Found transplant point at: " + transplantFile.getAbsolutePath() + "\r\n");
            } catch (NumberFormatException e) {
                throw new ParseException(0, object);
            }
        } catch (IOException e) {
            String err = "Cannot found transplant file at " + transplantFile.getAbsolutePath() + "\r\n";
            log(err);
            throw new CoverageRunOrderException(err);
        } catch (ParseException e) {
            String err = "Cannot parse transplant file at " + transplantFile.getAbsolutePath() + "\r\n";
            log(err);
            throw new CoverageRunOrderException(err);
        }
    }

    private void log(String err) {
        if (this.logger != null) this.logger.info(err);
    }

    /**
     * Inserts the Class order ordered by coverage value from max to min
     *
     * @param order Current order of class
     * @param o     New order to insert
     */
    private void insertOrdered(ArrayList<ClassOrder> order, ClassOrder o) {
        int i = 0;
        while (i < order.size() && o.coverage < order.get(i).coverage) {
            i++;
        }
        order.add(i, o);
    }

    /**
     * Find if this test class covers at least the class containing the transplant point.
     *
     * @param c Test class
     * @return Coverage metric. The higher the more the test cover the transplant point's class
     * @throws IOException
     */
    private float findXMLCoverage(Class c) throws Exception {
        float result = 0.0f;
        //Read directly from the XML, less accurate but allows to use junco with modified code
        File xmlFile = new File(coverageDir + "/" + c.getCanonicalName() + ".xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setValidating(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        NodeList classes = doc.getElementsByTagName("class");
        for (int i = 0; i < classes.getLength(); i++) {
            Node node = classes.item(i);
            String className = node.getAttributes().getNamedItem("name").getNodeValue().replace("/", ".");
            if (className.equals(positionClass)) {
                NodeList childs = node.getChildNodes();
                for (int j = 0; j < childs.getLength(); j++) {
                    Node n = childs.item(j);
                    if (n.getNodeName().equalsIgnoreCase("counter")) {
                        result += Float.valueOf(n.getAttributes().getNamedItem("covered").getNodeValue());

                        //if coveredOnly, we don´t care about the sorting,
                        //just whether the class is covered or not
                        if (coveredOnly) return result;
                    }
                }
                break;
            }
        }

        return result;
    }

    /**
     * Find if this test class covers the position of the transplant point.
     * More accurate, don´t admit transformed code
     *
     * @param c Test class containing the transplant point.
     * @return
     * @throws IOException
     */
    private float findExecCoverage(Class c) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new File(coverageDir + "/" + c.getCanonicalName() + ".exec"));
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);
        analyzer.analyzeAll(classesDir);
        IBundleCoverage bundle = coverageBuilder.getBundle(c.getCanonicalName());

        //Visit the bundle obtaining the coverage for the transplant point
        JuncoReportVisitor visitor = new JuncoReportVisitor(
                positionClass, positionLine, c.getSimpleName());
        visitor.setLogger(logger);
        visitor.setVerbose(false);
        visitor.visitBundle(bundle, null);
        return visitor.getCoverageSum();


    }

    @Override
    public TestsToRun orderTestClasses(TestsToRun classes) {
        ArrayList<Class> result = new ArrayList<Class>();
        ArrayList<ClassOrder> order = new ArrayList<ClassOrder>();

        try {
            for (Class c : classes) {
                //Obtain the coverage bundle
                float coverageSum = useXML ? findXMLCoverage(c) : findExecCoverage(c);

                //Insert the class ordered by its coverage value of the transplant point
                if (!coveredOnly || coverageSum > 0) insertOrdered(order, new ClassOrder(c, coverageSum));
            }
        } catch (Exception e) {
            log("[Error] " + e.getMessage() + ". Returning to normal order");
            return classes;
        }
        //Return the TestToRun
        for (ClassOrder o : order) {
            result.add(o.klass);
        }
        return new TestsToRun(result);
    }
}
