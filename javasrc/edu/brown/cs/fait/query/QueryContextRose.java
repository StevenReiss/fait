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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceBackFlow;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryContextRose extends QueryContext
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<IfaceValue,Integer>         priority_map;
private Map<IfaceValue,IfaceValue>      known_values;
private IfaceValue                      base_value;
private int                             use_conditions;
private List<IfaceMethod>               call_stack;
private int                             max_priority;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryContextRose(IfaceControl ctrl,QueryCallSites sites,
      IfaceValue var,IfaceValue val,int depth,
      int conds,List<IfaceMethod> stack,IfaceType thistype)
{
   super(ctrl,sites);
   max_priority = depth;
   base_value = val;
   priority_map = new HashMap<>();
   known_values = new HashMap<>();
   if (var != null) {
      priority_map.put(var,max_priority);
      if (val != null) known_values.put(var,val);
    }
   if (thistype != null) {
      IfaceValue thisref = ctrl.findRefValue(thistype,0);
      priority_map.put(thisref,max_priority);
    }
   use_conditions = conds;
   call_stack = stack;
}


private QueryContextRose(QueryContextRose ctx,QueryCallSites sites,
      Map<IfaceValue,Integer> pmap,Map<IfaceValue,IfaceValue> kmap)
{
   super(ctx.fait_control,sites);
   base_value = ctx.base_value;
   priority_map = pmap;
   known_values = kmap;
   max_priority = ctx.max_priority;
   use_conditions = ctx.use_conditions;
   call_stack = ctx.call_stack;
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
   boolean checkcond = true;
   
   if (priority_map.size() > 0) {
      boolean stackok = true;
      IfaceAstReference aref = backto.getLocation().getProgramPoint().getAstReference();
      if (aref != null && aref.getAstNode() != null && aref.getAstNode() instanceof Block &&
            aref.getStatus() == null) 
         stackok = false;
      
      for (Map.Entry<IfaceValue,Integer> ent : priority_map.entrySet()) {
         IfaceValue ref = ent.getKey();
         if (!stackok && ref.getRefStack() >= 0) {
            FaitLog.logD("QUERY","Spurrious stack reference " + ref + " AT " + backfrom.getLocation());
            continue;
          }
         IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,ref,(use_conditions > 0));
         if (bf == null) continue;
         IfaceValue sref = bf.getStartReference();
         if (sref != null) {
            if (sref.getRefStack() > 25) {
               FaitLog.logE("QUERY","Stack Reference too deep " + sref + " " + ref + " " + backfrom + " " + backto);
               continue;
             }
            npmap.put(sref,ent.getValue());
            if (known_values.get(ref) != null) {
               kpmap.put(sref,known_values.get(ref));
             }
          }
         
         if (bf.getAuxRefs() != null && bf.getAuxRefs().size() > 0) {
            int delta = 1;
            if (aref != null && aref.getAstNode() != null) {
               switch (aref.getAstNode().getNodeType()) {
                  case ASTNode.INFIX_EXPRESSION :
                     delta = 0;
                     break;
                }
             }
            int refval = ent.getValue() - delta;
            if (refval > 0) {
               checkcond = false;
               for (IfaceAuxReference auxref : bf.getAuxRefs()) {
                  if (auxref.getLocation().equals(backto.getLocation())) {    
                     Integer oval = npmap.get(auxref.getReference());
                     int rval = refval;
                     if (oval != null) rval = Math.max(rval,oval);
                     npmap.put(auxref.getReference(),rval);
                   }
                  else {
                     auxrefs.add(auxref);
                   }
                }
             }
          }
       }
    }
   if (use_conditions > 0 && checkcond) {
      IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,null,true);
      if (bf.getAuxRefs() != null && bf.getAuxRefs().size() > 0) {
         for (IfaceAuxReference auxref : bf.getAuxRefs()) {
            if (auxref.getLocation().equals(backto.getLocation())) {    
               Integer oval = npmap.get(auxref.getReference());
               if (oval == null) oval = use_conditions;
               npmap.put(auxref.getReference(),oval);
             }
            else {
               auxrefs.add(auxref);
             }
          }
       }
    }
   
   QueryContextRose nctx = null;
   if (npmap.equals(priority_map)) nctx = this;
   else if (!npmap.isEmpty()) nctx = new QueryContextRose(this,call_sites,npmap,kpmap);
   else if (use_conditions > 0) nctx = new QueryContextRose(this,call_sites,npmap,kpmap);
   
   if (auxrefs.isEmpty()) auxrefs = null;
   IfaceBackFlow bf = fait_control.getBackFlow(backfrom,backto,auxrefs);
   
   return new QueryBackFlowData(nctx,bf);
}




@Override protected QueryContext getPriorContextForCall(IfaceCall c,IfaceProgramPoint pt,
        QueryCallSites sites)
{
   IfaceMethod fm = c.getMethod();
   int delta = (fm.isStatic() ? 0 : 1);
   int act = fm.getNumArgs();
   
   Map<IfaceValue,Integer> npmap = new HashMap<>();
   Map<IfaceValue,IfaceValue> nvmap = new HashMap<>();
   boolean havedummy = false;
   for (IfaceValue ref : priority_map.keySet()) {
      int slot = ref.getRefSlot();
      IfaceValue nref = ref;
      if (slot >= 0) {
         if (slot >= fm.getLocalSize()) havedummy = true;
         if (slot >= act+delta || slot < delta) continue;
         int stk = act+delta-slot-1;
         nref = fait_control.findRefStackValue(ref.getDataType(),stk);
         npmap.put(nref,priority_map.get(ref));
       }
      if (nref != null && known_values.get(ref) != null) {
         nvmap.put(nref,known_values.get(ref));
       }
    }

   if (npmap.isEmpty()) {
      if (use_conditions <= 0) return null;
      if (!havedummy) return null;
//    return null;
    }
   QueryContextRose newctx = new QueryContextRose(this,sites,npmap,nvmap);
   newctx.use_conditions = Math.max(0,use_conditions-1);
   
   return newctx;
}





@Override protected QueryContext getReturnContext(IfaceLocation loc)
{
   Map<IfaceValue,Integer> npmap = new HashMap<>();
   Map<IfaceValue,IfaceValue> nvmap = new HashMap<>();
   boolean useret = false;
   for (IfaceValue ref : priority_map.keySet()) {
      int slot = ref.getRefStack();
      if (slot > 0) continue;           // ignore stack other than return value
      else if (slot == 0) {
         IfaceAstReference astr = loc.getProgramPoint().getAstReference();
         if (astr != null) {
            ASTNode astn = astr.getAstNode();
            if (astn instanceof ClassInstanceCreation) {
               ClassInstanceCreation cic = (ClassInstanceCreation) astn;
               JcompType jt = JcompAst.getJavaType(cic.getType());
               IfaceType it = fait_control.findDataType(jt.getName());
               IfaceValue v0ref = fait_control.findRefValue(it,0);
               npmap.put(v0ref,priority_map.get(ref));
               useret = true;
               continue;
             }
          }
         useret = true;
         npmap.put(ref,priority_map.get(ref));
       }
      if (known_values.get(ref) != null) {
         nvmap.put(ref,known_values.get(ref));
       }
    }
   
   if (!useret || npmap.isEmpty()) return null;
   
   // need to use getNextSite here
   QueryCallSites ncallsites = call_sites.getNextSites(loc);
   QueryContextRose newctx = new QueryContextRose(this,ncallsites,npmap,nvmap);
   newctx.use_conditions = Math.max(0,use_conditions-1);
   return newctx;
}



@Override protected QueryContext addRelevantArgs(QueryContext priorc,IfaceState st0,QueryBackFlowData bfd)
{
   IfaceProgramPoint pt = st0.getLocation().getProgramPoint();
   IfaceMethod mthd = pt.getCalledMethod();
   QueryContextRose prior = (QueryContextRose) priorc;
  
   int ct = mthd.getNumArgs();
   int retused = -1;
   int thisused = -1;
   IfaceValue voidused = null;
   for (IfaceValue ref : priority_map.keySet()) {
      int slot = ref.getRefStack();
      if (slot == 0) retused = priority_map.get(ref);
      if (!mthd.isStatic() && slot == ct+1) thisused = priority_map.get(ref);
      if (ref.getRefSlot() == 100000) {
         voidused = ref;
         retused = priority_map.get(ref);
       }
    }
   
   IfaceAstReference xref = pt.getAstReference();
   if (xref != null && thisused < 0) {
      Expression ex = null;
      if (xref.getAstNode() instanceof MethodInvocation) {
         MethodInvocation mi = (MethodInvocation) xref.getAstNode();
         ex = mi.getExpression();
       }
      else if (xref.getAstNode() instanceof ClassInstanceCreation) {
         ClassInstanceCreation cic = (ClassInstanceCreation) xref.getAstNode();
         ex = cic.getExpression();
       }
      else if (xref.getAstNode() instanceof ConstructorInvocation) {
         for (IfaceValue ref : priority_map.keySet()) {
            if (ref.getRefSlot() == 0) thisused = priority_map.get(ref);
          }
       }
      else if (xref.getAstNode() instanceof SuperConstructorInvocation) {
         for (IfaceValue ref : priority_map.keySet()) {
            if (ref.getRefSlot() == 0) thisused = priority_map.get(ref);
          }
       }
      if (ex != null) {
         JcompSymbol js = JcompAst.getReference(ex);
         IfaceMethod fmthd = xref.getMethod();
         if (js != null) {
            int slot = fmthd.getLocalOffset(js);
            if (slot >= 0) {
               for (IfaceValue ref : priority_map.keySet()) {
                  if (ref.getRefSlot() == slot) thisused = priority_map.get(ref);
                }
             }
          }
       }
    }
   
   Map<IfaceValue,Integer> npmap = new HashMap<>();
   Map<IfaceValue,IfaceValue> nvmap = new HashMap<>();
   if (prior != null) {
      npmap.putAll(prior.priority_map);
      nvmap.putAll(prior.known_values);
      if (voidused != null) npmap.remove(voidused);
    }
   boolean chng = false;
   
   List<IfaceAuxReference> arefs = getArgumentReferences(st0,retused > 0,thisused > 0,voidused != null);
   for (IfaceAuxReference aref : arefs) {
      if (aref.getLocation() == st0.getLocation()) {
         Integer oval = priority_map.get(aref.getReference());
         int rval = retused;
         if (rval < 0) rval = thisused;
         if (oval != null) rval = Math.max(rval,oval);
         npmap.put(aref.getReference(),rval);
         chng = true;
       }
      else {
         bfd.addAuxReference(aref);
       }
    }
   
   if (chng) {
      if (prior == null) prior = this;
      return new QueryContextRose(prior,call_sites,npmap,nvmap);
    }
   
   return prior;
}








@Override protected QueryContext newReference(IfaceValue newref,QueryCallSites sites,
      IfaceState newstate,IfaceState oldstate)
{
   boolean ignore = false;
   if (newstate == null || oldstate == null) ignore = true;
   else {
      IfaceCall c1 = newstate.getLocation().getCall();
      IfaceCall c2 = oldstate.getLocation().getCall();
      if (c1 != c2) ignore = true;
    }
   
   if (sites == null && getCallSites() != null) ignore = false;
   if (ignore) return this;
   
   Map<IfaceValue,Integer> pmap = new HashMap<>();
   Map<IfaceValue,IfaceValue> vmap = new HashMap<>();
   int p = 0;
   if (priority_map != null) {
      for (Map.Entry<IfaceValue,Integer> ent : priority_map.entrySet()) {
         p = Math.max(p,ent.getValue());
         IfaceValue ref = ent.getKey();
         if (ref.getRefField() != null) {
            pmap.put(ref,ent.getValue());
          }
       }
    }
   
   if (p == 0) {
      if (call_stack == null) return this;
    }
   
   pmap.put(newref,p);
   
   QueryContextRose nctx = new QueryContextRose(this,sites,pmap,vmap);
   nctx.call_stack = null;
   
   return nctx;
}



@Override protected QueryContext mergeWith(QueryContext octx)
{
   QueryContextRose ctx = (QueryContextRose) octx;
   if (octx == this) return this;
   
   QueryCallSites sites = call_sites;
   
   if (call_sites == null || ctx.call_sites == null) sites = null;
// if (!call_sites.equals(ctx.call_sites)) return null;
   
   Map<IfaceValue,Integer> npmap = new HashMap<>(priority_map);
   Map<IfaceValue,IfaceValue> kpmap = new HashMap<>(known_values);
   
   for (Map.Entry<IfaceValue,Integer> ent : ctx.priority_map.entrySet()) {
      IfaceValue ref = ent.getKey();
      Integer pri = ent.getValue();
      Integer opri = npmap.get(ref);
      IfaceValue known = ctx.known_values.get(ref);
      if (opri == null) {
         npmap.put(ref,pri);
         if (known != null) kpmap.put(ref,known);
       }
      else {
         if (pri > opri) npmap.put(ref,pri);
         if (known == null) kpmap.remove(ref);
       }
    }
   
   if (npmap.equals(priority_map) && kpmap.equals(known_values)) return this;
   if (npmap.equals(ctx.priority_map) && kpmap.equals(ctx.known_values)) return ctx;
   
   QueryContextRose newctx = new QueryContextRose(this,sites,npmap,kpmap);
   newctx.use_conditions = Math.max(use_conditions,ctx.use_conditions);  
   
   return newctx;
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
   boolean havethis = false;
   for (Map.Entry<IfaceValue,Integer> ent : prior.priority_map.entrySet()) {
      IfaceValue ref = ent.getKey();
      Integer opri = priority_map.get(ref);
      if (ref.getRefSlot() == 0) havethis = true;
      if (opri == null) return "Value Computed";
      else if (opri != ent.getValue()) return "Value Changed";
    }
   if (priority_map.size() != prior.priority_map.size()) {
      return "Value Computed";
    }
   IfaceAstReference astref = state.getLocation().getProgramPoint().getAstReference();
   if (havethis) {
      if (astref != null) {
         ASTNode an = astref.getAstNode();
         if (an instanceof Assignment) {
            Assignment asgn = (Assignment) an;
            ASTNode lhsv = asgn.getLeftHandSide();
            boolean isrel = false;
            if (lhsv instanceof SimpleName) {
               JcompSymbol lhssym = JcompAst.getReference(lhsv);
               if (lhssym != null && lhssym.isFieldSymbol()) isrel = true;
             }
            else if (lhsv instanceof FieldAccess) {
               FieldAccess facc = (FieldAccess) lhsv;
               if (facc.getExpression() instanceof ThisExpression) isrel = true;
             }
            if (isrel) {
               return "Field Set";
             }
          }
       }
    }
   if (astref != null && astref.getAstNode() instanceof ArrayAccess) {
      ArrayAccess ai = (ArrayAccess) astref.getAstNode();
      if (ai.getParent() instanceof Assignment) {
         Assignment asgn = (Assignment) ai.getParent();
         if (asgn.getLeftHandSide() == ai) {
            if (astref.getAfterChild() == ai.getIndex()) {
               Expression lhs = ai.getArray();
               while (lhs instanceof ArrayAccess) {
                  ArrayAccess aa1 = (ArrayAccess) lhs;
                  lhs = aa1.getArray();
                }
               JcompSymbol js = JcompAst.getReference(lhs);
               if (js != null) {
                  int lcl = state.getLocation().getMethod().getLocalOffset(js);
                  if (lcl > 0) {
                     for (Map.Entry<IfaceValue,Integer> ent : prior.priority_map.entrySet()) {
                        IfaceValue ref = ent.getKey();
                        if (ref.getRefSlot() == lcl) return "Array Set";
                      }
                   }
                  else if (havethis && js.isFieldSymbol()) return "Field Array Set";
                }
             }
          }
       }
    }
   
   return null;
}


@Override protected boolean isReturnRelevant(IfaceState st0,IfaceCall call)
{
   if (!call.getMethod().isEditable())
      return false;
   return true;
}


@Override boolean isCallRelevant(IfaceCall callfrom,IfaceCall callto)
{
   if (call_stack == null) return true;
   
   IfaceMethod mfrom = callfrom.getMethod();
   if (mfrom.getName().startsWith("TEST_")) return true;
   // this precludes calls to routines from methods on the stack
   // we need a better way of limiting here -- track whether we 
   // should use call stack or not?
   // if (!call_stack.contains(mfrom)) return false;
   boolean fnd = false;
   boolean stkfnd = false;
   for (IfaceMethod im : call_stack) {
      if (im == callto.getMethod()) fnd = true;
      if (fnd && im == callfrom.getMethod()) stkfnd = true;
    }
   if (!fnd || stkfnd)
      return true;
   
   return false;
}

@Override protected boolean followCalls()
{
   return true;
}


@Override protected double getNodePriority()
{
   double p = 1;
   for (Integer i : priority_map.values()) {
      if (i != null) p = Math.max(p,i);
    }
   
   return p/max_priority;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected void localOutputXml(IvyXmlWriter xw,IfaceProgramPoint where)
{
   for (IfaceValue val : priority_map.keySet()) {
      xw.begin("REFERENCE");
      int slot = val.getRefSlot();
      int stk = val.getRefStack();  
      IfaceField fld = val.getRefField();
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
      xw.end("REFERENCE");
    }
}



@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("[");
   for (Map.Entry<IfaceValue,Integer> ent : priority_map.entrySet()) {
      IfaceValue refval = ent.getKey();
      String ref = "?";
      if (refval.getRefSlot() >= 0) ref = "v" + refval.getRefSlot();
      else if (refval.getRefStack() >= 0) ref = "s" + refval.getRefStack();
      else if (refval.getRefIndex() != null) ref = "[]" + refval.getRefIndex();
      else if (refval.getRefField() != null) ref = refval.getRefField().toString();
      buf.append(ref);
      buf.append(":");
      buf.append(ent.getValue());
      IfaceValue actval = known_values.get(refval);
      if (actval != null) {
         buf.append("=");
         buf.append(actval);
       }
      buf.append(",");
    }
   buf.append("C:");
   buf.append(use_conditions);
   buf.append("]");
   return buf.toString();
}



/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object o)
{
   if (o instanceof QueryContextRose) {
      QueryContextRose qcr = (QueryContextRose) o;
      if (use_conditions != qcr.use_conditions) return false;
      if (base_value != qcr.base_value) return false;
      if (priority_map.size() != qcr.priority_map.size()) return false;
      for (Map.Entry<IfaceValue,Integer> ent1 : priority_map.entrySet()) {
         boolean fnd = false;
         for (Map.Entry<IfaceValue,Integer> ent2 : qcr.priority_map.entrySet()) {
//             if (ent2.getValue().equals(ent2.getValue())) continue;
            IfaceValue v1 = ent1.getKey();
            IfaceValue v2 = ent2.getKey();
            if (v1.getRefBase() != v2.getRefBase()) continue;
            if (v1.getRefField() != v2.getRefField()) continue;
            if (v1.getRefSlot() != v2.getRefSlot()) continue;
            if (v1.getRefStack() != v2.getRefStack()) continue;
            fnd = true;
            break;
          }
         if (!fnd) return false;
       }
      if (call_stack == null && qcr.call_stack != null) return false;
      if (call_stack != null && qcr.call_stack == null) return false;
      if (call_sites == null && qcr.call_sites != null) return false;
      if (call_sites != null && !call_sites.equals(qcr.call_sites)) return false;
      return true;
    }
   return false;
}



@Override public int hashCode()
{
   int hash = use_conditions;
   if (base_value != null) hash += base_value.hashCode();
   for (Map.Entry<IfaceValue,Integer> ent : priority_map.entrySet()) {
//       hash += ent.getValue().hashCode();
      hash += ent.getKey().hashCode();
    }
   if (call_stack != null) hash += 1;
   
   return hash;
}



}       // end of class QueryContextRose




/* end of QueryContextRose.java */

