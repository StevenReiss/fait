/********************************************************************************/
/*										*/
/*		FlowField.java							*/
/*										*/
/*	Handle general field processing 					*/
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


class FlowField implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl			fait_control;
private FlowQueue			flow_queue;
private Map<FaitField,IfaceValue>	field_map;
private Map<FaitField,Set<FlowLocation>> field_accessors;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowField(FaitControl fc,FlowQueue fq)
{
   fait_control = fc;
   flow_queue = fq;
   field_map = new HashMap<FaitField,IfaceValue>();
   field_accessors = new HashMap<FaitField,Set<FlowLocation>>();
}



/********************************************************************************/
/*										*/
/*	Methods to access field values						*/
/*										*/
/********************************************************************************/

IfaceValue handleFieldGet(FlowLocation loc,IfaceState st,boolean thisref,IfaceValue base)
{
   FaitInstruction ins = loc.getInstruction();
   FaitField fld = ins.getFieldReference();
   if (fld == null)
      System.err.println("FIELD NOT FOUND");

   if (loc != null) {
      synchronized (field_accessors) {
	 Set<FlowLocation> locs = field_accessors.get(fld);
	 if (locs == null) {
	    locs = new HashSet<FlowLocation>();
	    field_accessors.put(fld,locs);
	  }
	 locs.add(loc);
       }
    }

   IfaceValue v0 = null;
   flow_queue.initialize(fld.getDeclaringClass());

   if (thisref) {
      v0 = st.getFieldValue(fld);
      if (v0 != null) {
	 return v0;
       }
    }

   synchronized (field_map) {
      v0 = field_map.get(fld);
      boolean nat = (base == null ? false : base.isNative());
      if (v0 == null) {
	 v0 = fait_control.findInitialFieldValue(fld,nat);
	 if (!v0.mustBeNull()) flow_queue.initialize(fld.getType());
	 field_map.put(fld,v0);
       }
      else if (nat && v0.mustBeNull()) {
	 IfaceValue v1 = fait_control.findInitialFieldValue(fld,nat);
	 if (!v1.mustBeNull()) {
	    v0 = v0.mergeValue(v1);
	    field_map.put(fld,v0);
	  }
       }
    }

   return v0;
}




/********************************************************************************/
/*										*/
/*	Method to set field values						*/
/*										*/
/********************************************************************************/

void handleFieldSet(FlowLocation loc,IfaceState st,boolean thisref,
      IfaceValue v0,IfaceValue base)
{
   FaitInstruction ins = loc.getInstruction();
   FaitField fld = ins.getFieldReference();
   IfaceCall mthd = loc.getCall();

   if (v0.mustBeNull()) {
      v0 = v0.restrictByType(fld.getType(),false,loc);
    }

   if (thisref || mthd.getMethod().isStaticInitializer()) {
      st.setFieldValue(fld,v0);
    }

   IfaceValue v2 = null;

   synchronized (field_map) {
      if (mthd.getMethod().isStaticInitializer() && !field_map.containsKey(fld)) {
	 field_map.put(fld,v0);
       }

      IfaceValue v1 = field_map.get(fld);
      if (v1 == null) {
	 boolean nat = (base == null ? false : base.isNative());
	 if (nat) v1 = fait_control.findInitialFieldValue(fld,nat);
	 // else v1 = fait_control.findInitialFieldValue(fld,nat);
	 else v1 = v0;
	 field_map.put(fld,v1);
       }

      v2 = v1.mergeValue(v0);
      if (v2 == v1) return;

      field_map.put(fld,v2);
    }

   IfaceLog.logD1("Field " + fld + " = " + v2);

   handleFieldChanged(fld);
}


void handleFieldChanged(FaitField fld)
{
   Set<FlowLocation> locs;

   synchronized (field_accessors) {
      locs = field_accessors.get(fld);
    }

   if (locs == null) return;
   for (FlowLocation fl : locs) {
      IfaceLog.logD1("Queue for field change " + fl);
      flow_queue.queueMethodChange(fl.getCall(),fl.getInstruction());
    }
}




}	// end of class FlowField




/* end of FlowField.java */

