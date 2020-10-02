/********************************************************************************/
/*                                                                              */
/*              QueryContextRose.java                                           */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.fait.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceBackFlow;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.FaitConstants.FaitOperator;
import edu.brown.cs.fait.iface.FaitConstants.TestBranch;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryContextRose extends QueryContext implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<IfaceValue,Integer>         priority_map;
private Map<IfaceValue,IfaceValue>      known_values;
private IfaceValue                      base_reference;
private IfaceValue                      base_value;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryContextRose(IfaceControl ctrl,IfaceValue var,IfaceValue val)
{
   super(ctrl);
   base_reference = var;
   base_value = val;
   priority_map = new HashMap<>();
   known_values = new HashMap<>();
   if (var != null) {
      priority_map.put(var,10);
      if (val != null) known_values.put(var,val);
    }
}


private QueryContextRose(QueryContextRose ctx,Map<IfaceValue,Integer> pmap,Map<IfaceValue,IfaceValue> kmap)
{
   super(ctx.fait_control);
   base_reference = ctx.base_reference;
   base_value = ctx.base_value;
   priority_map = pmap;
   known_values = kmap;
}



/********************************************************************************/
/*                                                                              */
/*      Prior state methods                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected QueryBackFlowData getPriorStateContext(IfaceState backfrom,IfaceState backto)
{
   Map<IfaceValue,Integer> npmap = new HashMap<>();
   Map<IfaceValue,IfaceValue> kpmap = new HashMap<>();
   List<IfaceAuxReference> auxrefs = new ArrayList<>();
   
   for (Map.Entry<IfaceValue,Integer> ent : priority_map.entrySet()) {
      IfaceValue ref = ent.getKey();
      IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,ref);
      if (bf == null) continue;
      IfaceValue sref = bf.getStartReference();
      if (sref != null) {
         npmap.put(sref,ent.getValue());
         if (known_values.get(ref) != null) {
            kpmap.put(sref,known_values.get(ref));
          }
       }
      
      if (bf.getAuxRefs() != null) {
         int refval = ent.getValue() - 1;
         for (IfaceAuxReference auxref : bf.getAuxRefs()) {
            if (auxref.getLocation().equals(backto.getLocation())) {         
               npmap.put(auxref.getReference(),refval);
             }
            else {
               auxrefs.add(auxref);
             }
          }
       }
    }
   
   QueryContextRose nctx = null;
   if (npmap.equals(priority_map)) nctx = this;
   else if (!npmap.isEmpty()) nctx = new QueryContextRose(this,npmap,kpmap);
   
   if (auxrefs.isEmpty()) auxrefs = null;
   IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,auxrefs);
   
   return new QueryBackFlowData(nctx,bf);
}




@Override protected QueryContext getPriorContextForCall(IfaceCall c,IfaceProgramPoint pt)
{
   IfaceMethod fm = c.getMethod();
   int delta = (fm.isStatic() ? 0 : 1);
   int act = fm.getNumArgs();
   Map<IfaceValue,Integer> npmap = new HashMap<>();
   Map<IfaceValue,IfaceValue> nvmap = new HashMap<>();
   for (IfaceValue ref : priority_map.keySet()) {
      int slot = ref.getRefSlot();
      IfaceValue nref = ref;
      if (slot >= 0) {
         if (slot >= act+delta || slot == 0) continue;
         int stk = act+delta-slot-1;
         nref = fait_control.findRefStackValue(ref.getDataType(),stk);
         npmap.put(nref,priority_map.get(ref));
       }
      if (nref != null && known_values.get(ref) != null) {
         nvmap.put(nref,known_values.get(ref));
       }
    }

   if (npmap.isEmpty()) return null;
   return new QueryContextRose(this,npmap,nvmap);
}





@Override protected QueryContext getReturnContext(IfaceCall arg0)
{
   Map<IfaceValue,Integer> npmap = new HashMap<>();
   Map<IfaceValue,IfaceValue> nvmap = new HashMap<>();
   boolean useret = false;
   for (IfaceValue ref : priority_map.keySet()) {
      int slot = ref.getRefSlot();
      if (slot > 0) continue;           // ignore stack other than return value
      else if (slot == 0) {
         useret = true;
         npmap.put(ref,priority_map.get(ref));
       }
      if (known_values.get(ref) != null) {
         nvmap.put(ref,known_values.get(ref));
       }
    }
   
   if (!useret || npmap.isEmpty()) return null;
   
   return new QueryContextRose(this,npmap,nvmap);
}



@Override protected void addRelevantArgs(IfaceState st0,QueryBackFlowData bfd)
{
   IfaceProgramPoint pt = st0.getLocation().getProgramPoint();
   IfaceMethod mthd = pt.getCalledMethod();
   if (mthd == null) return;
   int ct = mthd.getNumArgs();
   
   boolean retused = false;
   boolean thisused = false;
   for (IfaceValue ref : priority_map.keySet()) {
      int slot = ref.getRefSlot();
      if (slot == 0) retused = true;
      if (slot == ct) thisused = true;
    }
   if (retused && mthd.getReturnType() != null &&
         !mthd.getReturnType().isVoidType()) {
      int ct1 = (mthd.isStatic() ? 0 : 1);
      for (int i = 0; i < ct+ct1; ++i) {
         IfaceValue vs = st0.getStack(i);
         vs = QueryFactory.dereference(fait_control,vs,st0);
         IfaceValue vr = fait_control.findRefStackValue(vs.getDataType(),i);
         IfaceAuxReference ref = fait_control.getAuxReference(st0.getLocation(),vr);
         bfd.addAuxReference(ref);
       }
      if (!mthd.isStatic()) {
         IfaceValue thisv = st0.getStack(ct);
         if (thisv != null) thisv = QueryFactory.dereference(fait_control,thisv,st0);
         if (thisv != null) {
            for (IfaceEntity ent : thisv.getEntities()) {
               IfacePrototype proto = ent.getPrototype();
               if (proto != null) {
                  List<IfaceAuxReference> refs = proto.getSetLocations(fait_control);
                  if (refs != null) {
                     for (IfaceAuxReference aref : refs) {
                        bfd.addAuxReference(aref);
                      }
                   }
                }
             }
          }
       }
    }
   else if (!mthd.isStatic()) {
      IfaceValue v0 = st0.getStack(ct);
      if (v0 != null && thisused) {
         for (int i = 0; i < ct; ++i) {
            IfaceValue vs = st0.getStack(i);
            IfaceValue vr = fait_control.findRefStackValue(vs.getDataType(),i);
            IfaceAuxReference ref = fait_control.getAuxReference(st0.getLocation(),vr);
            bfd.addAuxReference(ref);
          }
       }
    }
}




@Override protected List<QueryContext> getTransitionContext(IfaceState arg0)
{
   // method body goes here
   
   return null;
}



@Override protected boolean handleInternalCall(IfaceState st0,QueryBackFlowData bfd,QueryNode n)
{
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Relevancy methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override protected boolean isPriorStateRelevant(IfaceState state)
{
   for (Map.Entry<IfaceValue,IfaceValue> ent : known_values.entrySet()) {
      IfaceValue ref = ent.getKey();
      IfaceValue val = QueryFactory.dereference(fait_control,ref,state);
      if (val == null || val.isReference()) continue;
      TestBranch tb = val.branchTest(ent.getValue(),FaitOperator.EQL);
      if (tb == TestBranch.NEVER) return false;
    }
   
   return true;
}


@Override protected String addToGraph(QueryContext priorctx,IfaceState state)
{
   QueryContextRose prior = (QueryContextRose) priorctx;
   for (Map.Entry<IfaceValue,Integer> ent : prior.priority_map.entrySet()) {
      IfaceValue ref = ent.getKey();
      Integer opri = priority_map.get(ref);
      if (opri == null) return "Value Computed";
      else if (opri != ent.getValue()) return "Value Changed";
    }
   
   return null;
}


@Override protected boolean isReturnRelevant(IfaceState st0,IfaceCall call)
{
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected void localOutputXml(IvyXmlWriter arg0,IfaceProgramPoint arg1)
{
   // method body goes here
}



@Override protected String localDisplayContext()
{
   return "";
}



/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object arg0)
{
   // method body goes here

   return false;
}



@Override public int hashCode()
{
   // method body goes here

   return 0;
}



}       // end of class QueryContextRose




/* end of QueryContextRose.java */

