/********************************************************************************/
/*										*/
/*		EntityBase.java 						*/
/*										*/
/*	Basic entity holder abstract class					*/
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


abstract class EntityBase implements IfaceEntity, EntityConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int		entity_id;

private static int		entity_counter = 0;
private static List<EntityBase> all_entities = new ArrayList<EntityBase>();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected EntityBase()
{
   synchronized (EntityBase.class) {
      entity_id = entity_counter++;
      all_entities.add(this);
    }
}



/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

@Override public int getId()				{ return entity_id; }

@Override public IfaceType getDataType()		{ return null; }
@Override public IfaceLocation getLocation()	        { return null; }

@Override public UserEntity getUserEntity()		{ return null; }
@Override public IfacePrototype getPrototype()          { return null; }

@Override public boolean isNative()		{ return false; }
@Override public boolean isFixed()              { return false; }
@Override public boolean isUserEntity()         { return false; }
@Override public boolean isMutable()            { return false; }




/********************************************************************************/
/*										*/
/*	Static access methods							*/
/*										*/
/********************************************************************************/

static EntityBase getEntity(int id)
{
   return all_entities.get(id);
}




/********************************************************************************/
/*										*/
/*	IFaceEntity methods							*/
/*										*/
/********************************************************************************/

public Collection<IfaceEntity> mutateTo(IfaceType dt,EntityFactory factory)
{
   return null;
}


@Override public final void setFieldContents(IfaceValue fv,IfaceField fld)	
{
   if (fld != null) setFieldContents(fv,fld.getKey());
}

@Override public void setFieldContents(IfaceValue fv,String key)        { }


@Override public boolean addToFieldContents(IfaceValue fv,String key)
{
   return false;
}

@Override public final boolean addToFieldContents(IfaceValue fv,IfaceField fld)
{
   if (fld == null) return false;
   return addToFieldContents(fv,fld.getKey());
}

@Override public IfaceValue getFieldValue(String key) 	
{
   return null;
}

@Override public final IfaceValue getFieldValue(IfaceField fld)
{
   if (fld == null) return null;
   return getFieldValue(fld.getKey());
}

@Override public void setArrayContents(IfaceValue fv)			{ }
@Override public IfaceValue getArrayValue(IfaceValue id,IfaceControl ctl)       { return null; }


@Override public List<IfaceValue> getContents(List<IfaceValue> rslt)
{
   IfaceValue v0 = getArrayValue(null,null);
   if (v0 != null) {
      if (rslt == null) rslt = new ArrayList<>();
      rslt.add(v0);
    }
   IfacePrototype pt = getPrototype();
   if (pt != null) {
      rslt = pt.getContents(rslt);
    }
   return rslt;
}
@Override public boolean addToArrayContents(IfaceValue fv,IfaceValue idx,IfaceLocation loc)
{
   return false;
}


@Override public boolean setArraySize(IfaceValue sz)               { return false; }





@Override public boolean replaceArrayContents(IfaceValue fv,IfaceLocation loc)
{
   return false;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void handleUpdates(IfaceUpdater upd)		 
{
   if (getPrototype() != null) getPrototype().handleUpdates(upd);
}



/********************************************************************************/
/*                                                                              */
/*      Lambda-related methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public Map<Object,IfaceValue> getBindings()           { return null; }
@Override public String getMethodName()                         { return null; }
@Override public IfaceMethod getMethod()                        { return null; }
@Override public boolean isFunctionRef()                        { return false; }


/********************************************************************************/
/*                                                                              */
/*      Clean up methods                                                        */
/*                                                                              */
/********************************************************************************/

static void clearAll()
{
   entity_counter = 0;
   all_entities.clear();
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw,IfaceValue relativevalue)
{
   xw.begin("ENTITY");
   xw.field("ID",entity_id);
   if (isFixed()) xw.field("FIXED",true);
   if (isMutable()) xw.field("MUTABLE",true);
   if (isNative()) xw.field("NATIVE",true);
   if (isUserEntity()) xw.field("USER",true);
   
   
   outputLocalXml(xw);
   IfaceType dt = getDataType();
   if (dt != null) dt.outputXml(xw);
   
   IfaceLocation loc = getLocation();
   if (loc != null) {
      loc.outputXml(xw);
    }
   
   if (relativevalue != null) {
      xw.textElement("DESCRIPTION",getDescription(relativevalue));
    }
   
   xw.end("ENTITY");
}


protected void outputLocalXml(IvyXmlWriter xw)          { }


protected String getLocationName()
{
   IfaceLocation loc = getLocation();
   if (loc == null) return null;
   String s = loc.getMethod().getFullName();
   int idx = s.lastIndexOf(".");
   if (idx >= 0) idx = s.lastIndexOf(".",idx-1);
   if (idx > 0) s = s.substring(idx+1);
   int line = loc.getProgramPoint().getLineNumber();
   if (line > 0) s += " @ " + line;
   return s;
}



protected String getRelativeTypeName(IfaceValue val)
{
   if (val.getDataType() == getDataType()) return null;
   String s1 = getDataType().getName();
   String s2 = s1;
   int idx = s1.length();
   for ( ; ; ) {
      int idx1 = s1.lastIndexOf(".",idx);
      if (Character.isUpperCase(s1.charAt(idx1+1))) {
         s2 = s1.substring(idx1+1);
         idx = idx1-1;
         if (idx <= 0) break;
       }
      else break;
    }
   for (IfaceSubtype st : getDataType().getSubtypes()) {
      if (val == null || getDataType().getValue(st) != val.getDataType().getValue(st)) {
         s2 += "@" + getDataType().getValue(st);
       }
    }
   return s2;
}






protected String getDescription(IfaceValue val) 
{
   String s1 = getRelativeTypeName(val);
   if (isFixed()) {
      String s3 = "SYSTEM";
      if (isMutable()) s3 += "*";
      if (s1 != null) s3 += " " + s1;
      return s3;
    }
   String s2 = getLocationName();
   if (s1 == null && s2 == null) {
      if (isFunctionRef()) return "FUNCTION_REF";
      return "OBJECT";
    }
   if (s1 == null && s2 != null) return s2;
   if (s1 != null && s2 == null) return s1;
   return s1 + " " + s2;
}




}	// end of class EntityBase




/* end of EntityBase.java */

