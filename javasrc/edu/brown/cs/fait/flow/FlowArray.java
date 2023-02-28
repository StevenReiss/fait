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
/*	Private Storage							*/
/*										*/
/********************************************************************************/

private IfaceControl		fait_control;
private FlowQueue		flow_queue;

private Map<IfaceEntity,Set<FlowLocation>> entity_map;
private Map<IfaceEntity,Map<FlowLocation,Integer>> setter_map;



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
   setter_map = new HashMap<>();
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
   else {
      if (as.setArraySize(sz)) {
         noteArrayChange(as);
       }
    }

   acls = as.getDataType();
 //  acls = acls.getArrayType();

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
   if (arr.isBad()) 
      return arr;
   
   for (IfaceEntity xe : arr.getEntities()) {
      if (xe.getDataType().isArrayType()) {
	 addReference(xe,loc);
       }
    }
   
   cv = arr.getArrayContents(idx);
   
   if (cv == null || cv.getDataType().isVoidType()) {
      FaitLog.logE("Void type from array access");
    }
   
   IfaceType ft = cv.getDataType();
   IfaceType rt = ft.getComputedType(cv,FaitOperator.ELEMENTACCESS,arr,idx);
   if (rt != ft) {
      ft.checkCompatibility(rt,loc,cv,-1);
      cv = cv.changeType(rt);
    }

   if (FaitLog.isTracing()) FaitLog.logD1("Array access " + arr + "[" + idx + "] = " + cv);
   
   return cv;
}


IfaceValue handleArrayLength(FlowLocation loc,IfaceValue arr)
{
   IfaceValue v1 = arr.getArrayLength();
   
   if (FaitLog.isTracing()) 
      FaitLog.logD1("Array Length " + arr + " = " + v1);
   
   for (IfaceEntity xe : arr.getEntities()) {
      if (xe.getDataType().isArrayType()) {
         addReference(xe,loc);
       }
    }
   
   return v1;
}



/********************************************************************************/
/*										*/
/*	Methods to handle storing into array					*/
/*										*/
/********************************************************************************/

void handleArraySet(FlowLocation loc,IfaceValue arr,IfaceValue val,IfaceValue idx,int stackref)
{
   if (val.isBad()) return;

   if (FaitLog.isTracing()) FaitLog.logD1("Add to array set " + arr + "[" + idx + "] = " + val);
   
   // IfaceType btyp = arr.getDataType().getBaseType();
   // need declared type of the array, not the current type
   // val = FlowScanner.checkAssignment(val,btyp,loc);

   for (IfaceEntity ce : arr.getEntities()) {
      if (ce.getDataType().isArrayType()) {
         addReference(ce,loc,stackref);
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


Collection<IfaceAuxReference> getSetterRefs(IfaceValue arr)
{
   if (arr == null) return null;
   if (arr.isReference()) {
      arr = arr.getRefBase();
    }
   List<IfaceAuxReference> rslt = null;
   for (IfaceEntity ent : arr.getEntities()) {
      if (ent.getDataType().isArrayType()) {
         IfaceType btyp = ent.getDataType().getBaseType();
         Map<FlowLocation,Integer> sets = setter_map.get(ent);
         if (sets != null) {
            if (rslt == null) rslt = new ArrayList<>();
            for (Map.Entry<FlowLocation,Integer> set : sets.entrySet()) {
               IfaceValue refv = fait_control.findRefStackValue(btyp,set.getValue());
               IfaceAuxReference ref = fait_control.getAuxReference(set.getKey(),refv,
                     IfaceAuxRefType.ARRAY_REF);
               rslt.add(ref);
             }
          }
         if (btyp.isArrayType()) {
           IfaceValue aval = ent.getArrayValue(null,fait_control);
           if (aval != null) {
              Collection<IfaceAuxReference> nrefs = getSetterRefs(aval);
              if (nrefs != null && !nrefs.isEmpty()) {
                 if (rslt == null) rslt = new ArrayList<>();
                 rslt.addAll(nrefs);
               }
            }
          }
       }
    }
   
   return rslt;
}

/********************************************************************************/
/*										*/
/*	Handler reference associations						*/
/*										*/
/********************************************************************************/

private void addReference(IfaceEntity ent,FlowLocation loc)
{
   if (loc == null) return;
   
   synchronized (entity_map) {
      Set<FlowLocation> locs = entity_map.get(ent);
      if (locs == null) {
	 locs = new HashSet<>(4);
	 entity_map.put(ent,locs);
       }
      locs.add(loc);
    }
}



private void addReference(IfaceEntity ent,FlowLocation loc,int stackref)
{
   if (loc == null || stackref < 0) return;
   
   synchronized (entity_map) {
      Map<FlowLocation,Integer> locs = setter_map.get(ent);
      if (locs == null) {
	 locs = new HashMap<>(4);
	 setter_map.put(ent,locs);
       }
      locs.put(loc,stackref);
    }
}




void noteArrayChange(IfaceEntity arr)
{
   Collection<FlowLocation> locs = null;

   synchronized (entity_map) {
      locs = entity_map.get(arr);
      if (locs != null) locs = new ArrayList<>(locs);
    }

   if (locs == null) return;
   
   for (FlowLocation loc : locs) {
      flow_queue.queueMethodChange(loc.getCall(),loc.getProgramPoint());
      if (FaitLog.isTracing()) FaitLog.logD1("Array change method " + loc);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

void handleUpdate(IfaceUpdater upd)
{
   Collection<IfaceEntity> rement = upd.getEntitiesToRemove();
   for (Iterator<Map.Entry<IfaceEntity,Set<FlowLocation>>> it = entity_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<IfaceEntity,Set<FlowLocation>> ent = it.next();
      IfaceEntity ie = ent.getKey();
      if (rement.contains(ie)) {
         it.remove();
         continue;
       }
      int ct = 0;
      for (Iterator<FlowLocation> it1 = ent.getValue().iterator(); it1.hasNext(); ) {
         FlowLocation loc = it1.next();
         if (upd.isLocationRemoved(loc)) it1.remove();
         else ++ct;
      }
      if (ct == 0) it.remove();
    }
   
   for (Iterator<Map.Entry<IfaceEntity,Map<FlowLocation,Integer>>> it = setter_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<IfaceEntity,Map<FlowLocation,Integer>> ent = it.next();
      IfaceEntity ie = ent.getKey();
      if (rement.contains(ie)) {
         it.remove();
         continue;
       }
      int ct = 0;
      for (Iterator<FlowLocation> it1 = ent.getValue().keySet().iterator(); it1.hasNext(); ) {
         FlowLocation loc = it1.next();
         if (upd.isLocationRemoved(loc)) it1.remove();
         else ++ct;
       }
      if (ct == 0) it.remove();
    }
}



}	// end of class FlowArray




/* end of FlowArray.java */








































































































































































































































































































































































