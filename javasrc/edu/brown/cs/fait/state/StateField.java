/********************************************************************************/
/*										*/
/*		StateField.java 						*/
/*										*/
/*	Field manipulations							*/
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



class StateField implements StateConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl		        fait_control;
private Map<String,IfaceValue>      	field_map;
private Set<String>		        anonymous_fields;
private Map<String,Set<IfaceLocation>>	field_uses;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

StateField(IfaceControl fc)
{
   fait_control = fc;
   field_map = new HashMap<>();
   anonymous_fields = new HashSet<>();
   field_uses = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Field access methods							*/
/*										*/
/********************************************************************************/

IfaceValue getFieldValue(StateBase state,IfaceField fld,IfaceValue base,boolean thisref,IfaceLocation src)
{
   IfaceType ctyp = fld.getDeclaringClass();
   IfaceValue v0 = null;
   String key = fld.getKey();

   if (src != null) {
      // add location to set of uses of the field
      synchronized (field_uses) {
	 Set<IfaceLocation> uses = field_uses.get(key);
	 if (uses == null) {
	    uses = new HashSet<IfaceLocation>();
	    field_uses.put(key,uses);
	  }
	 uses.add(src);
       }
    }

   if (thisref) {
      // use local value from state if it is available
      v0 = state.getFieldValue(fld);
      if (v0 != null) {
	 return v0;
       }
    }

   boolean allok = true;
   if (v0 == null) {
      // get value from the base entities if appropriate
      if (base != null && !anonymous_fields.contains(key) && fait_control.isProjectClass(ctyp)) {
         for (IfaceEntity ent : base.getEntities()) {
            IfaceValue fv = ent.getFieldValue(key);
            if (fv != null) {
               if (v0 == null) v0 = fv;
               else v0 = v0.mergeValue(fv);
             }
            else {
               // some entities doesn't have a value for this field, ignore this approach
               allok = false;
             }
          }
         if (!allok) v0 = null;
       }
    }

   if (v0 == null) {
      // if we still don't have a value, get the global value for the field
      synchronized (field_map) {
         anonymous_fields.add(key);
         v0 = field_map.get(key);
         if (v0 == null) {
            v0 = fait_control.findInitialFieldValue(fld,true);
            field_map.put(key,v0);
          }
         if (!allok) {
            IfaceValue v1 = fait_control.findInitialFieldValue(fld,true);
            v0 = v0.mergeValue(v1);
            field_map.put(key,v0);
          }
       }
    }
   
   FaitLog.logD1("Access Field " + fld.getFullName() + " " + base);
   FaitLog.logD1("   Return Value " + v0);
   
   return v0;
}






boolean setFieldValue(StateBase st,IfaceField fld,
      boolean thisref,IfaceValue v0,IfaceValue base,IfaceLocation src)
{
   IfaceType ctyp = fld.getDeclaringClass();
   IfaceType ftyp = fld.getType();
   String key = fld.getKey();
   boolean vol = fld.isVolatile();
   
   if (v0.mustBeNull()) {
      v0 = v0.restrictByType(ftyp);
    }
   
   FaitLog.logD1("Set Field " + fld.getFullName() + " " + base);
   FaitLog.logD1("   Add Value " + v0);

   IfaceMethod fm = src.getMethod();

   if (thisref || fm.isStaticInitializer()) {
      // set in state if local reference
      if (!vol && v0 != null) st.setFieldValue(fld,v0);
    }

   boolean chng = false;

   if (base != null && fait_control.isProjectClass(ctyp)) {
      // set the value in all entities
      for (IfaceEntity ent : base.getEntities()) {
	 IfaceValue vb = ent.getFieldValue(key);
	 if (vb == null) ent.setFieldContents(v0,fld);
	 else chng |= ent.addToFieldContents(v0,fld);
       }
    }
   else {
      synchronized (field_map) {
	 anonymous_fields.add(key);
       }
    }

   synchronized(field_map) {
      IfaceValue s1 = field_map.get(key);
      if (s1 == null) {
	 if (!fait_control.isProjectClass(ctyp))
	    s1 = fait_control.findInitialFieldValue(fld,true);
       }
      if (s1 == null) {
	 field_map.put(key,v0);
       }
      else {
	 IfaceValue s2 = s1.mergeValue(v0);
	 if (s2 != s1) {
	    field_map.put(key,s2);
	    chng = true;
	  }
       }
    }

   if (chng) handleFieldChanged(fld);

   return chng;
}



/********************************************************************************/
/*										*/
/*	Propogate field changes 						*/
/*										*/
/********************************************************************************/

void handleFieldChanged(IfaceField fld)
{
   synchronized (field_uses) {
      Set<IfaceLocation> uses = field_uses.get(fld.getKey());
      if (uses == null) return;
      for (IfaceLocation loc : uses) {
         fait_control.queueLocation(loc);
       }
    }
}



void handleUpdates(IfaceUpdater upd)
{
   for (Iterator<Map.Entry<String,Set<IfaceLocation>>> it = field_uses.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String,Set<IfaceLocation>> ent = it.next();
      boolean gone = true;
      for (Iterator<IfaceLocation> it1 = ent.getValue().iterator(); it1.hasNext(); ) {
         IfaceLocation loc = it1.next();
         if (upd.isLocationRemoved(loc)) {
            it1.remove();;
          }
         else gone = false;
       }
      if (gone) it.remove();
    }
}


}	// end of class StateField




/* end of StateField.java */

