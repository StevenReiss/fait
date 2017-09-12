/********************************************************************************/
/*										*/
/*		FlowFactory.java						*/
/*										*/
/*	Factory for setting up flow evaluation					*/
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
import edu.brown.cs.ivy.jcode.JcodeMethod;

import java.util.*;


public class FlowFactory implements FlowConstants
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

public FlowFactory(FaitControl fc)
{
   fait_control = fc;
}




/********************************************************************************/
/*										*/
/*	Methods to do initial analysis						*/
/*										*/
/********************************************************************************/

public void analyze(int nthread)
{
   Collection<JcodeMethod> start = fait_control.getStartMethods();
   FlowQueue fq = new FlowQueue(fait_control);
   List<IfaceValue> sargl = new LinkedList<IfaceValue>();
   sargl.add(fait_control.findMainArgsValue());

   for (JcodeMethod fm : start) {
      IfaceCall ic = fait_control.findCall(fm,sargl,InlineType.NONE);
      fq.queueMethodStart(ic);
    }

   FlowProcessor fp = new FlowProcessor(nthread,fait_control,fq);
   fp.process();
}




/********************************************************************************/
/*                                                                              */
/*      Methods to queue locations                                              */
/*                                                                              */
/********************************************************************************/

public void queueLocation(FaitLocation loc)
{
   FlowLocation fl = (FlowLocation) loc;
   fl.queueLocation();
}


public void handleCallback(FaitLocation frm,JcodeMethod fm,List<IfaceValue> args,String cbid) 
{
   frm.handleCallback(fm,args,cbid);
}
   


}	// end of class FlowFactory




/* end of FlowFactory.java */

