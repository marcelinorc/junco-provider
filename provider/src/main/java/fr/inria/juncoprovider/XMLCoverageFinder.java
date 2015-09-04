package fr.inria.juncoprovider;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 *
 * Class in charge of finding the coverage of a position. This class will give a VEEERY relaxed metric of coverage
 * Is ONLY meant to filter out test cases that REALLY DON'T cover some position AT ALL.
 *
 * If the test case, covers at least some portion of the class containing the position of code, will be included.
 *
 * This method, less accurate, allows to use Jacoco with transformed code.
 *
 * Created by marodrig on 04/09/2015.
 */
public class XMLCoverageFinder {

    /**
     * Returns whether the position is covered at all or not
     * @param xmlCoverageFile File containing the coverage information
     * @param positionClass Class containing the position
     * @return True if the coverage covers the class at least partially
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public boolean isCovered(String xmlCoverageFile, String positionClass) throws IOException, SAXException, ParserConfigurationException {
        return  findCoverage(xmlCoverageFile, positionClass, true) != 0.0;
    }

    /**
     * Returns the a metric of coverage for the position.
     * @param xmlCoverageFile
     * @param positionClass
     * @return Number of lines covering
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public float coverage(String xmlCoverageFile, String positionClass)
            throws IOException, SAXException, ParserConfigurationException {
        return  findCoverage(xmlCoverageFile, positionClass, false) ;
    }

    /**
     * Returns the coverage of a position
     * @param xmlCoverageFile File containing the coverage information
     * @param positionClass Class where the line of code
     * @param coveredOnly
     * @return
     * @throws ParserConfigurationException
     */
    private float findCoverage(String xmlCoverageFile, String positionClass, boolean coveredOnly)
            throws ParserConfigurationException, IOException, SAXException {
        float result = 0.0f;
        //Read directly from the XML, less accurate but allows to use junco with modified code
        File xmlFile = new File(xmlCoverageFile);
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

}
