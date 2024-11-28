/********************************************************************************/
/*										*/
/*		CallTest.java							*/
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



package edu.brown.cs.fait.call;

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProject;
import edu.brown.cs.fait.iface.IfaceSpecial;
import org.junit.Assert;
import org.junit.Test;



public class CallTest implements FaitConstants
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

public CallTest()
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
      
@Test public void callCheck()
{ 
   IfaceMethod fm1 = fait_control.findMethod("java.util.regex.Pattern","matcher",null);
   IfaceMethod fm2 = fait_control.findMethod("java.lang.StringBuilder","toString",null);
   IfaceMethod fm3 = fait_control.findMethod("java.lang.Object","notify",null);

   IfaceSpecial s1 = fait_control.getCallSpecial(null,fm1);
   IfaceSpecial s2 = fait_control.getCallSpecial(null,fm2);
   IfaceSpecial s3 = fait_control.getCallSpecial(null,fm3);

   Assert.assertNotNull("matcher should be special",s1);
   Assert.assertNotNull("StringBuilder.toString should be special",s2);
   Assert.assertNull("Object.notify should not be special",s3);
}





}	// end of class CallTest




/* end of CallTest.java */


