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

import java.util.*;



class FlowCall implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl		fait_control;
private FlowQueue		flow_queue;
private Map<IfaceCall,Set<IfaceCall>> rename_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowCall(IfaceControl fc,FlowQueue fq)
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

CallReturn handleCall(FlowLocation loc,IfaceState st0,FlowQueueInstance wq)
{
   IfaceProgramPoint inspt = loc.getProgramPoint();
   boolean virt = inspt.isVirtualCall();
   IfaceCall method = loc.getCall();
   IfaceMethod dbgmthd = inspt.getReferencedMethod();
   if (dbgmthd == null) {
      FaitLog.logW("NO METHOD FOUND: " + inspt);
      return CallReturn.NOT_DONE;
    }
   
   LinkedList<IfaceValue> args = getCallArguments(loc,st0);
   if (args == null) return CallReturn.NOT_DONE;
   
   if (FaitLog.isTracing()) {
      for (IfaceValue av : args) FaitLog.logD1("Arg = " + av);
      FaitLog.logD1("Saftey state = " + getSafetyStatus(st0));
    }
   
   IfaceMethod tgt = findProperMethod(loc,args);
   
   flow_queue.initialize(tgt.getDeclaringClass());
   
   IfaceValue rslt = processCall(tgt,args,virt,loc,st0,null);
   
   if (tgt.getExceptionTypes().size() > 0) {
      for (IfaceCall c0 : method.getAllMethodsCalled(inspt)) {
         wq.handleThrow(loc,c0.getExceptionValue(),st0);
       }
    }
   else {
      for (IfaceCall c0 : method.getAllMethodsCalled(inspt)) {
	 IfaceValue ev0 = c0.getExceptionValue();
	 if (ev0 != null) {
            wq.handleThrow(loc,ev0,st0);
          }      
       }
    }

   IfaceType rtyp = tgt.getReturnType();
   IfaceSpecial spl = fait_control.getCallSpecial(inspt,tgt);
   if (rtyp == null && rslt != null) 
      rtyp = rslt.getDataType();
   if (rtyp != null && !rtyp.isVoidType()) {
      if (spl != null && spl.isConstructor() && rslt != null) {
	 IfaceValue fv = spl.getReturnValue(loc.getProgramPoint(),tgt);
	 rslt = fv;
       }
      if (rslt == null || !rslt.isGoodEntitySet()) return CallReturn.NOT_DONE;
      st0.pushStack(rslt);
    }
   else if (rslt == null) {
      if (spl != null && spl.getNeverReturns()) {
         return CallReturn.NO_RETURN;
       }
      return CallReturn.NOT_DONE;
    }

   if (!method.getMethod().isStatic()) {
      if (method.getMethod().getDeclaringClass() == tgt.getDeclaringClass() && !tgt.isStatic()) {
	 st0.discardFields();
       }
    }

   if (FaitLog.isTracing()) {
      FaitLog.logD1("Return = " + rslt);
      FaitLog.logD1("Return Safety State = " + getSafetyStatus(st0));
    }

   return CallReturn.CONTINUE;
}




/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

void handleCallback(IfaceMethod bm,List<IfaceValue> args,String cbid)
{
   IfaceState st0 = fait_control.createState(0,null);
   processCall(bm,args,true,null,st0,cbid);
}



/********************************************************************************/
/*										*/
/*	Return methods								*/
/*										*/
/********************************************************************************/

void handleReturn(IfaceCall cm,IfaceValue v0,IfaceSafetyStatus sts,IfaceLocation loc)
{
   IfaceMethod bm = cm.getMethod();
   
   if (v0 != null && v0.mustBeNull()) {
      v0 = fait_control.findNullValue(bm.getReturnType());
    }
   
   List<IfaceAnnotation> annots = bm.getReturnAnnotations();
   if (annots != null && v0 != null) {
      IfaceType dt = v0.getDataType();
      IfaceType rt = dt.getAnnotatedType(annots);
      if (rt != dt) {
         dt.checkCompatibility(rt,loc);
         v0 = v0.changeType(rt);
       }
    }
   
   if (FaitLog.isTracing()) {
      FaitLog.logD1("Return value = " + v0);
      FaitLog.logD1("Safety Return = " + sts);
    }

   boolean fg = cm.addResult(v0,sts);
   if (fg) flow_queue.handleReturnSetup(bm);
   if (fg) {
      queueReturn(v0,cm,sts,loc);
      Set<IfaceCall> s = rename_map.get(cm);
      if (s != null) {
	 for (IfaceCall c0 : s) {
	    queueReturn(v0,c0,sts,loc);
	  }
       }
    }
   else {
      if (FaitLog.isTracing())
         FaitLog.logD1("No return change");
    }
}



private void queueReturn(IfaceValue v,IfaceCall cm,IfaceSafetyStatus sts,IfaceLocation loc)
{
   if (FaitLog.isTracing()) FaitLog.logD1("Return change " + cm + " = " + v + " [" + sts + "]");

   for (IfaceLocation cloc : cm.getCallSites()) {
      if (FaitLog.isTracing()) FaitLog.logD1("Queue for return " + cloc);
      flow_queue.queueMethodChange(cloc.getCall(),cloc.getProgramPoint());
    }

   for (IfaceMethod fm : cm.getMethod().getParentMethods()) {
      for (IfaceCall xcm : fait_control.getAllCalls(fm)) {
	 handleReturn(xcm,v,sts,loc);
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
   IfaceProgramPoint ins = loc.getProgramPoint();
   IfaceMethod tgt = ins.getReferencedMethod();
   LinkedList<IfaceValue> args = new LinkedList<IfaceValue>();
   if (tgt == null) {
      FaitLog.logE("FAIT: Bad target " + ins);
      return null;
    }
   
   int ct = tgt.getNumArgs();
   for (int i = 0; i < ct; ++i) {
      IfaceValue v0 = st0.popStack();
      args.addFirst(v0);
    }

   int delta = 0;
   if (!tgt.isStatic()) {
      delta = 1;
      IfaceValue v0 = st0.popStack();
      args.addFirst(v0);
    }
   
   for (int i = 0; i < tgt.getNumArgs(); ++i) {
      IfaceType rtype = tgt.getArgType(i);
      if (rtype == null) {
         FaitLog.logW("NULL argument type for " + tgt + " " + i);
         return null;
       }
      IfaceValue v0 = args.get(i+delta);
      if (v0 == null) continue;
      IfaceValue v1 = flow_queue.castValue(rtype,v0,loc);
      if (v1 != v0) {
         args.set(i+delta,v1);
         if (!v1.getDataType().isPrimitiveType()) {
            v0.getDataType().checkCompatibility(rtype,loc);
          }
       }   
    }

   return args;
}



private IfaceMethod findProperMethod(IfaceLocation loc,List<IfaceValue> args)
{
   IfaceProgramPoint ins = loc.getProgramPoint();
   IfaceMethod tgt = ins.getReferencedMethod();
   boolean ifc = ins.isInterfaceCall();

   if (!tgt.isStatic()) {
      IfaceType ttyp = tgt.getDeclaringClass();
      ttyp = ttyp.getAnnotatedType(FaitAnnotation.DEREFED);
      IfaceValue arg0 = args.get(0);
      IfaceValue arg0x = arg0.restrictByType(ttyp);
      if (arg0x != null && arg0x != arg0) {
         IfaceBaseType t0 = arg0x.getDataType().getJavaType();
         IfaceBaseType t1 = arg0.getDataType().getJavaType();
         if (t0 != t1) {
            args.set(0,arg0x);
          }
       }
    }
   
   if (!tgt.hasCode() && tgt.isStatic()) {
      // handle inherited static methods
      IfaceMethod ntgt = fait_control.findInheritedMethod(tgt.getDeclaringClass(),
	    tgt.getName(),tgt.getDescription());
      if (ntgt != null && ntgt.hasCode()) tgt = ntgt;
    }
   while (tgt.getDeclaringClass().isInterfaceType() && !ifc) {
      // handle calls using invokeinterface to non-interfaces
      Collection<IfaceMethod> pars = tgt.getParentMethods();
      if (pars == null || pars.size() != 1) break;
      tgt = pars.iterator().next();
    }
   if (!tgt.isStatic() && ins.isVirtualCall()) {
      // for virtual calls, get the innermost method based on actual type
      // String nm = tgt.getName();
      // String cnm = tgt.getDeclaringClass().getName();
      // if (nm.equals("read") && cnm.equals("java.io.InputStream"))
	 // System.err.println("HANDLE READ");

      IfaceValue v0 = args.get(0);
      IfaceType dt0 = v0.getDataType();
      IfaceType dt1 = tgt.getDeclaringClass();
      if (dt0 != null && dt0 != dt1 && dt0.isDerivedFrom(dt1) && !dt0.isArrayType()) {
	 IfaceMethod ntgt = fait_control.findInheritedMethod(dt0,
	       tgt.getName(),tgt.getDescription());
	 if (ntgt != null && ntgt != tgt) {
	    tgt = ntgt;
	  }
       }
    }

   return tgt;
}





private IfaceValue processCall(IfaceMethod bm,List<IfaceValue> args,boolean virt,
      FlowLocation loc,IfaceState st0,String cbid)
{
   IfaceValue rslt = null;
   IfaceMethod orig = bm;
   IfaceProgramPoint ppt = null;
   if (loc != null) ppt = loc.getProgramPoint();
   
   LinkedList<IfaceValue> nargs = checkCall(loc,bm,args);
   if (nargs != null) {
      rslt = handleSpecialCases(bm,args,loc,st0);
      if (rslt != null) return rslt;
    }

   if (nargs != null) {
      ProtoInfo pi = handlePrototypes(ppt,bm,nargs,loc);
      if (pi != null) {
	 rslt = pi.getResult();
	 if (!pi.checkAnyway()) {
	    nargs = null;
	    virt = false;
	  }
       }
    }

   if (nargs != null) {
      IfaceCall mi = findCall(loc,bm,nargs,getSafetyStatus(st0));
      mi.addCallbacks(loc,nargs);
      Collection<IfaceMethod> c = mi.replaceWith(ppt,nargs);
      if (c == null) return rslt;
      for (IfaceMethod m0 : c) {
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



private LinkedList<IfaceValue> checkCall(FlowLocation loc,IfaceMethod fm,List<IfaceValue> args)
{
   int xid = (fm.isStatic() ? 0 : -1);
   
   LinkedList<IfaceValue> nargs = new LinkedList<IfaceValue>();
   IfaceType dt = null;
   for (IfaceValue cv : args) {
      if (cv == null) return null;
      if (xid < 0) {
	 dt = fm.getDeclaringClass();
	 if (!flow_queue.canBeUsed(dt)) {
            if (loc != null) {
               flow_queue.checkInitialized(loc.getCall(),loc.getProgramPoint());
               if (dt.isInProject()) flow_queue.requeueIfInitialized(loc,dt);
             }
            return null;
          }
       }
      else dt = fm.getArgType(xid);
      if (dt == null) {
         FaitLog.logE("Call with NULL or BAD argument type " + fm + " " + xid);
         return null;
       }
      dt = dt.getRunTimeType();
      IfaceValue ncv = cv.restrictByType(dt);
      if (ncv.mustBeNull() && !ncv.getDataType().isCompatibleWith(dt))
         ncv = fait_control.findNullValue(dt);
      else if (ncv.isBad()) {
         FaitLog.logE("Call with BAD argument " + fm + " " + xid);
         return null;
       }
      if (xid < 0) {
	 if (ncv.mustBeNull()) return null;
	 ncv = ncv.forceNonNull();
         if (ncv.getDataType().getJavaType() != dt.getJavaType()) {
            IfaceType ndt = dt.getAnnotatedType(ncv.getDataType());
            ncv = ncv.makeSubtype(ndt);
          }
       }
      if (!ncv.isGoodEntitySet() && !dt.isPrimitiveType() && !ncv.canBeNull()) return null;
      ncv.getDataType().checkCompatibility(dt,loc);
      nargs.add(ncv);
      ++xid;
    }

   return nargs;
}




private IfaceValue handleSpecialCases(IfaceMethod fm,List<IfaceValue> args,FlowLocation loc,IfaceState state)
{
   if (fm.getName().equals("arraycopy") &&
         fm.getDeclaringClass().getName().equals("java.lang.System")) {
      flow_queue.handleArrayCopy(args,loc);
      return fait_control.findAnyValue(fait_control.findDataType("void"));
    }
   
   if (fm.getName().equals("toString") && !fait_control.isInProject(fm)) {
      return fait_control.findAnyValue(fait_control.findDataType("java.lang.String"));
    }
   
   if (fm.getName().equals("hashCode") && !fait_control.isInProject(fm)) {
      return fait_control.findAnyValue(fait_control.findDataType("int"));
    }
   
   if (fm.getDeclaringClass().getName().equals("edu.brown.cs.karma.KarmaUtils")) {
      if (fm.getName().equals("event")) {
         IfaceValue v0 = args.get(0);
         String sv = v0.getStringValue();
         if (sv != null) state.updateSafetyStatus(sv,fait_control);
       }
      return fait_control.findAnyValue(fm.getReturnType());
    }

   
   return null;
}



private ProtoInfo handlePrototypes(IfaceProgramPoint ppt,IfaceMethod fm,LinkedList<IfaceValue> args,
      FlowLocation loc)
{
   IfaceValue rslt = null;
   int nsrc = 0;

   if (fm.isStatic()) return null;
   
   boolean iscopy = fm.getName().equals("toArray") || fm.getName().equals("copyInto");
   iscopy &= args.size() == 2;

   IfaceValue cv = args.get(0);
   for (IfaceEntity ce : cv.getEntities()) {
      IfacePrototype pt = ce.getPrototype();
      // TODO: if ce is fixed and pt is null, convert FIXED to PROTO
      if (pt != null && pt.isMethodRelevant(fm)) {
         IfaceValue arrval = null;
         if (iscopy) {
            arrval = args.get(1).getArrayContents();
          }
         IfaceValue nv = pt.handleCall(fm,args,loc);
         IfaceCall fc = fait_control.findPrototypeMethod(ppt,fm);
         fc.noteCallSite(loc);
         if (nv != null) {
            if (rslt == null) rslt = nv;
            else rslt = rslt.mergeValue(nv);
          }
         if (iscopy) {
            IfaceValue narrval = args.get(1).getArrayContents();
            if (narrval != arrval && narrval != null) {
               flow_queue.handleArrayChange(args.get(1));
             }
          }
       }
      else if (!ce.isUserEntity()) ++nsrc;
    }

   if (rslt == null && nsrc > 0) return null;

   return new ProtoInfo(nsrc > 0,rslt);
}



private IfaceValue checkVirtual(IfaceMethod bm,List<IfaceValue> args,
      FlowLocation loc,IfaceState st,IfaceMethod orig,IfaceValue rslt,String cbid)
{
   IfaceValue cv = args.get(0);
   boolean isnative = cv.isAllNative();
   
   for (IfaceMethod km : bm.getChildMethods()) {
      if (km == orig || km == bm) continue;
      if (isnative && fait_control.isProjectClass(km.getDeclaringClass()) &&
	    !fait_control.isProjectClass(cv.getDataType()))
	 continue;
      IfaceValue srslt = processCall(km,args,false,loc,st,cbid);
      if (srslt != null) {
	 if (rslt == null) rslt = srslt;
	 else rslt = rslt.mergeValue(srslt);
       }
    }

   if (rslt == null && bm.getParentMethods().size() == 0 && bm.isAbstract() &&
	 loc != null && loc.getCall() != null && args.size() > 0) {
      for (IfaceEntity ce : cv.getEntities()) {
	 IfaceType dt = ce.getDataType();
	 if (dt != null) {
	    IfaceMethod cem = fait_control.findInheritedMethod(dt,bm.getName(),bm.getDescription());
	    if (cem != null && !cem.isAbstract()) {
	       IfaceValue srslt = processCall(cem,args,true,loc,st,cbid);
	       IfaceCall kmi = findCall(loc,cem,null,st.getSafetyStatus());
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



private IfaceValue processActualCall(IfaceMethod fm,List<IfaceValue> args,boolean virt,
      FlowLocation loc,IfaceState st,IfaceCall mi,IfaceMethod fm0,
      LinkedList<IfaceValue> nargs,IfaceValue rslt,String cbid)
{
   IfaceMethod orig = fm;
   
   if (fm0 != fm) {
      IfaceCall mi0 = findCall(loc,fm0,nargs,getSafetyStatus(st));
      synchronized (rename_map) {
	 Set<IfaceCall> s = rename_map.get(mi0);
	 if (s == null) {
	    s = new HashSet<IfaceCall>();
	    rename_map.put(mi0,s);
	  }
	 s.add(mi);
       }
      if (mi.getIsAsync()) {
	 if (rslt == null) rslt = fait_control.findAnyValue(fait_control.findDataType("void"));
       }
      mi = mi0;
    }
   
   if (mi.addCall(nargs,getSafetyStatus(st))) {
      if (FaitLog.isTracing()) FaitLog.logD1("Call " + mi);
      if (mi.getMethod().hasCode()) {
         IfaceCall from = null;
         if (loc != null) from = loc.getCall();
	 flow_queue.queueMethodCall(mi,st,from);
       }
    }

   if (loc != null) mi.noteCallSite(loc);
   // else FaitLog.logD1("No call site given");        // callbacks have no call site

   if (mi.hasResult()) {
      IfaceValue prslt = null;
      if (mi.isClone()) {
	 IfaceValue cv = nargs.get(0);
	 prslt = fait_control.findNativeValue(cv.getDataType());
       }
      else if (mi.isReturnArg0()) {
	 prslt = nargs.get(0);
       }
      else {
	 prslt = mi.getResultValue();
	 if (prslt != null) {
	    if (prslt.isNative()) flow_queue.initialize(prslt.getDataType());
	  }
       }
      if (prslt != null) {
         IfaceType ptyp = prslt.getDataType();
         if (!ptyp.isVoidType()) {
            IfaceType ntyp = ptyp.getCallType(mi,prslt,args);
            if (ntyp != null && ntyp != ptyp) {
               prslt = prslt.changeType(ntyp);
             }
          }
         rslt = prslt.mergeValue(rslt);
       }
      
      IfaceSafetyStatus sts = mi.getResultSafetyStatus();
      if (sts != null) {
         st.setSafetyStatus(mi.getResultSafetyStatus());
       }
    }
    

   if (loc != null && mi.getCanExit() && !loc.getCall().getCanExit()) {
      loc.getCall().setCanExit();
      queueReturn(null,loc.getCall(),st.getSafetyStatus(),loc);
    }

   if (virt) rslt = checkVirtual(fm,nargs,loc,st,orig,rslt,cbid);

   return rslt;
}



private IfaceCall findCall(FlowLocation loc,IfaceMethod tgt,List<IfaceValue> args,IfaceSafetyStatus sts)
{
   if (loc == null)
      return fait_control.findCall(null,tgt,null,sts,InlineType.NONE);

   InlineType il = canBeInlined(loc.getProgramPoint(),tgt);
   
   IfaceCall cm = loc.getCall().getMethodCalled(loc.getProgramPoint(),tgt);
   if (cm != null) {
      IfaceCall ncm = cm.getAlternateCall(sts,loc.getProgramPoint());
      if (ncm == cm) return cm;
      if (FaitLog.isTracing()) 
         FaitLog.logD1("Use alternative call for " + sts);
      return ncm;
    }

   cm = fait_control.findCall(loc.getProgramPoint(),tgt,args,sts,il);

   loc.getCall().noteMethodCalled(loc.getProgramPoint(),tgt,cm);

   return cm;
}



private InlineType canBeInlined(IfaceProgramPoint pt,IfaceMethod fm)
{
   if (fm.isStatic()) return InlineType.NONE;
   if (fm.isStaticInitializer()) return InlineType.NONE;
   if (fm.isNative()) return InlineType.NONE;

   if (fm.isAbstract()) return InlineType.DEFAULT;
   IfaceSpecial isp = fait_control.getCallSpecial(pt,fm);
   if (isp != null && isp.getCallbackId() != null) return InlineType.DEFAULT;

   if (fait_control.isInProject(fm)) {
      if (fm.getNumArgs() > 0 && !fm.isConstructor()) {
         IfaceType col = fait_control.findDataType("java.util.Collection");
         IfaceType map = fait_control.findDataType("java.util.Map");
         for (int i = 0; i < fm.getNumArgs(); ++i) {
            IfaceType typ = fm.getArgType(i);
            if (typ.isDerivedFrom(col) || typ.isDerivedFrom(map)) {
               return InlineType.SOURCES;
             }
          }
       }
      return InlineType.THIS;
    }
   
   if (isp != null) return InlineType.SPECIAL;
   
   return InlineType.DEFAULT;
}



void handleException(IfaceValue v0,IfaceCall cm)
{
   if (cm.addException(v0)) {
      if (FaitLog.isTracing()) FaitLog.logD1("Exception change " + cm + " :: " + v0);
      for (IfaceLocation loc : cm.getCallSites()) {
	 if (FaitLog.isTracing()) FaitLog.logD1("Queue for exception: " + loc);
	 flow_queue.queueMethodChange(loc.getCall(),loc.getProgramPoint());
       }
      for (IfaceMethod pm : cm.getMethod().getParentMethods()) {
	 for (IfaceCall xcm : fait_control.getAllCalls(pm)) {
	    handleException(v0,xcm);
	  }
       }
    }
}


IfaceSafetyStatus getSafetyStatus(IfaceState st)
{
   if (st != null) return st.getSafetyStatus();
   
   return null;
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

