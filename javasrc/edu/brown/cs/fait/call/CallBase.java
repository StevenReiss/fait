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
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcode.JcodeMethod;

import java.util.*;


class CallBase implements CallConstants, IfaceCall
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl	fait_control;
private JcodeMethod	for_method;
private int		inline_counter;
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
private FaitMethodData	method_data;

private Map<JcodeInstruction,IfaceEntity>	array_map;
private Map<JcodeInstruction,IfaceEntity>	entity_map;
private Map<JcodeInstruction,IfaceEntity>	userentity_map;

private Map<JcodeInstruction,Map<JcodeMethod,IfaceCall>> method_map;
private Set<FaitLocation> call_set;

private Set<JcodeInstruction>	dead_set;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CallBase(FaitControl fc,JcodeMethod fm,int ct)
{
   fait_control = fc;
   for_method = fm;
   inline_counter = ct;
   special_data = fc.getCallSpecial(fm);

   if (fm.getReturnType().isVoid()) result_set = null;
   else result_set = fc.findAnyValue(fm.getReturnType());
   exception_set = null;

   start_state = fc.createState(fm.getLocalSize());

   int idx = 0;
   if (!fm.isStatic()) {
      IfaceValue fv = fc.findAnyValue(fm.getDeclaringClass());
      fv = fv.forceNonNull();
      start_state.setLocal(idx++,fv);
    }
   for (int i = 0; ; ++i) {
      JcodeDataType atyp = fm.getArgType(i);
      if (atyp == null) break;
      IfaceValue fv = fc.findAnyValue(atyp);
      if (i == 0 && fm.isStatic() && fm.getName().equals("main"))
	 fv = fc.findMainArgsValue();
      start_state.setLocal(idx++,fv);
      if (atyp.isCategory2()) ++idx;
    }

   num_adds = 0;
   num_result = 0;

   is_clone = false;
   is_arg0 = false;
   is_proto = false;
   can_exit = false;

   if (fm.getName().equals("clone") && fm.getDeclaringClass().isJavaLangObject()) {
      is_clone = true;
      ++num_result;
    }
   else if (special_data != null) {
      is_arg0 = special_data.returnsArg0();
      can_exit = special_data.getExits();
      // this null needs to be a FaitLocaiton with fm as the method
      IfaceValue rv = special_data.getReturnValue(for_method);
      addResult(rv);
      if (rv != null) {
	 IfaceValue ev = null;
	 for (JcodeDataType edt : fm.getExceptionTypes()) {
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
   else if (fm.getNumInstructions() == 0 && !fait_control.isInProject(fm)) {
      if (result_set == null || !result_set.isGoodEntitySet())
	 result_set = fc.findMutableValue(fm.getReturnType());
      ++num_result;
    }

   method_data = fc.createMethodData(this);

   array_map = Collections.synchronizedMap(new HashMap<JcodeInstruction,IfaceEntity>(4));
   entity_map = Collections.synchronizedMap(new HashMap<JcodeInstruction,IfaceEntity>(4));
   userentity_map = Collections.synchronizedMap(new HashMap<JcodeInstruction,IfaceEntity>(4));
   method_map = new HashMap<JcodeInstruction,Map<JcodeMethod,IfaceCall>>();
   call_set = new HashSet<FaitLocation>();
   dead_set = new HashSet<JcodeInstruction>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public JcodeMethod getMethod() 	{ return for_method; }
@Override public JcodeDataType getMethodClass()	{ return for_method.getDeclaringClass(); }

@Override public int getInstanceNumber()	{ return inline_counter; }

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




/********************************************************************************/
/*										*/
/*	Methods to handle saved maps						*/
/*										*/
/********************************************************************************/

@Override public FaitValue getAssociation(AssociationType typ,JcodeInstruction ins)
{
   if (method_data == null) return null;

   return method_data.getAssociation(typ,ins);
}


@Override public void setAssociation(AssociationType typ,JcodeInstruction ins,IfaceValue v)
{
   if (method_data != null) method_data.setAssociation(typ,ins,v);
}


@Override public IfaceEntity getArrayEntity(JcodeInstruction ins)
{
   return array_map.get(ins);
}

@Override public void setArrayEntity(JcodeInstruction ins,IfaceEntity e)
{
   array_map.put(ins,e);
}


@Override public IfaceEntity getBaseEntity(JcodeInstruction ins)
{
   return entity_map.get(ins);
}

@Override public void setBaseEntity(JcodeInstruction ins,IfaceEntity e)
{
   entity_map.put(ins,e);
}


@Override public FaitEntity.UserEntity getUserEntity(JcodeInstruction ins)
{
   return (FaitEntity.UserEntity) userentity_map.get(ins);
}

@Override public void setUserEntity(JcodeInstruction ins,FaitEntity.UserEntity e)
{
   userentity_map.put(ins,(IfaceEntity) e);
}


@Override public void addDeadInstruction(JcodeInstruction ins)
{
   synchronized (dead_set) {
      dead_set.add(ins);
    }
}



@Override public void removeDeadInstruction(JcodeInstruction ins)
{
   synchronized (dead_set) {
      dead_set.remove(ins);
    }
}


@Override public Collection<JcodeInstruction> getDeadInstructions()
{
   synchronized (dead_set) {
      return new ArrayList<JcodeInstruction>(dead_set);
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
      ++num_result;
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

@Override public Collection<JcodeMethod> replaceWith(List<IfaceValue> args)
{
   if (special_data == null) return Collections.singletonList(for_method);
   
   String nm = special_data.getReplaceName();
   
   if (special_data.isConstructor()) {
      IfaceValue fv = special_data.getReturnValue(for_method);
      List<Integer> vals = special_data.getCallbackArgs();
      List<IfaceValue> nargs = new ArrayList<IfaceValue>();
      nargs.add(fv);
      if (vals != null) {
         for (Integer i : vals) {
            IfaceValue av = null;
            int iv = i;
            if (iv == -1) {
               av = fait_control.findNullValue();
             }
            else {
               av = args.get(i);
             }
            if (av != null) nargs.add(av);
          }
       }
      String rtyp = fv.getDataType().getName();
      nm = rtyp + ".<init>";
      args.clear();
      args.addAll(nargs);
    }
   
   if (nm == null)
      return Collections.singletonList(for_method);
  
   JcodeMethod nfm = null;
   Collection<JcodeMethod> rslt = new ArrayList<JcodeMethod>();

   StringTokenizer tok = new StringTokenizer(nm);
   while (tok.hasMoreTokens()) {
      nm = tok.nextToken();
      int idx = nm.lastIndexOf(".");
      if (idx > 0) {
	 String cls = nm.substring(0,idx);
	 String mnm = nm.substring(idx+1);
	 nfm = fait_control.findMethod(cls,mnm,null);
       }
      else {
	 IfaceValue v0 = args.get(0);
	 JcodeDataType dt = v0.getDataType();
	 while (dt != null) {
	    nfm = fait_control.findMethod(dt.getName(),nm,null);
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



@Override public IfaceValue fixReplaceArgs(JcodeMethod fm,LinkedList<IfaceValue> args)
{
   IfaceValue rslt = null;

   fixArgs(fm,args);

   return rslt;
}



private void fixArgs(JcodeMethod fm,List<IfaceValue> args)
{
   int bct = 1;
   if (fm.isStatic()) bct = 0;
   int act = 0;

   for (int i = 0; ; ++i) {
      JcodeDataType aty = fm.getArgType(i);
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

@Override public void addCallbacks(FaitLocation loc,List<IfaceValue> args)
{
   if (special_data == null) return;
   Iterable<String> it = special_data.getCallbacks();
   if (it == null) return;
   
   IfaceLog.logD("Check callbacks " + args);
   
   List<Integer> argnos = special_data.getCallbackArgs();
   List<IfaceValue> nargs = new ArrayList<IfaceValue>();
   for (Integer iv0 : argnos) {
      int i0 = iv0;
      IfaceValue cv = args.get(i0);
      nargs.add(cv);
    }
   IfaceValue cv0 = nargs.get(0);
   JcodeDataType typ = cv0.getDataType();
   if (typ == null) return;
   
   for (String cbn : it) {
      JcodeMethod fm = findCallbackMethod(typ,cbn,nargs.size(),true);
      if (fm != null) {
         List<IfaceValue> rargs = new ArrayList<IfaceValue>(nargs);
         fixArgs(fm,rargs);
         IfaceLog.logD("Use callback " + fm + " " + rargs);
         fait_control.handleCallback(loc,fm,rargs,special_data.getCallbackId());
       }
      else IfaceLog.logD("No callback found for " + cbn + " in " + typ);
    }
}


@Override public JcodeMethod findCallbackMethod(JcodeDataType cls,String mthd,int asz,boolean intf)
{
   for (JcodeMethod fm : fait_control.findAllMethods(cls,mthd,null)) {
      if (fm.getName().equals(mthd)) {
         if (fm.isStatic() && fm.getNumArguments() >= asz) return fm;
         else if (fm.getNumArguments() + 1 >= asz) return fm;
       }
    }
   
   JcodeDataType fdt = cls.getSuperType();
   if (fdt != null) {
      JcodeMethod fm = findCallbackMethod(fdt,mthd,asz,intf);
      if (fm != null) return fm;
    }
   
   if (intf) {
      for (JcodeDataType sdt : cls.getInterfaces()) {
         JcodeMethod fm = findCallbackMethod(sdt,mthd,asz,true);
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

@Override public void noteCallSite(FaitLocation loc)
{
   if (loc == null) return;

   synchronized (call_set) {
      call_set.add(loc);
    }
}


@Override public Collection<FaitLocation> getCallSites()
{
   synchronized (call_set) {
      return new ArrayList<FaitLocation>(call_set);
    }
}


@Override public void noteMethodCalled(JcodeInstruction ins,JcodeMethod m,IfaceCall called)
{
   synchronized (method_map) {
      Map<JcodeMethod,IfaceCall> mm = method_map.get(ins);
      if (mm == null) {
	 mm = new HashMap<JcodeMethod,IfaceCall>(4);
	 method_map.put(ins,mm);
       }
      mm.put(m,called);
    }
}


@Override public IfaceCall getMethodCalled(JcodeInstruction ins,JcodeMethod m)
{
   synchronized (method_map) {
      Map<JcodeMethod,IfaceCall> mm = method_map.get(ins);
      if (mm == null) return null;
      return mm.get(m);
    }
}


@Override public Collection<IfaceCall> getAllMethodsCalled(JcodeInstruction ins)
{
   Collection<IfaceCall> rslt = new ArrayList<IfaceCall>();
   synchronized (method_map) {
      Map<JcodeMethod,IfaceCall> mm = method_map.get(ins);
      if (mm != null) rslt.addAll(mm.values());
    }
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Incremental update methods						*/
/*										*/
/********************************************************************************/

@Override public void clearForUpdate(IfaceUpdater upd)
{
   // this needs to be handled
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
	 JcodeDataType dt;
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
   JcodeMethod fm = getMethod();

   return fm.getDeclaringClass().getName() + "." + fm.getName() + fm.getDescription() + " " + hashCode();
}


}	// end of class CallBase




/* end of CallBase.java */



