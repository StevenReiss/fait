/********************************************************************************/
/*										*/
/*		QueryProcessor.java						*/
/*										*/
/*	Processor for queries							*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
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



package edu.brown.cs.fait.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Deque;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceAstStatus.Reason;
import edu.brown.cs.ivy.jcomp.JcompSymbol;

class QueryProcessor implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Deque<QueryQueueItem> query_queue;
private IfaceControl	      fait_control;
private Map<QueryQueueItem,QueryNode> known_items;
private QueryContextMap         context_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

QueryProcessor(IfaceControl ctrl,QueryQueueItem qqi,QueryNode qn)
{
   fait_control = ctrl;
   query_queue = new LinkedList<>();
   known_items = new HashMap<>();
   context_map = new QueryContextMap();

   if (qqi != null) {
      query_queue.add(qqi);
      known_items.put(qqi,qn);
    }
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void process()
{
   for ( ; ; ) {
      QueryQueueItem qqi = query_queue.poll();
      if (qqi == null) break;
      IfaceState st0 = fait_control.findStateForLocation(qqi.getCall(),qqi.getProgramPoint());
      if (st0 == null) {
         FaitLog.logD("QUERY","NO PRIOR STATE: " + qqi.getCall().hashCode());
         st0 = fait_control.findStateForLocation(qqi.getCall(),qqi.getProgramPoint());
       }
      QueryNode node = known_items.get(qqi);
      computeNext(qqi,st0,node);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Process a single queue item                                             */
/*                                                                              */
/********************************************************************************/

private void computeNext(QueryQueueItem qqi,IfaceState cur,QueryNode node)
{
   QueryContext ctx = qqi.getContext();
   IfaceCall call = qqi.getCall();
   IfaceProgramPoint pt = qqi.getProgramPoint();
   
   FaitLog.logD("QUERY","Compute Next: " + " " + ctx + " " + qqi.getProgramPoint() + " " +
         qqi.getCall().getMethod() + " (" + node.getId() + ")");
   FaitLog.logD("QUERY","Next Info: " + cur + " " + qqi.getCall().hashCode());
         
   QueryContext oldctx = context_map.get(call,pt);
   if (oldctx != null) {
      if (oldctx == ctx) return;
      QueryContext newctx = oldctx.mergeWith(ctx);
      if (newctx == oldctx) {
         QueryQueueItem oqqi = new QueryQueueItem(call,pt,newctx);
         QueryNode onode = known_items.get(oqqi);
         if (onode != null && onode != node) {
            QueryGraph graph = node.getGraph();
            graph.addNode(onode,node);
          }
       }
      if (newctx == null || newctx == oldctx) return;
      ctx = newctx;
    }
   context_map.put(call,pt,ctx);
   
   if (cur != null && cur.isStartOfMethod()) {
      node.getGraph().markAsEndNode(node);	// allow this to be a starting point
      QueryCallSites sites = ctx.getCallSites().getPriorSites();
      QueryContext priorctx = ctx.getPriorContextForCall(call,pt,sites);
      if (priorctx == null) {
	 return;
       }
      QueryGraph graph = node.getGraph();
      node = graph.addNode(call,pt,ctx,QueryNodeType.ENTRY,
            "Start of Method " + call.getMethod().getName(),node);
//    graph.markAsEndNode(node);
      IfaceLocation loc = ctx.getCallSites().getCallSite();
      // need to handle case where we initiated the call -- go to call site rather than
      // all call sites
      if (loc != null) {                // CHANGE IF CALL_SITES don't work
         processCallSite(call,priorctx,node,loc);
       }
      else {
         for (IfaceCall call0 : call.getAlternateCalls()) {
            for (IfaceLocation callloc : call0.getCallSites()) {
               if (!ctx.isCallRelevant(callloc.getCall(),call0)) continue;
               processCallSite(call,priorctx,node,callloc);
             }
          }
       }
    }
   else if (cur != null) {
      for (int i = 0; i < cur.getNumPriorStates(); ++i) {
	 IfaceState st0 = cur.getPriorState(i);
         FaitLog.logD("QUERY","PRIOR STATE " + i + " " + st0);
	 handleFlowFrom(cur,st0,ctx,node);
       }
    }
}


private void processCallSite(IfaceCall from,QueryContext priorctx,QueryNode node,IfaceLocation callloc)
{
   QueryGraph graph = node.getGraph();
   IfaceState st0 = fait_control.findStateForLocation(callloc.getCall(),
         callloc.getProgramPoint());
   if (!priorctx.isPriorStateRelevant(st0)) return;
   QueryNode nn = graph.addNode(callloc.getCall(),callloc.getProgramPoint(),priorctx,
         QueryNodeType.CALL,
         "Call to Method " + from.getMethod().getName(),node);
   graph.markAsEndNode(nn);
   QueryQueueItem nqqi = new QueryQueueItem(callloc,priorctx);
   addItem(nqqi,nn);
}



private void handleFlowFrom(IfaceState backfrom,IfaceState st0,QueryContext ctx,QueryNode node)
            {
   if (st0.getLocation() == null) {
      // handle intermediate states
      for (int i = 0; i < st0.getNumPriorStates(); ++i) {
	 IfaceState st1 = st0.getPriorState(i);
	 handleFlowFrom(backfrom,st1,ctx,node);
       }
      return;
    }
   else {
      handleActualFlowFrom(backfrom,st0,ctx,node);
    }
}




private void handleActualFlowFrom(IfaceState backfrom,IfaceState st0,QueryContext ctx,QueryNode node)
{
   QueryBackFlowData bfd = ctx.getPriorStateContext(backfrom,st0);
   QueryContext priorctx = bfd.getContext();
   IfaceProgramPoint pt = st0.getLocation().getProgramPoint();
   boolean cntxrel = false;
   
   if (st0.isMethodCall()) {
      IfaceMethod mthd = pt.getCalledMethod();
      if (mthd != null) {
         FaitLog.logD("QUERY","Call to: " + mthd.getFullName());
         priorctx = ctx.addRelevantArgs(priorctx,st0,bfd);
         if (priorctx != ctx) cntxrel = true;
         FaitLog.logD("QUERY","CHECK CALL " + mthd.getFullName() + " " + ctx + " " + cntxrel);
       }
    }
   
   boolean islinked = false;
   
   if (bfd.getAuxRefs() != null) {
      for (IfaceAuxReference aref : bfd.getAuxRefs()) {
	 islinked |= handleAuxReference(aref,ctx,node,st0);
       }
    }
   
   if (priorctx == null && !islinked) node.getGraph().markAsEndNode(node);
   else if (cntxrel) {
      QueryGraph graph = node.getGraph();
      IfaceLocation ploc = st0.getLocation();
      QueryNode nn = graph.addNode(ploc.getCall(),ploc.getProgramPoint(),priorctx,
            QueryNodeType.REFERENCE,"State Transition",node);
      nn.setPriority(priorctx.getNodePriority());
      
      node = nn;
    }
   
   if (st0.isMethodCall() &&
         (priorctx == null || !priorctx.isPriorStateRelevant(st0) || priorctx.followCalls())) {
      // the context is determined by the call, not by anything prior to the call
      IfaceCall call2 = st0.getLocation().getCall();
      QueryContext retctx = ctx.getReturnContext(st0.getLocation());
      if (retctx != null) {
         IfaceProgramPoint ppt2  = st0.getLocation().getProgramPoint();
         if (call2.getAllMethodsCalled(ppt2).isEmpty()) {
            islinked |= ctx.handleInternalCall(st0,bfd,node);
            if (!islinked) node.getGraph().markAsEndNode(node);
            FaitLog.logT("QUERY","No call found");
          }
         else {
            for (IfaceCall from : call2.getAllMethodsCalled(ppt2)) {
               // if return did not include value of interest, skip
               if (!retctx.isReturnRelevant(st0,from)) continue;
//             if (from.getReturnStates().size() > 1) {
//                int ct = from.getMethod().getLocalSize();
//                IfaceType t0 = fait_control.findDataType("int");
//                IfaceValue ref = fait_control.findRefValue(t0,ct+10);
//              }
               
               for (IfaceState st1 : from.getReturnStates()) {
                  if (!retctx.isPriorStateRelevant(st1)) continue;
                  // get the return expression state
                  IfaceState st2 = getReturnState(st1);
                  QueryGraph graph = node.getGraph();
                  QueryNode nn = graph.addNode(from,
                        st2.getLocation().getProgramPoint(),retctx,
                        QueryNodeType.RETURN,
                        "Result of method " + from.getMethod().getName(),node);
                  QueryQueueItem nqqi = new QueryQueueItem(st2.getLocation(),retctx);
                  addItem(nqqi,nn);
                }
             }
          }
       }
    }
   
   if (priorctx != null && priorctx.isPriorStateRelevant(st0)) {
      String reason = ctx.addToGraph(priorctx,st0);
      if (reason != null) {
         QueryGraph graph = node.getGraph();
         IfaceLocation ploc = st0.getLocation();
         node = graph.addNode(ploc.getCall(),ploc.getProgramPoint(),priorctx,
               QueryNodeType.COMPUTED,reason,node);
         node.getGraph().markAsEndNode(node);
       }
      node.setPriority(priorctx.getNodePriority());
      QueryQueueItem nqqi = new QueryQueueItem(st0.getLocation(),priorctx);
      addItem(nqqi,node);
    }
   else if (priorctx != null) {
      List<QueryContext> nctxs = priorctx.getTransitionContext(st0);
      if (nctxs != null && nctxs.size() > 0) {
	 for (QueryContext ctx1 : nctxs) {
	    QueryGraph graph = node.getGraph();
	    IfaceLocation ploc = st0.getLocation();
	    QueryNode nn = graph.addNode(ploc.getCall(),ploc.getProgramPoint(),ctx1,
		  QueryNodeType.STATE_TRANSITION,"State Transition",node);
	    if (ctx1.isEndState(st0)) {
	       graph.markAsEndNode(nn);
	       continue;
	     }
	    QueryQueueItem nqqi = new QueryQueueItem(ploc,ctx1);
	    addItem(nqqi,nn);
	  }
       }
      else {
	 node.getGraph().markAsEndNode(node);
       }
    }
   // STILL need to handle flows based on exceptions
}



void handleInitialReferences(Collection<IfaceAuxReference> refs,QueryContext ctx,
      QueryNode nd,IfaceState st0)
{
   for (IfaceAuxReference ref : refs) {
      handleAuxReference(ref,ctx,nd,st0);
    }
}



private boolean handleAuxReference(IfaceAuxReference ref,QueryContext ctx,QueryNode node,IfaceState st0)
{
   IfaceLocation loc = ref.getLocation();
   IfaceValue v0 = ref.getReference();
   IfaceState st1 = fait_control.findStateForLocation(loc);
   boolean linked = false;
   
   QueryGraph graph = node.getGraph();
   // Might want to use a null call_sites here if not in same method
   QueryCallSites qcs = ctx.getCallSites();
   IfaceMethod m1 = ref.getLocation().getMethod();
   IfaceMethod m2 = st0.getLocation().getMethod();
   if (m1 != m2) qcs = null;
   
   QueryContext nctx = ctx.newReference(v0,qcs,st0,st1);
   if (nctx != null && nctx.isPriorStateRelevant(st1)) {
      String desc = "Referenced value";
      if (v0.getRefSlot() >= 0) {
	 Object v = loc.getCall().getMethod().getItemAtOffset(v0.getRefSlot(),loc.getProgramPoint());
	 if (v != null) {
	    if (v instanceof JcompSymbol) {
	       desc = "Variable " + ((JcompSymbol) v).getFullName() + " referenced";
	     }
	    else {
	       desc = "Variable " + v.toString() + " referenced";
	     }
	  }
       }
      QueryNode nn = graph.addNode(loc.getCall(),loc.getProgramPoint(),nctx,
            QueryNodeType.REFERENCE,desc,node);
      QueryQueueItem nqqi = new QueryQueueItem(loc,nctx);
      addItem(nqqi,nn);
      linked = true;
    }
   
   return linked;
}



/********************************************************************************/
/*                                                                              */
/*      Find proper state for return from method                                */
/*                                                                              */
/********************************************************************************/

private IfaceState getReturnState(IfaceState st1)
{
   IfaceState st2 = st1;
   
   IfaceLocation loc = st1.getLocation();
   IfaceProgramPoint ppt = loc.getProgramPoint();
   IfaceAstReference ast = ppt.getAstReference();
   if (ast != null) {
      IfaceState st3 = st2;
      while (ast.getStatus() != null && ast.getStatus().getReason() == Reason.RETURN) {
         st2 = st3;
         st3 = getPriorReturnState(st2);
         if (st3 == null) break;
         loc = st3.getLocation();
         ppt = loc.getProgramPoint();
         ast = ppt.getAstReference();
       }
    }
   else {
      // handle byte-code return
    }
   
   return st2;
}



private IfaceState getPriorReturnState(IfaceState st)
{
   if (st.getNumPriorStates() > 1) return null;
   
   for (int i = 0; i < st.getNumPriorStates(); ++i) {
      IfaceState st1 = st.getPriorState(i);
      if (st1.getLocation() == null) {
         IfaceState st2 = getPriorReturnState(st1);
         if (st2 != null) return st2;
       }
      else return st1;
    }
   
   return null;
}




/********************************************************************************/
/*                                                                              */
/*      Add to processing queue                                                 */
/*                                                                              */
/********************************************************************************/

void addItem(QueryQueueItem qqi,QueryNode gn)
{
   if (!known_items.containsKey(qqi)) {
      FaitLog.logD("QUERY","Queue Node " + gn + " at " + qqi.getProgramPoint());
      known_items.put(qqi,gn);
      query_queue.addFirst(qqi);
    }
   else {
      QueryNode gn1 = known_items.get(qqi);
      if (gn1 != gn) {
         if (qqi.getProgramPoint() != gn1.getProgramPoint()) {
            QueryNode gn2 = gn1.getGraph().addNode(qqi.getCall(),qqi.getProgramPoint(),
                  qqi.getContext(),QueryNodeType.MERGE,"Control Flow Merge",gn);
            known_items.put(qqi,gn2);
            gn = gn2;
          }
	 FaitLog.logD("QUERY","Add Link to " + gn1 + " from " + gn + " for " + qqi.getProgramPoint()
          + " (" + gn1.getId() + ") <- (" + gn.getId() + ")");
	 gn.getGraph().addNode(gn1,gn);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Map from location to context                                            */
/*                                                                              */
/********************************************************************************/

private static class QueryContextMap {
   
   private Map<IfaceCall,Map<IfaceProgramPoint,QueryContext>> local_map;
   
   QueryContextMap() {
      local_map = new HashMap<>();
    }
   
   QueryContext get(IfaceCall c,IfaceProgramPoint ppt) {
      Map<IfaceProgramPoint,QueryContext> m1 = local_map.get(c);
      if (m1 == null) return null;
      return m1.get(ppt);
    }
   
   void put(IfaceCall c,IfaceProgramPoint ppt,QueryContext ctx) {
      Map<IfaceProgramPoint,QueryContext> m1 = local_map.get(c);
      if (m1 == null) {
         m1 = new HashMap<>();
         local_map.put(c,m1);
       }
      m1.put(ppt,ctx);
    }
   
}       // end of inner class QueryContextMap

}	// end of class QueryProcessor




/* end of QueryProcessor.java */





















































































































































































































