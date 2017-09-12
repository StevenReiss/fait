/********************************************************************************/
/*										*/
/*		FlowCall.java							*/
/*										*/
/*	Handle call and return							*/
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
import edu.brown.cs.ivy.jcode.JcodeMethod;
import edu.brown.cs.ivy.jcode.JcodeTryCatchBlock;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeInstruction;

import java.util.*;



class FlowCall implements FlowConstants, JcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl		fait_control;
private FlowQueue		flow_queue;
private Map<IfaceCall,Set<IfaceCall>> rename_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowCall(FaitControl fc,FlowQueue fq)
{
   fait_control = fc;
   flow_queue = fq;
   rename_map = new HashMap<IfaceCall,Set<IfaceCall>>();
}




/********************************************************************************/
/*										*/
/*	Handle calls								*/
/*										*/
/********************************************************************************/

boolean handleCall(FlowLocation loc,IfaceState st0,FlowQueueInstance wq)
{
   JcodeInstruction ins = loc.getInstruction();
   boolean ifc = (ins.getOpcode() == INVOKEINTERFACE);
   boolean virt = (ifc || ins.getOpcode() == INVOKEVIRTUAL);
   IfaceCall method = loc.getCall();
   
   /**/
   JcodeMethod dbgmthd = ins.getMethodReference();
   if (dbgmthd == null) 
      System.err.println("NO METHOD FOUND: " + ins);
   /**/
   
   LinkedList<IfaceValue> args = getCallArguments(loc,st0);
   if (args == null) return false;

   for (IfaceValue av : args) IfaceLog.logD1("Arg = " + av);

   JcodeMethod tgt = findProperMethod(loc,args);

   flow_queue.initialize(tgt.getDeclaringClass());

   IfaceValue rslt = processCall(tgt,args,virt,loc,st0,null);

   if (tgt.getExceptionTypes().size() > 0) {
      for (IfaceCall c0 : method.getAllMethodsCalled(ins)) {
	 handleThrow(wq,loc,c0.getExceptionValue(),st0);
       }
    }
   else {
      for (IfaceCall c0 : method.getAllMethodsCalled(ins)) {
	 IfaceValue ev0 = c0.getExceptionValue();
	 if (ev0 != null) handleThrow(wq,loc,ev0,st0);
       }
    }

   if (!tgt.getReturnType().equals(fait_control.findDataType("V"))) {
      IfaceSpecial spl = fait_control.getCallSpecial(tgt);
      if (spl != null && spl.isConstructor() && rslt != null) {
         IfaceValue fv = spl.getReturnValue(tgt);
         rslt = fv;
       }
      if (rslt == null || !rslt.isGoodEntitySet()) return false;
      method.setAssociation(AssociationType.RETURN,ins,rslt);
      st0.pushStack(rslt);
    }
   else if (rslt == null) return false;

   if (!method.getMethod().isStatic()) {
      if (method.getMethod().getDeclaringClass() == tgt.getDeclaringClass() && !tgt.isStatic()) {
	 st0.discardFields();
       }
    }

   IfaceLog.logD1("Return = " + rslt);

   return true;
}




/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

void handleCallback(JcodeMethod bm,List<IfaceValue> args,String cbid)
{
   processCall(bm,args,true,null,null,cbid);
}



/********************************************************************************/
/*										*/
/*	Return methods								*/
/*										*/
/********************************************************************************/

void handleReturn(IfaceCall cm,IfaceValue v0)
{
   JcodeMethod bm = cm.getMethod();

   if (v0 != null && v0.mustBeNull()) {
      v0 = fait_control.findNullValue(bm.getReturnType());
    }

   boolean fg = cm.addResult(v0);
   if (fg) flow_queue.handleReturnSetup(bm);
   if (fg) {
      queueReturn(v0,cm);
      Set<IfaceCall> s = rename_map.get(cm);
      if (s != null) {
	 for (IfaceCall c0 : s) {
	    queueReturn(v0,c0);
	  }
       }
    }
}



private void queueReturn(IfaceValue v,IfaceCall cm)
{
   IfaceLog.logD1("Return change " + cm + " = " + v);

   for (FaitLocation loc : cm.getCallSites()) {
      IfaceLog.logD1("Queue for return " + loc);
      flow_queue.queueMethodChange(loc.getCall(),loc.getInstruction());
    }

   for (JcodeMethod fm : cm.getMethod().getParentMethods()) {
      if (fm == cm.getMethod()) continue;
      for (IfaceCall xcm : fait_control.getAllCalls(fm)) {
	 handleReturn(xcm,v);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Code to handle actual call						*/
/*										*/
/********************************************************************************/

private LinkedList<IfaceValue> getCallArguments(FlowLocation loc,IfaceState st0)
{
   JcodeInstruction ins = loc.getInstruction();
   JcodeMethod tgt = ins.getMethodReference();
   LinkedList<IfaceValue> args = new LinkedList<IfaceValue>();
   IfaceCall method = loc.getCall();
   if (tgt == null) {
      IfaceLog.logW("FAIT: Bad target " + ins);
      return null;
    }

   int ct = tgt.getNumArguments();
   for (int i = 0; i < ct; ++i) {
      IfaceValue v0 = st0.popStack();
      args.addFirst(v0);
      if (v0 != null) {
	 switch (ct-i) {
	    case 1 :
	       method.setAssociation(AssociationType.ARG1,ins,v0);
	       break;
	    case 2 :
	       method.setAssociation(AssociationType.ARG2,ins,v0);
	       break;
	    case 3 :
	       method.setAssociation(AssociationType.ARG3,ins,v0);
	       break;
	    case 4 :
	       method.setAssociation(AssociationType.ARG4,ins,v0);
	       break;
	    case 5 :
	       method.setAssociation(AssociationType.ARG5,ins,v0);
	       break;
	    case 6 :
	       method.setAssociation(AssociationType.ARG6,ins,v0);
	       break;
	    case 7 :
	       method.setAssociation(AssociationType.ARG7,ins,v0);
	       break;
	    case 8 :
	       method.setAssociation(AssociationType.ARG8,ins,v0);
	       break;
	    case 9 :
	       method.setAssociation(AssociationType.ARG9,ins,v0);
	       break;
	    default :
	       break;
	  }
       }
    }

   if (!tgt.isStatic()) {
      IfaceValue v0 = st0.popStack();
      args.addFirst(v0);
      method.setAssociation(AssociationType.THISARG,ins,v0);
      if (tgt.getDeclaringClass().isJavaLangObject() &&
	    (tgt.getName().equals("wait")  || tgt.getName().equals("notify") ||
		  tgt.getName().equals("notifyAll")))
	 method.setAssociation(AssociationType.SYNC,ins,v0);
    }

   return args;
}



private JcodeMethod findProperMethod(FaitLocation loc,List<IfaceValue> args)
{
   JcodeInstruction ins = loc.getInstruction();
   JcodeMethod tgt = ins.getMethodReference();
   boolean ifc = (ins.getOpcode() == INVOKEINTERFACE);
   
   if (tgt.getNumInstructions() == 0 && tgt.isStatic()) {
      // handle inherited static methods
      JcodeMethod ntgt = fait_control.findInheritedMethod(tgt.getDeclaringClass().getDescriptor(),
	    tgt.getName(),tgt.getDescription());
      if (ntgt != null && ntgt.getNumInstructions() > 0) tgt = ntgt;
    }
   while (tgt.getDeclaringClass().isInterface() && !ifc) {
      // handle calls using invokeinterface to non-interfaces
      Collection<JcodeMethod> pars = tgt.getParentMethods();
      if (pars == null || pars.size() != 1) break;
      tgt = pars.iterator().next();
    }
   if (!tgt.isStatic() && ins.getOpcode() == INVOKEVIRTUAL) {
      // for virtual calls, get the innermost method based on actual type
      // String nm = tgt.getName();
      // String cnm = tgt.getDeclaringClass().getName();
      // if (nm.equals("read") && cnm.equals("java.io.InputStream"))
	 // System.err.println("HANDLE READ");

      IfaceValue v0 = args.get(0);
      JcodeDataType dt0 = v0.getDataType();
      JcodeDataType dt1 = tgt.getDeclaringClass();
      if (dt0 != null && dt0 != dt1 && dt0.isDerivedFrom(dt1) && !dt0.isArray()) {
	 JcodeMethod ntgt = fait_control.findInheritedMethod(dt0.getDescriptor(),
	       tgt.getName(),tgt.getDescription());
	 if (ntgt != null && ntgt != tgt) {
	    tgt = ntgt;
	  }
       }
    }

   return tgt;
}





private IfaceValue processCall(JcodeMethod bm,List<IfaceValue> args,boolean virt,
      FlowLocation loc,IfaceState st0,String cbid)
{
   IfaceValue rslt = null;
   JcodeMethod orig = bm;

   LinkedList<IfaceValue> nargs = checkCall(loc,bm,args);
   if (nargs != null) {
      rslt = handleSpecialCases(bm,args,loc);
      if (rslt != null) return rslt;
    }

   if (nargs != null) {
      ProtoInfo pi = handlePrototypes(bm,nargs,loc);
      if (pi != null) {
	 rslt = pi.getResult();
	 if (!pi.checkAnyway()) {
	    nargs = null;
	    virt = false;
	  }
       }
    }

   if (nargs != null) {
      IfaceCall mi = findCall(loc,bm,nargs);
      mi.addCallbacks(loc,nargs);
      Collection<JcodeMethod> c = mi.replaceWith(nargs);
      if (c == null) return rslt;
      for (JcodeMethod m0 : c) {
	 LinkedList<IfaceValue> vargs = nargs;
	 if (c.size() > 1) vargs = new LinkedList<IfaceValue>(nargs);
	 IfaceValue xrslt = mi.fixReplaceArgs(m0,vargs);
	 if (xrslt != null) rslt = xrslt.mergeValue(rslt);
	 // if (loc != null) m0.addCallSite(loc);
	 rslt = processActualCall(bm,args,virt,loc,st0,mi,m0,vargs,rslt,cbid);
       }
    }
   else if (virt) {
      rslt = checkVirtual(bm,args,loc,st0,orig,rslt,cbid);
    }

   return rslt;
}



private LinkedList<IfaceValue> checkCall(FlowLocation loc,JcodeMethod fm,List<IfaceValue> args)
{
   int xid = (fm.isStatic() ? 0 : -1);

   LinkedList<IfaceValue> nargs = new LinkedList<IfaceValue>();
   JcodeDataType dt = null;
   for (IfaceValue cv : args) {
      if (xid < 0) {
	 dt = fm.getDeclaringClass();
	 if (!flow_queue.canBeUsed(dt)) return null;
       }
      else dt = fm.getArgType(xid);
      boolean prj = fait_control.isProjectClass(dt);
      if (prj && fait_control.isProjectClass(cv.getDataType())) prj = false;
      IfaceValue ncv = cv.restrictByType(dt,prj,loc);
      if (ncv.mustBeNull()) ncv = fait_control.findNullValue(dt);
      if (xid < 0) {
	 if (ncv.mustBeNull()) return null;
	 ncv = ncv.forceNonNull();
	 ncv = ncv.makeSubtype(dt);
       }
      if (!ncv.isGoodEntitySet() && !dt.isPrimitive() && !ncv.mustBeNull()) return null;
      nargs.add(ncv);
      ++xid;
    }

   return nargs;
}




private IfaceValue handleSpecialCases(JcodeMethod fm,List<IfaceValue> args,FlowLocation loc)
{
   IfaceSpecial spl = fait_control.getCallSpecial(fm);
   if (spl == null) return null;

   if (spl.getIsArrayCopy()) {
      flow_queue.handleArrayCopy(args,loc);
      return fait_control.findAnyValue(fait_control.findDataType("V"));
    }

   return null;
}



private ProtoInfo handlePrototypes(JcodeMethod fm,LinkedList<IfaceValue> args,
      FlowLocation loc)
{
   IfaceValue rslt = null;
   int nsrc = 0;

   if (fm.isStatic()) return null;

   IfaceValue cv = args.get(0);
   for (IfaceEntity ce : cv.getEntities()) {
      IfacePrototype pt = ce.getPrototype();
      // TODO: if ce is fixed and pt is null, convert FIXED to PROTO
      if (pt != null) {
	 if (pt.isMethodRelevant(fm)) {
	    IfaceValue nv = pt.handleCall(fm,args,loc);
	    IfaceCall fc = fait_control.findPrototypeMethod(fm);
	    fc.noteCallSite(loc);
	    if (nv != null) {
	       if (rslt == null) rslt = nv;
	       else rslt = rslt.mergeValue(nv);
	     }
	  }
       }
      else if (!ce.isUserEntity()) ++nsrc;
    }

   if (rslt == null && nsrc > 0) return null;

   return new ProtoInfo(nsrc > 0,rslt);
}



private IfaceValue checkVirtual(JcodeMethod bm,List<IfaceValue> args,
      FlowLocation loc,IfaceState st,JcodeMethod orig,IfaceValue rslt,String cbid)
{
   IfaceValue cv = args.get(0);
   boolean isnative = cv.isAllNative();

   for (JcodeMethod km : bm.getChildMethods()) {
      if (km == orig) continue;
      if (isnative && fait_control.isProjectClass(km.getDeclaringClass()) &&
	    !fait_control.isProjectClass(cv.getDataType()))
	 continue;
      IfaceValue srslt = processCall(km,args,true,loc,st,cbid);
      if (srslt != null) {
	 if (rslt == null) rslt = srslt;
	 else rslt = rslt.mergeValue(srslt);
       }
    }

   if (rslt == null && bm.getParentMethods().size() == 0 && bm.isAbstract() &&
	 loc.getCall() != null && args.size() > 0) {
      for (IfaceEntity ce : cv.getEntities()) {
	 JcodeDataType dt = ce.getDataType();
	 if (dt != null) {
	    JcodeMethod cem = fait_control.findInheritedMethod(dt.getName(),bm.getName(),bm.getDescription());
	    if (cem != null && !cem.isAbstract()) {
	       IfaceValue srslt = processCall(cem,args,true,loc,st,cbid);
	       IfaceCall kmi = findCall(loc,cem,null);
	       if (kmi != null && kmi.hasResult()) {
		  if (rslt == null) rslt = srslt;
		  else rslt = rslt.mergeValue(srslt);
		}
	     }
	  }
       }
    }

   return rslt;
}



private IfaceValue processActualCall(JcodeMethod fm,List<IfaceValue> args,boolean virt,
      FlowLocation loc,IfaceState st,IfaceCall mi,JcodeMethod fm0,
      LinkedList<IfaceValue> nargs,IfaceValue rslt,String cbid)
{
   JcodeMethod orig = fm;
   
   if (fm0 != fm) {
      IfaceCall mi0 = findCall(loc,fm0,nargs);
      synchronized (rename_map) {
	 Set<IfaceCall> s = rename_map.get(mi0);
	 if (s == null) {
	    s = new HashSet<IfaceCall>();
	    rename_map.put(mi0,s);
	  }
	 s.add(mi);
       }
      if (mi.getIsAsync()) {
	 if (rslt == null) rslt = fait_control.findAnyValue(fait_control.findDataType("V"));
       }
      mi = mi0;
    }
   flow_queue.queueForInitializers(mi,st);

   if (mi.addCall(nargs)) {
      IfaceLog.logD1("Call " + mi);
      if (mi.getMethod().getNumInstructions() > 0)
	 flow_queue.queueMethodCall(mi,st);
    }

   if (loc != null) mi.noteCallSite(loc);
   // else IfaceLog.logD1("No call site given");        // callbacks have no call site


   if (mi.hasResult()) {
      if (mi.isClone()) {
	 IfaceValue cv = nargs.get(0);
	 IfaceValue prslt = fait_control.findNativeValue(cv.getDataType());
	 if (prslt != null) rslt = prslt.mergeValue(rslt);
       }
      else if (mi.isReturnArg0()) {
	 IfaceValue prslt = nargs.get(0);
	 if (prslt != null) rslt = prslt.mergeValue(rslt);
       }
      else {
	 IfaceValue prslt = mi.getResultValue();
	 if (prslt != null) {
	    if (prslt.isNative()) flow_queue.initialize(prslt.getDataType());
	    rslt = prslt.mergeValue(rslt);
	  }
       }
    }

   if (loc != null && mi.getCanExit() && !loc.getCall().getCanExit()) {
      loc.getCall().setCanExit();
      queueReturn(null,loc.getCall());
    }

   if (virt) rslt = checkVirtual(fm,args,loc,st,orig,rslt,cbid);

   return rslt;
}



private IfaceCall findCall(FlowLocation loc,JcodeMethod tgt,List<IfaceValue> args)
{
   if (loc == null)
      return fait_control.findCall(tgt,null,InlineType.NONE);

   IfaceCall cm = loc.getCall().getMethodCalled(loc.getInstruction(),tgt);
   if (cm != null) return cm;

   InlineType il = canBeInlined(tgt);
   cm = fait_control.findCall(tgt,args,il);

   loc.getCall().noteMethodCalled(loc.getInstruction(),tgt,cm);

   return cm;
}



private InlineType canBeInlined(JcodeMethod fm)
{
   if (fm.isStatic()) return InlineType.NONE;
   if (fm.isStaticInitializer()) return InlineType.NONE;
   if (fm.isNative()) return InlineType.NONE;

   if (fm.isAbstract()) return InlineType.DEFAULT;
   IfaceSpecial isp = fait_control.getCallSpecial(fm);
   if (isp != null && isp.getCallbackId() != null) return InlineType.DEFAULT;

   if (fait_control.isInProject(fm)) return InlineType.THIS;

   return InlineType.DEFAULT;
}



/********************************************************************************/
/*										*/
/*	Exception handling							*/
/*										*/
/********************************************************************************/

void handleThrow(FlowQueueInstance wq,FaitLocation loc,IfaceValue vi,IfaceState st0)
{
   IfaceCall cm = loc.getCall();
   IfaceValue v0 = null;

   if (vi == null) {
      v0 = fait_control.findMutableValue(fait_control.findDataType("Ljava/lang/Throwable;"));
      v0 = v0.forceNonNull();
    }
   else v0 = vi.forceNonNull();

   for (JcodeTryCatchBlock tcb : loc.getMethod().getTryCatchBlocks()) {
      boolean inside = false;
      for (JcodeInstruction ins = tcb.getStart();
	 ins != null && ins != tcb.getEnd() && !inside;
	 ins = ins.getNext()) {
	 if (ins == loc.getInstruction()) inside = true;
       }
      if (!inside) continue;
      IfaceValue v1 = v0;
      if (tcb.getException() != null) v1 = v0.restrictByType(tcb.getException(),false,loc);
      else v0 = null;
      JcodeInstruction ins0 = tcb.getStart();
      IfaceState st1 = st0.cloneState();
      IfaceState st2 = wq.getState(ins0);
      if (st2 == null) {
	 for (JcodeInstruction ins = tcb.getStart(); ins != null && ins != tcb.getEnd(); ins = ins.getNext()) {
	    st2 = wq.getState(ins);
	    if (st2 != null) break;
	  }
       }
      if (st2 == null) continue;
      st1.resetStack(st2);
      st1.pushStack(v1);
      wq.mergeState(st1,tcb.getHandler());
      IfaceLog.logD1("Handle throw to " + tcb.getHandler());

      if (vi != null) {
	 IfaceValue cv = (IfaceValue) cm.getAssociation(AssociationType.CATCH,tcb.getHandler());
	 if (cv == null) cv = v1;
	 else cv = cv.mergeValue(v1);
	 cm.setAssociation(AssociationType.CATCH,tcb.getHandler(),cv);
       }

      if (v0 == null) break;
    }
   // handle exceptions in the code

   if (v0 != null && !v0.isEmptyEntitySet()) {
      handleException(v0,cm);
    }
}



void handleException(IfaceValue v0,IfaceCall cm)
{
   if (cm.addException(v0)) {
      IfaceLog.logD1("Exception change " + cm + " :: " + v0);
      for (FaitLocation loc : cm.getCallSites()) {
	 IfaceLog.logD1("Queue for exception: " + loc);
	 flow_queue.queueMethodChange(loc.getCall(),loc.getInstruction());
       }
      for (JcodeMethod pm : cm.getMethod().getParentMethods()) {
	 for (IfaceCall xcm : fait_control.getAllCalls(pm)) {
	    handleException(v0,xcm);
	  }
       }
    }
}





/********************************************************************************/
/*										*/
/*	Class to hold prototype call data					*/
/*										*/
/********************************************************************************/


private static class ProtoInfo {

   private boolean check_anyway;
   private IfaceValue return_value;

   ProtoInfo(boolean chk,IfaceValue rslt) {
      check_anyway = chk;
      return_value = rslt;
    }

   boolean checkAnyway()			{ return check_anyway; }
   IfaceValue getResult()			{ return return_value; }

}	// end of inner class ProtoInfo



}	// end of class FlowCall




/* end of FlowCall.java */

