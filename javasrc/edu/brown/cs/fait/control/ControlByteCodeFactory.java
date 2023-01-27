/********************************************************************************/
/*										*/
/*		ControlByteCodeFactory.java					*/
/*										*/
/*	Create internal objects for byte-code related items			*/
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



package edu.brown.cs.fait.control;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

import edu.brown.cs.ivy.jcode.JcodeAnnotation;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcode.JcodeField;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcode.JcodeLocalVariable;
import edu.brown.cs.ivy.jcode.JcodeMethod;
import edu.brown.cs.ivy.jcode.JcodeTryCatchBlock;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.ivy.xml.IvyXmlWriter;



class ControlByteCodeFactory implements JcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ControlMain	fait_control;
private JcodeFactory	code_factory;
private Map<JcodeField,InsField> field_map;
private Map<JcodeMethod,InsMethod> method_map;
private Map<JcodeInstruction,Reference<InsPoint>> point_map;



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

ControlByteCodeFactory(ControlMain cm,JcompTyper typer,JcodeFactory jf)
{
   fait_control = cm;
   code_factory = jf;
   field_map = new HashMap<>();
   method_map = new HashMap<>();
   point_map = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

List<IfaceMethod> findAllMethods(IfaceBaseType typ,String name)
{
   Collection<JcodeMethod> ms = code_factory.findAllMethods(getJcodeType(typ),name,null);
   List<IfaceMethod> rslt = new ArrayList<>();
   if (ms != null) {
      for (JcodeMethod jm : ms) {
         IfaceMethod im = getMethod(jm);
         if (im != null) rslt.add(im);
       }
    }
   return rslt;
}


IfaceMethod findMethod(IfaceBaseType typ,String method,String sign)
{
   typ = typ.getRunTimeType();
   JcodeMethod jm = code_factory.findMethod(null,typ.getName(),method,sign);
   return getMethod(jm);
}
  


IfaceField findField(IfaceType typ,String name)
{
   JcodeField jf = code_factory.findField(null,typ.getName(),name);
   if (jf == null) return null;
   return getField(jf);
}



/********************************************************************************/
/*                                                                              */
/*      Lambda methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceBaseType buildMethodType(String typ)
{
   JcodeDataType jdt = code_factory.findJavaType(typ);
   IfaceType rtyp = getFaitType(jdt.getReturnType());
   JcodeDataType [] atyps = jdt.getArgumentTypes();
   List<IfaceType> atypl = new ArrayList<>();
   for (int i = 0; i < atyps.length; ++i) {
      IfaceType atyp = getFaitType(atyps[i]);
      atypl.add(atyp);
    }
   
   return fait_control.createMethodType(rtyp,atypl);
}


/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private JcodeDataType getJcodeType(IfaceBaseType typ)
{
   return code_factory.findNamedType(typ.getName());
}



private IfaceType getFaitType(JcodeDataType jdt)
{
   if (jdt == null) return null;
   
   IfaceBaseType bt =  fait_control.findJavaType(jdt.getName());  
   return fait_control.findDataType(bt,null);
}


IfaceAnnotation [] getAnnotations(JcodeInstruction ins)
{
   List<JcodeAnnotation> ans = ins.getAnnotations();
   if (ans == null) return null;
   IfaceAnnotation [] rslt = new IfaceAnnotation[ans.size()];
   for (int i = 0; i < ans.size(); ++i) {
      rslt[i] = new InsAnnotation(ans.get(i));
    }
   return rslt;
}

/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

void updateAll()
{
   updateFields();
   updateMethods();
   updatePoints();
}


/********************************************************************************/
/*										*/
/*	Methods for representing fields 					*/
/*										*/
/********************************************************************************/

public IfaceField getField(JcodeField fld)
{
   synchronized (field_map) {
      InsField ifld = field_map.get(fld);
      if (ifld == null) {
         ifld = new InsField(fld);
         field_map.put(fld,ifld);
       }
      return ifld;
    }
}


private void updateFields()
{
   Map<JcodeField,InsField> nflds = new HashMap<>();
   synchronized (field_map) {
       for (InsField ifld : field_map.values()) {
           IfaceType ityp = ifld.getDeclaringClass();
           JcodeField fld = code_factory.findField(null,ityp.getName(),ifld.getFieldName());
           if (fld != null) {
              ifld.updateTo(fld);
              nflds.put(fld,ifld);
            }
        }
       field_map = nflds;
    }
}


private class InsField implements IfaceField {

   private JcodeField for_field;

   InsField(JcodeField fld) {
      for_field = fld;
    }

   @Override public String getFullName()        { return for_field.getFullName(); }

   @Override public boolean isStatic()		{ return for_field.isStatic(); }
   @Override public boolean isFinal()           { return for_field.isFinal(); }

   @Override public  boolean isVolatile()	{ return for_field.isVolatile(); }
   
   @Override public String getKey() {
      String cnm = for_field.getDeclaringClass().getName();
      cnm = cnm.replace("/",".");
      cnm = cnm.replace("$",".");
      return cnm + "." + for_field.getName();
    }
   
   @Override public IfaceValue getConstantValue() {
      Object o = for_field.getConstantValue();
      if (o == null) return null;
      IfaceType typ = getType();
      if (typ.isFloatingType() && o instanceof Number) {
         double v = ((Number) o).doubleValue();
         return fait_control.findRangeValue(typ,v,v);
       }
      else if (typ.isNumericType() && o instanceof Number) {
         Long v = ((Number) o).longValue();
         return fait_control.findRangeValue(typ,v,v);
       }
      else if (typ.isStringType() && o instanceof String) {
         return fait_control.findConstantStringValue((String) o);
       }
      else if (typ.isBooleanType() && o instanceof Boolean) {
         boolean b = (Boolean) o;
         return fait_control.findConstantValue(b);
       }
      return null;
    }
   
   @Override public IfaceType getDeclaringClass() {
      return getFaitType(for_field.getDeclaringClass());
    }

   @Override public IfaceType getType() {
      return getFaitType(for_field.getType());
    }

   void updateTo(JcodeField fld)                { for_field = fld; }
   String getFieldName()                        { return for_field.getName(); }
   
   @Override public List<IfaceAnnotation> getAnnotations() {
      List<JcodeAnnotation> jan = for_field.getAnnotations();
      if (jan == null) return null;
      List<IfaceAnnotation> rslt = new ArrayList<>();
      for (JcodeAnnotation ja : jan) {
         rslt.add(new InsAnnotation(ja));
       }
      return rslt;
    }
   
   @Override public boolean equals(Object o) {
      if (o instanceof InsField) {
         InsField insf = (InsField) o;
         return for_field == insf.for_field;
       }
      else if (o instanceof IfaceField) {
         IfaceField f = (IfaceField) o;
         return f.getFullName().equals(getFullName());
       }
      else if (o instanceof String) {
         return getFullName().equals(o.toString());
       }
      return false;
    }

   @Override public int hashCode() {
      return getFullName().hashCode();
    }

   @Override public String toString() {
      return for_field.getFullName();
    }
   
}	// end of inner class InsField




/********************************************************************************/
/*										*/
/*	Methods for representing methods					*/
/*										*/
/********************************************************************************/

public IfaceMethod getMethod(JcodeMethod m)
{
   if (m == null) return null;
   
   synchronized (method_map) {
      InsMethod im = method_map.get(m);
      if (im == null) {
         im = new InsMethod(m);
         method_map.put(m,im);
       }
      return im;
    }
}


private void updateMethods()
{
   Map<JcodeMethod,InsMethod> nmthds = new HashMap<>();
   
   synchronized (method_map) {
      for (InsMethod im : method_map.values()) {
         IfaceType typ = im.getDeclaringClass().getRunTimeType();
         JcodeMethod jm = code_factory.findMethod(null,typ.getName(),im.getName(),im.getDescription());
         if (jm != null) {
            im.updateTo(jm);
            nmthds.put(jm,im);
          }
       }
      method_map = nmthds;
    }
}



private class InsMethod implements IfaceMethod {

   private JcodeMethod for_method;

   InsMethod(JcodeMethod m) {
      for_method = m;
    }

   @Override public String getFullName()	{ return for_method.getFullName(); }
   @Override public String getName()		{ return for_method.getName(); }
   @Override public String getDescription()	{ return for_method.getDescription(); }
   @Override public String getFile() {
      return for_method.getFile();
    }
   
   @Override public boolean isStatic()		{ return for_method.isStatic(); }
   @Override public boolean isVarArgs() 	{ return for_method.isVarArgs(); }
   @Override public boolean isStaticInitializer() {
      return for_method.isStaticInitializer();
   }
   @Override public boolean isConstructor()	{ return for_method.isConstructor(); }
   @Override public boolean isNative()		{ return for_method.isNative(); }
   @Override public boolean isAbstract()	{ return for_method.isAbstract(); }
   @Override public boolean isPrivate() 	{ return for_method.isPrivate(); }
   @Override public boolean isSynchronized()	{ return for_method.isSynchronized(); }
   @Override public boolean isEditable()	{ return false; }
   @Override public boolean hasCode()		{ return for_method.getNumInstructions() != 0; }

   @Override public IfaceType getReturnType() {
      return getFaitType(for_method.getReturnType());
    }
   @Override public int getNumArgs()		{ return for_method.getNumArguments(); }
   @Override public IfaceType getArgType(int i) {
      IfaceType t0 = getFaitType(for_method.getArgType(i));
      List<IfaceAnnotation> ann = getArgAnnotations(i);
      if (ann != null) {
         t0 = t0.getAnnotatedType(ann);
       }
      return t0;
    }

   @Override public IfaceType getDeclaringClass() {
      return getFaitType(for_method.getDeclaringClass());
    }

   @Override public List<IfaceType> getExceptionTypes() {
      List<IfaceType> rslt = new ArrayList<>();
      for (JcodeDataType jdt : for_method.getExceptionTypes()) {
	 rslt.add(getFaitType(jdt));
       }
      return rslt;
    }

   @Override public List<IfaceMethod> getParentMethods() {
      List<IfaceMethod> rslt = new ArrayList<>();
      for (JcodeMethod fm : for_method.getParentMethods()) {
         if (fm == for_method) continue;
         IfaceMethod f = fait_control.findMethod(fm.getDeclaringClass().getName(),
               fm.getName(),fm.getDescription());
         rslt.add(f);
       }
      return rslt;
    }

   @Override public List<IfaceMethod> getChildMethods() {
      List<IfaceMethod> rslt = new ArrayList<>();
      for (JcodeMethod fm : for_method.getChildMethods()) {
         if (fm == for_method) continue;
         IfaceMethod f = fait_control.findMethod(fm.getDeclaringClass().getName(),
               fm.getName(),fm.getDescription());
         rslt.add(f);
       }
      return rslt;
    }

   @Override public Collection<JcodeTryCatchBlock> getTryCatchBlocks() {
      return for_method.getTryCatchBlocks();
    }

   @Override public IfaceProgramPoint getStart() {
      if (for_method.getNumInstructions() == 0) return null;
      JcodeInstruction ins = for_method.getInstruction(0);
      return getPoint(ins);
    }

   @Override public IfaceProgramPoint getNext(IfaceProgramPoint pt) {
      JcodeInstruction ins = pt.getInstruction();
      JcodeInstruction nins = for_method.getInstruction(ins.getIndex() + 1);
      if (nins == null) return null;
      return getPoint(nins);
    }

   @Override public int getLocalSize()		        { return for_method.getLocalSize(); }
   @Override public int getLocalOffset(Object o)        { return -1; }
   @Override public Object getItemAtOffset(int off,IfaceProgramPoint pt) {
      JcodeLocalVariable lcl = for_method.getLocalVariable(off,pt.getInstruction());
      if (lcl == null) return null;
      return lcl.getName();
    }
   @Override public Collection<Object> getExternalSymbols() {
      return null;
    }
   
   @Override public void setExternalValue(Object o,IfaceValue v)  { }
   @Override public IfaceValue getExternalValue(Object o)         { return null; }
   
   
   @Override public IfaceType getLocalType(int slot,IfaceProgramPoint pt) {
      JcodeLocalVariable jlv = for_method.getLocalVariable(slot,pt.getInstruction());
      if (jlv == null) return null;
      return getFaitType(jlv.getDataType());
    }
   
   @Override public List<IfaceAnnotation> getLocalAnnotations(int slot,IfaceProgramPoint pt) {
      JcodeLocalVariable jlv = for_method.getLocalVariable(slot,pt.getInstruction());
      if (jlv == null) return null;
      return getAnnotations(for_method.getLocalAnnotations(jlv));
    }
   
   @Override public List<IfaceAnnotation> getReturnAnnotations() {
      return getAnnotations(for_method.getReturnAnnotations());
    }
   
   @Override public List<IfaceAnnotation> getArgAnnotations(int i) {
      return getAnnotations(for_method.getArgumentAnnotations(i));
    }
   
   private List<IfaceAnnotation> getAnnotations(List<JcodeAnnotation> in) {
      if (in == null) return null;
      List<IfaceAnnotation> rslt = new ArrayList<>();
      for (JcodeAnnotation ja : in) {
         rslt.add(new InsAnnotation(ja));
       }
      return rslt;
    }
   
   void updateTo(JcodeMethod jm)                { for_method = jm; }
   

   @Override public boolean equals(Object o) {
      if (o instanceof InsMethod) {
	 InsMethod insm = (InsMethod) o;
	 return for_method == insm.for_method;
       }
      return false;
    }

   @Override public int hashCode() {
      return for_method.hashCode();
    }

   @Override public String toString() {
      return for_method.toString();
    }

}	// end of inner class InsMethod




/********************************************************************************/
/*                                                                              */
/*      Annotations                                                             */
/*                                                                              */
/********************************************************************************/

private class InsAnnotation implements IfaceAnnotation {
   
   private JcodeAnnotation for_annotation;
   
   InsAnnotation(JcodeAnnotation ja) {
      for_annotation = ja;
    }
   
   @Override public String getAnnotationName() {
      String nm = for_annotation.getDescription();
      int idx = nm.lastIndexOf(".");
      if (idx > 0) nm = nm.substring(idx+1);
      if (nm.equals("Secure")) {
         Object v = getAnnotationValue("value");
         if (v != null) nm = v.toString();
       }
      return nm;
    }
   
   @Override public Object getAnnotationValue(String key) {
      if (key == null) key = "value";
      Map<String,Object> map = for_annotation.getValues();
      if (map == null) return null;
      return map.get(key);
    }
   
   @Override public String toString() {
      return for_annotation.toString();
    }
   
}       // end of inner class InsAnnotation



/********************************************************************************/
/*										*/
/*	Methods for creating program points					*/
/*										*/
/********************************************************************************/

public IfaceProgramPoint getPoint(JcodeInstruction ins)
{
   if (ins == null) return null;
   
   synchronized (point_map) {
      Reference<InsPoint> ref = point_map.get(ins);
      InsPoint ip = null;
      if (ref != null) ip = ref.get();
      if (ip == null) {
         ip = new InsPoint(ins);
         point_map.put(ins,new WeakReference<>(ip));
       }
      return ip;
    }
}


private void updatePoints()
{
   Map<JcodeInstruction,Reference<InsPoint>> npoints = new HashMap<>();
   synchronized (point_map) {
      for (Reference<InsPoint> ref : point_map.values()) {
         InsPoint ip = ref.get();
         if (ip == null) continue;
         JcodeInstruction ins = ip.getInstruction();
         JcodeMethod jm = ins.getMethod();
         JcodeInstruction nins = ins;
         if (method_map.get(jm) == null) {
            JcodeDataType typ = jm.getDeclaringClass();
            JcodeMethod xjm = code_factory.findMethod(null,typ.getName(),jm.getName(),jm.getDescription());
            // if the instruction has changed, then we should be removing the method before this
            nins = xjm.getInstruction(ins.getIndex());
            ip.setInstruction(nins);
          }
         npoints.put(nins,new WeakReference<>(ip));
       }
      point_map = npoints;
    }
}



private class InsPoint implements IfaceProgramPoint {

   private JcodeInstruction for_instruction;

   InsPoint(JcodeInstruction ins) {
      for_instruction = ins;
    }
   
   @Override public JcodeInstruction getInstruction()		{ return for_instruction; }
   void setInstruction(JcodeInstruction ins)                    { for_instruction = ins; }

   @Override public IfaceAstReference getAstReference() 	{ return null; }
   
   @Override public IfaceMethod getMethod() {
      JcodeMethod jm = for_instruction.getMethod();
      return ControlByteCodeFactory.this.getMethod(jm);
    }
   
   @Override public IfaceProgramPoint getNext() {
      return getPoint(for_instruction.getNext());
    }
   
   @Override public IfaceProgramPoint getPrevious() {
      return getPoint(for_instruction.getPrevious());
    }

   @Override public boolean isByteCode()			{ return true; }

   @Override public boolean isMethodStart() {
      return for_instruction.getIndex() == 0;
    }

   @Override public IfaceMethod getReferencedMethod() {
      JcodeMethod m = for_instruction.getMethodReference();
      if (m == null) return null;
      IfaceMethod fm = fait_control.findMethod(m.getDeclaringClass().getName(),
            m.getName(),m.getDescription());
      if (fm != null) return fm;
      return ControlByteCodeFactory.this.getMethod(m);
    }
   
   
   @Override public int getNumArgs() {
      return for_instruction.getNumArgs();
   }
   
   @Override public IfaceMethod getCalledMethod() {
      switch (for_instruction.getOpcode()) {
         case INVOKEINTERFACE :
         case INVOKESPECIAL :
         case INVOKESTATIC :
         case INVOKEVIRTUAL :
            return getReferencedMethod();
       }
      return null;
    }

   @Override public IfaceField getReferencedField() {
      JcodeField f = for_instruction.getFieldReference();
      if (f == null) return null;
      IfaceField fld = getField(f);
      if (fld == null) {
         FaitLog.logE("Unknown field " + f);
       }
      return fld;
    }

   @Override public IfaceType getReferencedType() {
      String tnm = null;
      switch (for_instruction.getOpcode()) {
         default :
            return getFaitType(for_instruction.getTypeReference());
         case AALOAD :
            break;
         case BALOAD :
            tnm = "byte";
            break;
         case CALOAD :
            tnm = "char";
            break;
         case DALOAD :
            tnm = "double";
            break;
         case FALOAD :
            tnm = "float";;
            break;
         case IALOAD :
            tnm = "int";
            break;
         case LALOAD :
            tnm = "long";
            break;
         case SALOAD :
            tnm = "short";
            break;
       }
      if (tnm != null) return fait_control.findDataType(tnm);
      return null;
    }
   
   @Override public IfaceProgramPoint getReferencedTarget() {
      JcodeInstruction nins = for_instruction.getTargetInstruction();
      return getPoint(nins);
    }
   
   @Override public List<IfaceProgramPoint> getReferencedTargets() {
      List<IfaceProgramPoint> rslt = new ArrayList<>();
      for (JcodeInstruction nins : for_instruction.getTargetInstructions()) {
         IfaceProgramPoint pt = getPoint(nins);
         if (pt != null) rslt.add(pt);
       }
      return rslt;
    }

   @Override public boolean isInterfaceCall() {
      return for_instruction.getOpcode() == JcodeConstants.INVOKEINTERFACE;
    }

   @Override public boolean isVirtualCall() {
      if (for_instruction.getOpcode() == JcodeConstants.INVOKEINTERFACE) return true;
      if (for_instruction.getOpcode() == JcodeConstants.INVOKEVIRTUAL) return true;
      return false;
    }

   @Override public int getLineNumber() {
      return for_instruction.getLineNumber();
    }
   
   @Override public String getSourceFile() {
      JcodeMethod jm = for_instruction.getMethod();
      
      if (jm != null) return jm.getSourceFile();
      return null;
    }
   
   @Override public int getInstanceNumber() {
      IfaceMethod im = getMethod();
      IfaceMethod mthd = getReferencedMethod();
      IfaceProgramPoint pt0 = im.getStart();
      int ctr = 0;
      while (pt0 != null) {
         IfaceMethod m1 = pt0.getReferencedMethod();
         if (m1 == mthd) {
            ++ctr;
            if (this == pt0) return ctr;
          }
         pt0 = pt0.getNext();
       }
      return -1;
    }
   
   @Override public boolean equals(Object o) {
      if (o instanceof InsPoint) {
	 return for_instruction == ((InsPoint) o).for_instruction;
       }
      else if (o instanceof JcodeInstruction) {
	 return for_instruction == o;
       }
      return false;
    }

   @Override public int hashCode() {
      return for_instruction.hashCode();
    }

   @Override public void outputXml(IvyXmlWriter xw) {
      xw.begin("POINT");
      xw.field("KIND","BINARY");
      xw.field("LOC",for_instruction.getIndex());
      xw.field("OPCODE",for_instruction.getOpcode());
      xw.field("LINE",getLineNumber());
      xw.text(for_instruction.toString());
      xw.end("POINT");
    }
   
   @Override public String toString() {
      return "@" + for_instruction.getIndex() + ">" + for_instruction.getOpcode() + " " +
         for_instruction;
    }
}	// end of inner class InsPoint




}	// end of class ControlByteCodeFactory




/* end of ControlByteCodeFactory.java */
