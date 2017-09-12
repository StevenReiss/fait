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
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeField;
import edu.brown.cs.ivy.jcode.JcodeInstruction;

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

private Map<JcodeField,IfaceValue>	field_map;

private Stack<JcodeInstruction>	return_stack;
private List<StateBase> 	state_set;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

StateBase(int numlocal)
{
   local_values = new IfaceValue[numlocal];
   stack_values = new Stack<IfaceValue>();
   field_map = new HashMap<JcodeField,IfaceValue>(4);
   state_set = null;
   return_stack = null;

   Arrays.fill(local_values,null);
}



/********************************************************************************/
/*										*/
/*	Create a new state from this state					*/
/*										*/
/********************************************************************************/

@Override public IfaceState cloneState()
{
   StateBase ns = new StateBase(local_values.length);

   System.arraycopy(local_values,0,ns.local_values,0,local_values.length);

   for (IfaceValue iv : stack_values) {
      ns.stack_values.push(iv);
    }

   if (return_stack == null) ns.return_stack = null;
   else {
      ns.return_stack = new Stack<>();
      ns.return_stack.addAll(return_stack);
    }

   ns.state_set = null;

   ns.field_map = new HashMap<JcodeField,IfaceValue>(field_map);

   return ns;
}



/********************************************************************************/
/*										*/
/*	Stack management methods						*/
/*										*/
/********************************************************************************/

@Override public void pushStack(IfaceValue v)		{ stack_values.push(v); }

@Override public IfaceValue popStack()			{ return stack_values.pop(); }

@Override public void resetStack(IfaceState s)
{
   StateBase sb = (StateBase) s;
   while (stack_values.size() > sb.stack_values.size()) stack_values.pop();
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

@Override public IfaceValue getLocal(int idx)		{ return local_values[idx]; }

@Override public void setLocal(int idx,IfaceValue v)	{ local_values[idx] = v; }

@Override public boolean addToLocal(int idx,IfaceValue v)
{
   IfaceValue ov = local_values[idx];
   if (ov == null) local_values[idx] = v;
   else local_values[idx] = ov.mergeValue(v);

   return local_values[idx] != ov;
}



/********************************************************************************/
/*										*/
/*	Field caching methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getFieldValue(JcodeField fld)
{
   return field_map.get(fld);
}



@Override public void setFieldValue(JcodeField fld,IfaceValue v)
{
   if (fld.isVolatile() || v == null) return;

   field_map.put(fld,v);
}







@Override public Iterable<JcodeField> getKnownFields()
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
/*	Handle jsr return stack 						*/
/*										*/
/********************************************************************************/

@Override public void pushReturn(JcodeInstruction ins)
{
   if (return_stack == null) return_stack = new Stack<>();
   return_stack.push(ins);
}



@Override public JcodeInstruction popReturn()
{
   if (return_stack == null) return null;

   JcodeInstruction v = return_stack.pop();
   if (return_stack.empty()) return_stack = null;

   return v;
}



/********************************************************************************/
/*										*/
/*	Handle state merging for control path merges				*/
/*										*/
/********************************************************************************/

@Override public IfaceState mergeWith(IfaceState ifs)
{
   StateBase ucs = null;
   StateBase cs = (StateBase) ifs;

   if (cs.return_stack == null && return_stack == null) {
      ucs = this;
    }
   else if (cs.return_stack == null || return_stack == null) {
      ucs = this;
      return_stack = null;
    }
   else if (return_stack.equals(cs.return_stack)) {
      ucs = this;
    }
   else {
      if (state_set == null) {
	 state_set = new ArrayList<StateBase>();
	 state_set.add(this);
       }
      for (StateBase scs : state_set) {
	 if (scs.return_stack.equals(cs.return_stack)) {
	    ucs = scs;
	    break;
	  }
       }
      if (ucs == null) {
	 ucs = (StateBase) cs.cloneState();
	 ucs.state_set = state_set;
	 state_set.add(ucs);
	 return ucs;
       }
    }

   if (!ucs.checkMergeWithState(cs)) return null;

   return ucs;
}



@Override public boolean compatibleWith(IfaceState ifs)
{
   StateBase cs = (StateBase) ifs;

   if (return_stack == null) {
      if (cs.return_stack != null) return false;
    }
   else if (!return_stack.equals(cs.return_stack)) return false;

   for (int i = 0; i < local_values.length; ++i) {
      IfaceValue ov = local_values[i];
      IfaceValue nv = cs.local_values[i];
      if (ov == null) ov = nv;
      if (ov != nv) return false;
    }

   int j0 = stack_values.size();
   int j1 = cs.stack_values.size();
   if (j0 > j1) j0 = j1;
   for (int i = 0; i < j0; ++i) {
      IfaceValue oo = stack_values.elementAt(i);
      IfaceValue no = cs.stack_values.elementAt(i);

      if (oo == null) oo = no;
      if (no == null) no = oo;
      if (no != oo) return false;
    }

   return true;
}





private boolean checkMergeWithState(StateBase cs)
{
   boolean change = false;

   for (int i = 0; i < local_values.length; ++i) {
      IfaceValue ov = local_values[i];
      IfaceValue nv = (ov == null ? cs.local_values[i] : ov.mergeValue(cs.local_values[i]));
      if (nv != ov) {
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
       }

      if (no != oo) {
	 change = true;
	 stack_values.setElementAt(no,i);
       }
    }

   for (Iterator<Map.Entry<JcodeField,IfaceValue>> it = field_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<JcodeField,IfaceValue> ent = it.next();
      JcodeField fld = ent.getKey();
      IfaceValue val = ent.getValue();
      IfaceValue nval = cs.getFieldValue(fld);
      if (nval == null) {
	 it.remove();
	 change = true;
       }
      else {
	 nval = val.mergeValue(nval);
	 if (val != nval) {
	    field_map.put(fld,nval);
	    change = true;
	  }
       }
    }

   return change;
}



/********************************************************************************/
/*										*/
/*	Initialization management methods					*/
/*										*/
/********************************************************************************/

@Override public void startInitialization(JcodeDataType dt)	{ }

@Override public boolean testDoingInitialization(JcodeDataType dt)	{ return false; }
@Override public boolean addInitializations(IfaceState s)	{ return false; }



/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

@Override public void handleUpdate(IfaceUpdater upd)
{ }




}	// end of class StateBase




/* end of StateBase.java */

