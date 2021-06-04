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

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract class QueryContext implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected final IfaceControl fait_control;
protected final QueryCallSites call_sites;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected QueryContext(IfaceControl fc,QueryCallSites stack)
{
   fait_control = fc;
   if (stack == null) stack = new QueryCallSites();
   call_sites = stack;
}



/********************************************************************************/
/*                                                                              */
/*      General access methods                                                  */
/*                                                                              */
/********************************************************************************/

final QueryCallSites getCallSites()
{
   return call_sites;
}



/********************************************************************************/
/*                                                                              */
/*      Context-specific methods for auxilliary references                      */
/*                                                                              */
/********************************************************************************/

protected QueryContext newReference(IfaceValue newref,QueryCallSites sites,
      IfaceState newstate,IfaceState oldstate)
{
   return this;
}



/********************************************************************************/
/*										*/
/*	Context-dependent methods						*/
/*										*/
/********************************************************************************/

/**
 *      Processing is at the start of a method not explicitly instantiated.  
 *      Create a context for any call sites.  This could involve, for example,
 *      mapping any references to parameter variables to the proper stack
 *      location on a call.
 *
 *      Should return null if the call should not be pursued.
 ***/

protected abstract QueryContext getPriorContextForCall(IfaceCall c,IfaceProgramPoint pt,
        QueryCallSites sites);


protected abstract QueryBackFlowData getPriorStateContext(IfaceState backfrom,IfaceState backto);

protected boolean isEndState(IfaceState state)
{
   return false;
}

   // we are a given point inside a method.  Get the state by undoing the computation
   //	 at the location provided by state
   // return null if we know this state is irrelevant


protected List<QueryContext> getTransitionContext(IfaceState st0)
{ 
   return null;
}
   // return a set of contexts that represent a transition from that context
   //	 to the current one



protected abstract QueryContext getReturnContext(IfaceLocation loc);
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

protected boolean followCalls()
{
   return false;
}

boolean isCallRelevant(IfaceCall callfrom,IfaceCall callto)
{
   return true;
}

protected abstract QueryContext addRelevantArgs(QueryContext priorctx,IfaceState st0,QueryBackFlowData bfd);


protected boolean handleInternalCall(IfaceState st0,QueryBackFlowData bfd,QueryNode n)
{
   return false;
}


protected String addToGraph(QueryContext ctx,IfaceState st0)
{
   return null;
}


protected double getNodePriority()
{
   return 0;
}


/**
 *      Given another context for the given location (call,program point), return a merged
 *      context that is relevant to both.  This should return null if contexts should not 
 *      be merged.  It can also return either this or the passed in context if that is
 *      appropriate.
 **/

protected QueryContext mergeWith(QueryContext ctx)
{
   return null;
}


protected List<IfaceAuxReference> getArgumentReferences(IfaceState st0,boolean argvalues,boolean thisval)
{
   List<IfaceAuxReference> rslt = new ArrayList<>();
   
   IfaceProgramPoint pt = st0.getLocation().getProgramPoint();
   IfaceMethod mthd = pt.getCalledMethod();
   
   if (argvalues && mthd.getReturnType() != null && !mthd.getReturnType().isVoidType()) {
      int ct = mthd.getNumArgs();
      int ct1 = (mthd.isStatic() ? 0 : 1);
      for (int i = 0; i < ct+ct1; ++i) {
         IfaceValue vs = st0.getStack(i);
         vs = QueryFactory.dereference(fait_control,vs,st0);
         IfaceValue vr = fait_control.findRefStackValue(vs.getDataType(),i);
         IfaceAuxReference ref = 
            fait_control.getAuxReference(st0.getLocation(),vr,IfaceAuxRefType.ARGUMENT);
         rslt.add(ref);
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
                     rslt.addAll(refs);
                   }
                }
             }
          }
       }
    }
   else if (!mthd.isStatic() && thisval) {
      int ct = mthd.getNumArgs();
      for (int i = 0; i < ct; ++i) {
         IfaceValue vs = st0.getStack(i);
         IfaceValue vr = fait_control.findRefStackValue(vs.getDataType(),i);
         IfaceAuxReference ref = 
            fait_control.getAuxReference(st0.getLocation(),vr,IfaceAuxRefType.ARGUMENT);
         rslt.add(ref);
       }
    }
   
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

final void outputXml(IvyXmlWriter xw,IfaceProgramPoint where)
{
   xw.begin("CONTEXT");
   localOutputXml(xw,where);
   xw.end("CONTEXT");
}


abstract protected void localOutputXml(IvyXmlWriter xw,IfaceProgramPoint where);




/********************************************************************************/
/*										*/
/*	Equality methods							*/
/*										*/
/********************************************************************************/

@Override public abstract boolean equals(Object o);

@Override public abstract int hashCode();



}	// end of class QueryContext




/* end of QueryContext.java */

