/********************************************************************************/
/*                                                                              */
/*              QueryContextSubtype.java                                        */
/*                                                                              */
/*      Context to check a subtype value                                        */
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

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceBackFlow;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryContextSubtype extends QueryContext implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private final IfaceValue         for_value;
private final IfaceSubtype       for_subtype;
private final IfaceSubtype.Value subtype_value;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryContextSubtype(IfaceControl ctrl,IfaceValue v,IfaceSubtype.Value stv)
{
   super(ctrl);
   
   for_subtype = stv.getSubtype();
   for_value = v;
   subtype_value = stv;
}


@Override protected QueryContext newReference(IfaceValue newref)
{
   if (newref == for_value) return this;
   if (newref == null) return null;
   
   return new QueryContextSubtype(fait_control,newref,subtype_value);
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
   return newReference(nref);
}



@Override protected QueryBackFlowData getPriorStateContext(IfaceState backfrom,IfaceState backto)
{
   IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,for_value);
   QueryContext nctx = this;
   IfaceValue v = bf.getStartReference();
   if (v == null) nctx = null;
   if (v != for_value) nctx = newReference(v);
   return new QueryBackFlowData(nctx,bf);
}


@Override protected List<QueryContext> getTransitionContext(IfaceState st0)
{
   List<QueryContext> rslt = new ArrayList<>();
   // return a set of contexts that represent a transition from that context
   //    to the current one
   
   return rslt;
}


@Override protected boolean isEndState(IfaceState st0)
{
   
   return false;
}


@Override protected QueryContext getReturnContext(IfaceCall call)
{
   IfaceValue ref = fait_control.findRefStackValue(for_value.getDataType(),0);
   return newReference(ref);
}



@Override protected boolean isPriorStateRelevant(IfaceState st0) 
{
   IfaceValue v0 = null;
   if (for_value.getRefStack() >= 0) {
      v0 = st0.getStack(for_value.getRefStack());
    }
   else if (for_value.getRefSlot() >= 0) {
      v0 = st0.getLocal(for_value.getRefSlot());
    }
   else if (for_value.getRefField() != null) {
      v0 = st0.getFieldValue(for_value.getRefField());
    }
   
   if (v0 == null) return false;
   IfaceType t0 = v0.getDataType();
   IfaceSubtype.Value sv0 = t0.getValue(for_subtype);
   
   if (sv0 == subtype_value) return true;
   
   IfaceSubtype.Value sv1 = for_subtype.getMergeValue(sv0,subtype_value);
  
   if (sv1 == sv0) return true;
   
   List<IfaceValue> cnts = v0.getContents();
   if (cnts != null) {
      for (IfaceValue vc : cnts) {
         IfaceType tc = vc.getDataType();
         IfaceSubtype.Value csv0 = tc.getValue(for_subtype);
         if (csv0 == subtype_value) return true;
         IfaceSubtype.Value csv1 = for_subtype.getMergeValue(csv0,subtype_value);
         if (csv1 == csv0) return true;
         
       }
    }
   
   return false;
}


@Override protected boolean isReturnRelevant(IfaceState st0,IfaceCall call)
{
   // return true if the return should be investigated

   return true;
}



@Override protected void addRelevantArgs(IfaceState st0,QueryBackFlowData bfd)
{
   IfaceProgramPoint pt = st0.getLocation().getProgramPoint();
   IfaceMethod mthd = pt.getCalledMethod();
   if (mthd == null || mthd.isStatic()) return;
   int ct = mthd.getNumArgs();
   IfaceValue v0 = st0.getStack(ct);
   if (v0 == null) return;
   FaitLog.logD("INVOKE " + mthd.getName() + " " + v0.getRefSlot() + " " + for_value.getRefSlot());
   if (v0.getRefSlot() > 0 && v0.getRefSlot() == for_value.getRefSlot()) {
      for (int i = 0; i < ct; ++i) {
         IfaceValue vs = st0.getStack(i);
         IfaceValue vr = fait_control.findRefStackValue(vs.getDataType(),i);
         bfd.addAuxReference(vr);
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
   xw.field("SUBTYPE",for_subtype.getName());
   xw.field("VALUE",subtype_value);
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


@Override protected String localDisplayContext()
{
   String ref = "?";
   if (for_value.getRefSlot() >= 0) ref = "v" + for_value.getRefSlot();
   else if (for_value.getRefStack() >= 0) ref = "s" + for_value.getRefStack();
   else if (for_value.getRefField() != null) ref = for_value.getRefField().toString();
   
   return subtype_value.toString() + "@" + ref; 
}



/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public int hashCode()
{
   return subtype_value.hashCode() + for_value.hashCode();
}



@Override public boolean equals(Object o)
{
   if (o instanceof QueryContextSubtype) {
      QueryContextSubtype qc = (QueryContextSubtype) o;
      if (subtype_value != qc.subtype_value) return false;
      if (for_value != qc.for_value) {
         if (for_value.isReference() && qc.for_value.isReference()) {
            if (for_value.getRefBase() != qc.for_value.getRefBase()) return false;
            if (for_value.getRefField() != qc.for_value.getRefField()) return false;
            if (for_value.getRefSlot() != qc.for_value.getRefSlot()) return false;
            if (for_value.getRefStack() != qc.for_value.getRefStack()) return false;
          }
         else return false;
       }
      return true;
    }
   return false;
}



}       // end of class QueryContextSubtype




/* end of QueryContextSubtype.java */

