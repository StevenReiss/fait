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

private IfaceControl	fait_control;
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

@Test
public void flowTestOnsets()
{
   FaitLog.setLogLevel(FaitLog.LogLevel.DEBUG);
   FaitLog.setLogFile("/vol/spr/faittestonsets.log");
   
   IfaceProject proj = IfaceControl.Factory.createSimpleProject("/home/spr/sampler","spr.");
   fait_control = IfaceControl.Factory.createControl(proj);

   start_classes = new ArrayList<String>();
   start_classes.add("spr.onsets.OnsetMain");

   fait_control.analyze(1,false,ReportOption.FULL_STATS);

}



// @Test
public void flowTestOnsetsThreaded()
{
   IfaceProject proj = IfaceControl.Factory.createSimpleProject("/home/spr/sampler",
         "spr.onset.");
   fait_control = IfaceControl.Factory.createControl(proj);

   start_classes = new ArrayList<String>();
   start_classes.add("spr.onsets.OnsetMain");

   FaitLog.setLogLevel(FaitLog.LogLevel.DEBUG);

   fait_control.analyze(4,false,ReportOption.FULL_STATS);

   showResults();
}



@Test
public void flowTestSolar()
{
   IfaceProject proj = IfaceControl.Factory.createSimpleProject(
         "/home/spr/solar/java:/pro/ivy/java:/home/spr/jogamp/jogl-all.jar:/home/spr/jogamp/gluegen-rt.jar",
         "edu.brown.cs.");
   fait_control = IfaceControl.Factory.createControl(proj);

   start_classes = new ArrayList<String>();
   start_classes.add("edu.brown.cs.cs032.solar.SolarMain");

   FaitLog.setLogLevel(FaitLog.LogLevel.DEBUG);
   FaitLog.setLogFile("/vol/spr/faittestsolar.log");

   fait_control.analyze(4,false,ReportOption.FULL_STATS);

   showResults();
}




@Test
public void flowTestUpod()
{
   FaitLog.setLogLevel(FaitLog.LogLevel.DEBUG);
   FaitLog.setLogFile("/vol/spr/faittestupod.log");
   
   String lib = "/research/people/spr/upod/lib/";
   String glib = lib + "google/lib/";
   String gdep = lib + "google/deps/";
   String cp = "/research/people/spr/upod/java";
   cp += File.pathSeparator + "/research/ivy/java";
   cp += File.pathSeparator + lib + "nanohttpd.jar";
   cp += File.pathSeparator + lib + "json.jar"; 
   cp += File.pathSeparator + lib + "stringtemplate.jar"; 
   cp += File.pathSeparator + lib + "velocity-1.7.jar"; 
   cp += File.pathSeparator + lib + "velocity-1.7-dep.jar";
   cp += File.pathSeparator + lib + "jsoup-1.7.2.jar";
   cp += File.pathSeparator + glib + "gdata-base-1.0.jar";
   cp += File.pathSeparator + glib + "gdata-calendar-2.0.jar"; 
   cp += File.pathSeparator + glib + "gdata-client-1.0.jar"; 
   cp += File.pathSeparator + glib + "gdata-core-1.0.jar";
   cp += File.pathSeparator + gdep + "guava-11.0.2.jar";
   cp += File.pathSeparator + gdep + "jsr305.jar";
   cp += File.pathSeparator + "/research/s6/public/batik-1.7/batik.jar";
   File bdir = new File("/research/s6/public/batik-1.7/lib");
   for (File f : bdir.listFiles()) {
      String fnm = f.getPath();
      if (fnm.contains("batik-") && fnm.endsWith(".jar")) {
         cp += File.pathSeparator + fnm;
       }
    }
   
   
   IfaceProject proj = IfaceControl.Factory.createSimpleProject(cp,
         "edu.brown.cs.");
   fait_control = IfaceControl.Factory.createControl(proj);
   
   start_classes = new ArrayList<String>();
   start_classes.add("edu.brown.cs.upod.smartsign.SmartSignMain");
   
   fait_control.analyze(1,false,ReportOption.FULL_STATS);
   
   showResults();
}

// @Test
public void flowTestS6()
{
   String cp = "/pro/s6/java";
   cp += File.pathSeparator + "/pro/ivy/java";
   cp += File.pathSeparator + "/pro/ivy/lib";
   cp += System.getenv("ECLIPSEPATH");
   cp += File.pathSeparator + "/pro/s6/lib/json.jar";
   cp += File.pathSeparator + "/pro/ivy/lib/asm5.jar";
   cp += File.pathSeparator + "/pro/s6/lib/junit.jar";
   cp += File.pathSeparator + "/pro/s6/lib/jsoup.jar";
   cp += File.pathSeparator + "/pro/s6/lib/ddmlib.jar";
   cp += File.pathSeparator + "/pro/s6/lib/jtar-1.1.jar";
      
   IfaceProject proj = IfaceControl.Factory.createSimpleProject(cp,"edu.brown.cs.");
   fait_control = IfaceControl.Factory.createControl(proj);

   start_classes = new ArrayList<String>();
   start_classes.add("edu.brown.cs.s6.engine.EngineMain");

   FaitLog.setLogLevel(FaitLog.LogLevel.DEBUG);
   FaitLog.setLogFile("/vol/spr/faittests6.log");
   
   fait_control.analyze(1,false,ReportOption.FULL_STATS);
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
	 Collection<IfaceProgramPoint> cins = ic.getErrorLocations();
	 if (cins != null && !cins.isEmpty()) {
	    FaitLog.logD("Errors for " + ic.getLogName() + ":");
	    for (IfaceProgramPoint fi : cins) {
               Collection<IfaceError> errs = ic.getErrors(fi);
               if (errs != null) {
                  for (IfaceError err : errs) {
                     FaitLog.logD1("OP: " + fi.toString() + " " +
                           err.getErrorLevel() + " " + err.getErrorMessage());
                   }
                }
	     }
	  }
       }
    }
   for (IfaceCall ic : fait_control.getAllCalls()) {
      if (fait_control.isInProject(ic.getMethod())) {
	 Collection<IfaceProgramPoint> cins = ic.getErrorLocations();
	 if (cins != null && !cins.isEmpty()) {
	    FaitLog.logI1("Errors for " + ic.getLogName() + ":");
	    for (IfaceProgramPoint fi : cins) {
               Collection<IfaceError> errs = ic.getErrors(fi);
               if (errs != null) {
                  for (IfaceError err : errs) {
                     FaitLog.logD1("OP: " + fi.toString() + " " +
                           err.getErrorLevel() + " " + err.getErrorMessage());
                   }
                }
	     }
	  }
       }
    }
}



}	// end of class FlowTest




/* end of FlowTest.java */

