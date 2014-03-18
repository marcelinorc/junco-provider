package fr.inria.covermath;

import org.junit.Test;
import org.junit.Assert;

/**
 * Created by marcel on 23/02/14.
 */
public class ConditionalTrigonometryTest {

    @Test
    public void testAddConditional() {
       ConditionalTrigonometry t = new ConditionalTrigonometry();
        t.cosConditional(1);
        Assert.assertFalse(false);
    }

}
