//package org.lri.oak.structure;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class MavenTest {

    private static final double DELTA = 1e-15;
    
    @Test
    public void test() {
    	JsonObject j = new JsonObject();
    	j.add("ok", new JsonPrimitive(1));
    	System.out.println(j.toString());
        double a = 0.2;
        double b = 0.2;
        Assert.assertEquals(a, b, DELTA);
    }

}
