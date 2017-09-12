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
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcode.JcodeMethod;

import java.util.*;


class FlowQueue implements FlowConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl				fait_control;
private FlowField				field_control;
private FlowConditional 			conditional_control;
private FlowArray				array_control;
private FlowCall				call_control;

private Set<IfaceCall>				active_calls;

private Map<IfaceCall,FlowQueueInstance>	call_map;
private Map<IfaceCall,Set<JcodeInstruction>>	call_queue;

private Set<JcodeDataType>			class_setup;
private Map<JcodeDataType,Collection<IfaceCall>> classsetup_map;

private Set<JcodeDataType>			staticinit_set;
private Set<JcodeDataType>			staticinit_ran;
private Set<JcodeDataType>			staticinit_started;
private Map<JcodeDataType,Set<IfaceCall>>	staticinit_queue;
private List<IfaceCall> 			static_inits;


static final String [] preset_classes = new String [] {
   "Ljava/lang/Object;",
   "Ljava/lang/String;",
   "Ljava/lang/Thread;",
   "Ljava/lang/Class;",
   "Ljava/lang/ClassLoader;",
   "Ljava/lang/Boolean;",
   "Ljava/lang/Integer;",
   "Ljava/lang/Long;",
   "Ljava/lang/Short;",
   "Ljava/lang/ThreadGroup;",
   "Ljava/lang/SecurityManager;",
   "Ljava/lang/StringBuilder;",
   "Ljava/lang/StringBuffer;",
   "Ljava/security/AccessControlContext;",
   "Ljava/security/ProtectionDomain;",
   "Ljava/io/PrintStream;",
   "Ljava/io/FilterOutputStream;",
   "Ljava/io/OutputStream;",
   "Ljava/io/BufferedWriter;",
   "Ljava/io/FilterWriter;",
   "Ljava/io/Writer;",
   "Ljava/io/FileInputStream;",
   "Ljava/io/InputStream;",
   "Ljava/io/UnixFileSystem;",
   "Ljava/util/Properties;",
   "Ljava/util/Hashtable;",
   "Lsun/nio/cs/StreamEncoder;",
   "Lsun/security/provider/PolicyFile;",
   "Lsun/awt/X11GraphicsEnvironment;",
   "Lsun/java2d/SunGraphicsEnvironment;"
};


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowQueue(FaitControl fc)
{
   fait_control = fc;

   active_calls = new HashSet<IfaceCall>();

   call_map = new LinkedHashMap<IfaceCall,FlowQueueInstance>();
   call_queue = new HashMap<IfaceCall,Set<JcodeInstruction>>();

   class_setup = new HashSet<JcodeDataType>();
   for (String s : preset_classes) {
      JcodeDataType dt = fait_control.findDataType(s);
      if (dt != null) class_setup.add(dt);
    }
   classsetup_map = new HashMap<JcodeDataType,Collection<IfaceCall>>();

   staticinit_set = new HashSet<JcodeDataType>();
   staticinit_set.add(fait_control.findDataType("Ljava/lang/System;"));
   staticinit_set.add(fait_control.findDataType("Ljava/lang/Class;"));
   staticinit_ran = new HashSet<JcodeDataType>(staticinit_set);
   staticinit_started = new HashSet<JcodeDataType>(staticinit_set);
   static_inits = new ArrayList<IfaceCall>();
   staticinit_queue = new HashMap<JcodeDataType,Set<IfaceCall>>();

   field_control = new FlowField(fait_control,this);
   conditional_control = new FlowConditional(fait_control);
   array_control = new FlowArray(fait_control,this);
   call_control = new FlowCall(fait_control,this);
}



/********************************************************************************/
/*										*/
/*	Methods to add to the queue						*/
/*										*/
/********************************************************************************/

void queueMethodStart(IfaceCall c)
{
   queueMethod(c);
}



void queueMethodCall(IfaceCall c,IfaceState st)
{
   JcodeMethod m = c.getMethod();
   if (!m.isStatic() && !m.isConstructor() && !m.isPrivate()) {
      JcodeDataType dt = m.getDeclaringClass();
      if (fait_control.isProjectClass(dt)) initialize(dt);
      else {
	 synchronized (staticinit_set) {
	    if (!staticinit_set.contains(dt)) return;
	  }
       }
    }

   queueForInitializers(c,st);

   queueMethod(c);
}



void queueForInitializers(IfaceCall c,IfaceState st)
{
   JcodeMethod m = c.getMethod();
   IfaceState sst = c.getStartState();

   if (st != null && !m.isStatic() && !m.isConstructor()) {
      if (sst != null && sst.addInitializations(st)) {
	 recheckAllInitializers();
       }
    }
   else if (st != null && sst != null) {
      sst.addInitializations(st);
    }
}



void queueMethodChange(IfaceCall c,JcodeInstruction ins)
{
   if (call_map.get(c) == null) return;

   queueMethod(c,ins);
}



private void queueMethod(IfaceCall c)
{
   if (c.getMethod().getNumInstructions() == 0) {
      IfaceLog.logE("CALLING ABSTRACT " + c);
      // TODO: simulate call here
      return;
    }

   JcodeInstruction ins = c.getMethod().getInstruction(0);
   if (ins == null) return;
   queueMethod(c,ins);
}


private void queueMethod(IfaceCall c,JcodeInstruction ins)
{
   if (c == null) return;

   boolean chng = false;

   initialize(c.getMethodClass());

   synchronized (call_queue) {
      Set<JcodeInstruction> s = call_queue.get(c);
      if (s == null) {
	 s = new HashSet<JcodeInstruction>();
	 call_queue.put(c,s);
	 chng = true;
	 call_queue.notifyAll();
       }
      s.add(ins);
    }

   if (chng) {
      IfaceLog.logD1("Queue method " + c.getLogName() + " @ " + ins.getIndex());
    }
}




/********************************************************************************/
/*										*/
/*	Methods to setup a queue to process					*/
/*										*/
/********************************************************************************/

boolean allDone()
{
   synchronized (call_queue) {
      return active_calls.isEmpty() && call_queue.isEmpty();
    }
}


FlowQueueInstance setupNextFlowQueue()
{
   IfaceCall cm = null;
   Set<JcodeInstruction> inset = null;
   FlowQueueInstance fqi = null;
   boolean newfqi = false;

   synchronized (call_queue) {
      while (!allDone()) {
	 Iterator<Map.Entry<IfaceCall,Set<JcodeInstruction>>> it;
	 it = call_queue.entrySet().iterator();
	 while (it.hasNext()) {
	    Map.Entry<IfaceCall,Set<JcodeInstruction>> ent = it.next();
	    if (active_calls.contains(ent.getKey())) continue;
	    it.remove();
	    if (ent.getKey().getMethod().getNumInstructions() > 0) {
	       cm = ent.getKey();
	       active_calls.add(cm);
	       inset = ent.getValue();
	       break;
	     }

	  }
	 if (cm != null) break;
	 try {
	    call_queue.wait(10000);
	  }
	 catch (InterruptedException e) { }
       }

      if (cm == null) return null;

      fqi = call_map.get(cm);
      if (fqi == null) {
	 fqi = new FlowQueueInstance(cm);
	 call_map.put(cm,fqi);
	 newfqi = true;
       }
    }

   IfaceLog.logD("");
   IfaceLog.logD("START WORK ON " + cm.getLogName());

   if (newfqi) {
      IfaceState st0 = cm.getStartState();
      fqi.mergeState(st0,null);
    }
   else if (inset != null) {
      JcodeInstruction i0 = cm.getMethod().getInstruction(0);
      IfaceLog.logD1("Add instruction " + i0.getIndex());
      for (JcodeInstruction fi : inset) {
	 if (fi == i0) fqi.mergeState(cm.getStartState(),fi);
	 fqi.lookAt(fi);
       }
    }

   return fqi;
}




void doneWithFlowQueue(FlowQueueInstance fqi)
{
   synchronized (call_queue) {
      active_calls.remove(fqi.getCall());
      call_queue.notifyAll();
    }
}




/********************************************************************************/
/*										*/
/*	Method to handle static and class initialization			*/
/*										*/
/********************************************************************************/

void initialize(JcodeDataType dt)
{
   initialize(dt,false);
}


void initialize(JcodeDataType dt,boolean fakeinit)
{
   Collection<IfaceCall> inits = new ArrayList<IfaceCall>();

   synchronized(staticinit_set) {
      if (!staticinit_set.contains(dt)) {
	 staticinit_set.add(dt);
	 IfaceLog.logD1("Initialize " + dt.getName());

	 if (dt.getSuperType() != null) initialize(dt.getSuperType(),false);

	 int ctr = 0;
	 Collection<JcodeMethod> sinit = fait_control.findStaticInitializers(dt.getName());
	 if (sinit != null) {
	    for (JcodeMethod fm : fait_control.findStaticInitializers(dt.getName())) {
	       ++ctr;
	       IfaceCall c = fait_control.findCall(fm,null,InlineType.NONE);
	       static_inits.add(c);
	       if (!c.hasResult()) inits.add(c);
	     }
	  }
	 if (ctr == 0) {
	    staticinit_ran.add(dt);
	    staticinit_started.add(dt);
	  }
      }
   }

   for (IfaceCall c : inits) {
      queueMethod(c);
    }
}


boolean canBeUsed(JcodeDataType dt)
{
   synchronized(staticinit_set) {
      if (staticinit_set.contains(dt)) return true;
      if (class_setup.contains(dt)) return true;
    }

   return false;
}


boolean checkInitialized(IfaceState st,IfaceCall cm,JcodeInstruction ins)
{
   synchronized (staticinit_set) {
      JcodeDataType bc = cm.getMethodClass();
      int idx = ins.getIndex();
      if (idx == 0 && !getInitializerDone(bc)) {
	 if (cm.getMethod().isStaticInitializer()) {
	    staticinit_started.add(bc);
	    requeueForInit(staticinit_queue.get(bc));
	    staticinit_queue.remove(bc);
	  }
	 else {
	    if (!staticinit_started.contains(bc)) {
	       IfaceLog.logD1("Class not initialized requeue");
	       Set<IfaceCall> s = staticinit_queue.get(bc);
	       if (s == null) {
		  s = new HashSet<IfaceCall>();
		  staticinit_queue.put(bc,s);
		}
	       s.add(cm);
	       return false;
	     }
	  }
       }

      return true;
    }
}


void handleReturnSetup(JcodeMethod fm)
{
   if (fm.isStaticInitializer()) {
      JcodeDataType dt = fm.getDeclaringClass();
      synchronized (staticinit_set) {
	 staticinit_ran.add(dt);
	 Set<IfaceCall> cs = staticinit_queue.get(dt);
	 if (cs != null) {
	    for (IfaceCall nc : cs) {
	       IfaceLog.logD("Requeue for initialization: " + nc);
	       queueMethodStart(nc);
	     }
	  }
       }
    }
}


private void requeueForInit(Collection<IfaceCall> s)
{
   if (s == null) return;
   for (IfaceCall c : s) queueMethod(c);
}


private boolean getInitializerDone(JcodeDataType dt)
{
   synchronized (staticinit_set) {
      initialize(dt);
      if (!staticinit_ran.contains(dt)) return false;
    }

   return true;
}

private void recheckAllInitializers()
{
   for (JcodeDataType dt : classsetup_map.keySet()) {
      recheckInitializers(dt,false);
    }
}


private void recheckInitializers(JcodeDataType dt,boolean del)
{
   Collection<IfaceCall> s;

   synchronized(staticinit_set) {
      if (del) s = classsetup_map.remove(dt);
      else s = classsetup_map.get(dt);
    }

   if (s != null) {
      for (IfaceCall c : s) {
	 queueMethod(c);
	 IfaceLog.logD1("Queue for constructor: " + c);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

void handleUpdate(IfaceUpdater upd)
{
   for (FlowQueueInstance qi : call_map.values()) {
      qi.handleUpdate(upd);
    }
}

/********************************************************************************/
/*										*/
/*	Access control								*/
/*										*/
/********************************************************************************/

IfaceState handleAccess(FaitLocation loc,int act,IfaceState st)
{
   return conditional_control.handleAccess(loc,act,st);
}



IfaceState handleImplications(FlowQueueInstance wq,JcodeInstruction ins,
      IfaceState st0,TestBranch br)
{
   return conditional_control.handleImplications(wq,ins,st0,br);
}



/********************************************************************************/
/*										*/
/*	Array access								*/
/*										*/
/********************************************************************************/

IfaceValue handleNewArraySet(FlowLocation loc,JcodeDataType acls,int ndim,IfaceValue sz)
{
   return array_control.handleNewArraySet(loc,acls,ndim,sz);
}


IfaceValue handleArrayAccess(FlowLocation loc,IfaceValue arr,IfaceValue idx)
{
   return array_control.handleArrayAccess(loc,arr,idx);
}



void handleArraySet(FlowLocation loc,IfaceValue arr,IfaceValue val,IfaceValue idx)
{
   array_control.handleArraySet(loc,arr,val,idx);
}


void handleArrayCopy(List<IfaceValue> args,FlowLocation loc)
{
   array_control.handleArrayCopy(args,loc);
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
   field_control.handleFieldSet(loc,st,thisref,v0,base);
}



IfaceValue handleFieldGet(FlowLocation loc,IfaceState st,boolean thisref,IfaceValue base)
{
   return field_control.handleFieldGet(loc,st,thisref,base);
}



/********************************************************************************/
/*										*/
/*	Call management methods 						*/
/*										*/
/********************************************************************************/

boolean handleCall(FlowLocation loc,IfaceState st0,FlowQueueInstance wq)
{
   return call_control.handleCall(loc,st0,wq);
}



void handleCallback(JcodeMethod fm,List<IfaceValue> args,String cbid)
{
   call_control.handleCallback(fm,args,cbid);
}



void handleReturn(IfaceCall c0,IfaceValue v0)
{
   call_control.handleReturn(c0,v0);
}


void handleThrow(FlowQueueInstance wq,FaitLocation loc,IfaceValue v0,IfaceState st0)
{
   call_control.handleThrow(wq,loc,v0,st0);
}


void handleException(IfaceValue v0,IfaceCall cm)
{
   call_control.handleException(v0,cm);
}


}	// end of class FlowQueue




/* end of FlowQueue.java */
