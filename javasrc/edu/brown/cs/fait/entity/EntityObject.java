/********************************************************************************/
/*										*/
/*		EntityObject.java						*/
/*										*/
/*	Generic object entity							*/
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

import java.util.*;

abstract class EntityObject extends EntityBase
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitDataType	data_type;
private Map<FaitField,IfaceValue> field_map;

private boolean 	array_nonnull;
private boolean 	array_canbenull;
private IfaceValue	array_value;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected EntityObject(FaitDataType dt)
{
   data_type = dt;
   field_map = new HashMap<FaitField,IfaceValue>(4);
   array_value = null;
   array_nonnull = false;
   array_canbenull = false;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public FaitDataType getDataType()		{ return data_type; }



/********************************************************************************/
/*										*/
/*	Field mehtods								*/
/*										*/
/********************************************************************************/

@Override public void setFieldContents(IfaceValue fv,FaitField fld)
{
   synchronized (field_map) {
      field_map.put(fld,fv);
    }
}


@Override public boolean addToFieldContents(IfaceValue fv,FaitField fld)
{
   if (fv == null) return false;

   synchronized (field_map) {
      IfaceValue v1 = field_map.get(fld);
      IfaceValue v2 = fv.mergeValue(v1);
      if (v1 == v2) return false;

      field_map.put(fld,v2);
    }

   return true;
}


@Override public FaitValue getFieldValue(FaitField fld)
{
   synchronized (field_map) {
      return field_map.get(fld);
    }
}



protected void copyFields(EntityObject toobj)
{
   for (Map.Entry<FaitField,IfaceValue> ent : field_map.entrySet()) {
      toobj.setFieldContents(ent.getValue(),ent.getKey());
    }
}



/********************************************************************************/
/*										*/
/*	Array management methods						*/
/*										*/
/********************************************************************************/

@Override public synchronized void setArrayContents(IfaceValue fv)
{
   array_value = fv;
}



@Override public synchronized boolean addToArrayContents(IfaceValue fv,IfaceValue idx,FaitLocation loc)
{
   if (fv == null) return false;
   fv = fv.mergeValue(array_value);
   if (fv == array_value) return false;
   array_value = fv;
   return true;
}



@Override public FaitValue getArrayValue(IfaceValue idx)
{
   if (array_nonnull && !array_canbenull && array_value != null)
      return array_value.forceNonNull();

   return array_value;
}




}	// end of class EntityObject




/* end of EntityObject.java */

