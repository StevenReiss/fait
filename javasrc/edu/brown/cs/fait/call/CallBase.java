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

private FaitControl	fait_control;
private FaitMethod	for_method;
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

private Map<FaitInstruction,IfaceEntity>	array_map;
private Map<FaitInstruction,IfaceEntity>	entity_map;
private Map<FaitInstruction,IfaceEntity>	userentity_map;

private Map<FaitInstruction,Map<FaitMethod,IfaceCall>> method_map;
private Set<FaitLocation> call_set;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CallBase(FaitControl fc,FaitMethod fm,int ct)
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
      FaitDataType atyp = fm.getArgType(i);
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
      IfaceValue rv = special_data.getReturnValue(null);
      addResult(rv);
      if (rv != null) {
	 IfaceValue ev = null;
	 for (FaitDataType edt : fm.getExceptionTypes()) {
	    IfaceValue ecv = fc.findNativeValue(edt);
	    ecv = ecv.forceNonNull();
	    ev = ecv.mergeValue(ev);
	  }
	 exception_set = ev;
       }
    }

   if (fm.isNative()) {
      result_set = fc.findNativeValue(fm.getReturnType());
      ++num_result;
    }

   method_data = fc.createMethodData(this);

   array_map = Collections.synchronizedMap(new HashMap<FaitInstruction,IfaceEntity>(4));
   entity_map = Collections.synchronizedMap(new HashMap<FaitInstruction,IfaceEntity>(4));
   userentity_map = Collections.synchronizedMap(new HashMap<FaitInstruction,IfaceEntity>(4));
   method_map = new HashMap<FaitInstruction,Map<FaitMethod,IfaceCall>>();
   call_set = new HashSet<FaitLocation>();

}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public FaitMethod getMethod() 	{ return for_method; }
@Override public FaitDataType getMethodClass()	{ return for_method.getDeclaringClass(); }

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

@Override public FaitValue getAssociation(AssociationType typ,FaitInstruction ins)
{
   if (method_data == null) return null;

   return method_data.getAssociation(typ,ins);
}


@Override public void setAssociation(AssociationType typ,FaitInstruction ins,IfaceValue v)
{
   if (method_data != null) method_data.setAssociation(typ,ins,v);
}


@Override public IfaceEntity getArrayEntity(FaitInstruction ins)
{
   return array_map.get(ins);
}

@Override public void setArrayEntity(FaitInstruction ins,IfaceEntity e)
{
   array_map.put(ins,e);
}


@Override public IfaceEntity getBaseEntity(FaitInstruction ins)
{
   return entity_map.get(ins);
}

@Override public void setBaseEntity(FaitInstruction ins,IfaceEntity e)
{
   entity_map.put(ins,e);
}


@Override public FaitEntity.UserEntity getUserEntity(FaitInstruction ins)
{
   return (FaitEntity.UserEntity) userentity_map.get(ins);
}

@Override public void setUserEntity(FaitInstruction ins,FaitEntity.UserEntity e)
{
   userentity_map.put(ins,(IfaceEntity) e);
}



/********************************************************************************/
/*										*/
/*	Value access methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getThisValue()
{
   if (for_method.isStatic()) return null;

   synchronized (start_state) {
      return start_state.getLocal(0);
    }
}


@Override public Iterable<IfaceValue> getParameterValues()
{
   Collection<IfaceValue> rslt = new ArrayList<IfaceValue>();

   synchronized (start_state) {
      int idx = 0;
      if (!for_method.isStatic()) {
	 IfaceValue v0 = start_state.getLocal(idx++);
	 rslt.add(v0);
       }

      for (int i = 0; ; ++i) {
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

   synchronized (start_state) {
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

@Override public Collection<FaitMethod> replaceWith(List<IfaceValue> args)
{
   if (special_data == null) return Collections.singletonList(for_method);
   String nm = special_data.getReplaceName();
   if (nm == null) return Collections.singletonList(for_method);

   FaitMethod nfm = null;
   Collection<FaitMethod> rslt = new ArrayList<FaitMethod>();
   
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
         FaitDataType dt = v0.getDataType();
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



@Override public IfaceValue fixReplaceArgs(FaitMethod fm,LinkedList<IfaceValue> args)
{
   IfaceValue rslt = null;

   fixArgs(fm,args);

   return rslt;
}



private void fixArgs(FaitMethod fm,List<IfaceValue> args)
{
   int bct = 1;
   if (fm.isStatic()) bct = 0;
   int act = 0;

   for (int i = 0; ; ++i) {
      FaitDataType aty = fm.getArgType(i);
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

@Override public void addCallbacks(List<IfaceValue> args)
{ }


@Override public FaitMethod findCallbackMethod(FaitDataType cls,String mthd,int asz,boolean intf)
{
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Call tracking methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public void noteCallSite(FaitLocation loc)
{ 
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


@Override public void noteMethodCalled(FaitInstruction ins,FaitMethod m,IfaceCall called)
{
   synchronized (method_map) {
      Map<FaitMethod,IfaceCall> mm = method_map.get(ins);
      if (mm == null) {
         mm = new HashMap<FaitMethod,IfaceCall>(4);
         method_map.put(ins,mm);
       }
      mm.put(m,called);
    }
}


@Override public IfaceCall getMethodCalled(FaitInstruction ins,FaitMethod m)
{
   synchronized (method_map) {
      Map<FaitMethod,IfaceCall> mm = method_map.get(ins);
      if (mm == null) return null;
      return mm.get(m);
    }
}


@Override public Collection<IfaceCall> getAllMethodsCalled(FaitInstruction ins)
{
   Collection<IfaceCall> rslt = new ArrayList<IfaceCall>();
   synchronized (method_map) {
      Map<FaitMethod,IfaceCall> mm = method_map.get(ins);
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
	 FaitDataType dt;
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



}	// end of class CallBase




/* end of CallBase.java */

