/********************************************************************************/
/*										*/
/*		QueryGraph.java 						*/
/*										*/
/*	Representation of a flow graph subset relevant to a query		*/
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryGraph implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<Node> start_nodes;
private Set<Node> end_nodes;
private List<Node> all_nodes;

private static AtomicInteger node_counter = new AtomicInteger(0);



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

QueryGraph()
{
   start_nodes = new HashSet<>();
   all_nodes = new ArrayList<>();
   end_nodes = new HashSet<>();
}



/********************************************************************************/
/*										*/
/*	Graph Creation methods							*/
/*										*/
/********************************************************************************/

QueryNode addStartNode(IfaceCall call,IfaceProgramPoint pt,QueryContext ctx,String reason)
{
   Node n = new Node(call,pt,ctx,QueryNodeType.START,reason);
   start_nodes.add(n);
   all_nodes.add(n);

   FaitLog.logD("QUERY","START NODE " + n + " at " + pt + " " + n.getId());

   return n;
}

void markAsEndNode(QueryNode qn)
{
   FaitLog.logD("QUERY","MARK AS END NODE " + qn + " (" + qn.getId() + ")");
   end_nodes.add((Node) qn);
}



QueryNode addNode(IfaceCall call,IfaceProgramPoint pt,QueryContext ctx,
      QueryNodeType typ,String reason,QueryNode prior)
{
   Node n = new Node(call,pt,ctx,typ,reason);
   addNode(n,prior);
   all_nodes.add(n);

   FaitLog.logD("QUERY","Create Node " + n + " at " + pt + ": " + reason + " (" + n.getId() + ") -> (" + prior.getId() + ")");

   return n;
}


void addNode(QueryNode fromnode,QueryNode tonode)
{
   if (fromnode == tonode) return;
   if (tonode != null && fromnode != null) {
      Node tn = (Node) tonode;
      Node fn = (Node) fromnode;
      if (fn.linksTo(tn)) return;
      Arc a = new Arc(fn,tn);
      fn.addToArc(a);
      tn.addFromArc(a);
    }
}



void mergeNodes(QueryNode node,QueryNode into)
{
   FaitLog.logD("QUERY","MERGE NODE " + node.getId() + " INTO " +
         into.getId());
   all_nodes.remove(node);
   // might need to do more here
}



/********************************************************************************/
/*										*/
/*	Graph cleanup routines							*/
/*										*/
/********************************************************************************/

void cleanGraph()
{
   for (Iterator<Node> it = end_nodes.iterator(); it.hasNext(); ) {
      Node n = it.next();
      if (!n.getFromNodes().isEmpty()) {
         it.remove();
         FaitLog.logD("QUERY","CLEAN: UNMARK END NODE " + n.getId());
       }
    }
   
   removeDeadNodes();
   removeRedundantNodes();
}



private void removeDeadNodes()
{
   Queue<Node> workqueue = new LinkedList<>(all_nodes);

   while (!workqueue.isEmpty()) {
      Node n = workqueue.remove();
      if (n.getFromNodes().isEmpty()) {
	 if (!end_nodes.contains(n)) {
	    for (Node n1 : n.getToNodes()) {
	       workqueue.add(n1);
	     }
	    workqueue.remove(n);
	    FaitLog.logD("QUERY","CLEAN: DEAD NODE " + n.getId());
	    removeNode(n);
	  }
       }
    }
}



private void removeRedundantNodes()
{
   Queue<Node> workq = new LinkedList<>();
   List<Node> ends = new ArrayList<>(end_nodes);
   int ct = ends.size();
   for (int i = 0; i < ct; ++i) {
      Node ni = ends.get(i);
      if (!end_nodes.contains(ni)) continue;
      workq.add(ni);
      for (int j = i+1; j < ct; ++j) {
	  Node nj = ends.get(j);
	  if (equivalentNodes(ni,nj)) {
	     replaceNodeWith(nj,ni);
	     FaitLog.logD("QUERY","Clean: remove end node " + nj.getId() + " for " + ni.getId());
	   }
       }
    }

   Set<Node> done = new HashSet<>(workq);
   while (!workq.isEmpty()) {
      Node n = workq.remove();
      List<Node> eq = new ArrayList<>();
      for (Node n1 : n.getFromNodes()) {
	 if (n == n1) continue;
	 if (done.add(n1)) workq.add(n1);
	 for (Node n2 : n1.getToNodes()) {
	    if (n != n2 && equivalentNodes(n,n2)) {
	       eq.add(n2);
	     }
	  }
       }
      for (Node n1 : eq) {
	 replaceNodeWith(n1,n);
	 FaitLog.logD("QUERY","Clean: remove duplicate node " + n1.getId() + " for " + n.getId());

	 workq.remove(n1);
	 done.clear();
       }
    }
}



private void removeNode(Node n)
{
   FaitLog.logD("QUERY","REMOVE NODE " + n.getId());
   for (Node n1 : n.getToNodes()) {
      n1.removeArcFrom(n);
    }
   for (Node n1 : n.getFromNodes()) {
      n1.removeArcTo(n);
    }
   all_nodes.remove(n);
   start_nodes.remove(n);
   end_nodes.remove(n);
}


private boolean equivalentNodes(Node n1,Node n2)
{
   if (n1 == n2) return true;
   if (n1.getProgramPoint() != n2.getProgramPoint()) return false;
   if (n1.getCall().getMethod() != n2.getCall().getMethod()) return false;
   List<Node> n1f = n1.getFromNodes();
   List<Node> n2f = n2.getFromNodes();
   if (n1f.containsAll(n2f) || n2f.containsAll(n1f)) return true;
   return false;
}


private void replaceNodeWith(Node nold,Node nnew)
{
   FaitLog.logD("QUERY","REPLACE NODE " + nold.getId() + " INTO " +
         nnew.getId());
   for (Node np : nold.getToNodes()) {
      if (!np.linksTo(nnew)) {
	 addNode(nnew,np);
       }
    }
   for (Node nf : nold.getFromNodes()) {
      if (!nnew.linksFrom(nf)) addNode(nf,nnew);
    }
   removeNode(nold);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw,long time)
{
   xw.begin("GRAPH");
   xw.field("TIME",time);
   xw.field("SIZE",all_nodes.size());
   for (Node n : all_nodes) {
      n.outputXml(xw);
    }
   xw.end("GRAPH");
}



/********************************************************************************/
/*										*/
/*	Node -- graph node representation					*/
/*										*/
/********************************************************************************/

private class Node implements QueryNode {

   private int node_id;
   private IfaceCall for_call;
   private IfaceProgramPoint at_point;
   private QueryContext node_context;
   private List<Arc> to_arcs;
   private List<Arc> from_arcs;
   private String node_reason;
   private double node_priority;
   private QueryNodeType node_type;

   Node(IfaceCall c,IfaceProgramPoint pt,QueryContext ctx,QueryNodeType typ,String reason) {
      node_id = node_counter.incrementAndGet();
      for_call = c;
      at_point = pt;
      node_context = ctx;
      to_arcs = new ArrayList<>();
      from_arcs = new ArrayList<>();
      node_type = typ;
      node_reason = reason;
      node_priority = 0;
      if (ctx == null) {
         System.err.println("CHECK HERE");
       }
    }

   @Override public int getId() 				{ return node_id; }
   @Override public IfaceProgramPoint getProgramPoint() 	{ return at_point; }
   @Override public IfaceCall getCall() 			{ return for_call; }
   
   @Override public void setPriority(double p) {
      node_priority = Math.max(node_priority,p);
    }
   
   List<Node> getToNodes() {
      List<Node> rslt = new ArrayList<>();
      for (Arc a : to_arcs) {
         Node n = a.getToNode();
         rslt.add(n);
       }
      return rslt;
    }

   List<Node> getFromNodes() {
      List<Node> rslt = new ArrayList<>();
      for (Arc a : from_arcs) {
         Node n = a.getFromNode();
         rslt.add(n);
       }
      return rslt;
    }

   void addToArc(Arc a) {
      to_arcs.add(a);
    }

   void addFromArc(Arc a) {
      from_arcs.add(a);
    }

   void removeArcTo(Node n) {
      for (Iterator<Arc> it = to_arcs.iterator(); it.hasNext(); ) {
         Arc a = it.next();
         if (a.getToNode() == n) it.remove();
       }
    }

   void removeArcFrom(Node n) {
      for (Iterator<Arc> it = from_arcs.iterator(); it.hasNext(); ) {
         Arc a = it.next();
         if (a.getFromNode() == n) it.remove();
       }
    }

   boolean linksTo(Node n) {
      for (Arc a : to_arcs) {
         if (a.getToNode() == n) return true;
       }
      return false;
    }

   boolean linksFrom(Node n) {
      for (Arc a : from_arcs) {
	 if (a.getFromNode() == n) return true;
       }
      return false;
    }


   @Override public QueryGraph getGraph() {
      return QueryGraph.this;
    }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("NODE");
      xw.field("ID",node_id);
      xw.field("TYPE",node_type);
      xw.field("REASON",node_reason);
      if (start_nodes.contains(this)) xw.field("START",true);
      if (end_nodes.contains(this)) xw.field("END",true);
      xw.field("METHOD",for_call.getMethod().getFullName());
      xw.field("SIGNATURE",for_call.getMethod().getDescription());
      xw.field("CALLID",for_call.hashCode());
      xw.field("FILE",for_call.getMethod().getFile());
      if (node_priority != 0) xw.field("PRIORITY",node_priority);
      node_context.outputXml(xw,at_point);
      at_point.outputXml(xw);
      for (Arc a : to_arcs) {
         xw.textElement("TO",a.getToNode().getId());
       }
      for (Arc a : from_arcs) {
         xw.textElement("FROM",a.getFromNode().getId());
       }
      
      IfaceAstReference where = at_point.getAstReference();
      if (where != null) {
         xw.begin("LOCATION");
         xw.field("FILE",for_call.getMethod().getFile());
         ASTNode node = where.getAstNode();
         xw.field("OFFSET",node.getStartPosition());
         xw.field("LENGTH",node.getLength());
         xw.field("TYPE","Function");
         xw.begin("ITEM");
         xw.field("NAME",where.getMethod().getName());
         xw.field("QNAME",where.getMethod().getFullName());
         xw.field("TYPE","Function");
         ASTNode n = where.getMethod().getStart().getAstReference().getAstNode();
         xw.field("STARTOFFSET",n.getStartPosition());
         xw.field("LENGTH",n.getLength());
         String key = where.getMethod().getFullName();
         key += where.getMethod().getDescription();
         xw.field("HANDLE",key);
         xw.end("ITEM");
         xw.end("LOCATION");
       }
      else {
         // would like to get information here so we don't have to 
         // preload all project files
       }
      
      xw.end("NODE");
    }

   @Override public String toString() {
      return node_id + ":" + node_context + " " + to_arcs.size() + " " + from_arcs.size();
    }

}	// end of inner class Node




/********************************************************************************/
/*										*/
/*	Arc -- graph node arc representation					*/
/*										*/
/********************************************************************************/

private class Arc {

   private Node from_node;
   private Node to_node;

   Arc(Node f,Node t) {
      from_node = f;
      to_node = t;
    }

   Node getFromNode()			{ return from_node; }
   Node getToNode()			{ return to_node; }

   @Override public String toString() {
      return from_node.toString() + "->" + to_node.toString();
    }

}	// end of inner class Arc




}      // end of class QueryGraph




/* end of QueryGraph.java */

