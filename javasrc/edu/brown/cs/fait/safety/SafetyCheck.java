/********************************************************************************/
/*                                                                              */
/*              SafetyCheck.java                                                */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.fait.safety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.fait.iface.IfaceSafetyCheck;

abstract class SafetyCheck implements IfaceSafetyCheck
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String                  check_name;
private Value                   initial_state;
private Map<String,State>       value_set;
private Map<Integer,Value>      numvalue_set;
private Map<String,Integer>     event_set;
private Transitions             transition_set;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SafetyCheck(String name)
{
   check_name = name;
   initial_state = null;
   value_set = new HashMap<>();
   numvalue_set = new HashMap<>();
   event_set = new HashMap<>();
   transition_set = new Transitions();
}




/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

protected Value defineState(String name,boolean init)
{
   State st = value_set.get(name);
   if (st == null) {
      int ct = value_set.size();
      st = new State(name,ct);
      value_set.put(name,st);
      numvalue_set.put(st.ordinal(),st);
    }
   if (init) initial_state = st;
   return st;
}


protected void defineEvent(String name)
{
   if (event_set.get(name) != null) return;
   
   int ct = event_set.size();
   event_set.put(name,ct);
}


protected void defineTransition(Value fromstate,String event,Value tostate)
{
   defineEvent(event);
   transition_set.add(fromstate,event,tostate);
}


protected void defineDefault(Value fromstate,Value tostate)
{
   transition_set.addDefault(fromstate,tostate);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getName()
{
   return check_name;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public int update(String event,int value)
{
   if (value == 0) return value;
   
   Integer evtno = event_set.get(event);
   if (evtno == null) return value;
   
   int nval = 0;
   int sv = 1;
   for (int i = 0; i < event_set.size(); ++i) {
      if ((value & sv) != 0) {
         nval |= transition_set.getNextSet(i,evtno);
       }
      sv <<= 1;
    }
    
   return nval;
}




@Override public Value getInitialState()
{
   return initial_state;
}


@Override public Value getValueForOrdinal(int i)
{
   return numvalue_set.get(i);
}



protected Value getState(String nm)
{
   return value_set.get(nm);
}



/********************************************************************************/
/*                                                                              */
/*      State representation                                                    */
/*                                                                              */
/********************************************************************************/

private class State implements Value {
   
   private String state_name;
   private int state_value;
   
   private State(String name,int value) {
      state_name = name;
      state_value = value;
    }
   
   @Override public IfaceSafetyCheck getSafetyCheck()   { return SafetyCheck.this; }
   @Override public String toString()                   { return state_name; }
   @Override public int ordinal()                       { return state_value; }

}       // end of inner class State




/********************************************************************************/
/*                                                                              */
/*      Transition set                                                          */
/*                                                                              */
/********************************************************************************/

private class Transitions {
   
   private List<Transition> all_transitions;
   private int [][] event_map;
   
   Transitions() {
      all_transitions = new ArrayList<>();
      event_map = null;
    }
   
   void add(Value from,String evt,Value to) {
      all_transitions.add(new Transition(from,evt,to));
    }
   
   void addDefault(Value from,Value to) {
      all_transitions.add(new Transition(from,null,to));
    }
   
   int getNextSet(int from,int evtno) {
      if (event_map == null) buildEventMap();
      return event_map[from][evtno];
    }
   
   private synchronized void buildEventMap() {
      if (event_map != null) return;
      
      int nst = value_set.size();
      int nevt = event_set.size();
      event_map = new int[nst][nevt];
      for (int i = 0; i < nst; ++i) {
         for (int j = 0; j < nevt; ++j) {
            event_map[i][j] = 0;
          }
       }
      for (Transition t : all_transitions) {
         if (t.getEvent() == null) continue;
         int fvl = t.getFrom().ordinal();
         int eno = event_set.get(t.getEvent());
         int tvl = t.gotTo().ordinal();
         event_map[fvl][eno] = 1 << tvl;
       }
      for (Transition t : all_transitions) {
         if (t.getEvent() != null) continue;
         int fvl = t.getFrom().ordinal();
         int tvl = t.gotTo().ordinal();
         for (int i = 0; i < nevt; ++i) {
            if (event_map[fvl][i] == 0) {
               event_map[fvl][i] = 1 << tvl;
             }
          }
       }
    }
   
}       // end of inner class Transitions



private static class Transition {
   
   private Value from_state;
   private String for_event;
   private Value to_state;
   
   Transition(Value from,String evt,Value to) {
      from_state = from;
      for_event = evt;
      to_state = to;
    }
   
   Value getFrom()              { return from_state; }
   String getEvent()            { return for_event; }
   Value gotTo()                { return to_state; }
   
}


}       // end of class SafetyCheck




/* end of SafetyCheck.java */

