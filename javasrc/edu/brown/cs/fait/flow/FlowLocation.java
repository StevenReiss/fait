/********************************************************************************/
/*										*/
/*		FlowLocation.java						*/
/*										*/
/*	Implementation of a location for processing				*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
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



package edu.brown.cs.fait.flow;

import edu.brown.cs.fait.iface.*;


class FlowLocation implements FaitLocation, FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceCall       for_call;
private FaitInstruction for_instruction;
private FlowQueue       for_queue;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowLocation(FlowQueue fq,IfaceCall fc,FaitInstruction ins)
{
   for_queue = fq;
   for_call = fc;
   for_instruction = ins;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public FaitMethod getMethod()
{
   return for_call.getMethod();
}

@Override public IfaceCall getCall()		 { return for_call; }

@Override public FaitInstruction getInstruction()
{
   return for_instruction;
}


@Override public boolean sameBaseLocation(FaitLocation loc)
{
   return loc.getMethod() == getMethod() && loc.getInstruction() == for_instruction;
}

/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public boolean equals(Object o)
{
   if (o instanceof FlowLocation) {
      FlowLocation loc = (FlowLocation) o;
      return loc.for_call == for_call && loc.for_instruction == for_instruction;
    }

   return false;
}



@Override public int hashCode()
{
   int hc = 0;
   if (for_call != null) hc += for_call.hashCode();
   if (for_instruction != null) hc += for_instruction.hashCode();

   return hc;
}


/********************************************************************************/
/*                                                                              */
/*      Queueing methods                                                         */
/*                                                                              */
/********************************************************************************/

void queueLocation()
{
   for_queue.queueMethodChange(for_call,for_instruction);
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return for_call.getLogName() + " @ " + for_instruction.getIndex();
}

}	// end of class FlowLocation




/* end of FlowLocation.java */

