/********************************************************************************/
/*										*/
/*		StateTest.java							*/
/*										*/
/*	Tests for FAIT state management 					*/
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



package edu.brown.cs.fait.state;

import edu.brown.cs.fait.iface.*;

import org.junit.*;



public class StateTest implements FaitConstants
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

public StateTest()
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

@Test public void stackCheck()
{
   IfaceState s1 = fait_control.createState(4);
   IfaceType t1 = fait_control.findDataType("int");
   
   IfaceValue v1 = fait_control.findRangeValue(t1,1,1);
   IfaceValue v2 = fait_control.findRangeValue(t1,2,2);
   s1.pushStack(v1);
   s1.pushStack(v2);
   s1.pushStack(v1);
   s1.handleDup(false,0);
   Assert.assertSame(s1.popStack(),v1);
   Assert.assertSame(s1.popStack(),v1);
   Assert.assertSame(s1.popStack(),v2);
   Assert.assertSame(s1.popStack(),v1);
}



}	// end of class StateTest




/* end of StateTest.java */


