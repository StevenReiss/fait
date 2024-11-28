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

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProject;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class ProtoTest implements FaitConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl	fait_control;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ProtoTest()
{
   IfaceProject proj = IfaceControl.Factory.createSimpleProject("/home/spr/sampler",
         "spr.onsets.");
   fait_control = IfaceControl.Factory.createControl(proj);
}



/********************************************************************************/
/*										*/
/*	Simple Tests								*/
/*										*/
/********************************************************************************/

@Test public void mapTest()
{
   IfaceType dt = fait_control.findDataType("java.util.ArrayList");
   IfacePrototype p1 = fait_control.createPrototype(dt);
   Assert.assertNotNull(p1);
   IfaceMethod fm = fait_control.findMethod("java.util.ArrayList","add","(Ljava/lang/Object;)Z");
   Assert.assertNotNull(fm);
   Assert.assertTrue(p1.isMethodRelevant(fm));

   IfaceType t1 = fait_control.findDataType("spr.onsets.OnsetMain");
   IfaceValue v1 = fait_control.findAnyValue(t1);
   List<IfaceValue> args = new ArrayList<IfaceValue>();
   args.add(fait_control.findAnyValue(dt));
   args.add(v1);
   p1.handleCall(fm,args,null);
   IfaceMethod fm2 = fait_control.findMethod("java.util.ArrayList","get","(I)");
   Assert.assertNotNull(fm2);
   Assert.assertTrue(p1.isMethodRelevant(fm2));
   IfaceType t2 = fait_control.findDataType("int");
   IfaceValue v3 = fait_control.findConstantValue(t2,0);
   args.set(1,v3);
   IfaceValue v4 = p1.handleCall(fm2,args,null);
   IfaceValue v5 = v1.allowNull();
   Assert.assertSame(v4,v5);
}






}	// end of class ProtoTest




/* end of ProtoTest.java */


