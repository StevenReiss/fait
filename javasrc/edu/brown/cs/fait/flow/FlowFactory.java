/********************************************************************************/
/*										*/
/*		FlowFactory.java						*/
/*										*/
/*	Factory for setting up flow evaluation					*/
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

import java.util.*;


public class FlowFactory implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl	fait_control;
private FlowQueue       flow_queue;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public FlowFactory(IfaceControl fc)
{
   fait_control = fc;
   flow_queue = new FlowQueue(fc);
}




/********************************************************************************/
/*										*/
/*	Methods to do initial analysis						*/
/*										*/
/********************************************************************************/

public void analyze(int nthread,boolean update)
{
   if (!update) {
      Collection<IfaceMethod> start = fait_control.getStartMethods();
      List<IfaceValue> sargl = new LinkedList<IfaceValue>();
      IfaceSafetyStatus ists = fait_control.getInitialSafetyStatus();
      sargl.add(fait_control.findMainArgsValue());
      for (IfaceMethod fm : start) {
         IfaceCall ic = fait_control.findCall(null,fm,sargl,ists,InlineType.NONE);
         ic.addCall(sargl,ists);
         flow_queue.queueMethodStart(ic,null);
       }
    }

   FlowProcessor fp = new FlowProcessor(nthread,fait_control,flow_queue);
   fp.process();
}


public void analyze(IfaceMethod im,int nth)
{
   Set<IfaceType> done = new HashSet<>();
   
   List<IfaceValue> args = new ArrayList<>();
   IfaceValue thisv = null;
   IfaceType ctyp = im.getDeclaringClass();
   
   fait_control.clearCallSpecial(im);
   
   preloadClasses(ctyp,done);
   
   if (!im.isStatic()) {
      thisv = fait_control.findMutableValue(ctyp);
      IfaceEntity e0 = fait_control.findLocalEntity(null,ctyp,null);
      IfaceValue v0 = fait_control.findObjectValue(ctyp,
            fait_control.createSingletonSet(e0),FaitAnnotation.NON_NULL);
      // ensure thisv is unique so we can check it versus result
      thisv = thisv.mergeValue(v0);
      thisv = thisv.forceNonNull();
      args.add(thisv);
    }
   
   for (int i = 0; i < im.getNumArgs(); ++i) {
      IfaceType atyp = im.getArgType(i);
      if (atyp == null) {
         System.err.println("MISSING ARG TYPE");
         FaitLog.logI("MISSING ARG TYPE");
         return;
       }
      preloadClasses(atyp,done);
      args.add(fait_control.findMutableValue(atyp));
    }
   IfaceType rtyp = im.getReturnType();
   if (rtyp == null) {
      System.err.println("MISSING RETURN TYPE");
      FaitLog.logI("MISSING RETURN TYPE");
      return;
    }
   preloadClasses(rtyp,done);
   
   IfaceCall ic = fait_control.findCall(null,im,args,null,InlineType.NONE);
   ic.addCall(args,null);
   flow_queue.queueMethodStart(ic,null);
   FlowProcessor fp = new FlowProcessor(nth,fait_control,flow_queue);
   fp.process();
   IfaceValue retv = ic.getResultValue();
   boolean arg0 = false;
   if (thisv != null && thisv == retv) arg0 = true;
   
   FaitLog.logI("RETURNS " + ic + " " + retv + " " + arg0);
}


private void preloadClasses(IfaceType typ,Set<IfaceType> done)
{
   if (typ == null || !done.add(typ)) return;
   
   if (typ.getSuperType() != null) {
      preloadInitialize(typ.getSuperType());
    }
   
   for (IfaceType ityp : typ.getInterfaces()) {
      preloadClasses(ityp,done);
    }
   preloadInitialize(typ);
   
   for (IfaceType ctyp : typ.getChildTypes()) {
      preloadClasses(ctyp,done);
    }
}


private void preloadInitialize(IfaceType typ)
{
   if (typ == null) return;
   
   Collection<IfaceMethod> sinit = fait_control.findAllMethods(typ,"<clinit>");   
   if (sinit != null) {
      for (IfaceMethod sim : sinit) fait_control.clearCallSpecial(sim);
    }
   
   flow_queue.initialize(typ);
}




/********************************************************************************/
/*                                                                              */
/*      Methods to queue locations                                              */
/*                                                                              */
/********************************************************************************/

public void queueLocation(IfaceLocation loc)
{
   FlowLocation fl = (FlowLocation) loc;
   fl.queueLocation();
}

public void queueMethodCall(IfaceCall ic,IfaceProgramPoint pt)
{
   flow_queue.queueMethodChange(ic,pt);
}

public void initialize(IfaceType typ)
{
   flow_queue.initialize(typ);
}



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

public void handleStateUpdate(IfaceUpdater upd)
{
   flow_queue.handleUpdate(upd);
}



public void handleCallback(IfaceLocation frm,IfaceMethod fm,List<IfaceValue> args,String cbid) 
{
   frm.handleCallback(fm,args,cbid);
}
   

/********************************************************************************/
/*                                                                              */
/*      Query access methods                                                    */
/*                                                                              */
/********************************************************************************/

public IfaceState findStateForLocation(IfaceCall c,IfaceProgramPoint pt)
{
   return flow_queue.findStateForLocation(c,pt);
}


}	// end of class FlowFactory




/* end of FlowFactory.java */

