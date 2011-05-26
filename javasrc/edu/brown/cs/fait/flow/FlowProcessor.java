/********************************************************************************/
/*                                                                              */
/*              FlowProcessor.java                                              */
/*                                                                              */
/*      Manage flow processing using a thread pool                              */
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


import java.util.*;


class FlowProcessor implements FlowConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<Worker>    worker_threads;
private FlowQueue       flow_queue;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowProcessor(int nthread,FlowQueue q)
{
   flow_queue = q;
   worker_threads = new ArrayList<Worker>();
   for (int i = 0 ; i < nthread; ++i) {
      Worker w = new Worker(i);
      worker_threads.add(w);
    }
}
      



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process()
{
   for (Worker w : worker_threads) {
      w.start();
    }
   
   synchronized (flow_queue) {
      while (!flow_queue.allDone()) {
         try {
            flow_queue.wait(100000);
          }
         catch (InterruptedException e) { }
       }
    }
}



private FlowQueueInstance getNextInstance()
{
   synchronized (flow_queue) {
      while (!flow_queue.allDone()) {
         FlowQueueInstance fqi = flow_queue.setupNextFlowQueue();
         if (fqi != null) return fqi;
         try {
            flow_queue.wait(10000);
          }
         catch (InterruptedException e) { }
       }
    }
   
   return null;
}
         

/********************************************************************************/
/*                                                                              */
/*      Worker thread                                                           */
/*                                                                              */
/********************************************************************************/

private class Worker extends Thread {

   Worker(int idx) {
      super("FaitWorker_" + idx);
    }
   
   @Override public void run() {
      for ( ; ; ) {
         FlowQueueInstance fqi = getNextInstance();
         // process fqi
         flow_queue.doneWithFlowQueue(fqi);
       }
    }
   
}       // end of inner class Worker



}       // end of class FlowProcessor




/* end of FlowProcessor.java */

