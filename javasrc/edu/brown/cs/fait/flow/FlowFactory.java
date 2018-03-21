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

import java.util.*;


public class FlowFactory implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl	fait_control;
private FlowQueue       flow_queue;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public FlowFactory(IfaceControl fc)
{
   fait_control = fc;
   flow_queue = new FlowQueue(fc);
}




/********************************************************************************/
/*										*/
/*	Methods to do initial analysis						*/
/*										*/
/********************************************************************************/

public void analyze(int nthread,boolean update)
{
   if (!update) {
      Collection<IfaceMethod> start = fait_control.getStartMethods();
      List<IfaceValue> sargl = new LinkedList<IfaceValue>();
      sargl.add(fait_control.findMainArgsValue());
      for (IfaceMethod fm : start) {
         IfaceCall ic = fait_control.findCall(null,fm,sargl,InlineType.NONE);
         flow_queue.queueMethodStart(ic,null);
       }
    }

   FlowProcessor fp = new FlowProcessor(nthread,fait_control,flow_queue);
   fp.process();
}




/********************************************************************************/
/*                                                                              */
/*      Methods to queue locations                                              */
/*                                                                              */
/********************************************************************************/

public void queueLocation(IfaceLocation loc)
{
   FlowLocation fl = (FlowLocation) loc;
   fl.queueLocation();
}

public void queueMethodCall(IfaceCall ic,IfaceProgramPoint pt)
{
   flow_queue.queueMethodChange(ic,pt);
}


/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

public void handleStateUpdate(IfaceUpdater upd)
{
   flow_queue.handleUpdate(upd);
}



public void handleCallback(IfaceLocation frm,IfaceMethod fm,List<IfaceValue> args,String cbid) 
{
   frm.handleCallback(fm,args,cbid);
}
   


}	// end of class FlowFactory




/* end of FlowFactory.java */

