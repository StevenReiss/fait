/********************************************************************************/
/*										*/
/*		QueryConstants.java						*/
/*										*/
/*	Constants for handling user queries of the flow analysis		*/
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

import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceProgramPoint;

public interface QueryConstants
{


/********************************************************************************/
/*										*/
/*	Graph Node								*/
/*										*/
/********************************************************************************/

interface QueryNode { 

   QueryGraph getGraph();
   int getId();
   
   IfaceProgramPoint getProgramPoint();
   IfaceCall getCall();
   
}       // end of interface QueryNode



/********************************************************************************/
/*										*/
/*	Processing return value 						*/
/*										*/
/********************************************************************************/

class QueryContextNext {

   private IfaceCall for_call;
   private IfaceProgramPoint program_point;
   private QueryContext next_context;
   private QueryNode next_node;

   QueryContextNext(QueryContext ctx,QueryNode n) {
      next_context = ctx;
      next_node = n;
    }

   QueryContext getContext()			{ return next_context; }
   QueryNode getGraphNode()			{ return next_node; }
   IfaceCall getCall()				{ return for_call; }
   IfaceProgramPoint getProgramPoint()		{ return program_point; }

}	// end of inner class QueryContextNext



}	// end of interface QueryConstants




/* end of QueryConstants.java */

