package fr.inria.covermath;

/**
 * Created by marcel on 23/02/14.
 *
 * A class to test some coverage. In some method an "explosive" line is introduced
 * which will not be tested.
 *
 */
public class ConditionalTrigonometry {

    // A dummy variable to make some dummy conditions
    private double lastResult = 0;

    //A dummy Add procedure to test some logic branches
    public void sinConditional(double a) {

        double senA = Math.sin(a);

        if ( senA > 0.5 ) {
            setLastResult(getLastResult() + senA);
        }
        else {
            //This one will not be covered by the test
            setLastResult(getLastResult() / 0); //Or else...
        }
    }

    //Yet another dummy procedure to test some logic branches
    public void cosConditional(double a) {

        double cosA = Math.cos(a);

        if ( cosA > 0.5 ) {
            setLastResult(getLastResult() + cosA);
        }
        else {
            //This one will not be covered by the test
            setLastResult(getLastResult() / 0); //Or else...
        }
    }

    //Some lines to test full coverage
    public void fullCoverage(double a) {
        setLastResult(Math.sin(a) + Math.cos(a * 2));
        a = getLastResult() * 2;
        setLastResult(a + 10);
    }

    public double getLastResult() {
        return lastResult;
    }

    public void setLastResult(double lastResult) {
        this.lastResult = lastResult;
    }
}
