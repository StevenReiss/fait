/********************************************************************************/
/*										*/
/*		FlowScanner.java						*/
/*										*/
/*	Scanner to symbolically execute a method				*/
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
import edu.brown.cs.ivy.jcode.JcodeConstants;


abstract class FlowScanner implements FlowConstants, JcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected FlowQueue		flow_queue;
protected IfaceControl		fait_control;

protected static IfaceError CALL_NEVER_RETURNS;
protected static IfaceError BRANCH_NEVER_TAKEN;
protected static IfaceError DEREFERENCE_NULL;
protected static IfaceError UNREACHABLE_CODE;

static {
   CALL_NEVER_RETURNS = new FaitError(ErrorLevel.WARNING,"Call never returns");
   BRANCH_NEVER_TAKEN = new FaitError(ErrorLevel.NOTE,"Branch never taken");
   DEREFERENCE_NULL = new FaitError(ErrorLevel.WARNING,"Attempt to dereference null");
   UNREACHABLE_CODE = new FaitError(ErrorLevel.NOTE,"Unreachable code");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowScanner(IfaceControl fc,FlowQueue fq)
{
   fait_control = fc;
   flow_queue = fq;
}



/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

void scanCode(FlowQueueInstanceByteCode wq)
{
   throw new Error("Attempt to use wrong scanner");
}


void scanCode()
{
   throw new Error("Attempt to use wrong scanner");
}


/********************************************************************************/
/*                                                                              */
/*      Common methods                                                          */
/*                                                                              */
/********************************************************************************/

protected IfaceEntity getLocalEntity(IfaceCall call,IfaceProgramPoint inspt)
{
   IfaceEntity ns = call.getBaseEntity(inspt);
   
   if (ns == null) {
      IfaceType tyr = inspt.getReferencedType();
      IfacePrototype pt = fait_control.createPrototype(tyr);
      if (pt != null) {
	 ns = fait_control.findPrototypeEntity(tyr,pt,
	       new FlowLocation(flow_queue,call,inspt));
       }
      // might want to create fixed source for non-project methods
      else {
	 ns = fait_control.findLocalEntity(new FlowLocation(flow_queue,call,inspt),tyr);
       }
      call.setBaseEntity(inspt,ns);
    }
   
   return ns;
}



protected void setLocal(IfaceLocation loc,
      IfaceState state,int slot,IfaceValue val)
{
   IfaceMethod ic = loc.getMethod();
   IfaceType typ = ic.getLocalType(slot,loc.getProgramPoint());
   val = checkAssignment(val,typ,loc);
   
   state.setLocal(slot,val);
}


static IfaceValue checkAssignment(IfaceValue val,IfaceType typ,IfaceLocation loc)
{
   if (typ != null) {
      if (!val.getDataType().checkCompatibility(typ,loc)) {
         IfaceValue val1 = val.restrictByType(typ);
         if (val != val1) {
            if (!val.mustBeNull()) 
               FaitLog.logD("Value change on assignment " + val + " -> " + val1);
          }
       }
    }
   
   return val;
}



/********************************************************************************/
/*                                                                              */
/*      Implication methods                                                     */
/*                                                                              */
/********************************************************************************/

IfaceImplications getImpliedValues(IfaceValue v0,FaitOperator op,IfaceValue v1)
{
   return v0.getImpliedValues(v1,op);
   
}


}	// end of class FlowScanner




/* end of FlowScanner.java */

