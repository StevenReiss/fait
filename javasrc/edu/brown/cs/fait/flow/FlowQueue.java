/********************************************************************************/
/*										*/
/*		FlowQueue.java							*/
/*										*/
/*	Manage the queue of locations to execute				*/
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
import edu.brown.cs.ivy.file.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


class FlowQueue implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl				fait_control;
private FlowField				field_control;
private FlowArray				array_control;
private FlowCall				call_control;

private Map<IfaceCall,FlowQueueInstance>	call_map;
private SegmentedQueue				call_queue;

private Set<IfaceBaseType>			class_setup;

private Set<IfaceBaseType>	                staticinit_set;
private Set<IfaceBaseType>			staticinit_ran;
private Set<IfaceBaseType>			staticinit_started;
private Map<IfaceBaseType,Set<IfaceCall>>	staticinit_queue;
private Map<IfaceBaseType,Set<FlowLocation>>    staticinit_redos;
private List<IfaceCall> 			static_inits;


static final String [] preset_classes = new String [] {
   "java.lang.Object",
   "java.lang.String",
   "java.lang.Thread",
   "java.lang.Class",
   "java.lang.ClassLoader",
   "java.lang.Boolean",
   "java.lang.Integer",
   "java.lang.Long",
   "java.lang.Short",
   "java.lang.Double",
   "java.lang.Float",
   "java.lang.Byte",
   "java.lang.ThreadGroup",
   "java.lang.SecurityManager",
   "java.lang.StringBuilder",
   "java.lang.StringBuffer",
   "java.lang.System",
   "java.security.AccessControlContext",
   "java.security.ProtectionDomain",
   "java.io.PrintStream",
   "java.io.FilterOutputStream",
   "java.io.OutputStream",
   "java.io.BufferedWriter",
   "java.io.FilterWriter",
   "java.io.Writer",
   "java.io.FileInputStream",
   "java.io.InputStream",
   "java.io.UnixFileSystem",
   "java.util.Properties",
   "java.util.Hashtable",
   "sun.nio.cs.StreamEncoder",
   "sun.security.provider.PolicyFile",
   "sun.awt.X11GraphicsEnvironment",
   "sun.java2d.SunGraphicsEnvironment"
};


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowQueue(IfaceControl fc)
{
   fait_control = fc;

   call_map = new LinkedHashMap<>();
   call_queue = new SegmentedQueue();

   class_setup = new HashSet<>();
   for (String s : preset_classes) {
      IfaceType dt = fait_control.findDataType(s);
      if (dt != null) class_setup.add(dt.getJavaType());
    } 

   staticinit_set = new ConcurrentHashSet<>();
   staticinit_set.add(fait_control.findDataType("java.lang.System").getJavaType());
   staticinit_set.add(fait_control.findDataType("java.lang.Class").getJavaType());
   staticinit_ran = new ConcurrentHashSet<>(staticinit_set);
   staticinit_started = new ConcurrentHashSet<>(staticinit_set);
   static_inits = new ArrayList<>();
   staticinit_queue = new ConcurrentHashMap<>();
   staticinit_redos = new ConcurrentHashMap<>();

   field_control = new FlowField(fait_control,this);
   array_control = new FlowArray(fait_control,this);
   call_control = new FlowCall(fait_control,this);
}



/********************************************************************************/
/*										*/
/*	Methods to add to the queue						*/
/*										*/
/********************************************************************************/

void queueMethodStart(IfaceCall c,IfaceCall from)
{
   queueMethod(c,from);
}



void queueMethodCall(IfaceCall c,IfaceState st,IfaceCall from)
{
   IfaceMethod m = c.getMethod();
   if (!m.isStatic() && !m.isConstructor() && !m.isPrivate()) {
      IfaceType dt = m.getDeclaringClass();
      IfaceBaseType bt = dt.getJavaType();
      if (fait_control.isProjectClass(dt)) initialize(dt);
      else {
	 synchronized (staticinit_set) {
	    if (!staticinit_set.contains(bt)) return;
	  }
       }
    }

   queueMethod(c,from);
}







void queueMethodChange(IfaceCall c,IfaceProgramPoint ins)
{
   synchronized (call_map) {
      if (call_map.get(c) == null) return;
    }

   queueMethod(c,ins,null);
}


void queueMethodChange(FlowLocation loc)
{
   IfaceCall c = loc.getCall();

   queueMethod(c,loc.getProgramPoint(),null);
}



private void queueMethod(IfaceCall c,IfaceCall from)
{
   IfaceProgramPoint spt = c.getStartPoint();
   if (spt != null) queueMethod(c,spt,from);
}


private void queueMethod(IfaceCall c,IfaceProgramPoint ins,IfaceCall from)
{
   if (c == null) return;

   QueueLevel ql = QueueLevel.NORMAL;
   if (from != null) ql = from.getQueueLevel();
   IfaceMethod im = c.getMethod();
   if (im.isStaticInitializer()) ql = QueueLevel.STATIC_INIT;
   else if (im.isConstructor() && ql != QueueLevel.STATIC_INIT) ql = QueueLevel.INIT;
   c.setQueueLevel(ql);

   initialize(c.getMethodClass());
   c.loadClasses();

   boolean chng = call_queue.addCall(c,ins);

   if (FaitLog.isTracing()) {
      if (chng) {
	 FaitLog.logD1("Queue method " + c.getLogName() + " @ " + ins + " " + c.getQueueLevel());
       }
      else {
	 FaitLog.logD1("Requeue Method " + c.getLogName() + " @ "  + " " + c.getQueueLevel());
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Back Propagation queueing                                               */
/*                                                                              */
/********************************************************************************/

void queueBackPropagation(IfaceCall c,IfaceProgramPoint ins,IfaceValue ref,IfaceType t)
{
   
}




/********************************************************************************/
/*										*/
/*	Methods to setup a queue to process					*/
/*										*/
/********************************************************************************/

FlowQueueInstance setupNextFlowQueue()
{
   FlowQueueInstance fqi = null;
   boolean newfqi = false;

   Map.Entry<IfaceCall,Set<IfaceProgramPoint>> next = call_queue.getNextCall();
   if (next == null) return null;
   IfaceCall cm = next.getKey();
   Set<IfaceProgramPoint> inset = next.getValue();
   
   synchronized (call_map) {
      fqi = call_map.get(cm);
      if (fqi == null) {
	 fqi = FlowQueueInstance.createInstance(this,cm,QueueLevel.NORMAL);
	 call_map.put(cm,fqi);
	 newfqi = true;
       }
    }

   FaitLog.logD("");
   FaitLog.logD("START WORK ON " + cm.getLogName());

   if (newfqi) {
      IfaceState st0 = cm.getStartState();
      fqi.mergeState(st0);
      FaitLog.logD1("Safety start: " + st0.getSafetyStatus());
    }
   else if (inset != null) {
      IfaceProgramPoint i0 = cm.getMethod().getStart();
      FaitLog.logD1("Add instruction " + i0);
      for (IfaceProgramPoint loc : inset) {
         FlowLocation floc = new FlowLocation(this,cm,loc);
	 if (loc.equals(i0)) {
            IfaceState nst = fqi.mergeState(cm.getStartState(),floc);
            if (nst != null) FaitLog.logD1("Safety start: " + nst.getSafetyStatus());
          }
	 fqi.lookAt(loc);
       }
    }
   
   return fqi;
}




void doneWithFlowQueue(FlowQueueInstance fqi)
{
   call_queue.removeActiveCall(fqi.getCall());
}




/********************************************************************************/
/*										*/
/*	Method to handle static and class initialization			*/
/*										*/
/********************************************************************************/



void initialize(IfaceType dt)
{
   Collection<IfaceCall> inits = new ArrayList<IfaceCall>();
   IfaceBaseType bt = dt.getJavaType();

   if (staticinit_set.add(bt)) {
      if (FaitLog.isTracing()) {
         FaitLog.logD1("Initialize " + bt.getName());
       }
      
      if (dt.getSuperType() != null) initialize(dt.getSuperType());
      
      int ctr = 0;
      Collection<IfaceMethod> sinit = fait_control.findAllMethods(dt,"<clinit>");
      if (sinit != null) {
         for (IfaceMethod fm : sinit) {
            IfaceCall c = fait_control.findCall(null,fm,null,null,InlineType.NONE);
            static_inits.add(c);
            if (!c.hasResult()) {
               ++ctr;
               inits.add(c);
             }
          }
       }
      if (ctr == 0) {
         finishedInitialization(bt);
       }
    }
   synchronized(staticinit_set) {
      if (!staticinit_set.contains(bt)) {
	 staticinit_set.add(bt);
	 if (FaitLog.isTracing()) {
	    FaitLog.logD1("Initialize " + bt.getName());
	  }

	 if (dt.getSuperType() != null) initialize(dt.getSuperType());

	 int ctr = 0;
	 Collection<IfaceMethod> sinit = fait_control.findAllMethods(dt,"<clinit>");
	 if (sinit != null) {
	    for (IfaceMethod fm : sinit) {
	       IfaceCall c = fait_control.findCall(null,fm,null,null,InlineType.NONE);
	       static_inits.add(c);
	       if (!c.hasResult()) {
                  ++ctr;
                  inits.add(c);
                }
	     }
	  }
	 if (ctr == 0) {
	    finishedInitialization(bt);
	  }
      }
   }

   for (IfaceCall c : inits) {
      queueMethod(c,null);
    }
}



boolean canBeUsed(IfaceType dt)
{
   IfaceBaseType bt = dt.getJavaType();
   if (class_setup.contains(bt)) return true;
   if (staticinit_set.contains(bt)) return true;

   return false;
}


boolean checkInitialized(IfaceCall cm,IfaceProgramPoint ins)
{
   IfaceType bc = cm.getMethodClass();
   IfaceBaseType bt = bc.getJavaType();

   if (ins.isMethodStart() && !getInitializerDone(bc)) {
      if (cm.getMethod().isStaticInitializer()) {
         synchronized (staticinit_set) {
            staticinit_started.add(bt);
            requeueForInit(staticinit_queue.remove(bt));
          }
       }
      else {
         if (!staticinit_started.contains(bt)) {
            if (FaitLog.isTracing())
               FaitLog.logD1("Class not initialized requeue: " + bc);
            Set<IfaceCall> s0 = new HashSet<>();
            synchronized (staticinit_set) {
               Set<IfaceCall> s = staticinit_queue.putIfAbsent(bt,s0);
               if (s == null) s = s0;
               s.add(cm);
             }
            return false;
	  }
       }
    }
   
   return true;
}


void requeueIfInitialized(FlowLocation loc,IfaceType dt)
{
   IfaceBaseType bt = dt.getJavaType();
   Set<FlowLocation> s0 = new HashSet<>();
   synchronized (staticinit_set) {
      Set<FlowLocation> s = staticinit_redos.putIfAbsent(bt,s0);
      if (s == null) s = s0;
      s.add(loc);
    }
}



void handleReturnSetup(IfaceMethod fm)
{
   if (fm.isStaticInitializer()) {
      IfaceType dt = fm.getDeclaringClass();
      IfaceBaseType bt = dt.getJavaType();
      finishedInitialization(bt);
    }
}



private void finishedInitialization(IfaceBaseType bt)
{
   staticinit_ran.add(bt);
   staticinit_started.add(bt);
   Set<IfaceCall> cs = staticinit_queue.remove(bt);
   if (cs != null) {
      for (IfaceCall nc : cs) {
         if (FaitLog.isTracing())
            FaitLog.logD("Requeue for initialization: " + nc);
         queueMethodStart(nc,null);
       }
    }
   Set<FlowLocation> fs = staticinit_redos.remove(bt);
   if (fs != null) {
      for (FlowLocation loc : fs) {
         if (FaitLog.isTracing()) 
            FaitLog.logD("Requeue for virtual initialization: " + loc);
         queueMethodChange(loc);
       } 
    }
}



private void requeueForInit(Collection<IfaceCall> s)
{
   if (s == null) return;
   for (IfaceCall c : s) queueMethod(c,null);
}


private boolean getInitializerDone(IfaceType dt)
{
   IfaceBaseType bt = dt.getJavaType();

   initialize(dt);
   if (!staticinit_ran.contains(bt)) return false;
   
   return true;
}




/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

void handleUpdate(IfaceUpdater upd)
{
   synchronized (call_map) {
      for (FlowQueueInstance qi : call_map.values()) {
	 qi.handleUpdate(upd);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Array access								*/
/*										*/
/********************************************************************************/

IfaceValue handleNewArraySet(FlowLocation loc,IfaceType acls,int ndim,IfaceValue sz)
{
   return array_control.handleNewArraySet(loc,acls,ndim,sz);
}


IfaceValue handleArrayAccess(FlowLocation loc,IfaceValue arr,IfaceValue idx)
{
   return array_control.handleArrayAccess(loc,arr,idx);
}


IfaceValue handleArrayLength(FlowLocation loc,IfaceValue arr)
{
   return array_control.handleArrayLength(loc,arr);
}



void handleArraySet(FlowLocation loc,IfaceValue arr,IfaceValue val,IfaceValue idx)
{
   array_control.handleArraySet(loc,arr,val,idx);
}



void handleArrayCopy(List<IfaceValue> args,FlowLocation loc)
{
   array_control.handleArrayCopy(args,loc);
}

void handleArrayChange(IfaceValue arr)
{
   for (IfaceEntity ce : arr.getEntities()) {
      if (FaitLog.isTracing()) {
         FaitLog.logD1("Handle array change for " + ce + " (" + ce.hashCode() + ")" + " " +
           ce.getFieldValue("length"));
       }
      array_control.noteArrayChange(ce);
    }
}


void noteArrayChange(IfaceEntity arr)
{
   array_control.noteArrayChange(arr);
}




/********************************************************************************/
/*										*/
/*	Handle field access							*/
/*										*/
/********************************************************************************/

void handleFieldSet(FlowLocation loc,IfaceState st,boolean thisref,
      IfaceValue v0,IfaceValue base)
{
   IfaceField fld = loc.getProgramPoint().getReferencedField();
   
   if (fld == null) return;

   field_control.handleFieldSet(loc,fld,st,thisref,v0,base);
}


void handleFieldSet(IfaceField fld,FlowLocation loc,IfaceState st,
      boolean thisref,IfaceValue v0,IfaceValue base)
{
   field_control.handleFieldSet(loc,fld,st,thisref,v0,base);
}


IfaceValue handleFieldGet(FlowLocation loc,IfaceState st,boolean thisref,IfaceValue base)
{
   IfaceField jf = loc.getProgramPoint().getReferencedField();
   if (jf == null) return null;
   return field_control.handleFieldGet(loc,jf,st,thisref,base);
}


IfaceValue handleFieldGet(IfaceField jf,FlowLocation loc,IfaceState st,
      boolean thisref,IfaceValue base)
{
   return field_control.handleFieldGet(loc,jf,st,thisref,base);
}



/********************************************************************************/
/*										*/
/*	Call management methods 						*/
/*										*/
/********************************************************************************/

CallReturn handleCall(FlowLocation loc,IfaceState st0,FlowQueueInstance wq)
{
   return call_control.handleCall(loc,st0,wq);
}



void handleCallback(IfaceMethod fm,List<IfaceValue> args,String cbid)
{
   call_control.handleCallback(fm,args,cbid);
}



void handleReturn(IfaceCall c0,IfaceValue v0,IfaceState st,IfaceLocation loc)
{
   IfaceMethod bm = c0.getMethod();
   if (bm.isConstructor()) {
      IfaceType cnsttyp = bm.getDeclaringClass();
      for (Map.Entry<String,IfaceType> ent : cnsttyp.getJavaType().getFieldData().entrySet()) {
         field_control.initializeField(ent.getKey(),ent.getValue());
       }
    }
   
   call_control.handleReturn(c0,v0,st.getSafetyStatus(),loc);
}


void handleThrow(FlowQueueInstance wq,IfaceLocation loc,IfaceValue v0,IfaceState st0)
{
   // call_control.handleThrow(wq,loc,v0,st0);
   wq.handleThrow(loc,v0,st0);
}


void handleException(IfaceValue v0,IfaceCall cm)
{
   call_control.handleException(v0,cm);
}



/********************************************************************************/
/*										*/
/*	Casting methods 							*/
/*										*/
/********************************************************************************/

IfaceValue castValue(IfaceType rtyp,IfaceValue v0,IfaceLocation loc)
{
   if (v0 == null) return null;
   
   IfaceValue v1 = v0;
   IfaceType t0 = v0.getDataType();

   if (t0.equals(rtyp)) return v0;

   if (rtyp.isPrimitiveType()) {
      if (t0.isPrimitiveType()) {
	 if (rtyp.isNumericType() && t0.isNumericType()) {
	    if (v0.getIndexValue() != null) {
	       long val = v0.getIndexValue();
	       v1 = fait_control.findConstantValue(rtyp,val);
	     }
	    else v1 = fait_control.findAnyValue(rtyp);
	  }
	 else if (rtyp.isBooleanType() && t0.isNumericType()) {
	    if (v0.getIndexValue() != null) {
	       if (v0.getIndexValue() == 0) v1 = fait_control.findConstantValue(false);
	       else v1 = fait_control.findConstantValue(true);
	     }
	    else v1 = fait_control.findAnyValue(rtyp);
	  }
       }
      else {
	 if (t0.getAssociatedType() != null || v0.mustBeNull() || t0.isJavaLangObject()) {
	    // unbox v0 to get rtyp
	    v1 = fait_control.findAnyValue(rtyp);
	  }
         else {
          }
       }
      return v1;
    }
   else if (t0.isPrimitiveType()) {
      IfaceType tr = rtyp.getAssociatedType();
      IfaceType t1 = t0.getBaseType();
      if (tr != null && t0.isNumericType()) t1 = rtyp;
      if (t1 == null) return v0;
      t0 = t1;
      v0 = fait_control.findNativeValue(t0);
      if (t0.equals(rtyp)) return v0;
    }

   if (t0.isCompatibleWith(rtyp)) return v0;

   v1 = v0.restrictByType(rtyp);
   // FlowScanner.checkAssignment(v0,rtyp,loc);

   return v1;
}



/********************************************************************************/
/*                                                                              */
/*      Statistics methods                                                      */
/*                                                                              */
/********************************************************************************/

void printStatistics()
{
   for (IfaceCall fc : call_map.keySet()) {
      fc.outputStatistics();
    }
}



/********************************************************************************/
/*										*/
/*	Segmented Queue Implementation						*/
/*										*/
/********************************************************************************/

private static class SegmentedQueue {

   private Map<IfaceCall,Set<IfaceProgramPoint>> init_queue;
   private Map<IfaceCall,Set<IfaceProgramPoint>> constructor_queue;
   private Map<IfaceCall,Set<IfaceProgramPoint>> normal_queue;
   private Set<IfaceCall> active_calls;

   SegmentedQueue() {
      init_queue = new LinkedHashMap<>();
      constructor_queue = new LinkedHashMap<>();
      normal_queue = new LinkedHashMap<>();
      active_calls = new HashSet<>();
    }

   synchronized void removeActiveCall(IfaceCall call) {
      active_calls.remove(call);
      notifyAll();
    }

   synchronized boolean addCall(IfaceCall c,IfaceProgramPoint pt) {
      QueueLevel ql = c.getQueueLevel();
      boolean chng = false;
      Set<IfaceProgramPoint> s = null;
      switch (ql) {
         case STATIC_INIT :
            s = init_queue.get(c);
            if (s == null) {
               s = constructor_queue.remove(c);
               if (s == null) s = normal_queue.remove(c);
               if (s == null) s = new HashSet<>();
               init_queue.put(c,s);
               chng = true;
             }
            break;
         case INIT :
            s = constructor_queue.get(c);
            if (s == null) {
               s = normal_queue.remove(c);
               if (s == null) s = new HashSet<>();
               constructor_queue.put(c,s);
               chng = true;
             }
            break;
         case NORMAL :
            s = normal_queue.get(c);
            if (s == null) {
               s = new HashSet<>();
               normal_queue.put(c,s);
               chng = true;
             }
            break;
       }
   
      if (s != null && pt != null) s.add(pt);
   
      if (chng) notifyAll();
   
      return chng;
    }

   synchronized Map.Entry<IfaceCall,Set<IfaceProgramPoint>> getNextCall() {
      while (!allEmpty()) {
         Map.Entry<IfaceCall,Set<IfaceProgramPoint>> rslt = null;
         if (init_queue.size() > 0) {
            rslt = getNext(init_queue);
          }
         else if (constructor_queue.size() > 0) {
            rslt = getNext(constructor_queue);
          }
         else {
            rslt = getNext(normal_queue);
          }
         if (rslt != null) return rslt;
   
         try {
            wait(10000);
          }
         catch (InterruptedException e) { }
       }
      return null;
    }

   private synchronized boolean allEmpty() {
      return init_queue.isEmpty() && constructor_queue.isEmpty() && normal_queue.isEmpty() &&
      active_calls.isEmpty();
    }

   private Map.Entry<IfaceCall,Set<IfaceProgramPoint>> getNext(Map<IfaceCall,Set<IfaceProgramPoint>> q) {
      Iterator<Map.Entry<IfaceCall,Set<IfaceProgramPoint>>> it;
      it = q.entrySet().iterator();
      while (it.hasNext()) {
         Map.Entry<IfaceCall,Set<IfaceProgramPoint>> ent = it.next();
         IfaceCall ic = ent.getKey();
         if (active_calls.contains(ic)) continue;
         it.remove();
         if (ic.getMethod().hasCode()) {
            active_calls.add(ic);
            return ent;
          }
       }
   
      return null;
    }

}







}	// end of class FlowQueue




/* end of FlowQueue.java */













































