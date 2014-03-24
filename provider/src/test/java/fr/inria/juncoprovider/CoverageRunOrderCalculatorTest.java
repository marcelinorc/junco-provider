package fr.inria.juncoprovider;


import junit.framework.Assert;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

/*
 * Created by marcel on 20/03/14.
 */
public class CoverageRunOrderCalculatorTest {

    private String getResourcePath(String name) throws Exception {
        return getClass().getResource("/" + name).toURI().getPath();
    }

    /**
     * Test the creation when everything goes OK
     * @throws Exception
     */
    @Test
    public void testCreationOK() throws Exception {
        try {
            String classesDir = getResourcePath("resource_classes");
            String coverageDir = getResourcePath("coverage");
            String transplantFile = getResourcePath("transplant.json");

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
        String classesDir = getResourcePath("resource_classes");
        String coverageDir = getResourcePath("coverage");
        String transplantFile = getResourcePath("bad.json");

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
        CoverageRunOrderCalculator r = new CoverageRunOrderCalculator(
                getResourcePath("resource_classes"),
                getResourcePath("coverage"),
                getResourcePath("transplant.json"));

        //A class loader that read the test classes from the resources dir
        TestResourcesClassLoader loader = new TestResourcesClassLoader();
        loader.setResourceDir(getResourcePath("resource_testclasses"));
        ArrayList<Class> ac = new ArrayList<Class>();
        ac.add(loader.loadClass("fr.inria.testproject.ArithmeticTest"));
        ac.add(loader.loadClass("fr.inria.testproject.TrigonometryTest"));

        //Run the order
        TestsToRun tr = r.orderTestClasses(new TestsToRun(ac));

        //The ArithmeticTest must go first since in the transplant.json file states
        //that the transplantation point goes in class Arithmetic, line 15
        Assert.assertTrue(
                tr.getLocatedClasses()[0].getSimpleName().equals("ArithmeticTest"));
        Assert.assertTrue(
                tr.getLocatedClasses()[1].getSimpleName().equals("TrigonometryTest"));
    }

}
