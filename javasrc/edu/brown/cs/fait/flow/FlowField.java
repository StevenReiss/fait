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

private IfaceControl			fait_control;
private FlowQueue			flow_queue;
private Map<String,IfaceValue>	field_map;
private Map<String,Set<FlowLocation>> field_accessors;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowField(IfaceControl fc,FlowQueue fq)
{
   fait_control = fc;
   flow_queue = fq;
   field_map = new HashMap<>();
   field_accessors = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Methods to access field values						*/
/*										*/
/********************************************************************************/

IfaceValue handleFieldGet(FlowLocation loc,IfaceField fld,IfaceState st,boolean thisref,IfaceValue base)
{
   IfaceType ctyp = fld.getDeclaringClass();
   IfaceType ftyp = fld.getType();
   String key = fld.getKey();
   
   if (loc != null) {
      synchronized (field_accessors) {
	 Set<FlowLocation> locs = field_accessors.get(key);
	 if (locs == null) {
	    locs = new HashSet<FlowLocation>();
	    field_accessors.put(key,locs);
	  }
	 locs.add(loc);
       }
    }
   
   if (ctyp != null) {
      flow_queue.initialize(ctyp);
    }
   
   if (thisref) {
      IfaceValue v0 = st.getFieldValue(fld);
      if (v0 != null) {
	 return v0;
       }
    }
   
   synchronized (field_map) {
      IfaceValue v0 = field_map.get(key);
      boolean nat = (base == null ? false : base.isNative());
      if (v0 == null) {
	 v0 = fait_control.findInitialFieldValue(fld,nat);
	 if (!v0.mustBeNull()) flow_queue.initialize(ftyp);
	 field_map.put(key,v0);
       }
      else if (nat && v0.mustBeNull()) {
	 IfaceValue v1 = fait_control.findInitialFieldValue(fld,nat);
	 if (!v1.mustBeNull()) {
	    v0 = v0.mergeValue(v1);
            if (v0.getDataType().isVoidType())
               System.err.println("SET FIELD VOID");
	    field_map.put(key,v0);
	  }
       }
      
      if (base != null) {
         IfaceType ft = v0.getDataType();
         IfaceType rt = ft.getComputedType(v0,FaitOperator.FIELDACCESS,base);
         if (rt != ft) {
            ft.checkCompatibility(rt,loc);
            v0 = v0.changeType(rt);
          }
       }
      
      return v0;
    }
}





/********************************************************************************/
/*										*/
/*	Method to set field values						*/
/*										*/
/********************************************************************************/

void handleFieldSet(FlowLocation loc,IfaceField fld,IfaceState st,boolean thisref,
      IfaceValue v0,IfaceValue base)
{
   IfaceType ftyp = fld.getType();
   String key = fld.getKey();
   if (ftyp == null) return;
   
   IfaceCall mthd = loc.getCall();
   
   if (v0.mustBeNull()) {
      v0 = v0.restrictByType(ftyp);
    }
   
   v0 = FlowScanner.checkAssignment(v0,ftyp,loc);
   
   if (thisref || mthd.getMethod().isStaticInitializer()) {
      st.setFieldValue(fld,v0);
    }
   
   IfaceValue v2 = null;
   
   boolean chng = false;
   synchronized (field_map) {
      if (mthd != null && mthd.getMethod().isStaticInitializer() && !field_map.containsKey(key)) {
	 field_map.put(key,v0);
       }
      
      IfaceValue v1 = field_map.get(key);
      if (v1 == null || v1.getDataType().isVoidType()) {
	 boolean nat = (base == null ? false : base.isNative());
	 if (nat) v1 = fait_control.findInitialFieldValue(fld,nat);
	 // else v1 = fait_control.findInitialFieldValue(fld,nat);
	 else v1 = v0;
	 field_map.put(key,v1);
       }
      
      v2 = v1.mergeValue(v0);
      if (v2 != v1) {
        chng = true;
        field_map.put(key,v2);
       }
    }
   
   if (FaitLog.isTracing())
      FaitLog.logD1("Field " + key + " = " + v2 + " " + chng + " " + thisref + " " + v0);
   
   if (chng) handleFieldChanged(fld);
}



void initializeField(String name,IfaceType typ)
{
   String key = name;
   
   IfaceValue v1 = field_map.get(key);
   if (v1 == null || v1.getDataType().isVoidType()) {
      int idx = name.lastIndexOf(".");
      String fnm = name.substring(idx+1);
      String cnm = name.substring(0,idx);
      IfaceType ift = fait_control.findDataType(cnm);
      IfaceField ifld = fait_control.findField(ift,fnm);
      if (ifld != null && ifld.isStatic()) return;
      if (typ.isPrimitiveType()) {
         v1 = fait_control.findAnyValue(typ);
       }
      else {
         v1 = fait_control.findNullValue(typ);
       }
      if (FaitLog.isTracing()) {
         FaitLog.logD("Initialize field " + ifld + " = " + v1);
       }
      field_map.put(key,v1);
    }
}


void handleFieldChanged(IfaceField fld)
{
   Collection<FlowLocation> locs;

   synchronized (field_accessors) {
      locs = field_accessors.get(fld.getKey());
      if (locs == null) return;
      locs = new ArrayList<>(locs);
    }

   for (FlowLocation fl : locs) {
      if (FaitLog.isTracing()) FaitLog.logD1("Queue for field change " + fl);
      flow_queue.queueMethodChange(fl);
    }
}




}	// end of class FlowField




/* end of FlowField.java */

