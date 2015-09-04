package fr.inria.juncoprovider.sandbox;

import java.io.IOException;
import java.util.*;

/**
 * Created by marodrig on 26/03/2015.
 */
public class PrintTestCoverage {

    public static void main(String[] args) throws IOException {
        JuncoProcessor p = new JuncoProcessor(Arrays.asList(new String[]{"fr.inria.testproject.Trigonometry"}),
                "C:\\MarcelStuff\\PROJECTS\\DIVERSE\\junco-provider\\test-project\\target\\site\\junco",
                "C:\\MarcelStuff\\PROJECTS\\DIVERSE\\junco-provider\\test-project\\target");
        HashMap<String, Collection<String>> coverage = p.process();
        for (String k : coverage.keySet()) {
            System.out.println(k);
            for (String s : coverage.get(k)) {
                System.out.println(" - " + s);
            }
        }


    }

}
