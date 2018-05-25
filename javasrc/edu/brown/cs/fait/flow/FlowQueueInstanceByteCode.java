/********************************************************************************/
/*                                                                              */
/*              FlowQueueInstanceByteCode.java                                  */
/*                                                                              */
/*      Worker set for a byte-code related method                               */
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


import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcode.JcodeTryCatchBlock;

class FlowQueueInstanceByteCode extends FlowQueueInstance implements FlowConstants
{



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowQueueInstanceByteCode(FlowQueue fq,IfaceCall fc,QueueLevel lvl)
{
   super(fq,fc,lvl);
}



/********************************************************************************/
/*                                                                              */
/*      Scanning methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override void scanCode(IfaceControl ctrl,FlowQueue fq)
{
   FlowScannerByteCode scan = new FlowScannerByteCode(ctrl,fq,this);
   int ct0 = scan.scanCode();
   int ct1 = scan.scanBack();
   
   getCall().noteScan(ct0,ct1);
}


@Override void handleThrow(IfaceLocation loc,IfaceValue vi,IfaceState st0)
{
   IfaceCall cm = loc.getCall();
   
   IfaceValue v0 = null;
   if (vi == null) {
      IfaceType ext = getControl().findDataType("java.lang.Throwable",FaitAnnotation.NON_NULL);
      v0 = getControl().findMutableValue(ext);
      v0 = v0.forceNonNull();
    }
   else v0 = vi.forceNonNull();
   
   for (JcodeTryCatchBlock tcb : loc.getMethod().getTryCatchBlocks()) {
      boolean inside = isInside(tcb,loc.getProgramPoint());
      if (!inside) continue;
      IfaceValue v1 = v0;
      if (tcb.getException() != null) {
         IfaceType etyp = getControl().findDataType(tcb.getException().getName(),FaitAnnotation.NON_NULL);
         v1 = v0.restrictByType(etyp);
       }
      else v0 = null;
      JcodeInstruction ins0 = tcb.getStart();
      IfaceProgramPoint insp0 = getControl().getProgramPoint(ins0);
      IfaceState st1 = st0.cloneState();
      IfaceState st2 = getState(insp0);
      if (st2 == null) {
         // is this really needed?
         for (JcodeInstruction ins = ins0.getNext(); 
            ins != tcb.getEnd() && ins != null;
            ins = ins.getNext()) {
            IfaceProgramPoint insp = getControl().getProgramPoint(ins);
            st2 = getState(insp);
            if (st2 != null) break;
          }
       }
      if (st2 == null) continue;
      st1.resetStack(st2);
      st1.pushStack(v1);
      IfaceProgramPoint hdlr = getControl().getProgramPoint(tcb.getHandler());
      mergeState(st1,hdlr);
      if (FaitLog.isTracing()) FaitLog.logD1("Handle throw to " + tcb.getHandler());
      
      if (v0 == null) break;
    }
   // handle exceptions in the code
   
   if (v0 != null && !v0.isEmptyEntitySet()) {
      getWorkQueue().handleException(v0,cm);
    }
}


private boolean isInside(JcodeTryCatchBlock tcb,IfaceProgramPoint pt)
{
   JcodeInstruction ins = pt.getInstruction();
   if (ins == null) return false;
   for (JcodeInstruction sins = tcb.getStart();
      sins != null && sins != tcb.getEnd();
      sins = sins.getNext()) {
      if (sins == ins) return true;
    }
   
   return false;
}



}       // end of class FlowQueueInstanceByteCode




/* end of FlowQueueInstanceByteCode.java */

