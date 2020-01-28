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
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;


abstract class EntityObject extends EntityBase
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceType	data_type;
private Map<String,IfaceValue> field_map;
private IfacePrototype  proto_handler;

private IfaceValue	array_value;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected EntityObject(IfaceType dt,IfacePrototype ptyp)
{
   data_type = dt;
   field_map = new HashMap<>(4);
   array_value = null;
   proto_handler = ptyp;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceType getDataType()		{ return data_type; }

@Override public IfacePrototype getPrototype()          { return proto_handler; }




/********************************************************************************/
/*										*/
/*	Field mehtods								*/
/*										*/
/********************************************************************************/

@Override public void setFieldContents(IfaceValue fv,String key)
{
   synchronized (field_map) {
      field_map.put(key,fv);
    }
}


@Override public boolean addToFieldContents(IfaceValue fv,String key)
{
   if (fv == null) return false;
   
   synchronized (field_map) {
      IfaceValue v1 = field_map.get(key);
      IfaceValue v2 = fv.mergeValue(v1);
      if (v1 == v2) return false;
      
      field_map.put(key,v2);
    }
   
   return true;
}


@Override public IfaceValue getFieldValue(String fld)
{
   synchronized (field_map) {
      return field_map.get(fld);
    }
}



protected void copyFields(EntityObject toobj)
{
   for (Map.Entry<String,IfaceValue> ent : field_map.entrySet()) {
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



@Override public synchronized boolean addToArrayContents(IfaceValue fv,IfaceValue idx,IfaceLocation loc)
{
   if (fv == null) return false;
   fv = fv.mergeValue(array_value);
   if (fv == array_value) return false;
   array_value = fv;
   return true;
}


@Override public boolean replaceArrayContents(IfaceValue arr,IfaceLocation loc)
{
   return addToArrayContents(arr,null,loc);
}



@Override public IfaceValue getArrayValue(IfaceValue idx,IfaceControl ctl)
{
   if (array_value != null) return array_value;
   if (proto_handler != null) return proto_handler.getArrayContents(idx);
   return array_value;
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected void outputLocalXml(IvyXmlWriter xw)
{
   xw.field("KIND","OBJECT");
   if (proto_handler != null) xw.field("ISPROTO",true);
   if (array_value != null) xw.field("ISARRAY",true);
   
   for (Map.Entry<String,IfaceValue> ent : field_map.entrySet()) {
      xw.begin("FIELD");
      xw.field("NAME",ent.getKey());
      xw.text(ent.getValue().toString());
      xw.end("FIELD");
    }
}






}	// end of class EntityObject




/* end of EntityObject.java */

