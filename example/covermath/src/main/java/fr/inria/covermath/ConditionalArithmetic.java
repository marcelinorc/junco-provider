package fr.inria.covermath;

/**
 * Created by marcel on 23/02/14.
 *
 * A class to test some coverage. In some method an "explosive" line is introduced
 * which will not be tested.
 *
*/
public class ConditionalArithmetic {

    // A dummy variable to make some dummy conditions
    private int lastResult = 0;

    //A dummy Add procedure to test some logic branches
    public void addConditional(int a , int b) {
        if ( lastResult % 2 == 0 ) {
            lastResult += a;
        }
        else {
            //This one will not be covered by the test
            lastResult = lastResult / 0; //Or else...
        }
    }

    //Yet another dummy procedure to test some logic branches
    public void subConditional(int a , int b) {
        if ( lastResult % 2 == 0 ) {
            lastResult -= a;
        }
        else {
            //This one will not be covered by the test
            lastResult = lastResult / 0; //Or else...
        }
    }

    //All lines will be tested in this method
    public void fullCoverage(int a) {
        a = lastResult + a / 2;
        lastResult += lastResult + a;
    }

}
