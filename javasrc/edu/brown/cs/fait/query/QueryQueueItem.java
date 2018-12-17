/********************************************************************************/
/*                                                                              */
/*              QueryQueueItem.java                                             */
/*                                                                              */
/*      Data for back flow processing to answer query                           */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.fait.query;

import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;

class QueryQueueItem implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private final IfaceCall       for_call;
private final IfaceProgramPoint program_point;
private final QueryContext    query_context;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryQueueItem(IfaceLocation loc,QueryContext ctx)
{
   this(loc.getCall(),loc.getProgramPoint(),ctx);
}


QueryQueueItem(IfaceCall c,IfaceProgramPoint pt,QueryContext ctx)
{
   for_call = c;
   program_point = pt;
   query_context = ctx;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceCall getCall()                             { return for_call; }

IfaceProgramPoint getProgramPoint()             { return program_point; }

QueryContext getContext()                       { return query_context; }




/********************************************************************************/
/*                                                                              */
/*      Equality and comparison methods                                         */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object o)
{
   if (o instanceof QueryQueueItem) {
      QueryQueueItem qq = (QueryQueueItem) o;
      if (for_call != qq.for_call) return false;
      if (program_point != qq.program_point) return false;
      if (!query_context.equals(qq.query_context)) return false;
      // if (graph_node != qq.graph_node) return false;
      return true;
    }
   return false;
}


@Override public int hashCode()
{
   return for_call.hashCode() + program_point.hashCode() + 
        query_context.hashCode();
}



}       // end of class QueryQueueItem




/* end of QueryQueueItem.java */

