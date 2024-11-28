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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;

public interface QueryConstants extends FaitConstants
{


/********************************************************************************/
/*                                                                              */
/*      Node Types                                                              */
/*                                                                              */
/********************************************************************************/

enum QueryNodeType {
   START,
   COMPUTED,
   REFERENCE,
   CALL,
   RETURN,
   ENTRY,
   MERGE,
   STATE_TRANSITION,
}



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
   
   void setPriority(double p);
   
}       // end of interface QueryNode



/********************************************************************************/
/*                                                                              */
/*      Stack of calls while backward query processing                          */
/*                                                                              */
/********************************************************************************/

final class QueryCallSites {
   
   private final List<IfaceLocation> call_sites;
   
   QueryCallSites() {
      call_sites = null;
    }
   
   private QueryCallSites(List<IfaceLocation> locs) {
      call_sites = locs;
    }
   
   IfaceLocation getCallSite() {
      if (call_sites == null || call_sites.isEmpty()) return null;
      return call_sites.get(call_sites.size()-1);
    }
   
   QueryCallSites getPriorSites() {
      if (call_sites == null || call_sites.size() == 0) return this;
      if (call_sites.size() == 1) return new QueryCallSites();
      List<IfaceLocation> rslt = new ArrayList<>();
      for (int i = 0; i < call_sites.size()-1; ++i) rslt.add(call_sites.get(i));
      return new QueryCallSites(rslt);
    }
   
   
   QueryCallSites getNextSites(IfaceLocation from) {
      if (call_sites == null) {
         return new QueryCallSites(Collections.singletonList(from));
       }
      List<IfaceLocation> rslt = new ArrayList<>(call_sites);
      rslt.add(from);
      return new QueryCallSites(rslt);
    }
   
   
   List<IfaceLocation> getCallSites() {
      return call_sites;
    }
   
   @Override public boolean equals(Object o) {
      if (o instanceof QueryCallSites) {
         QueryCallSites qcs = (QueryCallSites) o;
         if (call_sites == null) {
            if (qcs.call_sites == null) return true;
          }
         else return call_sites.equals(qcs.call_sites);
       }
      return false;
    }
   
   @Override public int hashCode() {
      if (call_sites == null) return 0;
      return call_sites.hashCode();
    }
   
}       // end of inner class QueryCallSites

}	// end of interface QueryConstants




/* end of QueryConstants.java */

