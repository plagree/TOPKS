package org.dbweb.Arcomem.Integration;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.REXP;
import org.springframework.core.io.ClassPathResource;


public class JavarTest {
//    static Rengine rengine = new Rengine();
	
//	public static void main(String[] args) {
//		String  []engineArgs = new String[1];
//	    engineArgs [0] =   "--vanilla";
//	    rengine=new Rengine (engineArgs, false, null);
//		rengine.eval(String.format("greeting <- '%s'", "Hello R World"));
//		REXP result = rengine.eval("greeting");
//		System.out.println("Greeting from R: "+result.asString());
//				
//	}
	public static void main(String[] args) throws IOException, ScriptException {
		
		
		
		
		
		String  []engineArgs = new String[1];
	    engineArgs [0] =   "--vanilla";
	    Rengine rengine = new Rengine (engineArgs, false, null);
		
		
	    ClassPathResource rScript = new ClassPathResource("resources/Test.R");
	    
	    try {
			rengine.eval(String.format("source('%s')",
			        rScript.getFile().getAbsolutePath()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	          rengine.eval("x");
	          REXP result = rengine.eval("x");
	          
	         double[][]  mat = result.asMatrix();
	         int str= result.rtype;
	         System.out.println(str);
//	         System.out.println("res: "+mat.length);
	         System.out.println(result.rtype);
	      
	          


	         
//		String  []engineArgs = new String[1];
//	    engineArgs [0] =   "--vanilla";
//	    rengine=new Rengine (engineArgs, false, null);
//		rengine.eval(String.format("y<-'%s'","12"));
//		REXP result = rengine.eval("y");
//		System.out.println("Greeting from R: "+result.asString());
				
	}

}
