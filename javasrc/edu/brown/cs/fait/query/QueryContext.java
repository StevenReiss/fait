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

import java.util.List;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceValue;
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


protected QueryContext newReference(IfaceValue newref)
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

   IfaceCall call = qqi.getCall();
   IfaceProgramPoint pt = qqi.getProgramPoint();
   
   if (cur.isStartOfMethod()) {
      // need to handle case where we initiated the call -- go to call site rather than
      // all call sites
      QueryContext priorctx = getPriorContextForCall(call,pt);
      if (priorctx == null) return;
      QueryGraph graph = node.getGraph();
      node = graph.addNode(call,pt,this,"Start of Method " + call.getMethod().getName(),node);
      for (IfaceCall call0 : call.getAlternateCalls()) {
	 for (IfaceLocation callloc : call0.getCallSites()) {
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
   
   if (bfd.getAuxReferences() != null) {
      IfaceLocation loc = st0.getLocation();
      for (IfaceValue v0 : bfd.getAuxReferences()) {
	 // need to check relevance here
	 QueryGraph graph = node.getGraph();
	 QueryContext nctx = newReference(v0);
	 if (nctx.isPriorStateRelevant(st0)) {
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
	  }
       }
    }
   if (bfd.getAuxField() != null) {
      // queue all settings of field that are compatible
    }
   if (bfd.getAuxArray() != null) {
      // queue all assignments to array elements that are compatible
    }

   if (priorctx == null) {
      node.getGraph().markAsEndNode(node);
      return;
    }   
   if (priorctx.isPriorStateRelevant(st0)) {
      QueryQueueItem nqqi = new QueryQueueItem(st0.getLocation(),priorctx);
      qp.addItem(nqqi,node);	
    }
   else if (st0.isMethodCall()) {
      // svals are the conditions at start of call
      IfaceCall call2 = st0.getLocation().getCall();
      QueryContext retctx = getReturnContext(call2);
      if (retctx == null) return;
      IfaceProgramPoint ppt2  = st0.getLocation().getProgramPoint();

      // Set<IfaceSafetyCheck.Value> svals = getValues(st0);
      for (IfaceCall from : call2.getAllMethodsCalled(ppt2)) {
	 // if return did not include value of interest, skip
	 if (!retctx.isReturnRelevant(st0,from)) continue;
	 for (IfaceState st1 : from.getReturnStates()) {
	    if (!retctx.isPriorStateRelevant(st1)) continue;
	    QueryGraph graph = node.getGraph();
	    QueryNode nn = graph.addNode(from,st1.getLocation().getProgramPoint(),retctx,
                  "Result of method " + from.getMethod().getName(),node);
	    QueryQueueItem nqqi = new QueryQueueItem(st1.getLocation(),retctx);
	    qp.addItem(nqqi,nn);
	  }
       }
    }
   else {
      List<QueryContext> nctxs = priorctx.getTransitionContext(st0);
      if (nctxs.size() > 0) {
         for (QueryContext ctx : priorctx.getTransitionContext(st0)) {
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

protected abstract boolean isEndState(IfaceState state);
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


protected abstract boolean isReturnRelevant(IfaceState st0,IfaceCall call);
   // return true if the return should be investigated

protected abstract void addRelevantArgs(IfaceState st0,QueryBackFlowData bfd);



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

