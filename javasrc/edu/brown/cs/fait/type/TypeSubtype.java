/********************************************************************************/
/*                                                                              */
/*              TypeSubtype.java                                                */
/*                                                                              */
/*      Generic implementation of a subtype                                     */
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



package edu.brown.cs.fait.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

abstract class TypeSubtype implements IfaceSubtype, TypeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Combiner combiner_map;
private Combiner restrict_map;
private Map<Attr,Value> attribute_map;
private Map<String,Attr> name_map;
private Checker checker_map;
private int subtype_index;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected TypeSubtype()
{ 
   combiner_map = new Combiner();
   restrict_map = new Combiner();
   attribute_map = new HashMap<>();
   name_map = new HashMap<>();
   checker_map = new Checker();
   subtype_index = -1;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

protected void defineMerge(Value v1,Value v2,Value vr)
{
   combiner_map.addMapping(v1,v2,vr);
   combiner_map.addMapping(v2,v1,vr);
   combiner_map.addMapping(v1,v1,v1);
   combiner_map.addMapping(v2,v2,v2);
}


protected void defineRestrict(Value v1,Value v2,Value vr)
{
   restrict_map.addMapping(v1,v2,vr);
   restrict_map.addMapping(v1,v1,v1);
   restrict_map.addMapping(v2,v2,v2);
}

protected void defineRestrict(Value v1,Value v2,IfaceError er)
{
   restrict_map.addMapping(v1,v2,v2);
   checker_map.addMapping(v1,v2,er);
   
   restrict_map.addMapping(v1,v1,v1);
   restrict_map.addMapping(v2,v2,v2);
}


protected void defineAttribute(String nm,Value v)
{
   Attribute attr = new Attribute(nm);
   attribute_map.put(attr,v);
   name_map.put(nm,attr);
}



protected void defineError(Value v1,Value v2,IfaceError er)
{
   checker_map.addMapping(v1,v2,er);
}



void setIndex(int i)                            { subtype_index = i; }
int getIndex()                                  { return subtype_index; }




/********************************************************************************/
/*                                                                              */
/*      Default methods                                                         */
/*                                                                              */
/********************************************************************************/


@Override public IfaceSubtype.Value getDefaultConstantValue(IfaceBaseType bt,Object v)
{
   return getDefaultValue(bt);
}



@Override public IfaceSubtype.Value getDefaultValue(IfaceValue v)
{
   return getDefaultValue(v.getDataType().getJavaType());
}





@Override public IfaceSubtype.Value getDefaultUninitializedValue(IfaceType typ)
{
   return getDefaultValue(typ.getJavaType());
}


@Override public IfaceSubtype.Value getDefaultTypeValue(IfaceType typ)
{
   return getDefaultValue(typ.getJavaType());
}



IfaceSubtype.Value getDefaultValue(IfaceBaseType typ,List<IfaceAnnotation> annots)
{
   if (annots != null) {
      for (IfaceAnnotation an : annots) {
         String nm = an.getAnnotationName();
         Attr at = name_map.get(nm);
         if (at != null) {
            return attribute_map.get(at);
          }
       }
    }
   return getDefaultValue(typ);
}
   

public IfaceSubtype.Value getDefaultValue(IfaceAnnotation [] annots,IfaceSubtype.Value dflt)
{
   if (annots != null) {
      for (IfaceAnnotation an : annots) {
         String nm = an.getAnnotationName();
         Attr at = name_map.get(nm);
         if (at != null) {
            return attribute_map.get(at);
          }
       }
    }
   if (dflt != null) return dflt;
   
   return getDefaultValue((IfaceBaseType) null);
}


IfaceSubtype.Value getDefaultValue(IfaceBaseType typ,IfaceAnnotation [] annots)
{
   if (annots != null) {
      IfaceSubtype.Value v = getAnnotationValue(annots);
      if (v != null) return v;
    }
   return getDefaultValue(typ);
}


IfaceSubtype.Value getAnnotationValue(IfaceAnnotation [] annots)
{
   if (annots != null) {
      for (IfaceAnnotation an : annots) {
         String nm = an.getAnnotationName();
         Attr at = name_map.get(nm);
         if (at != null) {
            return attribute_map.get(at);
          }
       }
    }
   return null;
}







/********************************************************************************/
/*                                                                              */
/*      Merge methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSubtype.Value getMergeValue(Value v1,Value v2)
{
   if (v1 == v2) return v1;
   
   Value vr = combiner_map.getMapping(v1,v2);
   if (vr != null) return vr;
   return getDefaultValue((IfaceBaseType) null);
}


@Override public IfaceSubtype.Value getRestrictValue(Value v1,Value v2)
{
   if (v1 == v2) return v1;
   
   Value vr = restrict_map.getMapping(v1,v2);
   if (vr != null) return vr;
   return getDefaultValue((IfaceBaseType) null);
}


@Override public boolean isCompatibleWith(Value v1,Value v2)
{
   Value v3 = getMergeValue(v1,v2);
   if (v3 == v1) return true;
   if (v3 == null) return true;
   return false;
}


@Override public IfaceError checkCompatabilityWith(Value v1,Value v2)
{
   return checker_map.getMapping(v1,v2);
}



/********************************************************************************/
/*                                                                              */
/*      Attribute methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSubtype.Value getValueFor(IfaceSubtype.Attr attr)
{
   Value v1 = attribute_map.get(attr);
   if (v1 != null) return v1;
   return getDefaultValue((IfaceBaseType) null);
}


@Override public Collection<IfaceSubtype.Attr> getAttributes()
{
   return attribute_map.keySet();
}



/********************************************************************************/
/*                                                                              */
/*      Default computation methods                                             */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSubtype.Value getComputedValue(IfaceValue rslt,
      FaitOperator op,IfaceValue v0,IfaceValue v1)
{
   return null;
}



void checkImpliedTypes(TypeImplications rslt,FaitOperator op)
{
   switch (op) {
      case EQL :
         IfaceType t1 = rslt.getLhsTrueType();
         t1 = t1.restrictBy(rslt.getRhsTrueType());
         IfaceType t2 = rslt.getRhsTrueType();
         t2 = t2.restrictBy(rslt.getLhsTrueType());
         rslt.setLhsTypes(t1,null);
         rslt.setRhsTypes(t2,null);
         break;
      default :
         break;
    }
}

@Override public IfaceSubtype.Value getImpliedValue(FaitOperator op,IfaceValue v0,IfaceValue v1,
        boolean branch)
{
   return null;
}






/********************************************************************************/
/*                                                                              */
/*      Combiner map                                                            */
/*                                                                              */
/********************************************************************************/

private class Combiner {
   
   private Map<Value,Map<Value,Value>> value_map;
   
   Combiner() {
      value_map = new HashMap<>();
    }
   
   void addMapping(Value v1,Value v2,Value vr) {
      Map<Value,Value> sm = value_map.get(v1);
      if (sm == null) {
         sm = new HashMap<>();
         value_map.put(v1,sm);
       }
      sm.put(v2,vr);
    }
   
   Value getMapping(Value v1,Value v2) {
      Map<Value,Value> sm = value_map.get(v1);
      if (sm == null) return null;
      return sm.get(v2);
    }

}       // end of inner class Combiner



/********************************************************************************/
/*                                                                              */
/*      Attribute subclass                                                      */
/*                                                                              */
/********************************************************************************/

private class Attribute implements IfaceSubtype.Attr {
   
   private String attr_name;
   
   Attribute(String nm) {
      attr_name = nm;
    }
   
   @Override public String getName()                    { return attr_name; }
   @Override public IfaceSubtype getSubtype()           { return TypeSubtype.this; }
   
}       // end of inner class Attribute



/********************************************************************************/
/*                                                                              */
/*      Assignment checking                                                     */
/*                                                                              */
/********************************************************************************/

private class Checker {

   private Map<Value,Map<Value,IfaceError>> error_map;
   
   Checker() {
      error_map = new HashMap<>();
    }
   
   void addMapping(Value v1,Value v2,IfaceError er) {
      Map<Value,IfaceError> sm = error_map.get(v1);
      if (sm == null) {
         sm = new HashMap<>();
         error_map.put(v1,sm);
       }
      sm.put(v2,er);
    }
   
   IfaceError getMapping(Value v1,Value v2) {
      Map<Value,IfaceError> sm = error_map.get(v1);
      if (sm == null) return null;
      return sm.get(v2);
    }
   
}       // end of inner class Combiner




}      // end of class TypeSubtype




/* end of TypeSubtype.java */

