/********************************************************************************/
/*										*/
/*		EntityArray.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.fait.entity;


import edu.brown.cs.fait.iface.*;



class EntityArray extends EntityBase
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitDataType		base_class;
private FaitDataType		array_class;
private IfaceValue		array_values;
private IfaceValue		size_value;
private boolean 		null_stored;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

EntityArray(FaitControl ctrl,FaitDataType cls,IfaceValue size)
{
   base_class = cls;
   array_class = ctrl.findDataType("[" + cls.getDescriptor());

   if (size == null) size = ctrl.findAnyValue(ctrl.findDataType("I"));
   size_value = size;

   if (base_class.isPrimitive()) {
      array_values = ctrl.findRangeValue(base_class,0,0);
    }
   else {
      array_values = ctrl.findNullValue(base_class);
    }

   null_stored = false;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public FaitDataType getDataType()
{
   return array_class;
}


/********************************************************************************/
/*										*/
/*	Array Content Methods							*/
/*										*/
/********************************************************************************/

@Override public void setArrayContents(IfaceValue v)
{
   array_values = v;
   null_stored = v.canBeNull();
}



@Override public synchronized boolean addToArrayContents(IfaceValue v,IfaceValue idx,FaitLocation loc)
{
   if (v != null) {
      null_stored = v.canBeNull();
      v = v.restrictByType(base_class,false,loc);
    }

   v = array_values.mergeValue(v);
   if (v == array_values) return false;

   array_values = v;

   return true;
}



@Override public FaitValue getArrayValue(IfaceValue idx)
{
   return array_values;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void handleUpdates(IfaceUpdater upd)
{
   if (array_values != null) {
      IfaceValue fv = upd.getNewValue(array_values);
      if (fv != null) array_values = fv;
    }
   if (size_value != null) {
      IfaceValue fv = upd.getNewValue(size_value);
      if (fv != null) size_value = fv;
    }
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("[Array ");
   buf.append(array_class.getName());
   buf.append(" :: ");
   buf.append(array_values.toString());
   buf.append(" ]");
   return buf.toString();
}





}	// end of class EntityArray




/* end of EntityArray.java */

