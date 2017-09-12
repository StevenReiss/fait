/********************************************************************************/
/*										*/
/*		ValueFactory.java						*/
/*										*/
/*	Factory class for creating and managine values				*/
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



package edu.brown.cs.fait.value;

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeField;

import java.util.*;




public class ValueFactory implements ValueConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<JcodeDataType,ValueBase>	 any_map;
private Map<JcodeDataType,List<ValueInt>> range_map;
private Map<JcodeDataType,ValueBase>	 null_map;
private Map<JcodeDataType,List<ValueObject>> empty_map;

private Map<IfaceEntitySet,List<ValueObject>> object_map;

private ValueBase			string_value;
private ValueBase			null_value;
private ValueBase			bad_value;
private ValueBase			main_value;

private FaitControl			fait_control;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ValueFactory(FaitControl fc)
{
   fait_control = fc;
   any_map = new HashMap<JcodeDataType,ValueBase>();
   range_map = new WeakHashMap<JcodeDataType,List<ValueInt>>();
   null_map = new HashMap<JcodeDataType,ValueBase>();
   empty_map = new HashMap<JcodeDataType,List<ValueObject>>();
   object_map = new WeakHashMap<IfaceEntitySet,List<ValueObject>>();

   string_value = null;
   null_value = null;
   bad_value = null;
   main_value = null;
}




/********************************************************************************/
/*										*/
/*	Methods for creating values						*/
/*										*/
/********************************************************************************/

public ValueBase anyValue(JcodeDataType typ)
{
   synchronized (any_map) {
      ValueBase cv = any_map.get(typ);
      if (cv == null) {
	 if (typ.isPrimitive()) {
	    if (typ.isFloating()) {
	       cv = new ValueFloat(this,typ);
	     }
	    else if (typ.isVoid()) {
	       cv = new ValueObject(this,typ,fait_control.createEmptyEntitySet(),NullFlags.NON_NULL);
	     }
	    else {
	       cv = new ValueInt(this,typ);
	     }
	  }
	else cv = new ValueObject(this,typ,fait_control.createEmptyEntitySet(),NullFlags.CAN_BE_NULL);
	any_map.put(typ,cv);
       }
      return cv;
    }
}




public ValueBase rangeValue(JcodeDataType typ,long v0,long v1)
{
   if (typ.isFloating()) {
      return anyValue(typ);
    }
   if (v1 - v0 > VALUE_MAX_RANGE) {
      return anyValue(typ);
    }

   List<ValueInt> l = null;
   synchronized (range_map) {
      l = range_map.get(typ);
      if (l == null) {
	 l = new ArrayList<ValueInt>();
	 range_map.put(typ,l);
       }
    }
   synchronized (l) {
      for (ValueInt cvi : l) {
	 if (cvi.getMinValue() == v0 && cvi.getMaxValue() == v1) return cvi;
       }
      ValueInt cv = new ValueInt(this,typ,v0,v1);
      l.add(cv);
      return cv;
    }
}




public ValueBase objectValue(JcodeDataType typ,IfaceEntitySet ss,NullFlags flags)
{
   if (ss.isEmpty()) return emptyValue(typ,flags);

   List<ValueObject> l = null;

   synchronized (object_map) {
      l = object_map.get(ss);
      if (l == null) {
	 l = new ArrayList<ValueObject>();
	 object_map.put(ss,l);
       }
    }

   synchronized (l) {
      for (ValueObject cvo : l) {
	 if (cvo.getDataType() == typ && cvo.getNullFlags() == flags) return cvo;
       }
      ValueObject cv = new ValueObject(this,typ,ss,flags);
      l.add(cv);

      return cv;
    }
}




public ValueBase emptyValue(JcodeDataType typ,NullFlags flags)
{
   List<ValueObject> l = null;

   synchronized (empty_map) {
      l = empty_map.get(typ);
      if (l == null) {
	 l = new ArrayList<ValueObject>();
	 empty_map.put(typ,l);
       }
    }

   synchronized (l) {
      for (ValueObject cvo : l) {
	 if (cvo.getNullFlags() == flags) return cvo;
       }
      ValueObject vo = new ValueObject(this,typ,fait_control.createEmptyEntitySet(),flags);
      l.add(vo);

      return vo;
    }
}



/********************************************************************************/
/*										*/
/*	Methods for special values						*/
/*										*/
/********************************************************************************/

public ValueBase constantString()
{
   if (string_value == null) {
      JcodeDataType fdt = fait_control.findDataType("Ljava/lang/String;");
      FaitEntity sb = fait_control.findFixedEntity(fdt);
      IfaceEntitySet ss = fait_control.createSingletonSet(sb);
      string_value = objectValue(fdt,ss,NullFlags.NON_NULL);
    }

   return string_value;
}


public ValueBase constantString(String v)
{
   if (v == null) return constantString();

   JcodeDataType fdt = fait_control.findDataType("Ljava/lang/String;");
   IfaceEntity src = fait_control.findStringEntity(v);
   IfaceEntitySet ss = fait_control.createSingletonSet(src);

   return objectValue(fdt,ss,NullFlags.NON_NULL);
}



public ValueBase mainArgs()
{
   if (main_value == null) {
      JcodeDataType fdt = fait_control.findDataType("[Ljava/lang/String;");
      JcodeDataType sdt = fait_control.findDataType("Ljava/lang/String;");
      IfaceEntity ssrc = fait_control.findArrayEntity(sdt,null);
      ValueBase cv = nativeValue(sdt);
      cv = cv.forceNonNull();
      ssrc.setArrayContents(cv);
      IfaceEntitySet sset = fait_control.createSingletonSet(ssrc);
      main_value = objectValue(fdt,sset,NullFlags.NON_NULL);
    }

   return main_value;
}



/********************************************************************************/
/*										*/
/*	Null and bad values							*/
/*										*/
/********************************************************************************/

public ValueBase nullValue()
{
   if (null_value == null) {
      null_value = emptyValue(fait_control.findDataType("Ljava/lang/Object;"),NullFlags.NEW_NULL);
    }

   return null_value;
}



public ValueBase nullValue(JcodeDataType dt)
{
   synchronized (null_map) {
      ValueBase cv = null_map.get(dt);
      if (cv == null) {
	 cv = emptyValue(dt,NullFlags.NEW_NULL);
	 null_map.put(dt,cv);
       }
      return cv;
    }
}



public ValueBase badValue()
{ 
   synchronized (this) {
      if (bad_value == null) {
	 bad_value = new ValueBad(this,fait_control.findDataType("V"));
       }
    }
   IfaceLog.logD("Generate Bad Value");
   return bad_value;
}



/********************************************************************************/
/*										*/
/*	Native value support							*/
/*										*/
/********************************************************************************/

public ValueBase nativeValue(JcodeDataType typ)
{
   if (typ.isPrimitive()) return anyValue(typ);

   if (typ.isInterface() || typ.isAbstract() || typ.isJavaLangObject())
      return mutableValue(typ);

   IfaceEntity ent = fait_control.findFixedEntity(typ);
   IfaceEntitySet eset = fait_control.createSingletonSet(ent);

   return objectValue(typ,eset,NullFlags.CAN_BE_NULL);
}



public ValueBase mutableValue(JcodeDataType typ)
{
   if (typ.isPrimitive()) return anyValue(typ);

   IfaceEntity ent = fait_control.findMutableEntity(typ);
   IfaceEntitySet eset = fait_control.createSingletonSet(ent);

   return objectValue(typ,eset,NullFlags.CAN_BE_NULL);
}



public ValueBase anyObject()
{
   return anyValue(fait_control.findDataType("Ljava/lang/Object;"));
}



public ValueBase anyNewObject()
{
   ValueObject vo = (ValueObject) anyObject();
   return vo.forceNonNull();
}



/********************************************************************************/
/*										*/
/*	Field value setup							*/
/*										*/
/********************************************************************************/


public ValueBase initialFieldValue(JcodeField fld,boolean nat)
{
   JcodeDataType c = fld.getDeclaringClass();
   if (c.getName().startsWith("java.lang.")) nat = true;
   if (!fait_control.isProjectClass(c)) nat = true;
   ValueBase s0 = null;
   JcodeDataType ftyp = fld.getType();

   if (nat) {
      boolean nonnull = false;			// specialize as needed
      if (c.getName().equals("java.lang.System")) {
	 nonnull = true;
	 if (fld.getName().equals("in"))
	    ftyp = fait_control.findDataType("Ljava/io/FileInputStream;");
	 else if (fld.getName().equals("out") || fld.getName().equals("err"))
	    ftyp = fait_control.findDataType("Ljava/io/PrintStream;");
       }
      else if (c.getName().equals("java.lang.String")) nonnull = true;
      else if (c.getName().equals("java.lang.ThreadGroup")) nonnull = false;
      else if (c.getName().equals("java.lang.ClassLoader") &&
		  fld.getName().equals("parent")) nonnull = false;
      else if (c.getName().startsWith("java.lang.")) nonnull = true;
      if (ftyp.isAbstract()) s0 = mutableValue(ftyp);
      else s0 = nativeValue(ftyp);
      if (nonnull) s0 = s0.forceNonNull();
    }
   else {

      if (ftyp.isPrimitive()) {
	 s0 = anyValue(ftyp);
       }
      else {
	 s0 = emptyValue(fld.getType(),NullFlags.NEW_NULL);
       }
    }

   return s0;
}



/********************************************************************************/
/*										*/
/*	Methods for updating values on incremental change			*/
/*										*/
/********************************************************************************/

public void handleUpdates(IfaceUpdater upd)
{
   synchronized (object_map) {
      for (Map.Entry<IfaceEntitySet,List<ValueObject>> ent : object_map.entrySet()) {
	 IfaceEntitySet nes = upd.getNewEntitySet(ent.getKey());
	 if (nes != null) {
	    for (ValueObject vo : ent.getValue()) {
	       ValueBase nvo = objectValue(vo.getDataType(),nes,vo.getNullFlags());
	       upd.addValueMap(vo,nvo);
	     }
	  }
       }
    }

   // what about numeric values with user entities
}



/********************************************************************************/
/*										*/
/*	Package access methods							*/
/*										*/
/********************************************************************************/

FaitControl getFaitControl()			{ return fait_control; }




}	// end of class ValueFactory




/* end of ValueFactory.java */
