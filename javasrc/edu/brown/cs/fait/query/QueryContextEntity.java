/********************************************************************************/
/*                                                                              */
/*              QueryContextEntity.java                                         */
/*                                                                              */
/*      Context for computing entity back flow                                  */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.fait.query;

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceBackFlow;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryContextEntity extends QueryContext implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceEntity     base_entity;
private IfaceValue      for_value;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryContextEntity(IfaceControl fc,IfaceValue v,IfaceEntity ent)
{
   super(fc);
   for_value = v;
   base_entity = ent;
}


/********************************************************************************/
/*                                                                              */
/*      Handle auxiliiary references                                            */
/*                                                                              */
/********************************************************************************/

@Override protected QueryContext newReference(IfaceValue newref,
      IfaceState news,IfaceState olds)
{
   if (newref == for_value) return this;
   if (newref == null) return null;
   
  return new QueryContextEntity(fait_control,newref,base_entity);
}



/********************************************************************************/
/*                                                                              */
/*      Next state computation methods                                          */
/*                                                                              */
/********************************************************************************/

@Override protected QueryContext getPriorContextForCall(IfaceCall c,IfaceProgramPoint pt)
{
   int slot = for_value.getRefSlot();
   if (slot < 0) return null;
   
   IfaceMethod fm = c.getMethod();
   int delta = (fm.isStatic() ? 0 : 1);
   int act = fm.getNumArgs();
   if (slot >= act+delta) return null;
   int stk = act+delta-slot-1;
   IfaceValue nref = fait_control.findRefStackValue(for_value.getDataType(),stk);
   return newReference(nref,null,null);
}



@Override protected QueryBackFlowData getPriorStateContext(IfaceState backfrom,IfaceState backto)
{
   IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,for_value,false);
   QueryContext nctx = this;
   IfaceValue v = bf.getStartReference();
   if (v == null) nctx = null;
   else if (v != for_value) nctx = newReference(v,backto,backfrom);
   return new QueryBackFlowData(nctx,bf);  
}



@Override protected List<QueryContext> getTransitionContext(IfaceState st0)
{
   List<QueryContext> rslt = new ArrayList<>();
   // return a set of contexts that represent a transition from that context
   //    to the current one
   
   return rslt;
}







@Override protected QueryContext getReturnContext(IfaceCall call)
{
   IfaceValue ref = fait_control.findRefStackValue(for_value.getDataType(),0);
   return newReference(ref,null,null);
}



@Override protected boolean isPriorStateRelevant(IfaceState st0)
{
   IfaceValue v0 = QueryFactory.dereference(fait_control,for_value,st0);
   
   if (v0 == null) return false;
   if (v0.isReference() && v0.getRefField() != null) {
      IfaceValue v1 = fait_control.getFieldValue(st0,v0.getRefField(),null,false);
      if (v1 != null) v0 = v1;
    }
   
   if (v0.containsEntity(base_entity)) return true;
   
   List<IfaceValue> cnts = v0.getContents();
   if (cnts != null) {
      for (IfaceValue vc : cnts) {
         if (vc.containsEntity(base_entity)) return true;
       }
    }
   
   return false;
}









@Override protected void addRelevantArgs(IfaceState st0,QueryBackFlowData bfd)
{
   IfaceProgramPoint pt = st0.getLocation().getProgramPoint();
   IfaceMethod mthd = pt.getCalledMethod();
   if (mthd == null) return;
   
   if (for_value.getRefStack() == 0 && mthd.getReturnType() != null &&
         !mthd.getReturnType().isVoidType()) {
      int ct = mthd.getNumArgs();
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
      int ct = mthd.getNumArgs();
      IfaceValue v0 = st0.getStack(ct);
      if (v0 != null && v0.getRefSlot() > 0 && v0.getRefSlot() == for_value.getRefSlot()) {
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

@Override protected void localOutputXml(IvyXmlWriter xw,IfaceProgramPoint where)
{
   // method body goes here
   xw.field("ENTITY",base_entity.getId());
   int slot = for_value.getRefSlot();
   int stk = for_value.getRefStack();
   IfaceField fld = for_value.getRefField();
   if (slot >= 0) {
      xw.field("REFSLOT",slot);
      Object var = where.getMethod().getItemAtOffset(slot,where);
      if (var != null) {
         if (var instanceof JcompSymbol) {
            JcompSymbol js = (JcompSymbol) var;
            xw.field("REFSYM",js.getFullName());
          }
         else {
            xw.field("REFSYM",var.toString());
          }
       }
    }
   else if (stk >= 0) {
      xw.field("REFSTACK",stk);
    }
   else if (fld != null) {
      xw.field("REFFIELD",fld.getFullName());
    }
}



/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public int hashCode()
{
   return base_entity.getId() + for_value.hashCode();
}



@Override public boolean equals(Object o)
{
   if (o instanceof QueryContextEntity) {
      QueryContextEntity qe = (QueryContextEntity) o;
      if (qe.base_entity != base_entity) return false;
      if (for_value != qe.for_value) {
         if (for_value.isReference() && qe.for_value.isReference()) {
            if (for_value.getRefBase() != qe.for_value.getRefBase()) return false;
            if (for_value.getRefField() != qe.for_value.getRefField()) return false;
            if (for_value.getRefSlot() != qe.for_value.getRefSlot()) return false;
            if (for_value.getRefStack() != qe.for_value.getRefStack()) return false;
          }
         else return false;
       }
      return true;
    }
   return false;
}



}       // end of class QueryContextEntity




/* end of QueryContextEntity.java */

