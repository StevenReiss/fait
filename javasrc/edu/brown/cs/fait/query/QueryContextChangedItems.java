/********************************************************************************/
/*                                                                              */
/*              QueryChangedItemsContext.java                                   */
/*                                                                              */
/*      Query context for finding changed items                                 */
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.fait.iface.IfaceAstReference;
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
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryContextChangedItems extends QueryContext
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<VarData>    var_data;
private IfaceProgramPoint start_point;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryContextChangedItems(IfaceControl ctrl,IfaceProgramPoint pt)
{
   super(ctrl);
   var_data = new HashSet<>();
   start_point = pt;
}


private QueryContextChangedItems(QueryContextChangedItems ctx,Set<VarData> nset)
{
   super(ctx.fait_control);
   var_data = nset;
}



/********************************************************************************/
/*                                                                              */
/*      Prior state methods                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected QueryBackFlowData getPriorStateContext(IfaceState backfrom,IfaceState backto)
{
   Set<VarData> ndata = new HashSet<>();
   List<IfaceAuxReference> auxrefs = new ArrayList<>();
   
   for (VarData vd : var_data) {
      IfaceValue ref = vd.getReference();
      IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,ref,true);
      if (bf == null) continue;
      if (ref.getRefStack() >= 0) {
         IfaceValue sref = bf.getStartReference();
         if (sref != null) ndata.add(vd.changeReference(sref));
       }
      else ndata.add(vd);
      
      if (bf.getAuxRefs() != null) {
         for (IfaceAuxReference auxref : bf.getAuxRefs()) {
            if (auxref.getLocation().equals(backto.getLocation())) { 
               VarData nvd = new VarData(auxref.getReference(),vd);
               ndata.add(nvd);
             }
            else {
               // restrict to relevant methods
               auxrefs.add(auxref);
             }
          }
       }
    }
   
   IfaceProgramPoint pt0 = backto.getLocation().getProgramPoint();
   IfaceAstReference ast0 = pt0.getAstReference();
   if (ast0 != null) {
      ASTNode n = ast0.getAstNode();
      switch (n.getNodeType()) {
         case ASTNode.ASSIGNMENT :
            System.err.println("HANDLE ASSIGNMENT HERE");
            break;
         case ASTNode.IF_STATEMENT :
         case ASTNode.WHILE_STATEMENT :
         case ASTNode.DO_STATEMENT :
         case ASTNode.SWITCH_STATEMENT :
            System.err.println("HANDLE CONDITIONAL HERE");
            break;
       }
    }
   
   if (backto.isMethodCall()) {
      System.err.println("Copy data from return to args here");
    }
   
   QueryContextChangedItems nctx = null;
   if (ndata.equals(var_data)) nctx = this;
   else nctx = new QueryContextChangedItems(this,ndata);
   
   if (auxrefs.isEmpty()) auxrefs = null;
   IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,auxrefs);
   
   return new QueryBackFlowData(nctx,bf);
}




/********************************************************************************/
/*                                                                              */
/*      Handle calls                                                            */
/*                                                                              */
/********************************************************************************/

@Override protected QueryContext getPriorContextForCall(IfaceCall c,IfaceProgramPoint pt)
{
   // start of method -- handle return if needed
   
   IfaceMethod fm = c.getMethod();
   
   if (fm.equals(start_point.getMethod())) return null;
   
   int delta = (fm.isStatic() ? 0 : 1);
   int act = fm.getNumArgs();
   Set<VarData> ndata = new HashSet<>();
   for (VarData vd : var_data) {
      IfaceValue ref = vd.getReference();
      int slot = ref.getRefSlot();
      IfaceValue nref = ref;
      if (slot >= 0) {
         if (slot >= act+delta || slot == 0) continue;
         int stk = act+delta-slot-1;
         nref = fait_control.findRefStackValue(ref.getDataType(),stk);
         VarData nd = new VarData(nref,vd);
         ndata.add(nd);
       }
    }

   if (ndata.isEmpty()) return null;
   QueryContextChangedItems newctx = new QueryContextChangedItems(this,ndata);
   
   return newctx;
}



@Override protected QueryContext getReturnContext(IfaceCall arg0)
{
   // get context to use in the called method from a state 
   // for now, just assume if return is important, then all args are as well
   
   return null;
}



@Override protected List<QueryContext> getTransitionContext(IfaceState arg0)
{
   // check for program state transition

   return null;
}



@Override protected boolean isPriorStateRelevant(IfaceState arg0)
{
   return true;
}




@Override protected void addRelevantArgs(IfaceState st0,QueryBackFlowData bfd)
{
   IfaceProgramPoint pt = st0.getLocation().getProgramPoint();
   IfaceMethod mthd = pt.getCalledMethod();
   if (mthd == null) return;
   int ct = mthd.getNumArgs();
   int ct1 = (mthd.isStatic() ? 0 : 1);
   
   boolean retused = false;
   boolean thisused = false;
   for (VarData vd : var_data) {
      if (!vd.isRelevant()) continue;
      IfaceValue ref = vd.getReference();
      int slot = ref.getRefStack();
      if (slot == 0) retused = true;
      if (!mthd.isStatic() && slot == ct+1) thisused = true;
    }
   if (retused && mthd.getReturnType() != null &&
         !mthd.getReturnType().isVoidType()) {
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




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected void localOutputXml(IvyXmlWriter arg0,IfaceProgramPoint arg1)
{
   // method body goes here
}



/********************************************************************************/
/*                                                                              */
/*      Equality checking methods                                               */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object arg)
{
   if (arg instanceof QueryContextChangedItems) {
      QueryContextChangedItems ctx = (QueryContextChangedItems) arg;
      return var_data.equals(ctx.var_data);
    }

   return false;
}



@Override public int hashCode()
{
   return var_data.hashCode();
}




/********************************************************************************/
/*                                                                              */
/*      Information about a location                                            */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unused")
private static class VarData {
   
   private IfaceValue   ref_value;
   private String       ref_name;
   private boolean      is_changed;
   private boolean      is_relevant;
   
   VarData(IfaceValue v) {
      is_changed = false;
      is_relevant = false;
      ref_name = null;
      ref_value = v;
    }
   
   VarData(IfaceValue v,VarData base) {
      is_changed = true;
      is_relevant = base.is_relevant;
      ref_name = null;
      ref_value = v;
    }
   
   private VarData(VarData v0,IfaceValue v) {
      ref_name = v0.ref_name;
      is_changed = v0.is_changed;
      is_relevant = v0.is_relevant;
      ref_value = v;
    }
   
   private VarData(VarData v0,boolean ch,boolean rl) {
      ref_name = v0.ref_name;
      ref_value = v0.ref_value;
      is_changed = v0.is_changed | ch;
      is_relevant = v0.is_relevant | rl;
    }
   
   IfaceValue getReference()                            { return ref_value; }
   boolean isChanged()                                  { return is_changed; }
   boolean isRelevant()                                 { return is_relevant; }
   
   VarData changeReference(IfaceValue ref) {
      if (ref == ref_value) return this;
      return new VarData(this,ref);
    }
   
   VarData setChanged() {
      if (is_changed) return this;
      return new VarData(this,true,false);
    }
   
   VarData setRelevant() {
      if (is_relevant) return this;
      return new VarData(this,false,true);
    }
   
   @Override public int hashCode() {
      int hc = ref_value.hashCode();
      if (is_changed) hc += 100;
      if (is_relevant) hc += 200;
      return hc;
    }
   
   @Override public boolean equals(Object o) {
      if (o instanceof VarData) {
         VarData vd = (VarData) o;
         if (ref_value != vd.ref_value || is_changed != vd.is_changed ||
               is_relevant != vd.is_relevant) return false;
         return true;
       }
      return false;
    }
   
}       // end of inner class VarData




}       // end of class QueryChangedItemsContext




/* end of QueryChangedItemsContext.java */

