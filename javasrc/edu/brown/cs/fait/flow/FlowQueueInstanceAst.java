/********************************************************************************/
/*                                                                              */
/*              FlowQueueInstanceAst.java                                       */
/*                                                                              */
/*      Hold information processing for an AST-based call                       */
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


import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceAstStatus.Reason;

class FlowQueueInstanceAst extends FlowQueueInstance implements FlowConstants
{



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowQueueInstanceAst(FlowQueue fq,IfaceCall fc,QueueLevel lvl)
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
   FlowScannerAst scan = new FlowScannerAst(ctrl,fq,this);
   int ct0 = scan.scanCode();
   int ct1 = scan.scanBack();
   
   getCall().noteScan(ct0,ct1);
}


@Override public void handleThrow(IfaceLocation loc,IfaceValue v0,IfaceState st0)
{
   if (v0 == null) {
      IfaceType ext = getControl().findDataType("java.lang.Throwable",FaitAnnotation.NON_NULL);
      v0 = getControl().findMutableValue(ext);
      v0 = v0.forceNonNull();
    }
   
   if (FaitLog.isTracing()) FaitLog.logD1("Handle throw of " + v0);
   
   FlowAstStatus sts = new FlowAstStatus(Reason.EXCEPTION,v0);
   IfaceProgramPoint pt0 = loc.getProgramPoint();
   ASTNode where = pt0.getAstReference().getAstNode();
   IfaceProgramPoint pt = getControl().getAstReference(where,sts);
   IfaceState st1 = st0.cloneState();
   FlowLocation nloc = new FlowLocation(getWorkQueue(),getCall(),pt);
   mergeState(st1,nloc);
}


}       // end of class FlowQueueInstanceAst




/* end of FlowQueueInstanceAst.java */

