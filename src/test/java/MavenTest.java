//package org.lri.oak.structure;

import org.junit.Assert;
import org.junit.Test;

public class MavenTest {

    private static final double DELTA = 1e-15;
    
    @Test
    public void test() {
        double a = 0.2;
        double b = 0.2;
        Assert.assertEquals(a, b, DELTA);
    }

}
