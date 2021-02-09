/********************************************************************************/
/*										*/
/*		QueryContextSafetyCheck.java					*/
/*										*/
/*	Context for checking a safety condition 				*/
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceSafetyCheck;
import edu.brown.cs.fait.iface.IfaceSafetyStatus;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryContextSafetyCheck extends QueryContext implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final IfaceSafetyCheck	safety_check;
private final IfaceSafetyCheck.Value given_value;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

QueryContextSafetyCheck(IfaceControl ctrl,QueryCallSites sites,
      IfaceSafetyCheck chk,IfaceSafetyCheck.Value v)
{
   super(ctrl,sites);
   safety_check = chk;
   given_value = v;
}




/********************************************************************************/
/*										*/
/*	Next query item computation methods					*/
/*										*/
/********************************************************************************/

@Override protected QueryContext getPriorContextForCall(IfaceCall c,IfaceProgramPoint pt,
        QueryCallSites sites)
{
   return new QueryContextSafetyCheck(fait_control,sites,safety_check,given_value);
}


@Override protected QueryBackFlowData getPriorStateContext(IfaceState backfrom,IfaceState backto)
{
   return new QueryBackFlowData(this);
}


@Override protected List<QueryContext> getTransitionContext(IfaceState st0)
{
   List<QueryContext> rslt = new ArrayList<>();
   Set<IfaceSafetyCheck.Value> vals = getValues(st0);
   for (IfaceSafetyCheck.Value val : vals) {
      QueryContextSafetyCheck nchk = 
         new QueryContextSafetyCheck(fait_control,call_sites,safety_check,val);
      rslt.add(nchk);
    }

   return rslt;
}



@Override protected boolean isEndState(IfaceState st)
{
   if (given_value == null || given_value == safety_check.getInitialState()) 
      return true;
   
   return false;
}


@Override protected QueryContext getReturnContext(IfaceCall call)
{
   return this;
}



@Override protected boolean isPriorStateRelevant(IfaceState st0)
{
   Set<IfaceSafetyCheck.Value> vals = getValues(st0);
   if (!vals.contains(given_value)) return false;

   return true;
}



@Override protected boolean isReturnRelevant(IfaceState st0,IfaceCall call)
{
   Set<IfaceSafetyCheck.Value> svals = getValues(st0);
   IfaceSafetyStatus retsts = call.getResultSafetyStatus();
   IfaceSafetyStatus callsts = call.getStartState().getSafetyStatus();
   if (retsts != null && !retsts.getValue(safety_check).contains(given_value)) return false;
   Set<IfaceSafetyCheck.Value> pvals = callsts.getValue(safety_check);
   if (!intersects(pvals,svals)) return false;

   return true;
}


@Override protected QueryContext addRelevantArgs(QueryContext prior,IfaceState st0,QueryBackFlowData bfd)
{
   return prior;
}


@Override protected boolean handleInternalCall(IfaceState st0,QueryBackFlowData bfd,QueryNode n)
{  
   IfaceMethod im = st0.getLocation().getProgramPoint().getReferencedMethod();
   if (im != null && im.getName().equals("KarmaEvent")) {
      QueryGraph g = n.getGraph();
      QueryNode n1 = g.addNode(st0.getLocation().getCall(),st0.getLocation().getProgramPoint(),
            this,QueryNodeType.STATE_TRANSITION,"State Transition for event",n);
      g.markAsEndNode(n1);
    }
   return false;
}




/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

Set<IfaceSafetyCheck.Value> getValues(IfaceState st)
{
   if (st == null) return Collections.emptySet();
   IfaceSafetyStatus sts = st.getSafetyStatus();
   if (sts == null) return Collections.emptySet();
   return sts.getValue(safety_check);
}



static private boolean intersects(Set<?> s1,Set<?> s2)
{
   for (Object o : s1) {
      if (s2.contains(o)) return true;
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override protected void localOutputXml(IvyXmlWriter xw,IfaceProgramPoint where)
{
   xw.field("SAFETY",safety_check.getName());
   xw.field("VALUE",given_value);
}


@Override public String toString()
{
   return given_value.toString();
}




/********************************************************************************/
/*										*/
/*	Equality methods							*/
/*										*/
/********************************************************************************/

@Override public int hashCode()
{
   return safety_check.hashCode() + given_value.hashCode();
}



@Override public boolean equals(Object o)
{
   if (o == this) return true;

   if (o instanceof QueryContextSafetyCheck) {
      QueryContextSafetyCheck qc = (QueryContextSafetyCheck) o;
      if (safety_check != qc.safety_check) return false;
      if (given_value != qc.given_value) return false;
      return true;
    }
   return false;
}




}	// end of class QueryContextSafetyCheck




/* end of QueryContextSafetyCheck.java */

