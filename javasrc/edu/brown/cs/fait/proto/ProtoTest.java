/********************************************************************************/
/*										*/
/*		ProtoTest.java							*/
/*										*/
/*	Tests for FAIT prototypes						*/
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



package edu.brown.cs.fait.proto;

import edu.brown.cs.fait.iface.*;

import org.junit.*;

import java.util.*;
import java.io.*;


public class ProtoTest implements FaitConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl	fait_control;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ProtoTest()
{
   fait_control = FaitControl.Factory.getControl();

   fait_control.setProject(new TestProject());
}



/********************************************************************************/
/*										*/
/*	Simple Tests								*/
/*										*/
/********************************************************************************/

@Test public void mapTest()
{
   FaitDataType dt = fait_control.findDataType("Ljava/util/ArrayList;");
   IfacePrototype p1 = fait_control.createPrototype(dt);
   Assert.assertNotNull(p1);
   FaitMethod fm = fait_control.findMethod("java.util.ArrayList","add","(Ljava/lang/Object;)Z");
   Assert.assertNotNull(fm);
   Assert.assertTrue(p1.isMethodRelevant(fm));

   FaitDataType t1 = fait_control.findDataType("Lspr/onsets/OnsetMain;");
   IfaceValue v1 = fait_control.findAnyValue(t1);
   List<IfaceValue> args = new ArrayList<IfaceValue>();
   args.add(fait_control.findAnyValue(dt));
   args.add(v1);
   IfaceValue v2 = p1.handleCall(fm,args,null);
   
   v2 = fait_control.findAnyValue(dt);
   FaitMethod fm2 = fait_control.findMethod("java.util.ArrayList","get","(I)Ljava/lang/Object;");
   Assert.assertNotNull(fm2);
   Assert.assertTrue(p1.isMethodRelevant(fm2));
   FaitDataType t2 = fait_control.findDataType("I");
   IfaceValue v3 = fait_control.findRangeValue(t2,0,0);
   args.set(1,v3);
   IfaceValue v4 = p1.handleCall(fm2,args,null);
   IfaceValue v5 = v1.allowNull();
   Assert.assertSame(v4,v5);
}




/********************************************************************************/
/*										*/
/*	Project to test with							*/
/*										*/
/********************************************************************************/

private static class TestProject implements FaitProject {

   @Override public String getClasspath() {
      return "/home/spr/sampler";
    }

   @Override public Collection<String> getBaseClasses() {
      Collection<String> rslt = new ArrayList<String>();
      rslt.add("spr.onsets.OnsetMain");
      return rslt;
    }

   @Override public List<File> getDescriptionFile()		{ return null; }

   @Override public boolean isProjectClass(String cls) {
      if (cls.startsWith("spr.")) return true;
      return false;
    }
   @Override public Collection<String> getStartClasses()        { return null; }
   
   @Override public FaitMethodData createMethodData(FaitCall fc)   { return null; }
   
}	// end of inner class TestProject

}	// end of class ProtoTest




/* end of ProtoTest.java */


