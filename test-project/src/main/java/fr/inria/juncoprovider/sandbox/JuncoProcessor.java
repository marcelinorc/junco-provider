package fr.inria.juncoprovider.sandbox;


import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Reads Junco coverage information and returns which test uses the classes of the bundle
 * <p/>
 * Created by marodrig on 26/03/2015.
 */
public class JuncoProcessor {

    private final Collection<String> bundle;
    /**
     * Path to the Junco coverage path.
     */
    private String coveragePath;

    /**
     * Path where the build classes are
     */
    private String builtClassesPath;


    /**
     * Creates the test dependency extractor.
     *
     * @param coveragePath Path to the Junco coverage
     */
    public JuncoProcessor(Collection<String> bundle, String coveragePath, String builtClassesPath) {
        this.coveragePath = coveragePath;
        this.builtClassesPath = builtClassesPath;
        this.bundle = bundle;
    }

    /**
     * Processes the coverage information
     */
    protected HashMap<String, Collection<String>> process() throws IOException {

        HashMap<String, Collection<String>> result = new HashMap<>();

        File fcoverage = new File(coveragePath);
        if (!fcoverage.exists()) throw new FileNotFoundException(fcoverage.getAbsolutePath());

        for (File f : fcoverage.listFiles()) {
            //Obtain the coverage bundle
            if ( f.isDirectory() || !f.getName().endsWith(".exec") ) continue;
            ExecFileLoader loader = new ExecFileLoader();

            loader.load(f);
            final CoverageBuilder coverageBuilder = new CoverageBuilder();
            final Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);
            analyzer.analyzeAll(new File(builtClassesPath));

            Collection<String> coveredClassesName = new ArrayList<>();
            String n = f.getName();
            for (IClassCoverage c : coverageBuilder.getClasses()) {
                if ( c.getClassCounter().getCoveredCount() > 0 ) {
                    String className = c.getName().replace("/", ".");
                    if ( bundle == null || bundle.size() == 0 || bundle.contains(className) )
                        coveredClassesName.add(className);
                }
            }
            if ( coveredClassesName.size() > 0 )
                result.put(n.substring(0, n.lastIndexOf(".exec")), coveredClassesName);
        }
        return result;
    }

}
