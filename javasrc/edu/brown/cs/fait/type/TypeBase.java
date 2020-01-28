/********************************************************************************/
/*                                                                              */
/*              TypeBase.java                                                   */
/*                                                                              */
/*      Basic implementation of an extended type                                */
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceTypeImplications;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.xml.IvyXmlWriter;


class TypeBase implements IfaceType, FaitConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceBaseType   base_type;
private IfaceSubtype.Value [] sub_values;
private TypeFactory     type_factory;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TypeBase(TypeFactory fac,IfaceBaseType base,Map<IfaceSubtype,IfaceSubtype.Value> subs)
{
   type_factory = fac;
   base_type = base;
   sub_values = new IfaceSubtype.Value[fac.getNumSubtypes()];
   for (Map.Entry<IfaceSubtype,IfaceSubtype.Value> ent : subs.entrySet()) {
      TypeSubtype tst = (TypeSubtype) ent.getKey();
      int idx = tst.getIndex();
      sub_values[idx] = ent.getValue();
    }
}


TypeBase(TypeFactory fac,IfaceBaseType base,IfaceSubtype.Value [] subs)
{
   type_factory = fac;
   base_type = base;
   sub_values = subs;
}


/********************************************************************************/
/*                                                                              */
/*      Basic operations                                                        */
/*                                                                              */
/********************************************************************************/

@Override public IfaceBaseType getJavaType()    { return base_type; }

@Override public String getName()
{
   return base_type.getName();
}


@Override public String getSignature()
{
   return base_type.getSignature();
}


@Override public String getJavaTypeName()
{
   return base_type.getJavaTypeName();
}


@Override public boolean isCategory2()          { return base_type.isCategory2(); }
@Override public boolean isPrimitiveType()      { return base_type.isPrimitiveType(); }
@Override public boolean isFloatingType()       { return base_type.isFloatingType(); }
@Override public boolean isVoidType()           { return base_type.isVoidType(); }
@Override public boolean isInterfaceType()      { return base_type.isInterfaceType(); }
@Override public boolean isArrayType()          { return base_type.isArrayType(); }
@Override public boolean isIntType()            { return base_type.isIntType(); }
@Override public boolean isJavaLangObject()     { return base_type.isJavaLangObject(); }
@Override public boolean isNumericType()        { return base_type.isNumericType(); }
@Override public boolean isStringType()         { return base_type.isStringType(); }
@Override public boolean isBooleanType()        { return base_type.isBooleanType(); }
@Override public boolean isFunctionRef()        { return base_type.isFunctionRef(); }

@Override public boolean isAbstract()           { return base_type.isAbstract(); }

@Override public boolean isBroaderType(IfaceType t)
{
   return base_type.isBroaderType(t.getJavaType());
}

@Override public boolean isDerivedFrom(IfaceType t)
{
   return base_type.isDerivedFrom(t.getJavaType());
}

@Override public boolean isCompatibleWith(IfaceType dt)
{
   return checkCompatibility(dt,null,null,-1);
}


@Override public boolean checkCompatibility(IfaceType dt,IfaceLocation loc,IfaceValue v0,int stackloc) 
{
   if (dt == null) return false;
   if (!base_type.isCompatibleWith(dt.getJavaType())) return false;
   
   boolean rslt = true;
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceError er = st.checkCompatabilityWith(getValue(st),dt.getValue(st));
      if (er != null) {
         if (loc != null) {
            if (FaitLog.isTracing()) {
               FaitLog.logD("Note Error: " + er);
             }
            IfaceError er1 = er;
            if (stackloc >= 0) {
               er1 = new FaitError(er,stackloc);
             }
            loc.noteError(er1);
          }
         rslt = false;
       }
    }
   
   return rslt;
}




@Override public List<IfaceError> getCompatibilityErrors(IfaceType dt) 
{
   if (dt == null) return null;
   if (!base_type.isCompatibleWith(dt.getJavaType())) return null;
   
   List<IfaceError> erslt = null;
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceError er = st.checkCompatabilityWith(getValue(st),dt.getValue(st));
      if (er != null) {
         if (erslt == null) erslt = new ArrayList<>();
         erslt.add(er);
       }
    }
   
   return erslt;
}


@Override public IfaceType getBaseType()
{
   IfaceBaseType bt = base_type.getBaseType();
   
   if (base_type.isArrayType()) {
      return type_factory.createType(bt);
    }
   return type_factory.createType(bt,this);
}

@Override public IfaceType getSuperType()
{
   return type_factory.createType(base_type.getSuperType(),this);
}

@Override public IfaceType getArrayType()
{
   IfaceBaseType bt = base_type.getArrayType();
   return type_factory.createType(bt);
}

@Override public List<IfaceType> getInterfaces()
{
   List<IfaceBaseType> btys = base_type.getInterfaces();
   if (btys == null) return null;
   List<IfaceType> rslt = new ArrayList<>();
   for (IfaceBaseType bt : btys) {
      rslt.add(type_factory.createType(bt,this));
    }
   return rslt;
}

@Override public IfaceType findChildForInterface(IfaceType dt)
{
   IfaceBaseType bt = base_type.findChildForInterface(dt.getJavaType());
   return type_factory.createType(bt,dt);
}

@Override public IfaceType getCommonParent(IfaceType dt)
{
   if (this == dt || dt == null) return this;
   
   IfaceBaseType bt = base_type.getCommonParent(dt.getJavaType());
   
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[type_factory.getNumSubtypes()];
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      vals[i] = st.getMergeValue(getValue(st),dt.getValue(st));
    }
   
   IfaceType nt = type_factory.createType(bt,vals);
   
   return nt;
}


@Override public IfaceType restrictBy(IfaceType dt)
{
   if (this == dt || dt == null) return this;
   
   IfaceBaseType bt = getJavaType();
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[type_factory.getNumSubtypes()];
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      vals[i] = st.getRestrictValue(getValue(st),dt.getValue(st));
    }
   
   IfaceType nt = type_factory.createType(bt,vals);
   
   return nt;
}

@Override public List<IfaceType> getChildTypes()
{ 
   List<IfaceBaseType> lbts = base_type.getChildTypes();
   if (lbts == null) return null;
   List<IfaceType> rslt = new ArrayList<>();
   for (IfaceBaseType bt : lbts) {
      rslt.add(type_factory.createType(bt,this));
    }
   return rslt;
}

@Override public IfaceType getAssociatedType()
{
   IfaceBaseType bt = base_type.getAssociatedType();
   return type_factory.createType(bt,this);
}

@Override public boolean isEditable()           { return base_type.isEditable(); }
@Override public boolean isInProject()          { return base_type.isInProject(); }

@Override public IfaceType getRunTimeType()
{
   IfaceBaseType bt = base_type.getRunTimeType();
   if (bt == base_type) return this;
   
   return type_factory.createType(bt,this);
}



/********************************************************************************/
/*                                                                              */
/*      Extended type methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSubtype.Value getValue(IfaceSubtype st)
{
   int idx = ((TypeSubtype) st).getIndex();
   return sub_values[idx];
}


@Override public boolean checkValue(IfaceSubtype.Value v)
{
   int idx = ((TypeSubtype)(v.getSubtype())).getIndex();
   return sub_values[idx] == v;
}


@Override public boolean checkValue(IfaceAnnotation ... annots)
{
   if (annots == null || annots.length == 0) return true;
   boolean rslt = true;
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceSubtype.Value sv = st.getAnnotationValue(annots);
      if (sv != null) {
         if (sub_values[i] != sv) rslt = false;
       }
    }
   return rslt;
}



@Override public IfaceType getAnnotatedType(IfaceAnnotation ... an)
{
   if (an == null || an.length == 0 || (an.length == 1 && an[0] == null)) 
      return this;
   
   return type_factory.createType(this,an);
}


@Override public IfaceType getAnnotatedType(Collection<IfaceAnnotation> ans)
{
   if (ans == null || ans.isEmpty()) return this;
   
   return type_factory.createType(this,ans);
}


@Override public IfaceType getAnnotatedType(IfaceType anotyp)
{
   return type_factory.createType(base_type,anotyp);
}


@Override public List<String> getAnnotations()
{
   List<String> rslt = new ArrayList<>();
   
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceSubtype.Value oval = getValue(st);
      for (IfaceSubtype.Attr attr : st.getAttributes()) {
         if (st.getValueFor(attr) == oval) {
            rslt.add(attr.getName());
            break;
          }
       }
    }
   
   return rslt;
}


@Override public List<IfaceSubtype> getSubtypes()
{
   List<IfaceSubtype> rslt = new ArrayList<>();
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceSubtype.Value val = getValue(st);
      if (val != null) rslt.add(st);
    }
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Operation methods                                                       */
/*                                                                              */
/********************************************************************************/

public IfaceType getComputedType(IfaceValue rslt,FaitOperator op,IfaceValue ... args)
{
   IfaceValue lhs,rhs;
   
   if (args.length == 0 || args[0] == null) lhs = rslt;
   else lhs = args[0];
   if (args.length < 2 || args[1] == null) rhs = lhs;
   else rhs = args[1];
   
   int ct = type_factory.getNumSubtypes();
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[ct];
   boolean chng = false;
   for (int i = 0; i < ct; ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceSubtype.Value val = st.getComputedValue(rslt,op,lhs,rhs);
      if (val == null) val = getValue(st);
      else if (val != getValue(st)) chng = true;
      vals[i] = val;
    }
   if (!chng) return this;
   return type_factory.createType(getJavaType(),vals);
}


public IfaceType getCallType(IfaceCall c,IfaceValue rslt,List<IfaceValue> args)
{
   int ct = type_factory.getNumSubtypes();
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[ct];
   boolean chng = false;
   for (int i = 0; i < ct; ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceSubtype.Value val = st.getCallValue(c,rslt,args);
      if (val == null) val = getValue(st);
      else if (val != getValue(st)) chng = true;
      vals[i] = val;
    }
   if (!chng) return this;
   return type_factory.createType(getJavaType(),vals);
}


@Override public IfaceType getComputedType(FaitTypeOperator op)
{
   int ct = type_factory.getNumSubtypes();
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[ct];
   boolean chng = false;
   for (int i = 0; i < ct; ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceSubtype.Value oval = getValue(st);
      IfaceSubtype.Value val = st.getComputedValue(op,oval);
      if (val == null) val = oval;
      else if (val != oval) chng = true;
      vals[i] = val;
    }
   if (!chng) return this;
   return type_factory.createType(getJavaType(),vals);
}




@Override public IfaceTypeImplications getImpliedTypes(FaitOperator op,IfaceType tr)
{
   TypeImplications rslt = new TypeImplications(this,tr);
   
   int ct = type_factory.getNumSubtypes();
   for (int i = 0; i < ct; ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      st.checkImpliedTypes(rslt,op);
    }
   
   return rslt;
}



@Override public List<IfaceType> getBackTypes(FaitOperator op,IfaceValue ... vals)
{
   List<IfaceType> rslt = null;
   int ct = type_factory.getNumSubtypes();
   for (int j = 0; j < vals.length; ++j) {
      List<IfaceAnnotation> annots = null;
      for (int i = 0; i < ct; ++i) {
         TypeSubtype st = type_factory.getSubtype(i);
         IfaceAnnotation an = st.getArgumentAnnotation(op,j,vals);
         if (an != null) {
            if (annots == null) annots = new ArrayList<>();
            annots.add(an);
          }
       }
      if (annots != null) {
         IfaceAnnotation [] ans = new IfaceAnnotation[annots.size()];
         ans = annots.toArray(ans);
         IfaceType nt = vals[j].getDataType().getAnnotatedType(ans);
         if (rslt == null) {
            rslt = new ArrayList<>();
            for (int k = 0; k < j; ++k) rslt.add(null);
          }
         rslt.add(nt);
       }
      else if (rslt != null) rslt.add(null);
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Debugging methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw)
{
   xw.begin("TYPE");
   xw.field("BASE",getJavaType().getName());
   for (int i = 0; i < type_factory.getNumSubtypes(); ++i) {
      TypeSubtype st = type_factory.getSubtype(i);
      IfaceSubtype.Value val = getValue(st);
      if (val != null) {
         xw.begin("SUBTYPE");
         xw.field("NAME",st.getName());
         xw.field("VALUE",val);
         xw.end("SUBTYPE");
       }
    }
   xw.end("TYPE");
}



@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append(base_type.getName());
   for (int i = 0; i < sub_values.length; ++i) {
      if (sub_values[i] != null) {
         buf.append("@");
         buf.append(sub_values[i].toString());
       }
    }
   return buf.toString();
}




}       // end of class TypeBase




/* end of TypeBase.java */

