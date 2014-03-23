package fr.inria.juncoprovider;


import junit.framework.Assert;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

/*
 * Created by marcel on 20/03/14.
 */
public class CoverageRunOrderCalculatorTest {

    @Test
    public void testCreationOK() throws Exception {
        try {
            String classesDir = getClass().getResource("/resource_classes").toURI().getPath();
            String coverageDir = getClass().getResource("/coverage").toURI().getPath();
            String transplantFile = getClass().getResource("/transplant.json").toURI().getPath();

            CoverageRunOrderCalculator r = new CoverageRunOrderCalculator(
                    classesDir, coverageDir, transplantFile);

            Assert.assertNotNull(r);
        } catch (CoverageRunOrderException e ) {
            Assert.fail("This exception should not rise: " + e.getMessage());
        }
    }

    /**
     * Test what happens when the coverage dir and transplantation file are NOT found
     * @throws Exception
     */
    @Test(expected = CoverageRunOrderException.class)
    public void testCreationFails() throws Exception {
        CoverageRunOrderCalculator r = new CoverageRunOrderCalculator(
                new File("notexistingpath"),
                new File("notexistingpath"),
                new File("othernonexistingpath.json"));
    }

    /**
     * Test what happens when the  transplantation file is NOT found
     * @throws Exception
     */
    @Test
    public void testJSONFailure() throws Exception {
        String classesDir = getClass().getResource("/resource_classes").toURI().getPath();
        String coverageDir = getClass().getResource("/coverage").toURI().getPath();
        String transplantFile = getClass().getResource("/bad.json").toURI().getPath();

        try {
            CoverageRunOrderCalculator r = new CoverageRunOrderCalculator(
                classesDir, coverageDir, transplantFile);
        } catch (CoverageRunOrderException e) {
            Assert.assertTrue(e.getMessage().contains("Cannot parse transplant file at "));
        }
    }

    /**
     * Test that the class order is properly calculated
     */
    @Test
    public void testClassOrder() throws Exception {
        String classesDir = getClass().getResource("/resource_classes").toURI().getPath();
        String coverageDir = getClass().getResource("/coverage").toURI().getPath();
        String transplantFile = getClass().getResource("/transplant.json").toURI().getPath();

        CoverageRunOrderCalculator r = new CoverageRunOrderCalculator(
                classesDir, coverageDir, transplantFile);

        //A class loader that read classes from the resources dir
        TestResourcesClassLoader loader = new TestResourcesClassLoader();
        loader.setResourceDir(classesDir);
        ArrayList<Class> ac = new ArrayList<Class>();
        ac.add(loader.loadClass("fr.inria.testproject.Arithmetic"));
        ac.add(loader.loadClass("fr.inria.testproject.Trigonometry"));

        //Run the order
        TestsToRun tr = r.orderTestClasses(new TestsToRun(ac));

        Assert.assertTrue(
                tr.getLocatedClasses()[0].getSimpleName().equals("Arithmetic"));
        Assert.assertTrue(
                tr.getLocatedClasses()[1].getSimpleName().equals("Trigonometry"));
    }

}
