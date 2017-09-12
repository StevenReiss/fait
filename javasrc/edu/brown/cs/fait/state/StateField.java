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
import edu.brown.cs.ivy.jcode.JcodeField;
import edu.brown.cs.ivy.jcode.JcodeMethod;

import java.util.*;



class StateField implements StateConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl			fait_control;
private Map<JcodeField,IfaceValue>	field_map;
private Set<JcodeField>			anonymous_fields;
private Map<JcodeField,Set<FaitLocation>> field_uses;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

StateField(FaitControl fc)
{
   fait_control = fc;
   field_map = new HashMap<JcodeField,IfaceValue>();
   anonymous_fields = new HashSet<JcodeField>();
   field_uses = new HashMap<JcodeField,Set<FaitLocation>>();
}



/********************************************************************************/
/*										*/
/*	Field access methods							*/
/*										*/
/********************************************************************************/

IfaceValue getFieldValue(StateBase state,JcodeField fld,IfaceValue base,boolean thisref,FaitLocation src)
{
   IfaceValue v0 = null;

   if (src != null) {
      synchronized (field_uses) {
	 Set<FaitLocation> uses = field_uses.get(fld);
	 if (uses == null) {
	    uses = new HashSet<FaitLocation>();
	    field_uses.put(fld,uses);
	  }
	 uses.add(src);
       }
    }

   if (thisref) {
      v0 = state.getFieldValue(fld);
      if (v0 != null) {
	 return v0;
       }
    }

   boolean allok = true;
   if (base != null && !anonymous_fields.contains(fld) && fait_control.isProjectClass(fld.getDeclaringClass())) {
      for (IfaceEntity ent : base.getEntities()) {
	 IfaceValue fv = (IfaceValue) ent.getFieldValue(fld);
	 if (fv != null) {
	    if (v0 == null) v0 = fv;
	    else v0 = v0.mergeValue(fv);
	  }
	 else {
	    allok = false;
	  }
       }
      if (allok && v0 != null) return v0;
    }

   synchronized (field_map) {
      anonymous_fields.add(fld);
      v0 = field_map.get(fld);
      if (v0 == null) {
	 v0 = fait_control.findInitialFieldValue(fld,true);
	 field_map.put(fld,v0);
       }
      if (!allok) {
	 IfaceValue v1 = fait_control.findInitialFieldValue(fld,true);
	 v0 = v0.mergeValue(v1);
	 field_map.put(fld,v0);
       }
    }

   return v0;
}




boolean setFieldValue(StateBase st,JcodeField fld,
      boolean thisref,IfaceValue v0,IfaceValue base,FaitLocation src)
{
   if (v0.mustBeNull()) {
      v0 = v0.restrictByType(fld.getType(),false,src);
    }

   JcodeMethod fm = src.getMethod();

   if (thisref || fm.isStaticInitializer()) {
      st.setFieldValue(fld,v0);
    }

   IfaceValue v1 = field_map.get(fld);
   if (v1 == null) {
      v1 = v0;
      field_map.put(fld,v0);
    }

   boolean chng = false;

   if (base != null && fait_control.isProjectClass(fld.getDeclaringClass())) {
      for (IfaceEntity ent : base.getEntities()) {
	 IfaceValue vb = (IfaceValue) ent.getFieldValue(fld);
	 if (vb == null) ent.setFieldContents(v0,fld);
	 else chng |= ent.addToFieldContents(v0,fld);
       }
    }
   else {
      synchronized (field_map) {
	 anonymous_fields.add(fld);
       }
    }

   synchronized(field_map) {
      IfaceValue s1 = field_map.get(fld);
      if (s1 == null) {
	 if (!fait_control.isProjectClass(fld.getDeclaringClass()))
	    s1 = fait_control.findInitialFieldValue(fld,true);
       }
      if (s1 == null) {
	 field_map.put(fld,s1);
       }
      else {
	 IfaceValue s2 = s1.mergeValue(v0);
	 if (s2 != s1) {
	    field_map.put(fld,s2);
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

void handleFieldChanged(JcodeField fld)
{
   synchronized (field_uses) {
      Set<FaitLocation> uses = field_uses.get(fld);
      if (uses == null) return;
      // queue each element in use
    }
}




}	// end of class StateField




/* end of StateField.java */

