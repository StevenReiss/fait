/********************************************************************************/
/*										*/
/*		FlowTest.java							*/
/*										*/
/*	Tests for flow analysis 						*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.fait.flow;

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.ivy.jcode.JcodeInstruction;

import org.junit.*;

import java.util.*;
import java.io.*;

public class FlowTest implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl	fait_control;
private Collection<String> start_classes;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public FlowTest()
{
}




/********************************************************************************/
/*										*/
/*	Test case for onsets							*/
/*										*/
/********************************************************************************/

// @Test
public void flowTestOnsets()
{
   fait_control = FaitControl.Factory.getControl();
   fait_control.setProject(new TestProject());

   start_classes = new ArrayList<String>();
   start_classes.add("spr.onsets.OnsetMain");

   IfaceLog.setLevel(LogLevel.DEBUG);

   fait_control.analyze(1);

}



// @Test
public void flowTestOnsetsThreaded()
{
   fait_control = FaitControl.Factory.getControl();
   fait_control.setProject(new TestProject());

   start_classes = new ArrayList<String>();
   start_classes.add("spr.onsets.OnsetMain");

   IfaceLog.setLevel(LogLevel.DEBUG);

   fait_control.analyze(4);

   showResults();
}



@Test
public void flowTestSolar()
{
   fait_control = FaitControl.Factory.getControl();
   fait_control.setProject(new TestProjectSolar());

   start_classes = new ArrayList<String>();
   start_classes.add("edu.brown.cs.cs032.solar.SolarMain");

   IfaceLog.setLevel(LogLevel.DEBUG);

   fait_control.analyze(4);

   showResults();
}




@Test
public void flowTestUpod()
{
   IfaceLog.setLevel(LogLevel.DEBUG);
   
   fait_control = FaitControl.Factory.getControl();
   fait_control.setProject(new TestProjectUpod());
   
   start_classes = new ArrayList<String>();
   start_classes.add("edu.brown.cs.upod.smartsign.SmartSignMain");
   
   fait_control.analyze(1);
   
   showResults();
}

// @Test
public void flowTestS6()
{
   fait_control = FaitControl.Factory.getControl();
   fait_control.setProject(new TestProjectS6());

   start_classes = new ArrayList<String>();
   start_classes.add("edu.brown.cs.s6.engine.EngineMain");

   IfaceLog.setLevel(LogLevel.DEBUG);

   fait_control.analyze(1);
// fait_control.analyze(4);

   showResults();
}



/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

private void showResults()
{
   for (IfaceCall ic : fait_control.getAllCalls()) {
      if (!fait_control.isInProject(ic.getMethod())) {
	 Collection<JcodeInstruction> cins = ic.getDeadInstructions();
	 if (cins != null && !cins.isEmpty()) {
	    IfaceLog.logD("Dead instructions for " + ic.getLogName() + ":");
	    for (JcodeInstruction fi : cins) {
	       IfaceLog.logD1("OP: " + fi.toString());
	     }
	  }
       }
    }
   for (IfaceCall ic : fait_control.getAllCalls()) {
      if (fait_control.isInProject(ic.getMethod())) {
	 Collection<JcodeInstruction> cins = ic.getDeadInstructions();
	 if (cins != null && !cins.isEmpty()) {
	    IfaceLog.logI1("Dead instructions for " + ic.getLogName() + ":");
	    for (JcodeInstruction fi : cins) {
	       IfaceLog.logI("OP: " + fi.toString());
	     }
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Test project								*/
/*										*/
/********************************************************************************/

private class TestProject implements FaitProject {

@Override public String getClasspath() {
   return "/home/spr/sampler";
}

@Override public Collection<String> getBaseClasses() {
   Collection<String> rslt = new ArrayList<String>();
   rslt.add("spr.onsets.OnsetMain");
   return rslt;
}
@Override public Collection<String> getStartClasses()	     { return start_classes; }

@Override public List<File> getDescriptionFile()		{ return null; }

@Override public boolean isProjectClass(String cls) {
   if (cls.startsWith("spr.")) return true;
   return false;
}

@Override public FaitMethodData createMethodData(FaitCall fc)	{ return null; }

}	// end of inner class TestProject



private class TestProjectS6 implements FaitProject {

@Override public String getClasspath() {
   String cp = "/pro/s6/java:/pro/ivy/java:/pro/ivy/lib/jikesbt.jar:";
   cp += System.getenv("ECLIPSEPATH") + ":";
   cp += "/pro/s6/lib/asm-3.1.jar:/pro/s6/lib/json.jar:";
   cp += "/gpfs/main/research/ivy/lib/mysql.jar";
   return cp;
}

@Override public Collection<String> getBaseClasses() {
   Collection<String> rslt = new ArrayList<String>();
   rslt.add("edu.brown.cs.s6.engine.EngineMain");
   return rslt;
}
@Override public Collection<String> getStartClasses()	     { return start_classes; }

@Override public List<File> getDescriptionFile()		{ return null; }

@Override public boolean isProjectClass(String cls) {
   if (cls.startsWith("edu.brown.cs.")) return true;
   return false;
}

@Override public FaitMethodData createMethodData(FaitCall fc)	{ return null; }

}	// end of inner class TestProjectS6



private class TestProjectSolar implements FaitProject {

@Override public String getClasspath() {
   String cp = "/home/spr/solar/java:/pro/ivy/java:/home/spr/jogl/jogl-linux64/jogl.jar";
   return cp;
}

@Override public Collection<String> getBaseClasses() {
   Collection<String> rslt = new ArrayList<String>();
   rslt.add("edu.brown.cs.cs032.solar.SolarMain");
   return rslt;
}
@Override public Collection<String> getStartClasses()	     { return start_classes; }

@Override public List<File> getDescriptionFile()		{ return null; }

@Override public boolean isProjectClass(String cls) {
   if (cls.startsWith("edu.brown.cs.")) return true;
   return false;
}

@Override public FaitMethodData createMethodData(FaitCall fc)	{ return null; }

}	// end of inner class TestProjectSolar




private class TestProjectUpod implements FaitProject {

@Override public String getClasspath() {
   String lib = "/research/people/spr/upod/lib/";
   String glib = lib + "google/lib/";
   String gdep = lib + "google/deps/";
   String cp = "/research/people/spr/upod/java";
   cp += ":/research/ivy/java";
   cp += ":" + lib + "nanohttpd.jar";
   cp += ":" + lib + "json.jar"; 
   cp += ":" + lib + "stringtemplate.jar"; 
   cp += ":" + lib + "velocity-1.7.jar"; 
   cp += ":" + lib + "velocity-1.7-dep.jar";
   cp += ":" + lib + "jsoup-1.7.2.jar";
   cp += ":" + glib + "gdata-base-1.0.jar";
   cp += ":" + glib + "gdata-calendar-2.0.jar"; 
   cp += ":" + glib + "gdata-client-1.0.jar"; 
   cp += ":" + glib + "gdata-core-1.0.jar";
   cp += ":" + gdep + "guava-11.0.2.jar";
   cp += ":" + gdep + "jsr305.jar";
   cp += ":/research/s6/public/batik-1.7/batik.jar";
   File bdir = new File("/research/s6/public/batik-1.7/lib");
   for (File f : bdir.listFiles()) {
      String fnm = f.getPath();
      if (fnm.contains("batik-") && fnm.endsWith(".jar")) {
         cp += ":" + fnm;
       }
    }
   
   return cp;
}

@Override public Collection<String> getBaseClasses() {
   Collection<String> rslt = new ArrayList<String>();
   rslt.add("edu.brown.cs.upod.smartsign.SmartSignMain");
   return rslt;
}
@Override public Collection<String> getStartClasses()	     { return start_classes; }

@Override public List<File> getDescriptionFile()		
{ 
   List<File> files = new ArrayList<File>();
   files.add(new File("/research/people/spr/fait/lib/faitivy.xml"));
   files.add(new File("/research/people/spr/upod/upod.fait"));
   return files; 
}

@Override public boolean isProjectClass(String cls) {
   if (cls.startsWith("edu.brown.cs.")) return true;
   return false;
}

@Override public FaitMethodData createMethodData(FaitCall fc)	{ return null; }

}	// end of inner class TestProjectUpod




}	// end of class FlowTest




/* end of FlowTest.java */

