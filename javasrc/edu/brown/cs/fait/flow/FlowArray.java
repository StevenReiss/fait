/********************************************************************************/
/*										*/
/*		FlowArray.java							*/
/*										*/
/*	Handler for array values						*/
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


class FlowArray implements FlowConstants, FaitOpcodes
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl		fait_control;
private FlowQueue		flow_queue;

private Map<IfaceEntity,Set<FlowLocation>> entity_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowArray(FaitControl fc,FlowQueue fq)
{
   fait_control = fc;
   flow_queue = fq;
   entity_map = new HashMap<IfaceEntity,Set<FlowLocation>>();
}



/********************************************************************************/
/*										*/
/*	Handle array value creation						*/
/*										*/
/********************************************************************************/

IfaceValue handleNewArraySet(FlowLocation loc,FaitDataType acls,int ndim,IfaceValue sz)
{
   IfaceEntity as = loc.getCall().getArrayEntity(loc.getInstruction());

   if (as == null) {
      IfaceEntity as1 = null;
      FaitDataType bcls = acls;
      for (int i = 0; i < ndim; ++i) {
	 as = fait_control.findArrayEntity(bcls,sz);
	 sz = null;
	 if (as1 != null) {
	    IfaceEntitySet es = fait_control.createSingletonSet(as1);
	    IfaceValue cv = fait_control.findObjectValue(bcls,es,NullFlags.NON_NULL);
	    as.setArrayContents(cv);
	  }
	 bcls = bcls.getArrayType();
	 as1 = as;
       }
      loc.getCall().setArrayEntity(loc.getInstruction(),as);
    }

   acls = acls.getArrayType();

   IfaceLog.logD1("Array set = " + as + " " + acls.getName());

   return fait_control.findObjectValue(acls,fait_control.createSingletonSet(as),NullFlags.NON_NULL);
}



/********************************************************************************/
/*										*/
/*	Methods to handle array element load					*/
/*										*/
/********************************************************************************/

IfaceValue handleArrayAccess(FlowLocation loc,IfaceValue arr,IfaceValue idx)
{
   IfaceValue cv = null;
   boolean nat = false;

   for (IfaceEntity xe : arr.getEntities()) {
      if (xe.getDataType().isArray()) {
	 addReference(xe,loc);
	 IfaceValue cv1 = (IfaceValue) xe.getArrayValue(idx);
	 if (cv == null) cv = cv1;
	 else cv = cv.mergeValue(cv1);
       }
      else if (xe.isNative()) nat = true;
    }

   if (cv == null) {
      FaitDataType base = null;
      switch (loc.getInstruction().getOpcode()) {
	 default :
	 case AALOAD :
	    base = arr.getDataType();
	    if (base == null || !base.isArray())
	       base = fait_control.findDataType("Ljava/lang/Object;");
	    else base = base.getBaseDataType();
	    if (nat) cv = fait_control.findNativeValue(base);
	    else cv = fait_control.findNullValue(base);
	    break;
	 case BALOAD :
	    base = fait_control.findDataType("B");
	    break;
	 case CALOAD :
	    base = fait_control.findDataType("C");
	    break;
	 case DALOAD :
	    base = fait_control.findDataType("D");
	    break;
	 case FALOAD :
	    base = fait_control.findDataType("F");
	    break;
	 case IALOAD :
	    base = fait_control.findDataType("I");
	    break;
	 case LALOAD :
	    base = fait_control.findDataType("L");
	    break;
	 case SALOAD :
	    base = fait_control.findDataType("S");
	    break;
       }
      if (cv == null && base != null) cv = fait_control.findAnyValue(base);
    }

   return cv;
}



/********************************************************************************/
/*										*/
/*	Methods to handle storing into array					*/
/*										*/
/********************************************************************************/

void handleArraySet(FlowLocation loc,IfaceValue arr,IfaceValue val,IfaceValue idx)
{
   if (val.isBad()) return;

   IfaceLog.logD1("Add to array set " + arr + "[" + idx + "] = " + val);

   for (IfaceEntity ce : arr.getEntities()) {
      if (ce.getDataType().isArray()) {
	 if (ce.addToArrayContents(val,idx,loc)) {
	    noteArrayChange(ce);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle array copy						*/
/*										*/
/********************************************************************************/

void handleArrayCopy(List<IfaceValue> args,FlowLocation loc)
{
   if (args.size() < 3) return;
   IfaceValue src = args.get(0);
   IfaceValue dst = args.get(2);
   if (src == null || dst == null) return;

   IfaceValue sval = src.getArrayContents();
   if (sval != null) {
      for (IfaceEntity ent : src.getEntities()) {
	 addReference(ent,loc);
       }
      for (IfaceEntity ent : dst.getEntities()) {
	 if (ent.addToArrayContents(sval,null,loc)) {
	    noteArrayChange(ent);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Handler reference associations						*/
/*										*/
/********************************************************************************/

private void addReference(IfaceEntity ent,FlowLocation loc)
{
   synchronized (entity_map) {
      Set<FlowLocation> locs = entity_map.get(ent);
      if (locs == null) {
	 locs = new HashSet<FlowLocation>(4);
	 entity_map.put(ent,locs);
       }
      locs.add(loc);
    }
}



void noteArrayChange(IfaceEntity arr)
{
   Set<FlowLocation> locs = null;

   synchronized (entity_map) {
      locs = entity_map.get(arr);
    }

   if (locs == null) return;
   for (FlowLocation loc : locs) {
      flow_queue.queueMethodChange(loc.getCall(),loc.getInstruction());
      IfaceLog.logD1("Array change method " + loc);
    }
}




}	// end of class FlowArray




/* end of FlowArray.java */

