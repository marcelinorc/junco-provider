package fr.inria.covermath;


import org.junit.Test;
import org.junit.Assert;

/**
 * Created by marcel on 23/02/14.
 */
public class ConditionalArithmeticTest {

    @Test
    public void testAddConditional() {
       ConditionalArithmetic a = new ConditionalArithmetic();
        a.addConditional(2, 4);
        Assert.assertFalse(false);
    }

}
