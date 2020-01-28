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


import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.ivy.xml.IvyXmlWriter;



class EntityArray extends EntityBase
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceType		base_class;
private IfaceType		array_class;
private IfaceValue		array_values;
private IfaceValue		size_value;
private Map<Integer,IfaceValue> known_values;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

EntityArray(IfaceControl ctrl,IfaceType cls,IfaceValue size)
{
   base_class = cls;
   array_class = ctrl.findDataType(cls.getName() + "[]",FaitAnnotation.NON_NULL);

   if (size == null) {
     // size = ctrl.findAnyValue(ctrl.findDataType("int"));
      size = ctrl.findRangeValue(ctrl.findDataType("int"),0L,null);
    }
   else if (!size.getDataType().isNumericType()) {
      // size = ctrl.findAnyValue(ctrl.findDataType("int"));
      size = ctrl.findRangeValue(ctrl.findDataType("int"),0L,null);
    }
    
   size_value = size;

   if (base_class.isPrimitiveType()) {
      array_values = ctrl.findConstantValue(base_class,0);
    }
   else {
      array_values = ctrl.findNullValue(base_class);
    }
   
   known_values = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceType getDataType()
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
}



@Override public synchronized boolean addToArrayContents(IfaceValue v,IfaceValue idx,IfaceLocation loc)
{
   if (v != null) {
      v = v.restrictByType(base_class);
    }

   if (known_values != null) {
      if (idx == null) known_values = null;
      else {
         Integer idxv = idx.getIndexValue();
         if (idxv != null) {
            IfaceValue iv0 = known_values.get(idxv);
            IfaceValue v1 = v;
            if (iv0 != null) v1 = v1.mergeValue(iv0);
            known_values.put(idxv,v1);
          }
         else {
            known_values = null;
          }
       }
    }
   
   IfaceValue v1 = array_values.mergeValue(v);
   if (v1 == array_values) return false;
   if (v1.isBad()) {
      FaitLog.logE("Bad data on array merge");
      v1 = array_values.mergeValue(v);
      // create any value of result type
    }

   array_values = v1;

   return true;
}


@Override public synchronized boolean replaceArrayContents(IfaceValue v,IfaceLocation loc)
{
   size_value = null;
   return addToArrayContents(v,null,loc);
}

@Override public boolean setArraySize(IfaceValue sz)
{
   if (sz == null) size_value = null;
   else if (size_value != null) {
      IfaceValue nv = size_value.mergeValue(sz);
      if (nv != size_value) {
         size_value = nv;
         return true;
       }
    }
   
   return false;
}



@Override public IfaceValue getArrayValue(IfaceValue idx,IfaceControl ctl)
{
   Map<Integer,IfaceValue> known = known_values;
   if (known != null && idx != null) {
      Integer idxv = idx.getIndexValue();
      if (idxv != null) {
         IfaceValue v1 = known.get(idxv);
         if (v1 != null) return v1;
       }
    }
   
   return array_values;
}


@Override public IfaceValue getFieldValue(String fld)
{
   if (fld != null && fld.equals("length") && size_value != null) {
      return size_value;
    }

   return super.getFieldValue(fld);
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
   
   if (getPrototype() != null) getPrototype().handleUpdates(upd);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override protected void outputLocalXml(IvyXmlWriter xw)
{
   xw.field("KIND","ARRAY");
}



@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("[Array ");
   buf.append(array_class.getName());
   // buf.append(" :: ");
   // buf.append(array_values.toString());
   buf.append(" ]");
   return buf.toString();
}





}	// end of class EntityArray




/* end of EntityArray.java */

