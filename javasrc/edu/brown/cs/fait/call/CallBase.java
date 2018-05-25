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
private boolean 	is_clone;
private boolean 	is_arg0;
private boolean 	is_proto;
private boolean 	can_exit;
private int		num_adds;
private int		num_result;
private QueueLevel	queue_level;

private int             num_forward;
private int             num_backward;
private int             num_scan;

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

CallBase(IfaceControl fc,IfaceMethod fm,IfaceProgramPoint pt)
{
   this(fc);

   for_method = fm;
   special_data = fc.getCallSpecial(pt,fm);

   if (fm.getReturnType() != null && !fm.getReturnType().isVoidType()) {
      result_set = fc.findAnyValue(fm.getReturnType());
    }

   start_state = fc.createState(fm.getLocalSize());

   int idx = 0;
   if (!fm.isStatic()) {
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
      start_state.setLocal(idx++,fv);
      if (atyp.isCategory2()) ++idx;
    }

   if (fm.getName().equals("clone") && fm.getDeclaringClass().isJavaLangObject()) {
      is_clone = true;
      ++num_result;
    }
   else if (special_data != null) {
      is_arg0 = special_data.returnsArg0();
      can_exit = special_data.getExits();
      // this null needs to be a FaitLocaiton with fm as the method
      IfaceValue rv = special_data.getReturnValue(null,for_method);
      addResult(rv);
      if (rv != null) {
	 IfaceValue ev = null;
	 for (IfaceType edt : fm.getExceptionTypes()) {
	    IfaceValue ecv = fc.findNativeValue(edt);
	    ecv = ecv.forceNonNull();
	    ev = ecv.mergeValue(ev);
	  }
	 exception_set = ev;
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
   
   loadClasses();
   
   num_scan = 0;
   num_forward = 0;
   num_backward = 0;
}


private CallBase(IfaceControl fc)
{
   fait_control = fc;
   for_method = null;
   special_data = null;

   result_set = null;
   exception_set = null;

   start_state = null;

   num_adds = 0;
   num_result = 0;

   is_clone = false;
   is_arg0 = false;
   is_proto = false;
   can_exit = false;

   queue_level = QueueLevel.NORMAL;

   array_map = Collections.synchronizedMap(new IdentityHashMap<>(4));
   entity_map = Collections.synchronizedMap(new IdentityHashMap<>(4));
   userentity_map = Collections.synchronizedMap(new IdentityHashMap<>(4));
   method_map = new IdentityHashMap<>();
   call_set = new HashSet<>();
   error_set = new IdentityHashMap<>();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceMethod getMethod()	{ return for_method; }
@Override public IfaceControl getControl()	{ return fait_control; }

@Override public IfaceType getMethodClass()
{
   return for_method.getDeclaringClass();
}



@Override public IfaceState getStartState()	{ return start_state; }

@Override public IfaceValue getResultValue()	{ return result_set; }
@Override public IfaceValue getExceptionValue() { return exception_set; }

@Override public boolean isClone()		{ return is_clone; }
@Override public boolean isReturnArg0() 	{ return is_arg0; }

@Override public boolean getCanExit()		{ return can_exit; }
@Override public void setCanExit()		{ can_exit = true; }

@Override public boolean isPrototype()		{ return is_proto; }
@Override public void setPrototype()		{ is_proto = true; }


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

@Override public IfaceProgramPoint getStartPoint()
{
   return for_method.getStart();
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

@Override public boolean addCall(List<IfaceValue> args)
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

   if (chng && special_data != null && special_data.getDontScan()) chng = false;

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
   if (num_result > 0 || is_arg0) return true;
   return false;
}


@Override synchronized public boolean addResult(IfaceValue v)
{
   if (v == null) {				// handle void routines
      if (num_result++ == 0) return true;
      return false;
    }

   if (result_set == null || num_result == 0) {
      ++num_result;
      result_set = v;
    }
   else {
      ++num_result;
      if (result_set == v) return false;
      IfaceValue nv = result_set.mergeValue(v);
      if (nv == result_set) return false;
      result_set = nv;
    }

   if (is_arg0) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	Call replacement methods						*/
/*										*/
/********************************************************************************/

@Override public Collection<IfaceMethod> replaceWith(IfaceProgramPoint pt,List<IfaceValue> args)
{
   if (special_data == null) return Collections.singletonList(for_method);

   String nm = special_data.getReplaceName();

   if (special_data.isConstructor()) {
      IfaceValue fv = special_data.getReturnValue(pt,for_method);
      List<IfaceValue> nargs = special_data.getCallbackArgs(args,fv);
      String rtyp = fv.getDataType().getName();
      nm = rtyp + ".<init>";
      args.clear();
      args.addAll(nargs);
    }

   if (nm == null)
      return Collections.singletonList(for_method);

   IfaceMethod nfm = null;
   Collection<IfaceMethod> rslt = new ArrayList<>();

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
      nm = tok.nextToken();
      int idx = nm.lastIndexOf(".");
      if (idx > 0) {
	 String cls = nm.substring(0,idx);
	 String mnm = nm.substring(idx+1);
	 nfm = fait_control.findMethod(cls,mnm,desc);
	 if (nfm != null && nfm.isStatic()) nfm = null;
	 if (nfm == null) {
	    nfm = fait_control.findMethod(cls,mnm,statdesc);
	    if (nfm != null && !nfm.isStatic()) nfm = null;
	  }
       }
      else {
	 IfaceValue v0 = args.get(0);
	 IfaceType dt = v0.getDataType();
	 while (dt != null) {
	    nfm = fait_control.findMethod(dt.getName(),nm,desc);
	    if (nfm != null) break;
	    dt = dt.getSuperType();
	  }
       }
      if (nfm != null && !nfm.isNative() && !nfm.isAbstract())
	 rslt.add(nfm);
    }

   if (rslt.isEmpty()) rslt.add(for_method);

   return rslt;
}



@Override public IfaceValue fixReplaceArgs(IfaceMethod fm,LinkedList<IfaceValue> args)
{
   IfaceValue rslt = null;

   fixArgs(fm,args);

   return rslt;
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
   Collection<IfaceCall> rslt = new ArrayList<IfaceCall>();
   synchronized (method_map) {
      Map<IfaceMethod,IfaceCall> mm = method_map.get(ins);
      if (mm != null) rslt.addAll(mm.values());
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
   FaitLog.logD("Remove for update: " +  getMethod().getFullName());

   // first remove all entities created in this call
   for (IfaceEntity ie : entity_map.values()) {
      upd.addEntityToRemove(ie);
    }
   // TODO: handle user entities here as well

   // next find all items called from here and remove this as a caller
   // if we were the only caller, then remove that call as well
   for (Map<IfaceMethod,IfaceCall> mm : method_map.values()) {
      for (IfaceCall ic : mm.values()) {
	 CallBase cb = (CallBase) ic;
	 if (cb.removeCaller(this)) upd.removeCall(cb);
       }
    }

   // finally remove this call from any call site for it and requeue
   // that call to be evaluated.  Ignore if the call site will also
   // be eliminated
   for (IfaceLocation loc : call_set) {
      if (upd.shouldUpdate(loc.getCall())) continue;
      upd.addToWorkQueue(loc.getCall(),loc.getProgramPoint());
      CallBase cb = (CallBase) loc.getCall();
      cb.removeMethodCall(loc,this);
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
/*                                                                              */
/*      Handle backwards flow data                                              */
/*                                                                              */
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
   
   FaitLog.logD("Call Backwards " + argno + " " + settype);
}



@Override public void backFlowReturn(IfaceLocation loc,IfaceType settype)
{
   for (IfaceCall ic : getAllMethodsCalled(loc.getProgramPoint())) {
      FaitLog.logD("Return Backwards " + ic + " " + settype);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Statistics methods                                                      */
/*                                                                              */
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
   
   num_scan = 0;
   num_forward = 0;
   num_backward = 0;
}



}	// end of class CallBase




/* end of CallBase.java */



