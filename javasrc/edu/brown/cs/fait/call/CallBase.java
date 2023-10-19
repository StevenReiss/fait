/********************************************************************************/
/*										*/
/*		CallBase.java							*/
/*										*/
/*	Basic information for methods calls					*/
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



package edu.brown.cs.fait.call;

import edu.brown.cs.fait.iface.*;

import java.util.*;



class CallBase implements CallConstants, IfaceCall
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl	fait_control;
private IfaceMethod	for_method;
private IfaceSpecial	special_data;
private IfaceValue	result_set;
private IfaceValue	exception_set;
private IfaceState	start_state;
private Set<IfaceState> return_states;
private boolean 	is_clone;
private int      	return_arg;
private boolean 	is_proto;
private boolean 	can_exit;
private boolean 	set_fields;
private int		num_adds;
private int		num_result;
private boolean 	is_scanned;
private boolean 	is_affected;
private QueueLevel	queue_level;
private List<IfaceType> implied_arg_types;
private IfaceType	implied_return_type;
private IfaceSafetyStatus safety_result;
private CallBase	alternate_call;
private boolean         force_scan;

private int		num_forward;
private int		num_backward;
private int		num_scan;

private Map<IfaceProgramPoint,IfaceEntity> array_map;
private Map<IfaceProgramPoint,IfaceEntity> entity_map;
private Map<IfaceProgramPoint,IfaceEntity.UserEntity> userentity_map;

private Map<IfaceProgramPoint,Map<IfaceMethod,IfaceCall>> method_map;
private Set<IfaceLocation> call_set;

private Map<IfaceProgramPoint,Collection<IfaceError>> error_set;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CallBase(IfaceControl fc,IfaceMethod fm,IfaceProgramPoint pt,IfaceSafetyStatus sts)
{
   fait_control = fc;

   result_set = null;
   exception_set = null;

   num_adds = 0;
   num_result = 0;

   is_clone = false;
   return_arg = -1;
   is_proto = false;
   can_exit = false;
   is_scanned = false;
   set_fields = false;
   is_affected = false;
   force_scan = false;
   
   queue_level = QueueLevel.NORMAL;

   alternate_call = null;

   array_map = Collections.synchronizedMap(new IdentityHashMap<>(4));
   entity_map = Collections.synchronizedMap(new IdentityHashMap<>(4));
   userentity_map = Collections.synchronizedMap(new IdentityHashMap<>(4));
   method_map = new IdentityHashMap<>();
   call_set = new HashSet<>();
   error_set = new IdentityHashMap<>();

   for_method = fm;
   special_data = fc.getCallSpecial(pt,fm);

   if (fm.getReturnType() != null && !fm.getReturnType().isVoidType()) {
      result_set = fc.findMutableValue(fm.getReturnType());
    }

   int lclsz = fm.getLocalSize();
   if (lclsz == 0 && fm.getFullName().startsWith("java.lang.invoke.VarHandle.")) {
      // handle polymorphic methods
      lclsz = 10;
    }
   
   start_state = fc.createState(lclsz,sts);
   return_states = new HashSet<>();

   int idx = 0;
   if (!fm.isStatic()) {
      if (lclsz == 0) {
	 FaitLog.logE("Problem with method " + fm.getFullName() + " " + fm.getLocalSize() + " " +
	    fm.getDescription() + " " + fm.hasCode() + " " + fm.getClass() + " " + fm.getStart());
       }
      IfaceValue fv = fc.findAnyValue(fm.getDeclaringClass());
      fv = fv.forceNonNull();
      start_state.setLocal(idx++,fv);
    }
   for (int i = 0; ; ++i) {
      IfaceType atyp = fm.getArgType(i);
      if (atyp == null) break;
      IfaceValue fv = fc.findAnyValue(atyp);
      if (i == 0 && fm.isStatic() && fm.getName().equals("main"))
	 fv = fc.findMainArgsValue();
      if (idx >= lclsz) {
         System.err.println("PROBLEM WITH LOCALS");
       }
      start_state.setLocal(idx++,fv);
      if (atyp.isCategory2()) ++idx;
    }

   if (fm.getExternalSymbols() != null) {
      for (Object sym : fm.getExternalSymbols()) {
	 IfaceValue iv = fm.getExternalValue(sym);
	 int slot = fm.getLocalOffset(sym);
	 if (slot > 0 && iv != null) {
	    start_state.setLocal(slot,iv);
	  }
       }
    }

   if (fm.getName().equals("clone") && fm.getDeclaringClass().isJavaLangObject()) {
      is_clone = true;
      ++num_result;
    }
   else if (special_data != null) {
      is_clone = special_data.getIsClone();
      return_arg = special_data.getReturnArg();
      can_exit = special_data.getExits();
      set_fields = special_data.getSetFields();
      is_affected = special_data.isAffected();
      // this null needs to be a FaitLocaiton with fm as the method
      if (special_data.getDontScan()) {
	 IfaceValue rv = special_data.getReturnValue(null,for_method);
	 result_set = null;
	 addResult(rv,null,null);
	 if (rv != null) {
	    List<IfaceValue> excs = special_data.getExceptions(null,for_method);
	    IfaceValue ev = null;
	    if (excs != null) {
	       for (IfaceValue v0 : excs) {
		  ev = v0.mergeValue(ev);
		}
	     }
	    else {
	       for (IfaceType edt : fm.getExceptionTypes()) {
		  IfaceValue ecv = fc.findNativeValue(edt);
		  ecv = ecv.forceNonNull();
		  ev = ecv.mergeValue(ev);
		}
	     }
	    exception_set = ev;
	  }
       }
    }

   if (fm.isNative()) {
      if (result_set == null || !result_set.isGoodEntitySet())
	 result_set = fc.findNativeValue(fm.getReturnType());
      ++num_result;
    }
   else if (!fm.hasCode() && !fait_control.isInProject(fm)) {
      if (result_set == null || !result_set.isGoodEntitySet())
	 result_set = fc.findMutableValue(fm.getReturnType());
      ++num_result;
    }

   int act = fm.getNumArgs();
   implied_arg_types = new ArrayList<>();
   for (int i = 0; i < act; ++i) {
      implied_arg_types.add(fm.getArgType(i));
    }
   implied_return_type = fm.getReturnType();

   loadClasses();

   num_scan = 0;
   num_forward = 0;
   num_backward = 0;

   if (num_result > 0) is_scanned = false;
   else is_scanned = true;

   safety_result = null;
}





/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceMethod getMethod()	{ return for_method; }
@Override public IfaceControl getControl()	{ return fait_control; }

@Override public IfaceState getStartState()	{ return start_state; }

@Override public Set<IfaceState> getReturnStates()
{
   return return_states;
}

@Override public IfaceValue getResultValue()	{ return result_set; }

@Override public IfaceSafetyStatus getResultSafetyStatus()
{
   return safety_result;
}
@Override public IfaceValue getExceptionValue() { return exception_set; }

@Override public boolean isClone()		{ return is_clone; }
@Override public int getReturnArg() 	        { return return_arg; }

@Override public boolean isSetFields(boolean clr)
{
   boolean rslt = set_fields;
   if (clr) set_fields = false;
   return rslt;
}


@Override public boolean isAffected()
{
   return is_affected;
}

@Override public boolean getCanExit()		{ return can_exit; }
@Override public void setCanExit()		{ can_exit = true; }

@Override public boolean isPrototype()		{ return is_proto; }
@Override public void setPrototype()		{ is_proto = true; }

@Override public boolean isScanned()		{ return is_scanned; }


@Override public boolean getIsAsync()
{
   if (special_data != null) return special_data.getIsAsync();
   return false;
}


@Override public void loadClasses()
{
   if (special_data != null && special_data.getClassesToLoad() != null) {
      for (String s : special_data.getClassesToLoad()) {
	 IfaceType typ = fait_control.findDataType(s);
	 if (typ != null) fait_control.initialize(typ);
       }
    }
}



@Override public IfaceEntity getArrayEntity(IfaceProgramPoint ins)
{
   return array_map.get(ins);
}

@Override public void setArrayEntity(IfaceProgramPoint ins,IfaceEntity e)
{
   array_map.put(ins,e);
}


@Override public IfaceEntity getBaseEntity(IfaceProgramPoint ins)
{
   return entity_map.get(ins);
}



@Override public void setBaseEntity(IfaceProgramPoint ins,IfaceEntity e)
{
   entity_map.put(ins,e);
}


@Override public IfaceEntity.UserEntity getUserEntity(IfaceProgramPoint ins)
{
   return userentity_map.get(ins);
}

@Override public void setUserEntity(IfaceProgramPoint ins,IfaceEntity.UserEntity e)
{
   userentity_map.put(ins,e);
}



@Override public void addError(IfaceProgramPoint ins,IfaceError err)
{
   synchronized (error_set) {
      Collection<IfaceError> errs = error_set.get(ins);
      if (errs == null) {
	 errs = new HashSet<>(2);
	 error_set.put(ins,errs);
       }
      errs.add(err);
    }
}




@Override public void removeErrors(IfaceProgramPoint ins)
{
   synchronized (error_set) {
      error_set.remove(ins);
    }
}



@Override public List<IfaceProgramPoint> getErrorLocations()
{
   synchronized (error_set) {
      List<IfaceProgramPoint> rslt = new ArrayList<>(error_set.keySet());
      return rslt;
    }
}


@Override public Collection<IfaceError> getErrors(IfaceProgramPoint pt)
{
   synchronized (error_set) {
      return error_set.get(pt);
    }
}

/********************************************************************************/
/*										*/
/*	Value access methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getThisValue()
{
   if (for_method.isStatic()) return null;

   synchronized (this) {
      return start_state.getLocal(0);
    }
}


@Override public Iterable<IfaceValue> getParameterValues()
{
   Collection<IfaceValue> rslt = new ArrayList<IfaceValue>();

   synchronized (this) {
      int idx = 0;
      if (!for_method.isStatic()) {
	 IfaceValue v0 = start_state.getLocal(idx++);
	 rslt.add(v0);
       }

      for ( ; ; ) {
	 IfaceValue v0 = start_state.getLocal(idx++);
	 if (v0 == null) break;
	 rslt.add(v0);
	 if (v0.isCategory2()) ++idx;
       }
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Call methods								*/
/*										*/
/********************************************************************************/

@Override public boolean addCall(List<IfaceValue> args,IfaceSafetyStatus sts)
{
   boolean chng = false;

   synchronized (this) {
      int idx = 0;
      for (IfaceValue cv : args) {
	 IfaceValue ov = start_state.getLocal(idx);
	 if (!cv.isBad()) {
	    if (num_adds == 0) start_state.setLocal(idx,cv);
	    else chng |= start_state.addToLocal(idx,cv);
	  }
	 if (start_state.getLocal(idx).isBad()) {
	    start_state.setLocal(idx,ov);
	  }
	 if (cv.isCategory2()) ++idx;
	 ++idx;
       }
      if (num_adds++ == 0) chng = true;
    }

   if (special_data != null && special_data.getDontScan()) chng = false;
   else if (start_state.mergeSafetyStatus(sts)) chng = true;
   if (force_scan) {
      force_scan = false;
      chng = true;
    }

   return chng;
}


@Override synchronized public boolean addException(IfaceValue exc)
{
   if (exception_set == exc || exc == null) return false;
   if (exception_set == null) {
      exception_set = exc;
      return true;
    }

   IfaceValue ns = exception_set.mergeValue(exc);
   if (ns == exception_set) return false;
   exception_set = ns;
   return true;
}



@Override synchronized public boolean hasResult()
{
   if (num_result > 0 || return_arg >= 0) return true;
   return false;
}


@Override synchronized public boolean addResult(IfaceValue v,IfaceSafetyStatus sts,IfaceState fromstate)
{
   boolean chng = false;

   if (v == null) {				// handle void routines
      if (num_result++ == 0) chng = true;
    }
   else if (result_set == null || num_result == 0) {
      ++num_result;
      result_set = v;
      chng = true;
    }
   else {
      ++num_result;
      if (result_set != v) {
	 IfaceValue nv = result_set.mergeValue(v);
	 if (nv != result_set) {
	    result_set = nv;
	    chng = true;
	  }
       }
    }

   if (return_arg >= 0) chng = false;

   if (sts != null) {
      if (safety_result == null) {
	 safety_result = sts;
	 if (return_arg < 0) chng = true;
       }
      else {
	 IfaceSafetyStatus nsts = safety_result.merge(sts);
	 if (nsts != safety_result) {
	    FaitLog.logD1("Update safety status to " + nsts + " for " + hashCode());
	    safety_result = nsts;
	    chng = true;
	  }
       }
    }

   if (chng && fromstate != null) {
      addReturnStates(fromstate);
    }

   return chng;
}



private void addReturnStates(IfaceState st)
{
   if (st.getLocation() != null) return_states.add(st);
   else {
      for (int i = 0; i < st.getNumPriorStates(); ++i) {
	 IfaceState stp = st.getPriorState(i);
	 addReturnStates(stp);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Call replacement methods						*/
/*										*/
/********************************************************************************/

@Override public Map<IfaceMethod,List<IfaceValue>> replaceWith(IfaceProgramPoint pt,List<IfaceValue> args)
{
   if (special_data == null) return Collections.singletonMap(for_method,args);
   String nm = special_data.getReplaceName();

   if (special_data.isConstructor()) {
      if (nm == null) {
	 IfaceValue fv = special_data.getReturnValue(pt,for_method);
	 nm  = fv.getDataType().getName();
       }
    }

   if (nm == null)
      return Collections.singletonMap(for_method,args);

   Map<IfaceMethod,List<IfaceValue>> rslt = new HashMap<>();

   String desc = "(";
   String statdesc = "(";
   int act = 0;
   for (IfaceValue v : args) {
      String d = v.getDataType().getJavaTypeName();
      if (act++ > 0) desc += d;
      statdesc += d;
    }
   desc += ")";
   statdesc += ")";

   StringTokenizer tok = new StringTokenizer(nm);
   while (tok.hasMoreTokens()) {
      IfaceValue newval = null;
      nm = tok.nextToken();
      if (special_data.isConstructor()) {
	 String tynm = nm;
	 int idx1 = tynm.indexOf("(");
	 if (idx1 > 0) tynm = tynm.substring(0,idx1);
	 IfaceType rtyp = fait_control.findDataType(tynm);
	 newval = fait_control.findAnyValue(rtyp);
	 String nnm = tynm + ".<init>";
	 if (idx1 > 0) nnm += nm.substring(idx1);
	 nm = nnm;
       }
      String adesc = null;
      int aidx = nm.indexOf("(");
      if (aidx > 0) {
	 adesc = nm.substring(aidx);
	 nm = nm.substring(0,aidx);
       }

      int idx = nm.lastIndexOf(".");
      if (idx > 0) {
         IfaceMethod nfm = null;
	 String cls = nm.substring(0,idx);
	 String mnm = nm.substring(idx+1);
	 if (adesc != null) nfm = fait_control.findMethod(cls,mnm,adesc);
	 else {
	    nfm = fait_control.findMethod(cls,mnm,desc);
	    if (nfm != null && nfm.isStatic()) nfm = null;
	    if (nfm == null) {
	       nfm = fait_control.findMethod(cls,mnm,statdesc);
	       if (nfm != null && !nfm.isStatic()) nfm = null;
	     }
	  }
         if (nfm != null && !nfm.isNative() && !nfm.isAbstract()) {
            List<IfaceValue> nargs = special_data.getCallbackArgs(args,newval);
            if (nargs == null) nargs = args;
            rslt.put(nfm,nargs);
          }
       }
      else {
	 List<IfaceValue> nargs = special_data.getCallbackArgs(args,newval);
	 IfaceValue v0 = args.get(0);
	 if (nargs != null) v0 = nargs.get(0);
         else nargs = args;
         Set<IfaceMethod> done = new HashSet<>();
         for (IfaceEntity ent : v0.getEntities()) {
            IfaceMethod nfm = null;
            IfaceType dt = ent.getDataType();
            while (dt != null && !dt.isFunctionRef()) {
               if (adesc != null) nfm = fait_control.findMethod(dt.getName(),nm,adesc);
               else nfm = fait_control.findMethod(dt.getName(),nm,desc);
               if (nfm != null) break;
               dt = dt.getSuperType();
             }
            if (nfm != null && !nfm.isNative() && !nfm.isAbstract()) {
               if (done.add(nfm)) {
                  rslt.put(nfm,nargs);
                }
             }
          }
       }
    }


   if (rslt.isEmpty()) rslt.put(for_method,args);

   return rslt;
}



@Override public void fixReplaceArgs(IfaceMethod fm,LinkedList<IfaceValue> args)
{
   fixArgs(fm,args);
}



private void fixArgs(IfaceMethod fm,List<IfaceValue> args)
{
   int bct = 1;
   if (fm.isStatic()) bct = 0;
   int act = 0;

   for (int i = 0; ; ++i) {
      IfaceType aty = fm.getArgType(i);
      if (aty == null) break;
      if (act + bct >= args.size()) {
	 IfaceValue v = fait_control.findNativeValue(aty);
	 v = v.forceNonNull();
	 args.add(v);
       }
      ++act;
    }

   while (args.size() > act + bct) {
      args.remove(act+bct);
    }
}



/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

@Override public void addCallbacks(IfaceLocation loc,List<IfaceValue> args)
{
   if (special_data == null) return;
   Iterable<String> it = special_data.getCallbacks();
   if (it == null) return;

   if (FaitLog.isTracing()) FaitLog.logD1("Check callbacks " + args);

   List<IfaceValue> nargs = special_data.getCallbackArgs(args,null);
   IfaceValue cv0 = nargs.get(0);
   IfaceType typ = cv0.getDataType();
   if (typ == null) return;

   for (String cbn : it) {
      IfaceMethod fm = findCallbackMethod(typ,cbn,nargs.size(),true);
      if (fm != null) {
	 List<IfaceValue> rargs = new ArrayList<IfaceValue>(nargs);
	 fixArgs(fm,rargs);
	 if (FaitLog.isTracing()) FaitLog.logD("Use callback " + fm + " " + rargs);
	 fait_control.handleCallback(loc,fm,rargs,special_data.getCallbackId());
       }
      else {
	 if (FaitLog.isTracing())
	    FaitLog.logD("No callback found for " + cbn + " in " + typ);
       }
    }
}


@Override public IfaceMethod findCallbackMethod(IfaceType cls,String mthd,int asz,boolean intf)
{
   for (IfaceMethod fm : fait_control.findAllMethods(cls,mthd)) {
      if (fm.getName().equals(mthd)) {
	 if (fm.isStatic() && fm.getNumArgs() >= asz) return fm;
	 else if (fm.getNumArgs() + 1 >= asz) return fm;
       }
    }

   IfaceType fdt = cls.getSuperType();
   if (fdt != null) {
      IfaceMethod fm = findCallbackMethod(fdt,mthd,asz,intf);
      if (fm != null) return fm;
    }

   if (intf) {
      for (IfaceType sdt : cls.getInterfaces()) {
	 IfaceMethod fm = findCallbackMethod(sdt,mthd,asz,true);
	 if (fm != null) return fm;
       }
    }

   return null;
}


/********************************************************************************/
/*										*/
/*	Call tracking methods							*/
/*										*/
/********************************************************************************/

@Override public void noteCallSite(IfaceLocation loc)
{
   if (loc == null) return;

   synchronized (call_set) {
      call_set.add(loc);
    }
}


@Override public Collection<IfaceLocation> getCallSites()
{
   synchronized (call_set) {
      return new ArrayList<IfaceLocation>(call_set);
    }
}


@Override public void noteMethodCalled(IfaceProgramPoint ins,IfaceMethod m,IfaceCall called)
{
   synchronized (method_map) {
      Map<IfaceMethod,IfaceCall> mm = method_map.get(ins);
      if (mm == null) {
	 mm = new HashMap<IfaceMethod,IfaceCall>(4);
	 method_map.put(ins,mm);
       }
      mm.put(m,called);
    }
}


private void removeMethodCall(IfaceLocation loc,IfaceCall c)
{
   IfaceProgramPoint ipt = loc.getProgramPoint();
   synchronized (method_map) {
      Map<IfaceMethod,IfaceCall> mm = method_map.get(ipt);
      if (mm == null) return;
      for (Iterator<IfaceCall> it = mm.values().iterator(); it.hasNext(); ) {
	 IfaceCall ic = it.next();
	 if (ic == c) it.remove();
       }
    }
}


@Override public IfaceCall getMethodCalled(IfaceProgramPoint ins,IfaceMethod m)
{
   synchronized (method_map) {
      Map<IfaceMethod,IfaceCall> mm = method_map.get(ins);
      if (mm == null) return null;
      return mm.get(m);
    }
}


@Override public Collection<IfaceCall> getAllMethodsCalled(IfaceProgramPoint ins)
{
   Collection<IfaceCall> rslt = new ArrayList<>();
   synchronized (method_map) {
      if (ins == null) {
         for (Map<IfaceMethod,IfaceCall> mm : method_map.values()) {
            if (mm != null) rslt.addAll(mm.values());
          }
       }
      else {
         Map<IfaceMethod,IfaceCall> mm = method_map.get(ins);
         if (mm != null) rslt.addAll(mm.values());
       }
    }
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Incremental update methods						*/
/*										*/
/********************************************************************************/

@Override public void removeForUpdate(IfaceUpdater upd)
{
   FaitLog.logD("CALL","Remove for update: " +  getMethod().getFullName() + " " +
         method_map.size() + " " + entity_map.size() + " " + hashCode());
   
   // first remove all entities created in this call
   for (IfaceEntity ie : entity_map.values()) {
      upd.addEntityToRemove(ie);
    }
   entity_map.clear();

   // next find all items called from here and remove this as a caller
   // if we were the only caller, then remove that call as well
   for (Map<IfaceMethod,IfaceCall> mm : method_map.values()) {
      for (IfaceCall ic : mm.values()) {
	 CallBase cb = (CallBase) ic;
         FaitLog.logD("CALL","Call Removal " + ic.hashCode() + " " +
           cb.toString());
	 if (cb.removeCaller(this)) {
            // this seems to ignore too much later on
//          upd.removeCall(cb);
          }
       }
    }
   method_map.clear();
   
   return_states.clear();
   
   force_scan = true;
   
   // finally remove this call from any call site for it and requeue
   // that call to be evaluated.  Ignore if the call site will also
   // be eliminated
   requeueCall(upd);
}


private void requeueCall(IfaceUpdater upd)
{
   for (IfaceLocation loc : call_set) {
      CallBase cb = (CallBase) loc.getCall();
      cb.removeMethodCall(loc,this);
      if (!upd.shouldUpdate(cb)) {
         FaitLog.logD("CALL","Requeue " + cb.hashCode() + " " + loc);
	 upd.addToWorkQueue(cb,loc.getProgramPoint());
       }
    }
}



private boolean removeCaller(IfaceCall src)
{
   synchronized (call_set) {
      for (Iterator<IfaceLocation> it = call_set.iterator(); it.hasNext(); ) {
	 IfaceLocation loc = it.next();
	 if (loc.getCall() == src) it.remove();
       }
      return call_set.isEmpty();
    }
}

@Override public void handleUpdates(IfaceUpdater upd)
{
   // this needs to be handled
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   if (for_method == null) return "???";

   StringBuffer buf = new StringBuffer();
   buf.append(for_method.getName());
   buf.append(for_method.getDescription());

   if (start_state != null) {
      buf.append(" :: ");
      int idx = 0;
      int sid = -1;
      if (for_method.isStatic()) sid = 0;
      for (int i = sid; ; ++i) {
	 IfaceType dt;
	 if (i < 0) dt = for_method.getDeclaringClass();
	 else dt = for_method.getArgType(i);
	 if (dt == null) break;
	 IfaceValue va = start_state.getLocal(idx);
	 if (va != null) buf.append(va.toString());
	 else buf.append("null");
	 buf.append(" :: ");
	 if (dt.isCategory2()) ++idx;
	 ++idx;
       }
    }

   if (result_set != null && num_result > 0) {
      buf.append(" => ");
      buf.append(result_set.toString());
    }

   return buf.toString();
}


@Override public String getLogName()
{
   IfaceMethod fm = getMethod();

   return fm.getFullName() + fm.getDescription() + " " + hashCode();
}


@Override public QueueLevel getQueueLevel()
{
   return queue_level;
}


@Override public void setQueueLevel(QueueLevel lvl)
{
   if (lvl.ordinal() < queue_level.ordinal()) queue_level = lvl;
}



/********************************************************************************/
/*										*/
/*	Handle backwards flow data						*/
/*										*/
/********************************************************************************/

@Override public void backFlowParameter(IfaceValue ref,IfaceType settype)
{
   if (ref == null || ref.getRefSlot() < 0) return;
   int argno = -1;
   int slot = ref.getRefSlot();

   if (!for_method.isStatic() && slot == 0) {
      argno = 0;
    }
   else {
      int idx = 0;
      if (!for_method.isStatic()) idx = 1;
      for (int i = 0; i < for_method.getNumArgs(); ++i) {
	 if (slot == idx) {
	    argno = i;
	    break;
	  }
	 IfaceType atyp = for_method.getArgType(i);
	 if (atyp == null) return;
	 if (atyp.isCategory2()) ++idx;
	 ++idx;
       }
    }
   if (argno < 0) return;

   IfaceType typ0 = implied_arg_types.get(argno);
   IfaceType typ1 = typ0.getAnnotatedType(settype);
   if (typ0 == typ1) return;

   FaitLog.logD("Call Backwards " + argno + " " + typ1);
   implied_arg_types.set(argno,typ1);
}



@Override public void backFlowReturn(IfaceLocation loc,IfaceType settype)
{
   if (implied_return_type == null) return;	// handle constructors
   IfaceType typ0 = implied_return_type.getAnnotatedType(settype);
   if (typ0 == implied_return_type) return;
   implied_return_type = typ0;
   if (FaitLog.isTracing())
      FaitLog.logD("Return Backwards type change: " + typ0);

   for (IfaceCall ic : getAllMethodsCalled(loc.getProgramPoint())) {
      FaitLog.logD("Return Backwards " + ic + " " + typ0);
      // TODO: might want to requeue the call here knowing the type changed
    }
}


/********************************************************************************/
/*										*/
/*	Handle finding alternate calls based on safety status			*/
/*										*/
/********************************************************************************/

@Override public CallBase getAlternateCall(IfaceSafetyStatus sts,IfaceProgramPoint pt)
{
   if (sts == null) return this;

   if (special_data != null && special_data.getDontScan() && safety_result == null)
      return this;

   IfaceSafetyStatus osts = start_state.getSafetyStatus();

   if (sts == osts) return this;

   if (alternate_call != null) return alternate_call.getAlternateCall(sts,pt);

   CallBase cb = new CallBase(fait_control,for_method,pt,sts);
   alternate_call = cb;

   return cb;
}



@Override public Collection<IfaceCall> getAlternateCalls()
{
   if (alternate_call == null) return Collections.singleton(this);

   List<IfaceCall> rslt = new ArrayList<IfaceCall>();
   for (CallBase cb = this; cb != null; cb = cb.alternate_call) {
      rslt.add(cb);
    }

   return rslt;
}

/********************************************************************************/
/*										*/
/*	Statistics methods							*/
/*										*/
/********************************************************************************/

@Override public void noteScan(int fwd,int bwd)
{
   ++num_scan;
   num_forward += fwd;
   num_backward += bwd;
}


@Override public void outputStatistics()
{
   FaitLog.logS(getLogName() + ", " + num_scan + ", " + num_forward + ", " +
	 num_backward + ", " + num_adds + ", " + num_result);

   if (alternate_call != null) {
      alternate_call.outputStatistics();
    }
}


@Override public void resetStatistics()
{
   num_scan = 0;
   num_forward = 0;
   num_backward = 0;

   if (alternate_call != null) {
      alternate_call.resetStatistics();
    }
}

@Override public FaitStatistics getStatistics()
{
   FaitStatistics rslt = new FaitStatistics(num_scan,num_forward,num_backward);
   if (alternate_call != null) {
      FaitStatistics alts = alternate_call.getStatistics();
      rslt.add(alts);
    }

   return rslt;
}








}	// end of class CallBase




/* end of CallBase.java */



