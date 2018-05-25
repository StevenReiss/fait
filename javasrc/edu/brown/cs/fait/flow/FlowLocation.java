/********************************************************************************/
/*                                                                              */
/*              FlowLocation.java                                               */
/*                                                                              */
/*      Generic location                                                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
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



package edu.brown.cs.fait.flow;

import java.util.List;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceValue;

class FlowLocation implements IfaceLocation, FlowConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceCall             for_call;
private FlowQueue             for_queue;
private IfaceProgramPoint     program_point;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowLocation(FlowQueue fq,IfaceCall fc,IfaceProgramPoint pt)
{
   for_queue = fq;
   for_call = fc;
   program_point = pt;
   if (fc == null) {
      FaitLog.logD("Attempt to create a location without a call");
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods v                                                        */
/*                                                                              */
/********************************************************************************/

@Override public IfaceMethod getMethod()
{
   return for_call.getMethod();
}

@Override public IfaceCall getCall()		 { return for_call; }

@Override public IfaceProgramPoint getProgramPoint()
{
   return program_point;
}


@Override public void noteError(IfaceError er)
{
   if (er == null) return;
   if (for_call == null || program_point == null) return;
   for_call.addError(program_point,er);
}



public boolean sameBaseLocation(IfaceLocation loc)
{
   return loc.getMethod().equals(getMethod()) && program_point.equals(loc.getProgramPoint());
}




/********************************************************************************/
/*                                                                              */
/*      Queueing methods                                                        */
/*                                                                              */
/********************************************************************************/

void queueLocation() 
{
   for_queue.queueMethodChange(for_call,getProgramPoint());
}


@Override public void handleCallback(IfaceMethod fm,List<IfaceValue> args,String cbid)
{
   for_queue.handleCallback(fm,args,cbid);
}



/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

public boolean equals(Object o)
{
   if (o instanceof FlowLocation) {
      FlowLocation loc = (FlowLocation) o;
      if (for_call == null) {
         if (loc.for_call != null) return false;
         return program_point.equals(loc.getProgramPoint());
       }
      return for_call.equals(loc.for_call) && program_point.equals(loc.getProgramPoint());
    }
   return false;
}


public int hashCode() 
{
   int hc = 0;
   if (for_call != null) hc += for_call.hashCode();
   if (program_point != null) hc += program_point.hashCode();
   return hc;
}



/********************************************************************************/
/*                                                                              */
/*      Debugging methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   if (for_call == null) return "?@" + program_point;
   
   return for_call.getLogName() + "@" + program_point;
}




}       // end of class FlowLocation




/* end of FlowLocation.java */

