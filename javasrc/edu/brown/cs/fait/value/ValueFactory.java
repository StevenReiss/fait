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
import edu.brown.cs.ivy.file.ConcurrentHashSet;

import java.util.*;




public class ValueFactory implements ValueConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<IfaceType,ValueBase>	any_map;
private ConcurrentHashSet<ValueInt>	range_map;
private Map<IfaceType,ValueBase>	null_map;
private Map<IfaceType,ValueObject>	empty_map;

private Map<IfaceEntitySet,List<ValueObject>> object_map;

private Map<IfaceType,Map<Object,List<ValueRef>>> ref_map;

private ValueBase			string_value;
private ValueBase			null_value;
private ValueBase			bad_value;
private ValueBase			main_value;

private IfaceControl			fait_control;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ValueFactory(IfaceControl fc)
{
   fait_control = fc;
   any_map = new HashMap<>();
   range_map = new ConcurrentHashSet<>();
   null_map = new HashMap<>();
   empty_map = new HashMap<>();
   object_map = new WeakHashMap<>();
   ref_map = new WeakHashMap<>();

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

public ValueBase anyValue(IfaceType typ)
{
   synchronized (any_map) {
      if (typ == null) return anyObject();
      ValueBase cv = any_map.get(typ);
      if (cv == null) {
	 if (typ.isPrimitiveType()) {
	    if (typ.isFloatingType()) {
	       cv = new ValueFloat(this,typ);
	     }
	    else if (typ.isVoidType()) {
	       cv = new ValueObject(this,typ,fait_control.createEmptyEntitySet(),FaitAnnotation.NON_NULL);
	     }
	    else {
	       cv = new ValueInt(this,typ);
	     }
	  }
	 else if (typ.isStringType()) {
	    cv = constantString();
	  }
	 else {
            IfaceEntity ent = fait_control.findFixedEntity(typ);
            IfaceEntitySet eset = fait_control.createSingletonSet(ent);
            cv = objectValue(typ,eset,FaitAnnotation.NULLABLE);
            // cv = new ValueObject(this,typ,fait_control.createEmptyEntitySet(),FaitAnnotation.NULLABLE);
          }	
         any_map.put(typ,cv);
       }
      return cv;
    }
}




public ValueBase rangeValue(IfaceType typ,Long v0,Long v1)
{
   if (typ.isFloatingType()) {
      if (v0 != null && v1 != null)
	 return floatRangeValue(typ,(double) v0,(double) v1);
      else
	 return anyValue(typ);
    }
   if (v0 != null && v0.equals(v1)) {
      typ = fait_control.findConstantType(typ,v0);
    }
   if (v0 != null && v1 != null && v0 > v1) {
      FaitLog.logW("Inconsistent range value: " + v0 + " " + v1);
      return anyValue(typ);
    }

   ValueInt cv = new ValueInt(this,typ,v0,v1);
   ValueInt cv1 = range_map.addIfAbsent(cv);
   if (cv1 == null || cv1 == cv) {
      if (FaitLog.isTracing())
         FaitLog.logD("Create new INTVALUE " + typ + " " + v0 + " " + v1);
      cv1 = cv;
      // if (v0 != null && v0 == 0 && v1 != null && v1 == 0) {
	 // FaitLog.logD("RECHECK 0 " + typ + " " + typ.hashCode());
       // }
    }

   return cv1;
}






public ValueBase floatRangeValue(IfaceType typ,double v0,double v1)
{
   if (v0 == v1) {
     typ = fait_control.findConstantType(typ,v0);
    }

   return anyValue(typ);
   // return new ValueFloat(this,typ);
}




public ValueBase objectValue(IfaceType typ,IfaceEntitySet ss,IfaceAnnotation ... flags)
{
   return objectValue(typ,ss,null,flags);
}



public ValueBase objectValue(IfaceType typ,IfaceEntitySet ss,String conststr,IfaceAnnotation ... flags)
{
   if (ss.isEmpty()) return emptyValue(typ,flags);

   typ = typ.getAnnotatedType(flags);
   ss = ss.restrictByType(typ);

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
	 if (cvo.getDataType() == typ) return cvo;
       }
      ValueObject cv = null;
      if (typ.isStringType()) {
	 if (conststr != null)
	    cv = new ValueString(this,typ,conststr,ss);
	 else
	    cv = new ValueString(this,typ,ss,flags);
       }
      else {
	 cv = new ValueObject(this,typ,ss,flags);
       }
      l.add(cv);

      return cv;
    }
}




public ValueBase emptyValue(IfaceType typ,IfaceAnnotation ... flags)
{
   if (typ == null) return nullValue();
   ValueObject cvo = null;
   IfaceType typ1 = typ.getAnnotatedType(flags);

   synchronized (empty_map) {
      cvo = empty_map.get(typ1);
      if (cvo == null) {
	  cvo = new ValueObject(this,typ1,fait_control.createEmptyEntitySet());
	  empty_map.put(typ1,cvo);
       }
      return cvo;
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
      IfaceType fdt = fait_control.findConstantType("java.lang.String","");
      IfaceEntity sb = fait_control.findFixedEntity(fdt);
      IfaceEntitySet ss = fait_control.createSingletonSet(sb);
      string_value = objectValue(fdt,ss);
    }

   return string_value;
}


public IfaceValue constantStringNonNull()
{
   IfaceValue v0 = constantString();
   return v0.forceNonNull();
}


public ValueBase constantString(String v)
{
   if (v == null) return constantString();

   IfaceType fdt = fait_control.findConstantType("java.lang.String",v);
   IfaceEntity src = fait_control.findStringEntity(v);
   IfaceEntitySet ss = fait_control.createSingletonSet(src);

   return objectValue(fdt,ss,v);
}



public ValueBase mainArgs()
{
   if (main_value == null) {
      IfaceType stringtype = fait_control.findDataType("java.lang.String");
      stringtype = fait_control.findConstantType(stringtype,"Hello World");
      
      IfaceEntity ssrc = fait_control.findArrayEntity(stringtype,null);
      ValueBase cv = nativeValue(stringtype);
      cv = cv.forceNonNull();
      ssrc.setArrayContents(cv);
      IfaceEntitySet sset = fait_control.createSingletonSet(ssrc);
      
      main_value = objectValue(ssrc.getDataType(),sset,FaitAnnotation.NON_NULL);
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
      IfaceType typ = fait_control.findConstantType("java.lang.Object",null);
      null_value = emptyValue(typ);
    }

   return null_value;
}



public ValueBase nullValue(IfaceType dt)
{
   if (dt == null) return nullValue();
   
   synchronized (null_map) {
      ValueBase cv = null_map.get(dt);
      if (cv == null) {
         IfaceType dt1 = fait_control.findConstantType(dt,null);
	 cv = emptyValue(dt1,FaitAnnotation.MUST_BE_NULL);
	 null_map.put(dt,cv);
         null_map.put(dt1,cv);
       }
      return cv;
    }
}



public ValueBase badValue()
{
   synchronized (this) {
      if (bad_value == null) {
	 bad_value = new ValueBad(this,fait_control.findDataType("void"));
       }
    }
   // FaitLog.logD("Generate Bad Value");
   return bad_value;
}


public ValueBase markerValue(IfaceProgramPoint pt,Object data)
{
   IfaceType ty = fait_control.findDataType("void");

   return new ValueMarker(this,ty,pt,data);
}



/********************************************************************************/
/*										*/
/*	Native value support							*/
/*										*/
/********************************************************************************/

public ValueBase nativeValue(IfaceType typ)
{
   if (typ.isPrimitiveType()) return anyValue(typ);

   if (typ.isInterfaceType() || typ.isAbstract() || typ.isJavaLangObject())
      return mutableValue(typ);

   IfaceEntity ent = fait_control.findFixedEntity(typ);
   IfaceEntitySet eset = fait_control.createSingletonSet(ent);

   return objectValue(typ,eset,FaitAnnotation.NULLABLE);
}



public ValueBase mutableValue(IfaceType typ)
{
   if (typ == null) return anyObject();
   if (typ.isPrimitiveType()) return anyValue(typ);

   IfaceEntity ent = fait_control.findMutableEntity(typ);
   IfaceEntitySet eset = fait_control.createSingletonSet(ent);

   return objectValue(typ,eset,FaitAnnotation.NULLABLE);
}



public ValueBase anyObject()
{
   return anyValue(fait_control.findDataType("java.lang.Object"));
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

public IfaceValue initialFieldValue(IfaceField fld,boolean nat)
{
   IfaceType ftyp = fld.getType();
   if (ftyp == null) return nullValue();
   IfaceType ctyp = fld.getDeclaringClass();
   String fnm = fld.getFullName();
   if (ctyp.getName().startsWith("java.lang.")) nat = true;
   if (!fait_control.isProjectClass(ctyp)) nat = true;
   ValueBase s0 = null;

   if (fld.isStatic() && fld.isFinal()) {
       IfaceValue vinit = fld.getConstantValue();
       if (vinit != null) return vinit;
    }

   if (nat) {
      boolean nonnull = false;			// specialize as needed
      ftyp = ftyp.getComputedType(FaitTypeOperator.DONEINIT);
      if (ctyp.getName().equals("java.lang.System")) {
	 nonnull = true;
	 if (fnm.equals("in")) {
	    ftyp = fait_control.findDataType("java.io.FileInputStream");
            ftyp = ftyp.getComputedType(FaitTypeOperator.DONEINIT);
          }
	 else if (fnm.equals("out") || fnm.equals("err")) {
	    ftyp = fait_control.findDataType("java.io.PrintStream");
            ftyp = ftyp.getComputedType(FaitTypeOperator.DONEINIT);
          }
       }
      else if (ctyp.getName().equals("java.lang.String")) nonnull = true;
      else if (ctyp.getName().equals("java.lang.ThreadGroup")) nonnull = false;
      else if (ctyp.getName().equals("java.lang.ClassLoader") &&
	    fnm.equals("parent")) nonnull = false;
      else if (ctyp.getName().startsWith("java.lang.")) nonnull = true;
      if (ftyp.isAbstract()) s0 = mutableValue(ftyp);
      // else s0 = nativeValue(ftyp);
      else s0 = mutableValue(ftyp);
      if (nonnull) s0 = s0.forceNonNull();
    }
   else {
      if (ftyp.isPrimitiveType()) {
	 s0 = anyValue(ftyp);
       }
      else {
	 // s0 = emptyValue(ftyp,FaitAnnotation.NULLABLE);
         s0 = nullValue(ftyp);
       }
    }

   return s0;
}


public ValueBase refValue(IfaceType dt,int slot)
{
   Map<Object,List<ValueRef>> mm = null;
   synchronized (ref_map) {
      mm = ref_map.get(dt);
      if (mm == null) {
         mm = new HashMap<>();
         ref_map.put(dt,mm);
       }
    }  
   synchronized (mm) {
      List<ValueRef> lr = mm.get(slot);
      if (lr == null) {
         lr = new ArrayList<>();
         ValueRef vr = new ValueRef(this,dt,slot,null,null,null);
         lr.add(vr);
         mm.put(slot,lr);
         return vr;
       }
      else return lr.get(0);
    }
}


public ValueBase refValue(IfaceType dt,IfaceValue base,IfaceField fld)
{
   if (base == null && fld != null && !fld.isStatic()) {
      FaitLog.logE("Illegal field reference");
    }
   if (base != null && base.isBad()) {
      FaitLog.logE("Bad base value");
    }
   if (fld == null) {
      FaitLog.logE("Field reference to non-existant field");
    }
   
   Map<Object,List<ValueRef>> mm = null;
   synchronized (ref_map) {
      mm = ref_map.get(dt);
      if (mm == null) {
         mm = new HashMap<>();
         ref_map.put(dt,mm);
       }
    }  
   List<ValueRef> lr = null;
   synchronized (mm) {
      lr = mm.get(fld);
      if (lr == null) {
         lr = new ArrayList<>();
         mm.put(fld,lr);
       }
    }
   synchronized (lr) {
      for (ValueRef vr : lr) {
         if (vr.getRefBase() == base) return vr;
       }
      ValueRef nvr = new ValueRef(this,dt,NO_REF,base,fld,null);
      lr.add(nvr);
      return nvr;
    }
}


public ValueBase refValue(IfaceType dt,IfaceValue base,IfaceValue idx)
{
   if (idx == null) {
      FaitLog.logE("Index reference to non-existant index");
    }
   
   Map<Object,List<ValueRef>> mm = null;
   synchronized (ref_map) {
      mm = ref_map.get(dt);
      if (mm == null) {
         mm = new HashMap<>();
         ref_map.put(dt,mm);
       }
    }  
   List<ValueRef> lr = null;
   synchronized (mm) {
      lr = mm.get(idx);
      if (lr == null) {
         lr = new ArrayList<>();
         mm.put(idx,lr);
       }
    }
   synchronized (lr) {
      for (ValueRef vr : lr) {
         if (vr.getRefBase() == base) return vr;
       }
      ValueRef nvr = new ValueRef(this,dt,NO_REF,base,null,idx);
      lr.add(nvr);
      return nvr;
    }
}




/********************************************************************************/
/*										*/
/*	Methods for updating values on incremental change			*/
/*										*/
/********************************************************************************/

public void handleUpdates(IfaceUpdater upd)
{
   synchronized (object_map) {
      Collection<Map.Entry<IfaceEntitySet,List<ValueObject>>> ents = new ArrayList<>(object_map.entrySet());
      for (Map.Entry<IfaceEntitySet,List<ValueObject>> ent : ents) {
	 IfaceEntitySet oes = ent.getKey();
	 IfaceEntitySet nes = upd.getNewEntitySet(oes);
	 if (nes != null && nes != oes) {
	    for (ValueObject vo : ent.getValue()) {
	       ValueBase nvo = objectValue(vo.getDataType(),nes);
	       upd.addToValueMap(vo,nvo);
	     }
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Package access methods							*/
/*										*/
/********************************************************************************/

IfaceControl getFaitControl()			{ return fait_control; }




}	// end of class ValueFactory




/* end of ValueFactory.java */
