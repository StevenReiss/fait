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

import edu.brown.cs.ivy.jcomp.JcompAst;

import java.util.*;

import org.eclipse.jdt.core.dom.ASTNode;



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

CallReturn handleCall(FlowLocation loc,IfaceState st0,FlowQueueInstance wq,int varct,IfaceMethod intmethod)
{
   IfaceProgramPoint inspt = loc.getProgramPoint();
   boolean virt = inspt.isVirtualCall();
   IfaceCall method = loc.getCall();
   IfaceMethod dbgmthd;
   if (intmethod != null) dbgmthd = intmethod;
   else dbgmthd = inspt.getReferencedMethod();
   if (dbgmthd == null) {
      FaitLog.logW("NO METHOD FOUND: " + inspt);
      IfaceAstReference ref = inspt.getAstReference();
      if (ref != null) {
         ASTNode n = ref.getAstNode();
         FaitLog.logD("Call: " + n);
         FaitLog.logD("Def: " + JcompAst.getDefinition(n));
         FaitLog.logD("Ref: " + JcompAst.getReference(n));
       }
      return CallReturn.NOT_DONE;
    }
   // if (dbgmthd.getFullName().contains("ThreadPoolExecutor.execute") && loc.toString().contains("cose")) {
      // System.err.println("CHECK HERE");
    // }
   
   LinkedList<IfaceValue> args = getCallArguments(loc,st0,varct,dbgmthd);
   if (args == null) {
      return CallReturn.NOT_DONE;
    }

   if (FaitLog.isTracing()) {
      for (IfaceValue av : args) FaitLog.logD1("Arg = " + av);
      FaitLog.logD1("Saftey state = " + getSafetyStatus(st0));
    }

   IfaceMethod tgt = findProperMethod(loc,args,dbgmthd);
   IfaceSpecial spl = fait_control.getCallSpecial(inspt,tgt);

   flow_queue.initialize(tgt.getDeclaringClass());
   
   if (spl != null && spl.getIgnoreVirtualCalls()) {
      virt = false;
    }
   IfaceValue rslt = processCall(tgt,args,virt,loc,st0,null,varct);

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
   if (rtyp == null && rslt != null) rtyp = rslt.getDataType();
   if (rtyp != null && !rtyp.isVoidType()) {
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
   processCall(bm,args,true,null,st0,cbid,-1);
}



/********************************************************************************/
/*										*/
/*	Return methods								*/
/*										*/
/********************************************************************************/

void handleReturn(IfaceCall cm,IfaceValue v0,IfaceSafetyStatus sts,IfaceState st0,IfaceLocation loc,int stackref)
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
	 // dt.checkCompatibility(rt,loc,v0,stackref);
	 v0 = v0.changeType(rt);
       }
    }

   if (FaitLog.isTracing()) {
      FaitLog.logD1("Return value = " + v0);
      FaitLog.logD1("Safety Return = " + sts);
    }

   boolean fg = cm.addResult(v0,sts,st0);
   if (fg) flow_queue.handleReturnSetup(bm);
   if (fg) {
      queueReturn(v0,cm,sts,st0,loc);
      Set<IfaceCall> s = rename_map.get(cm);
      if (s != null) {
	 for (IfaceCall c0 : s) {
	    queueReturn(v0,c0,sts,st0,loc);
	  }
       }
    }
   else {
      if (FaitLog.isTracing())
	 FaitLog.logD1("No return change");
    }
}



private void queueReturn(IfaceValue v,IfaceCall cm,IfaceSafetyStatus sts,IfaceState st0,IfaceLocation loc)
{
   if (FaitLog.isTracing()) FaitLog.logD1("Return change " + cm + " = " + v + " [" + sts + "]");

   for (IfaceLocation cloc : cm.getCallSites()) {
      if (FaitLog.isTracing()) FaitLog.logD1("Queue for return " + cloc);
      flow_queue.queueMethodChange(cloc.getCall(),cloc.getProgramPoint());
    }
   
   IfaceSafetyStatus startsts = cm.getStartState().getSafetyStatus();
   for (IfaceMethod fm : cm.getMethod().getParentMethods()) {
      for (IfaceCall xcm : fait_control.getAllCalls(fm)) {
         boolean fnd = false;
         for (IfaceCall ccm : xcm.getAlternateCalls()) {
            IfaceSafetyStatus callsts = ccm.getStartState().getSafetyStatus();
            if (callsts == startsts) {
               fnd = true;
               handleReturn(ccm,v,sts,st0,loc,0);
               break;
             }
          }
	 if (!fnd) {
            handleReturn(xcm,v,sts,st0,loc,0);
          }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Code to handle actual call						*/
/*										*/
/********************************************************************************/

private LinkedList<IfaceValue> getCallArguments(FlowLocation loc,IfaceState st0,int varct,IfaceMethod tgt)
{
   IfaceProgramPoint ins = loc.getProgramPoint();
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
	    int stkloc = args.size() - i -delta - 1;
	    if (varct >= 0) stkloc = stkloc - 1 + varct;
	    v0.getDataType().checkCompatibility(rtype,loc,v0,stkloc);
	  }
       }
    }

   return args;
}



private IfaceMethod findProperMethod(IfaceLocation loc,List<IfaceValue> args,IfaceMethod tgt)
{
   IfaceProgramPoint ins = loc.getProgramPoint();
   boolean ifc = ins.isInterfaceCall();
   ifc |= tgt.isAbstract();
   
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
      FlowLocation loc,IfaceState st0,String cbid,int varct)
{
   IfaceValue rslt = null;
   IfaceMethod orig = bm;
   IfaceProgramPoint ppt = null;
   if (loc != null) ppt = loc.getProgramPoint();

   LinkedList<IfaceValue> nargs = checkCall(loc,bm,args,varct);
   
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
      rslt = handleSpecialCases(bm,args,loc,st0);
      if (rslt != null) return rslt;
    }

   if (nargs != null) {
      IfaceCall mi = findCall(loc,bm,nargs,getSafetyStatus(st0));
      mi.addCallbacks(loc,nargs);
      Map<IfaceMethod,List<IfaceValue>> c = mi.replaceWith(ppt,nargs);
      if (c == null) return rslt;
      IfaceValue prslt = rslt;
      for (Map.Entry<IfaceMethod,List<IfaceValue>> ent : c.entrySet()) {
	 IfaceMethod m0 = ent.getKey();
	 LinkedList<IfaceValue> vargs = new LinkedList<>(ent.getValue());
	 mi.fixReplaceArgs(m0,vargs);
	 IfaceValue xrslt = processActualCall(bm,args,virt,loc,st0,mi,m0,vargs,prslt,cbid,varct);
	 if (m0.getName().contains("<init>") && !bm.getName().contains("<init>")) {
	    if (xrslt == null || xrslt.getDataType().isVoidType()) {
	       xrslt = vargs.get(0);
	     }
	  }
	 if (rslt == null) rslt = xrslt;
	 else if (xrslt != null) rslt = rslt.mergeValue(xrslt);
       }
    }
   else if (virt) {
      rslt = checkVirtual(bm,args,loc,st0,orig,rslt,cbid,varct);
    }

   return rslt;
}



private LinkedList<IfaceValue> checkCall(FlowLocation loc,IfaceMethod fm,List<IfaceValue> args,int varct)
{
   int xid = (fm.isStatic() ? 0 : -1);

   LinkedList<IfaceValue> nargs = new LinkedList<IfaceValue>();
   IfaceType dt = null;
   for (int i = 0; i < args.size(); ++i) {
      IfaceValue cv = args.get(i);
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
      else {
	 dt = fm.getArgType(xid);
	 IfaceProgramPoint pt = null;
	 if (loc != null) pt = loc.getProgramPoint();
	 IfaceSpecial spl = fait_control.getCallSpecial(pt,fm);
	 if (spl != null) {
	    IfaceAnnotation [] annots = spl.getArgAnnotations(xid);
	    if (annots != null) {
	       dt = dt.getAnnotatedType(annots);
	     }
	  }
       }
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
      int stkloc = args.size() - i - 1;
      if (varct >= 0) stkloc = stkloc - 1 + varct;
      ncv.getDataType().checkCompatibility(dt,loc,ncv,stkloc);
      IfaceType dt1 = fait_control.findDataType(dt.getJavaType().getName());
      if (dt1 != dt) {
	 ncv.checkContentCompatibility(dt,loc,stkloc);
       }
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
      if (args.get(0).getDataType().isStringType()) return args.get(0);
      return fait_control.findAnyValue(fait_control.findDataType("java.lang.String"));
    }

   if (fm.getName().equals("hashCode") && !fait_control.isInProject(fm)) {
      return fait_control.findAnyValue(fait_control.findDataType("int"));
    }
   
   String evt = fait_control.getEventForCall(fm,args,loc);
   if (evt != null) {
      state.updateSafetyStatus(evt,fait_control);
    }

   if (fm.getDeclaringClass().getName().equals("edu.brown.cs.karma.KarmaUtils")) {
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
      FlowLocation loc,IfaceState st,IfaceMethod orig,IfaceValue rslt,String cbid,int varct)
{
   IfaceValue cv = args.get(0);
   boolean isnative = cv.isAllNative();
   Set<IfaceMethod> used = new HashSet<>();
   used.add(bm);
   if (orig != null) used.add(orig);

   for (IfaceMethod km : bm.getChildMethods()) {
      if (used.contains(km)) continue;
      used.add(km);
      if (isnative && fait_control.isProjectClass(km.getDeclaringClass()) &&
	    !fait_control.isProjectClass(cv.getDataType()))
	 continue;
      IfaceValue srslt = processCall(km,args,false,loc,st,cbid,varct);
      if (srslt != null) {
	 if (rslt == null) rslt = srslt;
	 else rslt = rslt.mergeValue(srslt);
       }
    }

   if (rslt == null &&
	 loc != null && loc.getCall() != null && args.size() > 0) {
      used.add(bm);
      for (IfaceEntity ce : cv.getEntities()) {
	 IfaceType dt = ce.getDataType();
	 if (dt != null) {
	    IfaceMethod cem = fait_control.findInheritedMethod(dt,bm.getName(),bm.getDescription());
	    if (cem != null && !cem.isAbstract() && !used.contains(cem)) {
	       used.add(cem);
	       IfaceValue srslt = processCall(cem,args,true,loc,st,cbid,varct);
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
      LinkedList<IfaceValue> nargs,IfaceValue rslt,String cbid,int varct)
{
   IfaceMethod orig = fm;
   IfaceSafetyStatus nsts = null;
   
   IfaceCall mi0 = findCall(loc,fm,nargs,getSafetyStatus(st));
   if (mi0 != null) {
      mi = mi0;
    } 
   
   if (fm0 != fm) {
      IfaceCall mi1 = findCall(loc,fm0,nargs,getSafetyStatus(st));
      synchronized (rename_map) {
	 Set<IfaceCall> s = rename_map.get(mi1);
	 if (s == null) {
	    s = new HashSet<>();
	    rename_map.put(mi1,s);
	  }
	 s.add(mi);
         FaitLog.logD1("Add to rename map for " + mi1.getMethod().getFullName() + 
               " " + mi1.hashCode() + " : " +
               mi.getMethod().getFullName() + " " + mi.hashCode());
       }
      if (mi.getIsAsync()) {
	 if (rslt == null) rslt = fait_control.findAnyValue(fait_control.findDataType("void"));
       }
      mi = mi1;
    }
   FaitLog.logD1("Using call " + mi.getMethod().getFullName() + " " + mi.hashCode());
   
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
         if (mi.isSetFields(true)) {
            IfaceType dt = prslt.getDataType();
            if (fm.isConstructor()) dt = fm.getDeclaringClass();
            Map<String,IfaceType> fd = dt.getJavaType().getFieldData();
            for (Map.Entry<String,IfaceType> ent : fd.entrySet()) {
                IfaceValue fval = fait_control.findMutableValue(ent.getValue());
                IfaceField fld = fait_control.findField(dt,ent.getKey());
                flow_queue.handleFieldSet(fld,loc,st,false,fval,null,-1);
             }
          }
         if (mi.isAffected() && !fm.isStatic()) {
            IfaceValue tv = nargs.get(0);
            IfaceValue argv = null;
            for (int i = 1; i < nargs.size(); ++i) {
               IfaceValue v0 = nargs.get(i);
               if (argv == null) {
                  IfaceType t0 = fait_control.findDataType("java.lang.Object");
                  argv = fait_control.findNativeValue(t0);
                }
               IfaceType t0 = argv.getDataType();
               IfaceType t1 = t0.getAnnotatedType(v0.getDataType());
               if (t1 != t0) argv = argv.changeType(t1);
             }
            if (argv != null) {
               for (IfaceEntity te : tv.getEntities()) {
                  te.addToArrayContents(argv,null,loc);
                }
             }
            IfaceValue v0 = tv.getArrayContents();
            if (v0 != null) {
               IfaceType tr = prslt.getDataType();
               IfaceType tr1 = tr.getAnnotatedType(v0.getDataType());
               if (tr1 != tr) prslt = prslt.changeType(tr1);
             }
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
      if (sts != null) nsts = sts;
    }


   if (loc != null && mi.getCanExit() && !loc.getCall().getCanExit()) {
      loc.getCall().setCanExit();
      queueReturn(null,loc.getCall(),st.getSafetyStatus(),st,loc);
    }

   if (virt) rslt = checkVirtual(fm,nargs,loc,st,orig,rslt,cbid,varct);

   if (nsts != null && nsts != st.getSafetyStatus()) {
      FaitLog.logD1("Replace safety status with " + mi.getResultSafetyStatus());
      st.setSafetyStatus(mi.getResultSafetyStatus());
    }
 
 return rslt;
}



private IfaceCall findCall(FlowLocation loc,IfaceMethod tgt,List<IfaceValue> args,IfaceSafetyStatus sts)
{
   if (loc == null)
      return fait_control.findCall(null,tgt,null,sts,InlineType.NONE);
   
   FaitLog.logD1("Find call safety state = " + sts);

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
   IfaceSpecial isp = fait_control.getCallSpecial(pt,fm);
   
   if (fm.isStatic() || fm.isStaticInitializer() || fm.isNative()) {
      if (isp != null) return InlineType.SPECIAL;
      return InlineType.NONE;
    }
   
   if (fm.isAbstract()) return InlineType.DEFAULT;
   if (isp != null && isp.getInlineType() != null &&
         isp.getInlineType() != InlineType.NORMAL)
      return isp.getInlineType();

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
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

void handleUpdate(IfaceUpdater upd)
{
   for (Iterator<Map.Entry<IfaceCall,Set<IfaceCall>>> it1 = rename_map.entrySet().iterator(); it1.hasNext(); ) {
      Map.Entry<IfaceCall,Set<IfaceCall>> ent = it1.next();
      if (upd.isCallRemoved(ent.getKey())) {
         it1.remove();
       }
      else {
         int ct = 0;
         for (Iterator<IfaceCall>  it2 = ent.getValue().iterator(); it2.hasNext(); ) {
            IfaceCall c2 = it2.next();
            if (upd.isCallRemoved(c2)) it2.remove();
            else ++ct;
          }
         if (ct ==  0) it1.remove();
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

