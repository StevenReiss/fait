/********************************************************************************/
/*										*/
/*		ValueTest.java							*/
/*										*/
/*	Tests for FAIT values							*/
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



package edu.brown.cs.fait.value;

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceProject;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import org.junit.Assert;
import org.junit.Test;

public class ValueTest implements FaitConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl	fait_control;
private IfaceValue [] value_set = new IfaceValue[64];



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ValueTest()
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

@Test public void createValues()
{
   IfaceType t1 = fait_control.findDataType("spr.onsets.OnsetMain");
   IfaceType t2 = fait_control.findConstantType("int",5);
   IfaceType t3 = fait_control.findDataType("spr.onsets.OnsetExprSet$Expr");
   value_set[0] = fait_control.findAnyValue(t2);
   value_set[1] = fait_control.findAnyValue(t1);
   value_set[2] = fait_control.findRangeValue(t2,Long.valueOf(1),Long.valueOf(2));

   IfaceEntity e1 = fait_control.findFixedEntity(t1);
   IfaceEntitySet s1 = fait_control.createSingletonSet(e1);
   value_set[3] = fait_control.findObjectValue(t1,s1,FaitAnnotation.NON_NULL);
   value_set[4] = fait_control.findObjectValue(t1,s1,FaitAnnotation.NON_NULL);
   Assert.assertSame("duplicate calls",value_set[3],value_set[4]);

   value_set[4] = fait_control.findEmptyValue(t1,FaitAnnotation.MUST_BE_NULL);
   value_set[5] = fait_control.findConstantStringValue();
   value_set[6] = fait_control.findConstantStringValue("HELLO WORLD");
   value_set[7] = fait_control.findMainArgsValue();
   value_set[8] = fait_control.findNullValue();
   value_set[9] = fait_control.findNullValue(t1);
   value_set[10] = fait_control.findBadValue();
   value_set[11] = fait_control.findNativeValue(t1);
   value_set[12] = fait_control.findMutableValue(t1);
   value_set[13] = fait_control.findAnyObjectValue();
   value_set[14] = fait_control.findAnyNewObjectValue();

   value_set[15] = fait_control.findRangeValue(fait_control.findDataType("double"),1.0,2.0);
   value_set[16] = fait_control.findAnyValue(fait_control.findDataType("double"));
   Assert.assertSame("Float ignores range",value_set[15],value_set[16]);

   
   IfaceField f1 = fait_control.findField(t1,"card_deck"); 
   IfaceField f2 = fait_control.findField(t1,"target_size");
   IfaceField f3 = fait_control.findField(t3,"expr_text");
   value_set[17] = fait_control.findInitialFieldValue(f1,false);
   value_set[18] = fait_control.findInitialFieldValue(f2,false);
   value_set[19] = fait_control.findInitialFieldValue(f3,false);

   value_set[20] = fait_control.findAnyValue(fait_control.findDataType("float"));
   value_set[21] = value_set[20].mergeValue(value_set[15]);
   Assert.assertNotSame("Merge float and double",value_set[20],value_set[21]);
   Assert.assertSame("Merge float and double",value_set[21],value_set[15]);

   value_set[22] = fait_control.findConstantValue(fait_control.findDataType("int"),2);
   value_set[23] = value_set[22].performOperation(t2,value_set[2],FaitOperator.SUB,null);    // 2 - {1,2}
   value_set[24] = value_set[23].performOperation(t2,value_set[22],FaitOperator.MUL,null);    // 2*(2-{1,2})
   value_set[25] = fait_control.findRangeValue(t2,Long.valueOf(0),Long.valueOf(2));
   Assert.assertSame("Integer arithmetic",value_set[24],value_set[25]);
   value_set[26] = fait_control.findAnyValue(fait_control.findDataType("byte"));
   value_set[27] = value_set[26].mergeValue(value_set[25]);
   Assert.assertSame("Integer merge",value_set[27],value_set[0]);
}




}	// end of class ValueTest




/* end of ValueTest.java */

