/********************************************************************************/
/*										*/
/*		QueryFactory.java						*/
/*										*/
/*	External facade for handling user queries of the flow analysis		*/
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

import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAstStatus;
import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceSafetyCheck;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceAstStatus.Reason;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class QueryFactory implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl	fait_control;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public QueryFactory(IfaceControl ctrl)
{
   fait_control = ctrl;
}



/********************************************************************************/
/*										*/
/*	Basic query processing							*/
/*										*/
/********************************************************************************/

public void processErrorQuery(IfaceCall call,IfaceProgramPoint pt,IfaceError err,
      IvyXmlWriter output)
{
   QueryContext ctx = null;

   if (err.getSubtype() != null) {
      IfaceState st0 = fait_control.findStateForLocation(call,pt);
      int sloc = err.getStackLocation();
      if (sloc < 0) return;
      IfaceValue v0 = st0.getStack(sloc);
      if (v0 != null) {
	 v0 = QueryFactory.dereference(v0,st0);
	 IfaceValue refv = fait_control.findRefStackValue(v0.getDataType(),sloc);
	 IfaceSubtype.Value stv = getRelevantSubtypeValue(v0,err.getSubtype());
	 ctx = new QueryContextSubtype(fait_control,refv,stv);
       }
    }
   if (err.getSafetyCheck() != null) {
      IfaceSafetyCheck.Value v = err.getSafetyValue();
      if (v == null) return;
      ctx = new QueryContextSafetyCheck(fait_control,err.getSafetyCheck(),v);
    }

   if (ctx == null) return;

   QueryGraph graph = new QueryGraph();
   QueryNode node = graph.addStartNode(call,pt,ctx,err.getErrorMessage());
   QueryQueueItem qitem = new QueryQueueItem(call,pt,ctx);
   QueryProcessor qp = new QueryProcessor(fait_control,qitem,node);
   qp.process();
   // graph.outputXml(output);	   // for debugging -- remove when clean works
   graph.cleanGraph();

   graph.outputXml(output);
}


public void processVariableQuery(IfaceCall call,IfaceProgramPoint pt,int offset,
      IvyXmlWriter output)
{
   // get ASTNode for offset
}



public static IfaceAuxReference getAuxReference(IfaceLocation loc,IfaceValue ref)
{
   return new QueryAuxReference(loc,ref);
}


/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

static IfaceValue dereference(IfaceValue value,IfaceState st0)
{
   IfaceValue v0 = value;
   while (v0 != null && v0.isReference()) {
      if (v0.getRefStack() >= 0) {
	 v0 = st0.getStack(v0.getRefStack());
       }
      else if (v0.getRefSlot() >= 0) {
	 v0 = st0.getLocal(v0.getRefSlot());
       }
      else if (v0.getRefField() != null) {
	 IfaceValue v1 = st0.getFieldValue(v0.getRefField());
         if (v1 == null) break;
         else v0 = v1;
       }
      else if (v0.getRefBase() != null && v0.getRefIndex() != null) {
         IfaceValue v1 = v0.getRefBase().getArrayContents();
         if (v1 != null) v0 = v1;
	 else v0 = null;
       }
      else v0 = null;
    }

   if (v0 == null && value.getRefStack() == 0) {
      IfaceLocation loc = st0.getLocation();
      IfaceProgramPoint pt = loc.getProgramPoint();
      IfaceAstReference ar = pt.getAstReference();
      if (ar != null) {
	 IfaceAstStatus sts = ar.getStatus();
	 if (sts != null) {
	    if (sts.getReason() == Reason.RETURN) {
	       v0 = sts.getValue();
	     }
	  }
       }
    }

   if (v0 == null) 
      v0 = value;

   return v0;
}



static IfaceSubtype.Value getRelevantSubtypeValue(IfaceValue v0,IfaceSubtype st)
{
   IfaceType t0 = v0.getDataType();
   IfaceSubtype.Value sv0 = t0.getValue(st);
   List<IfaceValue> cnts = v0.getContents();
   if (cnts != null) {
      for (IfaceValue vc : cnts) {
	 for (IfaceEntity ent : vc.getEntities()) {
	    IfaceType tc = ent.getDataType();
	    IfaceSubtype.Value csv0 = tc.getValue(st);
	    sv0 = st.getMergeValue(sv0,csv0);
	  }
       }
    }
   return sv0;
}




}	// end of class QueryFactory




/* end of QueryFactory.java */

