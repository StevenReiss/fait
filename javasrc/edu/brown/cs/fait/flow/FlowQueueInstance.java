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


abstract class FlowQueueInstance implements FlowConstants
{



/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

static FlowQueueInstance createInstance(FlowQueue fq,IfaceCall cm,QueueLevel lvl)
{
   IfaceMethod sym = cm.getMethod();
   if (sym.isEditable()) {
      return new FlowQueueInstanceAst(fq,cm,lvl);
    }
   else {
      return new FlowQueueInstanceByteCode(fq,cm,lvl);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private FlowQueue                          work_queue;
private LinkedList<IfaceProgramPoint>      work_list;
private IfaceCall                          for_call;
private Map<IfaceProgramPoint,IfaceState>  state_map;
private LinkedList<FlowBackElement>        back_list;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected FlowQueueInstance(FlowQueue fq,IfaceCall fc,QueueLevel lvl)
{
   for_call = fc;
   work_queue = fq;
   work_list = new LinkedList<>();
   back_list = new LinkedList<>();
   state_map = new HashMap<>();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceCall getCall()                             { return for_call; }
boolean isEmpty()                               { return work_list.isEmpty(); }
boolean isBackEmpty()                           { return back_list.isEmpty(); }
IfaceControl getControl()                       { return for_call.getControl(); }
FlowQueue getWorkQueue()                        { return work_queue; }


IfaceState getState(IfaceProgramPoint fi)        
{
   return state_map.get(fi); 
}


/********************************************************************************/
/*                                                                              */
/*      Local Instruction queue management                                      */
/*                                                                              */
/********************************************************************************/

IfaceProgramPoint getNext()
{
   if (work_list.isEmpty()) return null;
   return work_list.removeFirst();
}

void mergeState(IfaceState st)
{
   FlowLocation loc = new FlowLocation(work_queue,getCall(),getCall().getMethod().getStart());
   mergeState(st,loc);
}



IfaceState mergeState(IfaceState st,IfaceLocation here)          
{
   if (st == null || here == null) return null;
   IfaceProgramPoint ins = here.getProgramPoint();
   if (ins == null) return null;
   
   IfaceState ost = state_map.get(ins);
   if (ost == null) ost = st.cloneState();
   else {
      ost = ost.mergeWith(st);
      if (ost == null) return null;          // no change
    }
   
   ost.setLocation(here);
   state_map.put(ins,ost);
   work_list.addFirst(ins);
   
   return ost;
}




void lookAt(IfaceProgramPoint ins)
{
   if (ins != null && getState(ins) != null) work_list.addLast(ins);
}



/********************************************************************************/
/*                                                                              */
/*      Back propagation methods                                                */
/*                                                                              */
/********************************************************************************/

void queueBackPropagation(IfaceProgramPoint pt,IfaceValue ref,IfaceType typ)
{
   FlowBackElement fbe = new FlowBackElement(pt,ref,typ);
   for (FlowBackElement xfbe : back_list) {
      if (xfbe.getProgramPoint() == pt && xfbe.getReference() == ref &&
            xfbe.getSetType() == typ) {
         // FaitLog.logD1("Already back queued " + pt + " " + ref.getRefSlot() + " " +
               // ref.getRefStack() + " " + ref.getRefField() + " " + typ); 
         return;
       }
    }
   
   if (FaitLog.isTracing()) {
      FaitLog.logD1("Back queue " + pt + " " + ref.getRefSlot() + " " + 
            ref.getRefStack() + " " + ref.getRefField() + " " + typ); 
    }
   
   back_list.addFirst(fbe);
}



IfaceBackElement getNextBack()
{
   if (back_list.isEmpty()) return null;
   return back_list.removeFirst();
}




/********************************************************************************/
/*                                                                              */
/*      Scanning methods                                                        */
/*                                                                              */
/********************************************************************************/

abstract void scanCode(IfaceControl ctrl,FlowQueue fq);

abstract void handleThrow(IfaceLocation loc,IfaceValue v0,IfaceState st0);




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

