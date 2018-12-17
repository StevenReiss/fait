/********************************************************************************/
/*                                                                              */
/*              ServerRunner.java                                               */
/*                                                                              */
/*      Thread to control the actual analysis                                   */
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



package edu.brown.cs.fait.server;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceUpdateSet;

class ServerRunner extends Thread implements ServerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ServerProject for_project;
private int num_threads;
private String return_id;
private ReportOption report_option;
private String next_return;
private boolean restart_analysis;
private boolean pause_analysis;
private boolean abort_analysis;
private long    last_change;
private IfaceControl runner_control;

private static final long MIN_STABLE_TIME = 1000;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ServerRunner(ServerProject sp,int nth,String retid,ReportOption opt)
{
   super("FAIT_RUNNER_" + retid);
   if (nth <= 0) {
      nth = Runtime.getRuntime().availableProcessors();
      nth = Math.max(1,nth/2);
    }
   for_project = sp;
   num_threads = nth;
   report_option = opt;
   return_id = retid;
   next_return = retid;
   restart_analysis = false;
   pause_analysis = false;
   abort_analysis = false;
   last_change = 0;
   runner_control = null;
}



/********************************************************************************/
/*                                                                              */
/*      Control methods                                                         */
/*                                                                              */
/********************************************************************************/

synchronized void resumeAnalysis(int nth,String retid,ReportOption opt)
{
   if (nth > 0) num_threads = nth;
   if (retid != null) next_return = retid;
   if (opt != null) report_option = opt;
   
   FaitLog.logI("Resume analysis request");
   
   interrupt();
   restart_analysis = true;
   pause_analysis = false;
   abort_analysis = true;
   notifyAll();
   
   last_change = System.currentTimeMillis();
}


synchronized void pauseAnalysis()
{
   interrupt();
   FaitLog.logD("Pause analysis request");
   pause_analysis = true;
   restart_analysis = false;
   last_change = System.currentTimeMillis();
}



IfaceControl getControl()
{
   return runner_control;
}




/********************************************************************************/
/*                                                                              */
/*      Running methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public void run()
{
   for ( ; ; ) {
      long start = System.currentTimeMillis();
      long comp = start;
      long anal = start;
      abort_analysis = false;
      try {
         IfaceUpdateSet upd = for_project.compileProject();
         boolean update = false;
         if (runner_control == null) runner_control = IfaceControl.Factory.createControl(for_project);
         else {
            if (upd != null) runner_control.doUpdate(upd);
            update = true;
          }
         comp = System.currentTimeMillis();
         anal = comp;
         runner_control.analyze(num_threads,update);
         anal = System.currentTimeMillis();
         if (interrupted() || abort_analysis) {
            FaitLog.logI("Aborted analysis " + return_id);
            for_project.sendAborted(return_id,anal-comp,comp-start);
          }
         else {
            FaitLog.logI("Finished analysis " + return_id);
            for_project.sendAnalysis(return_id,runner_control,report_option,
                  anal-comp,comp-start,num_threads,update);
            synchronized (this) {
               if (!restart_analysis) {
                  FaitLog.logI("No restart -- pausing analysis");
                  pause_analysis = true;
                }
             }
          }
       }
      catch (Throwable t) {
         FaitLog.logE("Problem doing analysis",t);
         for_project.sendAborted(return_id,anal-comp,comp-start);
       }
      abort_analysis = false;
      return_id = next_return;
      synchronized (this) {
         for ( ; ; ) {
            while (pause_analysis || !for_project.isErrorFree()) {
               try {
                  wait(1000);
                }
               catch (InterruptedException e) { }
             }
            interrupted();
            FaitLog.logI("Check resume analysis");
            long now = System.currentTimeMillis();
            while (now - last_change < MIN_STABLE_TIME) {
               long delta = last_change + MIN_STABLE_TIME - now;
               try {
                  wait(delta);
                }
               catch (InterruptedException e) { }
               now = System.currentTimeMillis();
             }
            if (!pause_analysis) break;
          }
         restart_analysis = false;
       }
    }
}

}       // end of class ServerRunner




/* end of ServerRunner.java */

