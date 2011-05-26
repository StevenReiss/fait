/********************************************************************************/
/*                                                                              */
/*              FlowTest.java                                                   */
/*                                                                              */
/*      Tests for flow analysis                                                 */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.fait.flow;

import edu.brown.cs.fait.iface.*;

import org.junit.*;

import java.util.*;
import java.io.*;


public class FlowTest implements FlowConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private FaitControl     fait_control;
private Collection<String> start_classes;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public FlowTest()
{
   fait_control = FaitControl.Factory.getControl();
   
   fait_control.setProject(new TestProject());
   start_classes = null;
}
   



/********************************************************************************/
/*                                                                              */
/*      Test case for onsets                                                    */
/*                                                                              */
/********************************************************************************/

@Test public void flowTestOnsets()
{
   start_classes = new ArrayList<String>();
   start_classes.add("spr.onsets.OnsetMain");
   
   fait_control.analyze();
}



/********************************************************************************/
/*                                                                              */
/*      Test project                                                            */
/*                                                                              */
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
@Override public Collection<String> getStartClasses()        { return start_classes; }

@Override public List<File> getDescriptionFile()		{ return null; }

@Override public boolean isProjectClass(String cls) {
   if (cls.startsWith("spr.")) return true;
   return false;
}

@Override public FaitMethodData createMethodData(FaitCall fc)   { return null; }

}	// end of inner class TestProject

}       // end of class FlowTest




/* end of FlowTest.java */

