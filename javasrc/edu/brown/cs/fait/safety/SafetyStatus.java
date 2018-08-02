/********************************************************************************/
/*                                                                              */
/*              SafetyStatus.java                                               */
/*                                                                              */
/*      Hold the status of all active safety checks                             */
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



package edu.brown.cs.fait.safety;

import edu.brown.cs.fait.iface.IfaceSafetyCheck;
import edu.brown.cs.fait.iface.IfaceSafetyStatus;

class SafetyStatus implements IfaceSafetyStatus
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int []          cur_status;
private SafetyFactory   for_factory;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SafetyStatus(SafetyFactory sf)
{
   for_factory = sf;
   cur_status = new int [sf.getNumChecks()];
   for (int i = 0; i < cur_status.length; ++i) {
      IfaceSafetyCheck.Value v = sf.getCheck(i).getInitialState();
      cur_status[i] = 1 << v.ordinal();
    }
}


SafetyStatus(SafetyStatus sts)
{
   for_factory = sts.for_factory; 
   cur_status = new int[sts.cur_status.length];
   for (int i = 0; i < cur_status.length; ++i) {
      cur_status[i] = sts.cur_status[i];;
    }
}


private SafetyStatus(SafetyFactory sf,int [] sts)
{
   for_factory = sf;
   cur_status = sts;
}




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSafetyStatus update(String event)
{
   int [] newstatus = null;
   
   String check = null;
   String evt = event;
   int idx = event.indexOf(".");
   if (idx >= 0) {
      check = event.substring(0,idx);
      evt = event.substring(idx+1);
    }
   
   for (int i = 0; i < cur_status.length; ++i) {
      SafetyCheck sc = for_factory.getCheck(i);
      if (check != null && !sc.getName().equals(check)) continue;
      int nv = sc.update(evt,cur_status[i]);
      if (nv != cur_status[i]) {
         if (newstatus == null) {
            newstatus = new int[cur_status.length];
            System.arraycopy(cur_status,0,newstatus,0,cur_status.length);
          }
         newstatus[i] = nv;
       }
    }
   
   if (newstatus == null) return this;
   return new SafetyStatus(for_factory,newstatus);
}




@Override public IfaceSafetyStatus merge(IfaceSafetyStatus sts)
{
   if (sts == null) return this;
   
   int [] newstatus = null;
   SafetyStatus msts = (SafetyStatus) sts;
   
   for (int i = 0; i < cur_status.length; ++i) {
      int nv = cur_status[i] | msts.cur_status[i];
      if (nv != cur_status[i]) {
         if (newstatus == null) {
            newstatus = new int[cur_status.length];
            System.arraycopy(cur_status,0,newstatus,0,cur_status.length);
          }
         newstatus[i] = nv;
       }
    }
   
   if (newstatus == null) return this;
   return new SafetyStatus(for_factory,newstatus);
}








}       // end of class SafetyStatus




/* end of SafetyStatus.java */

