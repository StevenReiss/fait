/********************************************************************************/
/*                                                                              */
/*              FlowQueueInstance.java                                          */
/*                                                                              */
/*      Method and set of instructions to evaluate flows for                    */
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

import edu.brown.cs.fait.iface.*;

import java.util.*;


class FlowQueueInstance implements FlowConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LinkedList<FaitInstruction>      work_list;
private IfaceCall                        for_call;
private Map<FaitInstruction,IfaceState>  state_map;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowQueueInstance(IfaceCall fc)
{
   for_call = fc;
   work_list = new LinkedList<FaitInstruction>();
   state_map = new HashMap<FaitInstruction,IfaceState>();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceCall getCall()                             { return for_call; }
boolean isEmpty()                               { return work_list.isEmpty(); }
IfaceState getState(FaitInstruction fi)         { return state_map.get(fi); }

FaitInstruction getInstruction(int ino)
{
   FaitMethod fm = for_call.getMethod();
   return fm.getInstruction(ino);
}



/********************************************************************************/
/*                                                                              */
/*      Local Instruction queue management                                      */
/*                                                                              */
/********************************************************************************/

FaitInstruction getNext()
{
   if (work_list.isEmpty()) return null;
   return work_list.removeFirst();
}


void mergeState(IfaceState st,FaitInstruction ins)
{
   if (st == null) return;
   
   IfaceState ost = state_map.get(ins);
   if (ost == null) ost = st.cloneState();
   else {
      ost = ost.mergeWith(st);
      if (ost == null) return;          // no change
    }
   state_map.put(ins,ost);
   work_list.addFirst(ins);
}
   

void lookAt(FaitInstruction ins)
{
   if (ins != null && getState(ins) != null) work_list.addLast(ins);
}



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

void handleUpdate(IfaceUpdater upd)
{
   List<IfaceState> work = new ArrayList<IfaceState>(state_map.values());
   Set<IfaceState> done = new HashSet<IfaceState>();
   
   while (!work.isEmpty()) {
      int ln = work.size();
      IfaceState st = work.remove(ln-1);
      if (!done.contains(st)) {
         done.add(st);
         st.handleUpdate(upd);
       }
    }
}
         



}       // end of class FlowQueueInstance




/* end of FlowQueueInstance.java */

