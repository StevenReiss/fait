/********************************************************************************/
/*										*/
/*		FlowScanner.java						*/
/*										*/
/*	Scanner to symbolically execute a method				*/
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


import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceImplications;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcomp.JcompSymbol;


abstract class FlowScanner implements FlowConstants, JcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected FlowQueue		flow_queue;
protected IfaceControl		fait_control;
protected FlowQueueInstance	work_queue;

protected static IfaceError CALL_NEVER_RETURNS;
protected static IfaceError BRANCH_NEVER_TAKEN;
protected static IfaceError DEREFERENCE_NULL;
protected static IfaceError UNREACHABLE_CODE;
protected static IfaceError NO_IMPLEMENTATION;

static {
   CALL_NEVER_RETURNS = new FaitError(ErrorLevel.WARNING,"Call never returns");
   BRANCH_NEVER_TAKEN = new FaitError(ErrorLevel.NOTE,"Branch never taken");
   DEREFERENCE_NULL = new FaitError(ErrorLevel.WARNING,"Attempt to dereference null");
   UNREACHABLE_CODE = new FaitError(ErrorLevel.NOTE,"Unreachable code");
   NO_IMPLEMENTATION = new FaitError(ErrorLevel.NOTE,"No method implementation available");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowScanner(IfaceControl fc,FlowQueue fq,FlowQueueInstance wq)
{
   fait_control = fc;
   flow_queue = fq;
   work_queue = wq;
}



/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

abstract int scanCode();
abstract int scanBack();


/********************************************************************************/
/*										*/
/*	Common methods								*/
/*										*/
/********************************************************************************/

protected IfaceEntity getLocalEntity(IfaceCall call,IfaceLocation loc,IfaceType tyr)
{
   IfaceProgramPoint inspt = loc.getProgramPoint();
   IfaceEntity ns = call.getBaseEntity(inspt);
   
   // if doing incremental update, find prior local entity
   
   if (ns == null) {
      tyr = tyr.getAnnotatedType(FaitAnnotation.NON_NULL);

      IfacePrototype pt = fait_control.createPrototype(tyr);
      ns = fait_control.findLocalEntity(loc,tyr,pt);
      // if (pt != null) {
	 // ns = fait_control.findPrototypeEntity(tyr,pt,loc);
       // }
      // might want to create fixed source for non-project methods
      // else {
	 // ns = fait_control.findLocalEntity(loc,tyr,pt);
       // }
      call.setBaseEntity(inspt,ns);
    }

   return ns;
}



protected void setLocal(IfaceLocation loc,
      IfaceState state,int slot,IfaceValue val,int stackref)
{
   IfaceMethod ic = loc.getMethod();
   IfaceType typ = ic.getLocalType(slot,loc.getProgramPoint());
   val = checkAssignment(val,typ,loc,stackref);

   state.setLocal(slot,val);
}


static IfaceValue checkAssignment(IfaceValue val,IfaceType typ,IfaceLocation loc,int stackref)
{
   if (typ != null) {
      if (!val.getDataType().checkCompatibility(typ,loc,val,stackref)) {
	 IfaceValue val1 = val.restrictByType(typ);
	 if (val != val1) {
	    if (!val.mustBeNull()) {
	       if (FaitLog.isTracing()) {
		  FaitLog.logD("Value change on assignment " + val + " -> " + val1);
                }
	     }
	    // val = val1;
	    // should val be set here?
	  }
       }
    }

   return val;
}


protected IfaceValue getActualValue(IfaceState state,FlowLocation here,IfaceValue ref,boolean nonnull)
{
   if (ref == null) return null;
   if (!ref.isReference()) return ref;

   IfaceValue base = ref.getRefBase();
   if (base != null) {
      IfaceValue idx = ref.getRefIndex();
      if (base.mustBeNull()) {
	 return null;
       }
      if (idx != null) {
	 IfaceValue vrslt = flow_queue.handleArrayAccess(here,base,idx);
	 IfaceValue nrslt = getNonNullResult(nonnull,vrslt);
	 if (nrslt != null && nrslt != vrslt) flow_queue.handleArraySet(here,base,nrslt,idx,-1);
	 return nrslt;
       }
      else if (ref.getRefField() != null) {
	 boolean oref = false;		// set to true if base == this
	 if (base == state.getLocal(0)) oref = true;
	 IfaceValue vrslt = flow_queue.handleFieldGet(ref.getRefField(),here,
	       state,oref,base);
	 if (FaitLog.isTracing())
	    FaitLog.logD1("Field Access " + ref.getRefField() + " of " + base + " = " + vrslt);
	 IfaceValue nrslt = getNonNullResult(nonnull,vrslt);
	 if (nrslt != null && nrslt != vrslt)
	    flow_queue.handleFieldSet(ref.getRefField(),here,state,oref,nrslt,base,-1);
	 return nrslt;
       }
    }
   else if (ref.getRefSlot() >= 0) {
      IfaceValue vrslt = state.getLocal(ref.getRefSlot());
      IfaceValue nrslt = getNonNullResult(nonnull,vrslt);
      if (nrslt != null && nrslt != vrslt) {
	 if (FaitLog.isTracing()) {
	    FaitLog.logD1("Set Local " + ref.getRefSlot() + " to non-null value: " + nrslt);
	  }
	 state.setLocal(ref.getRefSlot(),nrslt);
       }
      if (nrslt != null && nrslt.isBad()) {
	 FaitLog.logD1("Access to bad value in slot " + ref.getRefSlot());
       }
      if (FaitLog.isTracing()) {
	 FaitLog.logD1("Local " + ref.getRefSlot() + " accessed as " + nrslt + " " + nonnull);
       }
      return nrslt;
    }
   else if (ref.getRefField() != null) {
      // static fields
      IfaceValue vrslt = flow_queue.handleFieldGet(ref.getRefField(),here,state,false,null);
      return vrslt;
    }
   else if (ref.getRefStack() >= 0) {
      IfaceValue vrslt = state.getStack(ref.getRefStack());
      if (vrslt != null && vrslt.isReference()) vrslt = getActualValue(state,here,vrslt,nonnull);
      IfaceValue nrslt = getNonNullResult(nonnull,vrslt);
      if (nrslt != null && nrslt != vrslt) {
	 state.setStack(ref.getRefStack(),nrslt);
       }
      if (FaitLog.isTracing()) {
	 FaitLog.logD1("Stack " + ref.getRefStack() + " accessed as " + nrslt);
       }
      return nrslt;
    }
   else {
      FaitLog.logE("ILLEGAL REF VALUE");
    }

   return ref;
}



protected IfaceValue getNonNullResult(boolean nonnull,IfaceValue v)
{
   if (v == null || !nonnull) return v;
   if (v.mustBeNull()) return null;
   if (!v.canBeNull()) return v;
   IfaceValue v1 = v.forceNonNull();
   return v1;
}



protected IfaceValue assignValue(IfaceState state,FlowLocation here,IfaceValue ref,IfaceValue v,int stackref)
{
   IfaceValue v1 = flow_queue.castValue(ref.getDataType(),v,here);

   IfaceValue base = ref.getRefBase();
   IfaceField fld = ref.getRefField();
   IfaceValue idx = ref.getRefIndex();
   int slot = ref.getRefSlot();
   int stk = ref.getRefStack();
   if (fld != null) {
      boolean thisref = false;	 
      if (base != null && !fld.isStatic() && base == state.getLocal(0)) thisref = true;
      flow_queue.handleFieldSet(fld,here,state,thisref,v1,base,stackref);
    }
   else if (idx != null) {
      flow_queue.handleArraySet(here,base,v1,idx,stackref);
    }
   else if (slot >= 0) {
      setLocal(here,state,slot,v1,stackref);
      if (FaitLog.isTracing()) FaitLog.logD("Assign to Local " + slot + " = " + v1);
    }
   else if (stk >= 0) {
      state.setStack(stk,v1);
    }
   else {
      FaitLog.logE("Bad assignment " + ref + " " + v + " " + stackref);
    }

   return v1;
}



/********************************************************************************/
/*                                                                              */
/*      Specialized flow errors                                                 */
/*                                                                              */
/********************************************************************************/

IfaceError callNeverReturnsError(JcompSymbol sym)
{
   if (sym == null) return CALL_NEVER_RETURNS;
   
   return callNeverReturnsError(sym.getFullName());
}

IfaceError callNeverReturnsError(String name)
{
   if (name == null) return CALL_NEVER_RETURNS;
   
   String msg = "Call never returns : " + name;
   return new FaitError(ErrorLevel.WARNING,msg);
}


IfaceError noImplementationError(JcompSymbol sym)
{
   if (sym == null) return NO_IMPLEMENTATION;
   
   return noImplementationError(sym.getFullName());
}


IfaceError noImplementationError(String name)
{
   if (name == null) return NO_IMPLEMENTATION;
   
   String msg = "No method implementation available : " + name;
   return new FaitError(ErrorLevel.WARNING,msg);
}


/********************************************************************************/
/*										*/
/*	Implication methods							*/
/*										*/
/********************************************************************************/

IfaceImplications getImpliedValues(IfaceValue v0,FaitOperator op,IfaceValue v1)
{
   return v0.getImpliedValues(v1,op);

}



/********************************************************************************/
/*										*/
/*	Back Propagation methods						*/
/*										*/
/********************************************************************************/

protected IfaceValue adjustRef(IfaceValue ref,int pop,int push)
{
   if (ref == null) return null;
   int idx = ref.getRefStack();
   if (idx >= 0) {
      if (idx < push) return null;
      if (pop == push) return ref;
      return fait_control.findRefStackValue(ref.getDataType(),idx-push+pop);
    }
   return ref;
}



protected void checkBackPropagation(FlowLocation loc,IfaceState st0,int sref,
      IfaceValue val,FaitOperator what,IfaceValue arg)
{
   // get state at start of program point
   if (val == null) val = st0.getStack(sref);
   IfaceType t1 = val.checkOperation(what,arg);
   if (t1 == null || t1 == val.getDataType()) return;
   IfaceValue vref = fait_control.findRefStackValue(val.getDataType(),sref);
   while (st0 != null && st0.getLocation() == null) {
      st0 = st0.getPriorState(0);
    }
   queueBackRefs(loc,st0,vref,t1);
}




protected void queueBackRefs(FlowLocation here,IfaceState st,IfaceValue nextref,IfaceType settype)
{
   // called with state pointing to the start state and nextref referring to a reference in it

   boolean doprior = false;

   if (nextref == null) return;
   if (nextref.getRefField() != null) {
      IfaceValue base = nextref.getRefBase();
      if (base != null && base == st.getLocal(0)) {
	 IfaceValue v0 = st.getFieldValue(nextref.getRefField());
	 if (v0 != null) {
	    IfaceValue n0 = v0.restrictByType(settype);
	    if (n0 != null && n0 != v0) {
	       st.setFieldValue(nextref.getRefField(),n0);
	       doprior = true;
	     }
	  }
       }
    }
   else {
      IfaceValue vref = getActualValue(st,here,nextref,false);
      if (vref == null || vref.mustBeNull()) return;
      if (!vref.getDataType().getJavaType().isCompatibleWith(settype.getJavaType()) &&
	   !vref.mustBeNull()) {
	 FaitLog.logD("Attempt to back assign different type: " + vref + " " + settype);
	 return;
       }
      //TODO: Restrict by type is not quite right here -- can be a merge point
      IfaceValue nref = vref.restrictByType(settype);
      if (nref != null && nref != vref) {
	 if (nextref.getRefStack() >= 0) {
	    IfaceValue vrslt = st.getStack(nextref.getRefStack());
	    IfaceValue nxref = nref;
	    if (vrslt.isReference()) {
	       nxref = vrslt.restrictByType(settype);
	     }
	    if (nxref != vrslt) {
	       st.setStack(nextref.getRefStack(),nxref);
	     }
	  }
	 else {
	    assignValue(st,here,nextref,nref,nref.getRefStack());
	  }
	 doprior = true;
       }
    }

   if (doprior) {
      queueIntermediateBack(here,st,nextref,settype);
    }
}


private void queueIntermediateBack(FlowLocation here,IfaceState st,IfaceValue nextref,IfaceType settype)
{
   for (int i = 0; i < st.getNumPriorStates(); ++i) {
      IfaceState stp = st.getPriorState(i);
      if (stp.getLocation() == null) {
	 queueIntermediateBack(here,stp,nextref,settype);
       }
      else {
	 work_queue.queueBackPropagation(stp.getLocation().getProgramPoint(),nextref,settype);
       }
    }
}

}	// end of class FlowScanner




/* end of FlowScanner.java */

