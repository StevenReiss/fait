/********************************************************************************/
/*										*/
/*		StateBase.java							*/
/*										*/
/*	Basic implementation of an evaluation state				*/
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



package edu.brown.cs.fait.state;

import edu.brown.cs.fait.iface.*;

import java.util.*;



class StateBase implements StateConstants, IfaceState
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceValue []		local_values;
private Stack<IfaceValue>	stack_values;
private IfaceSafetyStatus	safety_values;

private Map<IfaceField,IfaceValue> field_map;

private Object			prior_state;
private IfaceLocation		state_location;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

StateBase(int numlocal,IfaceSafetyStatus sts)
{
   local_values = new IfaceValue[numlocal];
   stack_values = new Stack<>();
   field_map = new HashMap<>(4);
   state_location = null;
   prior_state = null;
   safety_values = sts;

   Arrays.fill(local_values,null);
}



/********************************************************************************/
/*										*/
/*	Create a new state from this state					*/
/*										*/
/********************************************************************************/

@Override public IfaceState cloneState()
{
   StateBase ns = new StateBase(local_values.length,safety_values);
   ns.addPriorState(this);

   System.arraycopy(local_values,0,ns.local_values,0,local_values.length);

   for (IfaceValue iv : stack_values) {
      ns.stack_values.push(iv);
    }

   ns.field_map = new HashMap<>(field_map);
   ns.safety_values = safety_values;

   return ns;
}



/********************************************************************************/
/*										*/
/*	Stack management methods						*/
/*										*/
/********************************************************************************/

@Override public void pushStack(IfaceValue v)		{ stack_values.push(v); }

@Override public IfaceValue popStack()		
{
   if (stack_values.size() == 0) {
      FaitLog.logE("Attempt to pop empty stack");
      return null;
    }
   return stack_values.pop();
}

@Override public void resetStack(IfaceState s)
{
   StateBase sb = (StateBase) s;
   while (stack_values.size() > sb.stack_values.size()) stack_values.pop();
}


@SuppressWarnings("unchecked")
@Override public void copyStackFrom(IfaceState s)
{
   StateBase sb = (StateBase) s;
   stack_values = (Stack<IfaceValue>) sb.stack_values.clone();

   for (int i = 0; i < local_values.length; ++i) {
      IfaceValue v0 = local_values[i];
      if (v0 != null && v0.isBad())
	 local_values[i] = sb.local_values[i];
    }
}

@Override public IfaceValue getStack(int idx)
{
   int ct = stack_values.size();
   if (idx < 0 || idx >= ct) return null;
   return stack_values.get(ct-1-idx);
}

@Override public void setStack(int idx,IfaceValue v)
{
   int ct = stack_values.size();
   if (idx < 0 || idx >= ct) return;
   stack_values.set(ct-1-idx,v);
}

@Override public boolean stackIsCategory2()
{
   IfaceValue vf = stack_values.peek();
   return vf.isCategory2();
}


@Override public void handleDup(boolean dbl,int lvl)
{
   IfaceValue v1 = stack_values.pop();

   if (dbl && v1.isCategory2()) dbl = false;
   if (lvl == 2) {
      IfaceValue chk = stack_values.peek();
      if (dbl) {
	 IfaceValue x = stack_values.pop();
	 chk = stack_values.peek();
	 stack_values.push(x);
       }
      if (chk.isCategory2()) lvl = 1;
    }

   if (lvl == 0 && !dbl) {		// dup
      stack_values.push(v1);
      stack_values.push(v1);
    }
   else if (lvl == 0 && dbl) {		// dup2
      IfaceValue v2 = stack_values.pop();
      stack_values.push(v2);
      stack_values.push(v1);
      stack_values.push(v2);
      stack_values.push(v1);
    }
   else if (lvl == 1 && !dbl) { 	// dup_x1
      IfaceValue v2 = stack_values.pop();
      stack_values.push(v1);
      stack_values.push(v2);
      stack_values.push(v1);
    }
   else if (lvl == 1 && dbl) {		// dup2_x1
      IfaceValue v2 = stack_values.pop();
      IfaceValue v3 = stack_values.pop();
      stack_values.push(v2);
      stack_values.push(v1);
      stack_values.push(v3);
      stack_values.push(v2);
      stack_values.push(v1);
    }
   else if (lvl == 2 && !dbl) { 	 // dup_x2
      IfaceValue v2 = stack_values.pop();
      IfaceValue v3 = stack_values.pop();
      stack_values.push(v1);
      stack_values.push(v3);
      stack_values.push(v2);
      stack_values.push(v1);
    }
   else if (lvl == 2 && dbl) {		// dup2_x2
      IfaceValue v2 = stack_values.pop();
      IfaceValue v3 = stack_values.pop();
      IfaceValue v4 = stack_values.pop();
      stack_values.push(v2);
      stack_values.push(v1);
      stack_values.push(v4);
      stack_values.push(v3);
      stack_values.push(v2);
      stack_values.push(v1);
    }
}



/********************************************************************************/
/*										*/
/*	Local management methods						*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getLocal(int idx)	
{
   if (idx < 0 || idx >= local_values.length) return null;

   return local_values[idx];
}


@Override public void setLocal(int idx,IfaceValue v)
{
   local_values[idx] = v;
}



@Override public boolean addToLocal(int idx,IfaceValue v)
{
   IfaceValue ov = local_values[idx];
   if (ov == null) local_values[idx] = v;
   else local_values[idx] = ov.mergeValue(v);

   // if (local_values[idx] != ov) {
       // IfaceValue nv = ov.mergeValue(v);
       // System.err.println("MERGE -> " + nv + " " + ov + " " + v);
    // }

   return local_values[idx] != ov;
}



/********************************************************************************/
/*										*/
/*	Field caching methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getFieldValue(IfaceField nm)
{
   return field_map.get(nm);
}


@Override public void setFieldValue(IfaceField fld,IfaceValue v)
{
   if (v == null) return;

   field_map.put(fld,v);
}



@Override public Collection<IfaceField> getKnownFields()
{
   return field_map.keySet();
}


@Override public boolean hasKnownFields()
{
   return field_map.size() > 0;
}


@Override public void discardFields()
{
   field_map.clear();
}











/********************************************************************************/
/*										*/
/*	Handle state merging for control path merges				*/
/*										*/
/********************************************************************************/

@Override public IfaceState mergeWith(IfaceState ifs)
{
   StateBase cs = (StateBase) ifs;

   if (!checkMergeWithState(cs)) return null;

   return this;
}




private boolean checkMergeWithState(StateBase cs)
{
   boolean change = false;

   for (int i = 0; i < local_values.length; ++i) {
      IfaceValue ov = local_values[i];
      IfaceValue nv = (ov == null ? cs.local_values[i] : ov.mergeValue(cs.local_values[i]));
      if (nv != ov) {
         if (nv != null && nv.isBad()) {
            FaitLog.logD("STATE","Bad merge for local " + i + " " +
                  getLocation() + " " + cs.getLocation());
          }
	 change = true;
	 local_values[i] = nv;
       }
    }

   int j0 = stack_values.size();
   int j1 = cs.stack_values.size();
   if (j0 > j1) j0 = j1;

   for (int i = 0; i < j0; ++i) {
      IfaceValue oo = stack_values.elementAt(i);
      IfaceValue no = cs.stack_values.elementAt(i);

      if (no == null) no = oo;
      if (oo != null) {
	 no = oo.mergeValue(no);
         if (no != null && no.isBad()) {
            FaitLog.logD("STATE","Bad merge for stack " + i + " " +
               getLocation() + " " + cs.getLocation());
          }
       }

      if (no != oo) {
	 change = true;
	 stack_values.setElementAt(no,i);
       }
    }

   Map<IfaceField,IfaceValue> changes = null;
   for (Map.Entry<IfaceField,IfaceValue> ent : field_map.entrySet()) {
      IfaceField fld = ent.getKey();
      IfaceValue val = ent.getValue();
      IfaceValue nval = cs.getFieldValue(fld);
      if (nval == null) {
         if (changes == null) changes = new HashMap<>();
         changes.put(fld,null);
	 change = true;
       }
      else {
	 nval = val.mergeValue(nval);
         if (nval != null && nval.isBad()) {
            FaitLog.logD("STATE","Bad merge for field " + fld + " " +
                  getLocation() + " " + cs.getLocation());
          }
	 if (val != nval) {
            if (changes == null) changes = new HashMap<>();
            changes.put(fld,nval);
	    change = true;
	  }
       }
    }
   if (changes != null) {
      for (Map.Entry<IfaceField,IfaceValue> ent : changes.entrySet()) {
         if (ent.getValue() == null) field_map.remove(ent.getKey());
         else field_map.put(ent.getKey(),ent.getValue());
       }
    }

   if (safety_values != cs.safety_values && cs.safety_values != null) {
      if (safety_values == null) {
	 safety_values = cs.safety_values;
	 change = true;
       }
      else {
	 IfaceSafetyStatus nsts = safety_values.merge(cs.safety_values);
	 if (nsts != safety_values) {
	    safety_values = nsts;
	    change = true;
	  }
       }
    }

   StateBase priorbase = cs.getPriorBaseState();
   for (int i = 0; i < getNumPriorStates(); ++i) {
      StateBase sb = (StateBase) getPriorState(i);
      StateBase sbase = sb.getPriorBaseState();
      if (sbase == priorbase) {
	 removePriorState(sb);
       }
    }
   addPriorState(cs);

   return change;
}




private StateBase getPriorBaseState()
{
   if (state_location != null) return this;
   for (int i = 0; i < getNumPriorStates(); ++i) {
      StateBase sp = (StateBase) getPriorState(i);
      StateBase asp = sp.getPriorBaseState();
      if (asp != null) return asp;
    }
   return null;
}



/********************************************************************************/
/*										*/
/*	Initialization management methods					*/
/*										*/
/********************************************************************************/

@Override public void startInitialization(IfaceType dt) { }

@Override public boolean testDoingInitialization(IfaceType dt)	{ return false; }




/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

@Override public void handleUpdate(IfaceUpdater upd)
{
   for (int i = 0; i < local_values.length; ++i) {
      IfaceValue nvl = upd.getNewValue(local_values[i]);
      if (nvl != null) local_values[i] = nvl;
    }
   for (int i = 0; i < stack_values.size(); ++i) {
      IfaceValue ovl = stack_values.get(i);
      IfaceValue nvl = upd.getNewValue(ovl);
      if (nvl != null && nvl != ovl) stack_values.set(i,nvl);
    }
   for (Map.Entry<IfaceField,IfaceValue> ent : field_map.entrySet()) {
      IfaceValue ovl = ent.getValue();
      IfaceValue nvl = upd.getNewValue(ovl);
      if (nvl != null && nvl != ovl) ent.setValue(nvl);
    }
}



/********************************************************************************/
/*										*/
/*	Methods for handling back propagation					*/
/*										*/
/********************************************************************************/

@Override public void setLocation(IfaceLocation pt)		{ state_location = pt; }

@Override public IfaceLocation getLocation()			{ return state_location; }

@SuppressWarnings("unchecked")
void addPriorState(StateBase st)
{
   if (st == null) return;
   if (prior_state == null) prior_state = st;
   else {
      List<IfaceState> priors = null;
      if (prior_state instanceof IfaceState) {
	 if (prior_state == st) return;
	 priors = new ArrayList<>();
	 priors.add((IfaceState) prior_state);
	 prior_state = priors;
       }
      else {
	 priors = (List<IfaceState>) prior_state;
	 if (priors.contains(st)) return;
       }
      priors.add(st);
    }
}



@SuppressWarnings("unchecked")
private void removePriorState(StateBase st)
{
   if (st == null) return;
   if (prior_state == null) return;
   else if (prior_state == st) prior_state = null;
   else if (prior_state instanceof IfaceState) return;
   else {
     List<IfaceState> priors = (List<IfaceState>) prior_state;
     priors.remove(st);
     if (priors.size() == 0) prior_state = null;
    }
}

@Override public int getNumPriorStates()
{
   if (prior_state == null) return 0;
   else if (prior_state instanceof IfaceState) return 1;

   List<?> priors = (List<?>) prior_state;
   return priors.size();
}


@Override public IfaceState getPriorState(int idx)
{
   if (prior_state == null || idx < 0) return null;
   if (prior_state instanceof IfaceState) {
      if (idx == 0) return (IfaceState) prior_state;
      return null;
    }
   List<?> priors = (List<?>) prior_state;
   if (idx >= priors.size()) return null;
   Object o = priors.get(idx);	
   return (IfaceState) o;
}


@Override public boolean isStartOfMethod()
{
   if (getNumPriorStates() == 1) {
      IfaceState st0 = getPriorState(0);
      if (st0.getNumPriorStates() == 0 &&
	    st0.getLocation() == null &&
	    getLocation().getCall().getStartState() == st0)
	 return true;
    }

   return false;
}


@Override public boolean isMethodCall()
{
   if (state_location == null) return false;
   IfaceCall c = state_location.getCall();
   IfaceProgramPoint pt = state_location.getProgramPoint();
   if (!c.getAllMethodsCalled(pt).isEmpty()) return true;
   IfaceMethod im = pt.getCalledMethod();
   if (im != null) {
      return true;
    }
   return false;
}



/********************************************************************************/
/*										*/
/*	Methods forhandling safety checks					*/
/*										*/
/********************************************************************************/

@Override public IfaceSafetyStatus getSafetyStatus()
{
   return safety_values;
}


@Override public boolean mergeSafetyStatus(IfaceSafetyStatus sts)
{
   if (safety_values == null) {
      if (sts == null) return false;
      safety_values = sts;
      return true;
    }
   if (sts == null) return false;

   IfaceSafetyStatus nsts = safety_values.merge(sts);
   if (nsts == safety_values) return false;
   safety_values = nsts;

   if (FaitLog.isTracing()) {
      FaitLog.logD1("Safety state change: " + safety_values);
    }

   return true;
}


@Override public void setSafetyStatus(IfaceSafetyStatus sts)
{
   safety_values = sts;
}


@Override public void updateSafetyStatus(String event,IfaceControl ctrl)
{
   if (FaitLog.isTracing()) FaitLog.logD("Safety event: " + event);

   IfaceLocation loc = null;
   for (IfaceState sb = this; sb != null; sb = sb.getPriorState(0)) {
      loc = sb.getLocation();
      if (loc != null) break;
    }

   if (safety_values == null) {
      IfaceState st0 = getPriorState(0);
      if (st0 != null) safety_values = st0.getSafetyStatus();
    }
   if (safety_values == null) {
      IfaceSafetyStatus nsts = ctrl.getInitialSafetyStatus();
      if (nsts != null) {
	 IfaceSafetyStatus nnsts = nsts.update(event,loc);
	 if (nnsts == nsts) return;
       }
      safety_values = nsts;
    }

   safety_values = safety_values.update(event,loc);
   if (FaitLog.isTracing()) FaitLog.logD1("Result state: " + safety_values);
}



}	// end of class StateBase




/* end of StateBase.java */

