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
import java.util.Map;
import java.util.Deque;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceState;

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
      QueryNode node = known_items.get(qqi);
      qqi.getContext().computeNext(this,qqi,st0,node);
    }
}


void addItem(QueryQueueItem qqi,QueryNode gn)
{
   if (!known_items.containsKey(qqi)) {
      FaitLog.logD("Queue Node " + gn + " at " + qqi.getProgramPoint());
      known_items.put(qqi,gn);
      query_queue.addFirst(qqi);
    }
   else {
      QueryNode gn1 = known_items.get(qqi);
      if (gn1 != gn) {
         if (qqi.getProgramPoint() != gn1.getProgramPoint()) {
            QueryNode gn2 = gn1.getGraph().addNode(qqi.getCall(),qqi.getProgramPoint(),
                  qqi.getContext(),"Control Flow Merge",gn);
            known_items.put(qqi,gn2);
            gn = gn2;
          }
	 FaitLog.logD("Add Link to " + gn1 + " from " + gn + " for " + qqi.getProgramPoint());
	 gn.getGraph().addNode(gn1,gn);
       }
    }
}



}	// end of class QueryProcessor




/* end of QueryProcessor.java */





















































































































































































































