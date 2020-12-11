/********************************************************************************/
/*										*/
/*		QueryContext.java						*/
/*										*/
/*	Context for backward search						*/
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

import java.util.Collection;
import java.util.List;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceAstStatus.Reason;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract class QueryContext implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected final IfaceControl fait_control;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected QueryContext(IfaceControl fc)
{
   fait_control = fc;
}


protected QueryContext newReference(IfaceValue newref,IfaceState newstate,IfaceState oldstate)
{
   return this;
}



/********************************************************************************/
/*										*/
/*	Next State computation methods						*/
/*										*/
/********************************************************************************/

final void computeNext(QueryProcessor qp,QueryQueueItem qqi,IfaceState cur,QueryNode node)
{
   FaitLog.logD("Compute Next: " + " " + localDisplayContext() + " " +
	 qqi.getProgramPoint() + " " + qqi.getCall().getMethod() + " (" + node.getId() + ")");
   FaitLog.logD("Next Info: " + cur + " " + qqi.getCall().hashCode());

   if (cur == null)
      return;

   IfaceCall call = qqi.getCall();
   IfaceProgramPoint pt = qqi.getProgramPoint();
   
   if (cur.isStartOfMethod()) {
      // need to handle case where we initiated the call -- go to call site rather than
      // all call sites
      node.getGraph().markAsEndNode(node);	// allow this to be a starting point
      QueryContext priorctx = getPriorContextForCall(call,pt);
      if (priorctx == null) {
	 return;
       }
      QueryGraph graph = node.getGraph();
      node = graph.addNode(call,pt,this,"Start of Method " + call.getMethod().getName(),node);
      for (IfaceCall call0 : call.getAlternateCalls()) {
	 for (IfaceLocation callloc : call0.getCallSites()) {
            if (!isCallRelevant(call0,callloc.getCall())) continue;
	    IfaceState st0 = fait_control.findStateForLocation(callloc.getCall(),
		  callloc.getProgramPoint());
	    if (!priorctx.isPriorStateRelevant(st0)) continue;
	    QueryNode nn = graph.addNode(callloc.getCall(),callloc.getProgramPoint(),priorctx,
		  "Call to Method " + call.getMethod().getName(),node);
	    QueryQueueItem nqqi = new QueryQueueItem(callloc,priorctx);
	    qp.addItem(nqqi,nn);
	  }
       }
    }
   else {
      for (int i = 0; i < cur.getNumPriorStates(); ++i) {
	 IfaceState st0 = cur.getPriorState(i);
         FaitLog.logD("PRIOR STATE " + i + " " + st0);
	 handleFlowFrom(cur,st0,qp,node);
       }
    }
}



protected String localDisplayContext()
{
   return "";
}



/********************************************************************************/
/*										*/
/*	Handle state flow within method 					*/
/*										*/
/********************************************************************************/

private void handleFlowFrom(IfaceState backfrom,IfaceState st0,QueryProcessor qp,QueryNode node)
{
   if (st0.getLocation() == null) {
      // handle intermediate states
      for (int i = 0; i < st0.getNumPriorStates(); ++i) {
	 IfaceState st1 = st0.getPriorState(i);
	 handleFlowFrom(backfrom,st1,qp,node);
       }
      return;
    }

   QueryBackFlowData bfd = getPriorStateContext(backfrom,st0);
   QueryContext priorctx = bfd.getContext();
   addRelevantArgs(st0,bfd);
   boolean islinked = false;

   if (bfd.getAuxRefs() != null) {
      for (IfaceAuxReference aref : bfd.getAuxRefs()) {
	 islinked |= handleAuxReference(aref,qp,node,st0);
       }
    }

   if (priorctx == null && !islinked) node.getGraph().markAsEndNode(node);

   if (priorctx != null && priorctx.isPriorStateRelevant(st0)) {
      String reason = addToGraph(priorctx,st0);
      if (reason != null) {
         QueryGraph graph = node.getGraph();
         IfaceLocation ploc = st0.getLocation();
         node = graph.addNode(ploc.getCall(),ploc.getProgramPoint(),priorctx,
               reason,node);
       }
      node.setPriority(priorctx.getNodePriority());
      QueryQueueItem nqqi = new QueryQueueItem(st0.getLocation(),priorctx);
      qp.addItem(nqqi,node);
    }
   else if (st0.isMethodCall()) {
      // svals are the conditions at start of call
      IfaceCall call2 = st0.getLocation().getCall();
      QueryContext retctx = getReturnContext(call2);
      if (retctx == null) return;
      IfaceProgramPoint ppt2  = st0.getLocation().getProgramPoint();
      if (call2.getAllMethodsCalled(ppt2).isEmpty()) {
	 islinked |= handleInternalCall(st0,bfd,node);
	 if (!islinked) node.getGraph().markAsEndNode(node);
       }
      else {
	 for (IfaceCall from : call2.getAllMethodsCalled(ppt2)) {
	    // if return did not include value of interest, skip
	    if (!retctx.isReturnRelevant(st0,from)) continue;
	    for (IfaceState st1 : from.getReturnStates()) {
	       if (!retctx.isPriorStateRelevant(st1)) continue;
               // get the return expression state
               IfaceState st2 = getReturnState(st1);
	       QueryGraph graph = node.getGraph();
	       QueryNode nn = graph.addNode(from,st2.getLocation().getProgramPoint(),retctx,
		     "Result of method " + from.getMethod().getName(),node);
	       QueryQueueItem nqqi = new QueryQueueItem(st2.getLocation(),retctx);
	       qp.addItem(nqqi,nn);
	     }
	  }
       }
    }
   else if (priorctx != null) {
      List<QueryContext> nctxs = priorctx.getTransitionContext(st0);
      if (nctxs != null && nctxs.size() > 0) {
	 for (QueryContext ctx : nctxs) {
	    QueryGraph graph = node.getGraph();
	    IfaceLocation ploc = st0.getLocation();
	    QueryNode nn = graph.addNode(ploc.getCall(),ploc.getProgramPoint(),ctx,
		  "State Transition",node);
	    if (ctx.isEndState(st0)) {
	       graph.markAsEndNode(nn);
	       continue;
	     }
	    QueryQueueItem nqqi = new QueryQueueItem(ploc,ctx);
	    qp.addItem(nqqi,nn);
	  }
       }
      else {
	 node.getGraph().markAsEndNode(node);
       }
    }
   // STILL need to handle flows based on exceptions
}



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
   
   return st1;
}

   
   
private IfaceState getPriorReturnState(IfaceState st)
{
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

final void handleInitialReferences(Collection<IfaceAuxReference> refs,QueryProcessor qp,
      QueryNode nd,IfaceState st0)
{
   for (IfaceAuxReference ref : refs) {
      handleAuxReference(ref,qp,nd,st0);
    }
}


private boolean handleAuxReference(IfaceAuxReference ref,QueryProcessor qp,QueryNode node,IfaceState st0)
{
   IfaceLocation loc = ref.getLocation();
   IfaceValue v0 = ref.getReference();
   IfaceState st1 = fait_control.findStateForLocation(loc);
   boolean linked = false;

   QueryGraph graph = node.getGraph();
   QueryContext nctx = newReference(v0,st0,st1);
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
      QueryNode nn = graph.addNode(loc.getCall(),loc.getProgramPoint(),nctx,desc,node);
      QueryQueueItem nqqi = new QueryQueueItem(loc,nctx);
      qp.addItem(nqqi,nn);
      linked = true;
    }

   return linked;
}




/********************************************************************************/
/*										*/
/*	Context-dependent methods						*/
/*										*/
/********************************************************************************/

protected abstract QueryContext getPriorContextForCall(IfaceCall c,IfaceProgramPoint pt);
   // we are at the start of a method. Create the context for a call site -- i.e.
   //	if there is a variable that is a parameter, need to map that to the proper
   //	stack location for that parameter.
   // return null if we know the call is irrelevant



protected abstract QueryBackFlowData getPriorStateContext(IfaceState backfrom,IfaceState backto);

protected boolean isEndState(IfaceState state)
{
   return false;
}

   // we are a given point inside a method.  Get the state by undoing the computation
   //	 at the location provided by state
   // return null if we know this state is irrelevant


protected abstract List<QueryContext> getTransitionContext(IfaceState st0);
   // return a set of contexts that represent a transition from that context
   //	 to the current one



protected abstract QueryContext getReturnContext(IfaceCall call);
   // compute the context at a return point given the current context.	The state st0
   //	 can be used to determine what is being called
   // return null if this method call is not relevant to the context



protected abstract boolean isPriorStateRelevant(IfaceState st0);
   // return true if the state st0 is relevant to the given context


protected boolean isReturnRelevant(IfaceState st0,IfaceCall call)
{
   // return true if the return should be investigated
   return true;
}


boolean isCallRelevant(IfaceCall callfrom,IfaceCall callto)
{
   return true;
}

protected abstract void addRelevantArgs(IfaceState st0,QueryBackFlowData bfd);


protected boolean handleInternalCall(IfaceState st0,QueryBackFlowData bfd,QueryNode n)
{
   return false;
}


protected String addToGraph(QueryContext ctx,IfaceState st0)
{
   return null;
}


protected int getNodePriority()
{
   return 0;
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw,IfaceProgramPoint where)
{
   xw.begin("CONTEXT");
   localOutputXml(xw,where);
   xw.end("CONTEXT");
}


abstract protected void localOutputXml(IvyXmlWriter xw,IfaceProgramPoint where);



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return localDisplayContext();
}




/********************************************************************************/
/*										*/
/*	Equality methods							*/
/*										*/
/********************************************************************************/

@Override public abstract boolean equals(Object o);

@Override public abstract int hashCode();



}	// end of class QueryContext




/* end of QueryContext.java */

