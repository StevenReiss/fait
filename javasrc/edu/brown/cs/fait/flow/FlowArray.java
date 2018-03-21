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



class FlowArray implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private St	orage							*/
/*										*/
/********************************************************************************/

private IfaceControl		fait_control;
private FlowQueue		flow_queue;

private Map<IfaceEntity,Set<FlowLocation>> entity_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowArray(IfaceControl fc,FlowQueue fq)
{
   fait_control = fc;
   flow_queue = fq;
   entity_map = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Handle array value creation						*/
/*										*/
/********************************************************************************/

IfaceValue handleNewArraySet(FlowLocation loc,IfaceType acls,int ndim,IfaceValue sz)
{
   IfaceEntity as = loc.getCall().getArrayEntity(loc.getProgramPoint());

   if (as == null) {
      IfaceEntity as1 = null;
      IfaceType bcls = acls;
      for (int i = 0; i < ndim; ++i) {
	 as = fait_control.findArrayEntity(bcls,sz);
	 sz = null;
	 if (as1 != null) {
	    IfaceEntitySet es = fait_control.createSingletonSet(as1);
	    IfaceValue cv = fait_control.findObjectValue(bcls,es,FaitAnnotation.NON_NULL);
	    as.setArrayContents(cv);
	  }
	 bcls = bcls.getArrayType();
	 as1 = as;
       }
      loc.getCall().setArrayEntity(loc.getProgramPoint(),as);
    }

   acls = acls.getArrayType();

   if (FaitLog.isTracing()) FaitLog.logD1("Array set = " + as + " " + acls.getName());

   return fait_control.findObjectValue(acls,fait_control.createSingletonSet(as),FaitAnnotation.NON_NULL);
}



/********************************************************************************/
/*										*/
/*	Methods to handle array element load					*/
/*										*/
/********************************************************************************/

IfaceValue handleArrayAccess(FlowLocation loc,IfaceValue arr,IfaceValue idx)
{
   IfaceValue cv = null;
   
   for (IfaceEntity xe : arr.getEntities()) {
      if (xe.getDataType().isArrayType()) {
	 addReference(xe,loc);
       }
    }
   
   cv = arr.getArrayContents(idx);

   if (FaitLog.isTracing()) FaitLog.logD1("Array access " + arr + "[" + idx + "] = " + cv);
   
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

   if (FaitLog.isTracing()) FaitLog.logD1("Add to array set " + arr + "[" + idx + "] = " + val);
   
   // IfaceType btyp = arr.getDataType().getBaseType();
   // need declared type of the array, not the current type
   // val = FlowScanner.checkAssignment(val,btyp,loc);

   for (IfaceEntity ce : arr.getEntities()) {
      if (ce.getDataType().isArrayType()) {
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
	 locs = new HashSet<>(4);
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
      flow_queue.queueMethodChange(loc.getCall(),loc.getProgramPoint());
      if (FaitLog.isTracing()) FaitLog.logD1("Array change method " + loc);
    }
}




}	// end of class FlowArray




/* end of FlowArray.java */








































































































































































































































































































































































