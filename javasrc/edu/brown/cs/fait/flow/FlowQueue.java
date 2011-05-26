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
private FlowCall                                call_control;

private Set<IfaceCall>				active_calls;

private Map<IfaceCall,FlowQueueInstance>	call_map;
private Map<IfaceCall,Set<FaitInstruction>>	call_queue;

private Set<FaitDataType>			class_setup;
private Map<FaitDataType,Collection<IfaceCall>> classsetup_map;

private Set<FaitDataType>			staticinit_set;
private Set<FaitDataType>			staticinit_ran;
private Set<FaitDataType>			staticinit_started;
private Map<FaitDataType,Set<IfaceCall>>	staticinit_queue;
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
   "java.lang.ThreadGroup",
   "java.lang.SecurityManager",
   "java.lang.StringBuilder",
   "java.lang.StringBuffer",
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

FlowQueue(FaitControl fc)
{
   fait_control = fc;

   active_calls = new HashSet<IfaceCall>();

   call_map = new LinkedHashMap<IfaceCall,FlowQueueInstance>();
   call_queue = new HashMap<IfaceCall,Set<FaitInstruction>>();

   class_setup = new HashSet<FaitDataType>();
   for (String s : preset_classes) {
      FaitDataType dt = fait_control.findDataType(s);
      if (dt != null) class_setup.add(dt);
    }
   classsetup_map = new HashMap<FaitDataType,Collection<IfaceCall>>();

   staticinit_set = new HashSet<FaitDataType>();
   staticinit_set.add(fait_control.findDataType("java.lang.System"));
   staticinit_set.add(fait_control.findDataType("java.lang.Class"));
   staticinit_ran = new HashSet<FaitDataType>(staticinit_set);
   staticinit_started = new HashSet<FaitDataType>(staticinit_set);
   static_inits = new ArrayList<IfaceCall>();
   staticinit_queue = new HashMap<FaitDataType,Set<IfaceCall>>();

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
   FaitMethod m = c.getMethod();
   if (!m.isStatic() && !m.isConstructor() && !m.isPrivate()) {
      FaitDataType dt = m.getDeclaringClass();
      if (dt.isProjectClass()) initialize(dt);
      else {
	 synchronized (staticinit_set) {
	    if (!staticinit_set.contains(dt)) return;
	  }
       }
    }

   queueForConstructors(c,st);

   queueMethod(c);
}



void queueForConstructors(IfaceCall c,IfaceState st)
{
   FaitMethod m = c.getMethod();
   IfaceState sst = c.getStartState();

   if (st != null && !m.isStatic() && !m.isConstructor()) {
      if (sst != null && sst.addInitializations(st)) {
	 recheckAllConstructors();
       }
    }
   else if (st != null && sst != null) {
      sst.addInitializations(st);
    }
}



synchronized void queueMethodChange(IfaceCall c,FaitInstruction ins)
{
   if (call_map.get(c) == null) return;

   queueMethod(c,ins);
}



private void queueMethod(IfaceCall c)
{
   if (c.getMethod().getNumInstructions() == 0) return;

   FaitInstruction ins = c.getMethod().getInstruction(0);
   if (ins == null) return;
   queueMethod(c,ins);
}


private void queueMethod(IfaceCall c,FaitInstruction ins)
{
   if (c == null) return;

   boolean chng = false;

   initialize(c.getMethodClass());

   synchronized (call_queue) {
      Set<FaitInstruction> s = call_queue.get(c);
      if (s == null) {
	 s = new HashSet<FaitInstruction>();
	 call_queue.put(c,s);
	 chng = true;
       }
      s.add(ins);
    }

   if (chng) {
      synchronized (this) { notifyAll(); }
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
   Set<FaitInstruction> inset = null;
   FlowQueueInstance fqi = null;
   boolean newfqi = false;

   synchronized (call_queue) {
      while (!call_queue.isEmpty()) {
	 Iterator<Map.Entry<IfaceCall,Set<FaitInstruction>>> it;
	 it = call_queue.entrySet().iterator();
	 while (it.hasNext()) {
	    Map.Entry<IfaceCall,Set<FaitInstruction>> ent = it.next();
	    if (active_calls.contains(ent.getKey())) continue;
	    it.remove();
	    if (ent.getKey().getMethod().getNumInstructions() > 0) {
	       cm = ent.getKey();
	       active_calls.add(cm);
	       inset = ent.getValue();
	       break;
	     }
	
	  }
       }
      if (cm != null) {
	 fqi = call_map.get(cm);
	 if (fqi == null) {
	    fqi = new FlowQueueInstance(cm);
	    call_map.put(cm,fqi);
	    newfqi = true;
	  }
       }
    }

   if (newfqi) {
      IfaceState st0 = cm.getStartState();
      if (cm.getMethod().isConstructor()) {
	 FaitDataType dt = cm.getMethod().getDeclaringClass();
	 synchronized (staticinit_set) {
	    if (!class_setup.contains(dt)) {
	       st0.startInitialization(dt);
	       recheckConstructors(dt,false);
	     }
	  }
       }
      fqi.mergeState(st0,null);
    }
   else if (inset != null) {
      FaitInstruction i0 = cm.getMethod().getInstruction(0);
      for (FaitInstruction fi : inset) {
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
    }
   synchronized (this) {
      notifyAll();
    }
}
	



/********************************************************************************/
/*										*/
/*	Method to handle static and class initialization			*/
/*										*/
/********************************************************************************/

void initialize(FaitDataType dt)
{
   initialize(dt,false);
}


void initialize(FaitDataType dt,boolean fakeinit)
{
   synchronized(staticinit_set) {
      if (!staticinit_set.contains(dt)) {
	 staticinit_set.add(dt);
	 if (dt.getSuperType() != null) initialize(dt.getSuperType(),false);
       }
      int ctr = 0;
      for (FaitMethod fm : fait_control.findStaticInitializers(dt.getName())) {
	 ++ctr;
	 IfaceCall c = fait_control.findCall(fm,null,InlineType.NONE);
	 static_inits.add(c);
	 queueMethod(c);
       }
      if (ctr == 0) {
	 staticinit_ran.add(dt);
	 staticinit_started.add(dt);
       }
    }
}


boolean canBeUsed(FaitDataType dt)
{
   synchronized(staticinit_set) {
      if (staticinit_set.contains(dt)) return true;
      if (class_setup.contains(dt)) return true;
    }

   return false;
}


boolean checkInitialized(IfaceState st,IfaceCall cm,FaitInstruction ins)
{
   synchronized (staticinit_set) {
      FaitDataType bc = cm.getMethodClass();
      int idx = ins.getIndex();
      if (idx == 0 && !getInitializerDone(bc)) {
	 if (cm.getMethod().isStaticInitializer()) {
	    staticinit_started.add(bc);
	    requeueForInit(staticinit_queue.get(bc));
	    staticinit_queue.remove(bc);
	  }
	 else {
	    if (!staticinit_started.contains(bc)) {
	       // IfaceLog.log("\tClass not initialized requeue");
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

      if (idx == 0 && !cm.getMethod().isStatic() && !class_setup.contains(bc)) {
	 IfaceValue cv = st.getLocal(0);
	 if (cv.isNative() && !cm.getMethodClass().isProjectClass()) return true;
	 if (!st.testDoingInitialization(bc)) {
	    Collection<IfaceCall> c = classsetup_map.get(bc);
	    if (c == null) {
	       c = new HashSet<IfaceCall>();
	       classsetup_map.put(bc,c);
	     }
	    c.add(cm);
	    // IfaceLog.log("Initialization not finished before call for " + bc);
	    return false;
	  }
       }

      return true;
    }
}
	
	
void handleReturnSetup(FaitMethod fm)
{
   if (fm.isStaticInitializer()) {
      FaitDataType dt = fm.getDeclaringClass();
      synchronized (staticinit_set) {
         staticinit_ran.add(dt);
       }
    }
   else if (fm.isConstructor()) {
      FaitDataType dt = fm.getDeclaringClass();
      synchronized (staticinit_set) {
         if (!class_setup.contains(dt)) {
            class_setup.add(dt);
            recheckConstructors(dt,true);
          }
       }
    }
}
              
         
private void requeueForInit(Collection<IfaceCall> s)
{
   if (s == null) return;
   for (IfaceCall c : s) queueMethod(c);
}


private boolean getInitializerDone(FaitDataType dt)
{
   synchronized (staticinit_set) {
      initialize(dt);
      if (!staticinit_ran.contains(dt)) return false;
    }

   return true;
}

private void recheckAllConstructors()
{
   for (FaitDataType dt : classsetup_map.keySet()) {
      recheckConstructors(dt,false);
    }
}


private void recheckConstructors(FaitDataType dt,boolean del)
{
   Collection<IfaceCall> s;

   synchronized(staticinit_set) {
      if (del) s = classsetup_map.remove(dt);
      else s = classsetup_map.get(dt);
    }

   if (s != null) {
      for (IfaceCall c : s) {
	 queueMethod(c);
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



IfaceState handleImplications(FlowQueueInstance wq,FaitInstruction ins,
      IfaceState st0,TestBranch br)
{
   return conditional_control.handleImplications(wq,ins,st0,br);
}



/********************************************************************************/
/*										*/
/*	Array access								*/
/*										*/
/********************************************************************************/

IfaceValue handleNewArraySet(FlowLocation loc,FaitDataType acls,int ndim,IfaceValue sz)
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
/*                                                                              */
/*      Handle field access                                                     */
/*                                                                              */
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
/*                                                                              */
/*      Call management methods                                                 */
/*                                                                              */
/********************************************************************************/

boolean handleCall(FlowLocation loc,IfaceState st0,FlowQueueInstance wq)
{
   return call_control.handleCall(loc,st0,wq);
}



void handleCallback(FaitMethod fm,List<IfaceValue> args,String cbid)
{
   call_control.handleCallback(fm,args,cbid);
}



void handleReturn(IfaceCall c0,IfaceState st0,IfaceValue v0)
{
   call_control.handleReturn(c0,st0,v0);
}


void handleThrow(FaitLocation loc,IfaceValue v0,IfaceState st0)
{
   call_control.handleThrow(loc,v0,st0);
}


void handleException(IfaceValue v0,IfaceCall cm)
{
   call_control.handleException(v0,cm);
}


}	// end of class FlowQueue




/* end of FlowQueue.java */

