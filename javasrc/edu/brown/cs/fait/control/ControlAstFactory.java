/********************************************************************************/
/*										*/
/*		ControlAstFactory.java						*/
/*										*/
/*	Create internal objects for ast-related objects 			*/
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAstStatus;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.jcode.JcodeTryCatchBlock;
import edu.brown.cs.ivy.jcomp.JcompAnnotation;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;

class ControlAstFactory implements FaitConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ControlMain	fait_control;
private JcompTyper	jcomp_typer;

private Map<JcompType,AstType> type_map;
private Map<JcompSymbol,AstMethod> method_map;
private Map<JcompSymbol,AstField> field_map;
private Map<ControlAstReference,ControlAstReference> ref_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ControlAstFactory(ControlMain cm,JcompTyper typer)
{
   fait_control = cm;
   jcomp_typer = typer;
   type_map = new ConcurrentHashMap<>();
   method_map = new ConcurrentHashMap<>();
   field_map = new ConcurrentHashMap<>();
   ref_map = new ConcurrentSkipListMap<>(new RefComparator());
}



/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

List<IfaceMethod> findAllMethods(IfaceBaseType typ,String name)
{
   List<IfaceMethod> rslt = new ArrayList<>();
   String aname = null;
   if (name.equals("<clinit>")) aname = INITER_NAME;
   JcompType jt = getInternalType(typ);
   List<JcompSymbol> syms = jt.lookupMethods(jcomp_typer,name,aname);
   if (syms != null) {
      for (JcompSymbol js : syms) {
	 if (js.isBinarySymbol()) {
	    continue;
	  }
	 rslt.add(getMethod(js));
       }
    }

   // for (JcompSymbol js : jcomp_typer.findKnownMethods(typ.getName())) {
      // if (js.getName().equals(name)) {
	 // rslt.add(getMethod(js));
       // }
    // }

   return rslt;
}


IfaceMethod findMethod(IfaceBaseType typ,String name,String desc)
{
   JcompType atyps = null;
   if (desc != null) {
      List<JcompType> types = getParameterList(desc);
      atyps = JcompType.createMethodType(null,types,false,null);
    }
   JcompSymbol msym = getInternalType(typ).lookupMethod(jcomp_typer,name,atyps);
   if (msym == null && name.equals("<clinit>")) {
      msym = getInternalType(typ).lookupMethod(jcomp_typer,INITER_NAME,atyps);
    }
   return getMethod(msym);
}


IfaceField findField(IfaceBaseType typ,String name)
{
   int idx = name.lastIndexOf(".");
   if (idx > 0) name = name.substring(idx+1);
   JcompSymbol msym = getInternalType(typ).lookupField(jcomp_typer,name);
   if (msym == null) return null;

   return getField(msym);
}



private List<JcompType> getParameterList(String arr)
{
   if (arr == null) return null;
   List<JcompType> rslt = new ArrayList<>();
   int idx = 0;
   if (arr.charAt(idx) == '(') ++idx;
   while (idx < arr.length() && arr.charAt(idx) != ')') {
      idx = getTypeFromDescription(arr,idx,rslt);
    }

   return rslt;
}


private int getTypeFromDescription(String txt,int idx,List<JcompType> rslt)
{
   if (idx > txt.length()) return idx;
   switch (txt.charAt(idx)) {
      case 'B' :
	 rslt.add(jcomp_typer.findSystemType("byte"));
	 break;
      case 'C' :
	 rslt.add(jcomp_typer.findSystemType("char"));
	 break;
      case 'D' :
	 rslt.add(jcomp_typer.findSystemType("double"));
	 break;
      case 'F' :
	 rslt.add(jcomp_typer.findSystemType("float"));
	 break;
      case 'I' :
	 rslt.add(jcomp_typer.findSystemType("int"));
	 break;
      case 'J' :
	 rslt.add(jcomp_typer.findSystemType("long"));
	 break;
      case 'S' :
	 rslt.add(jcomp_typer.findSystemType("short"));
	 break;
      case 'V' :
	 rslt.add(jcomp_typer.findSystemType("void"));
	 break;
      case 'Z' :
	 rslt.add(jcomp_typer.findSystemType("boolean"));
	 break;
      case '[' :
	 idx = getTypeFromDescription(txt,idx+1,rslt);
	 JcompType jt = rslt.remove(rslt.size()-1);
	 rslt.add(jcomp_typer.findArrayType(jt));
	 return idx;
      case 'L' :
	 for (int i = idx+1; i < txt.length(); ++i) {
	    if (txt.charAt(i) == '<' || txt.charAt(i) == ';') {
	       String tnm = txt.substring(idx+1,i);
	       tnm = tnm.replace("$",".").replace("/",".");
	       rslt.add(jcomp_typer.findSystemType(tnm));
	       if (txt.charAt(i) == '<') {
		  ++i;
		  List<JcompType> dtyp = new ArrayList<JcompType>();
		  while (i < txt.length()) {
		     i = getTypeFromDescription(txt,i,dtyp);
		     if (i >= txt.length() || txt.charAt(i) == '>') break;
		   }
		  ++i;
		}
	       if (i < txt.length() && txt.charAt(i) == ';') ++i;
	       return i;
	     }
	  }
	 break;

    }
   return idx+1;
}


void updateAll(JcompTyper typer)
{
   updateTypes(typer);
   updateMethods();
   updateFields();
   updateRefs();
}



/********************************************************************************/
/*										*/
/*	AstType -- type representation						*/
/*										*/
/********************************************************************************/

IfaceBaseType getType(String name)
{
   return getType(getInternalType(name));

}


IfaceBaseType getType(JcompType t)
{
   if (t == null) return null;
   t = jcomp_typer.fixJavaType(t);

   AstType t1 = type_map.get(t);
   if (t1 == null || t1.getJcompType() != t) {
      t1 = new AstType(t);
      AstType t2 = type_map.putIfAbsent(t,t1);
      if (t2 != null) t1 = t2;
    }
   return t1;
}


IfaceType getFullType(JcompType t)
{
   return fait_control.findDataType(getType(t),null);
}

IfaceBaseType getMethodType(IfaceType rtn,List<IfaceType> args)
{
   JcompType rtyp = getInternalType(rtn.getJavaType());
   List<JcompType> argtyps = new ArrayList<>();
   for (IfaceType t : args) {
      argtyps.add(getInternalType(t.getJavaType()));
    }
   JcompType mtyp = JcompType.createMethodType(rtyp,argtyps,false,null);
   return getType(mtyp);
}


IfaceBaseType getFunctionRefType(IfaceBaseType bt,IfaceBaseType nstype)
{
   JcompType jns = null;
   if (nstype != null) jns = getInternalType(nstype);
   JcompType jt = JcompType.createFunctionRefType(getInternalType(bt),jns,null);
   return getType(jt);
}



private JcompType getInternalType(String name)
{
   if (name == null) return null;
   if (name.length() == 1 || name.contains(";") || name.startsWith("[")) {
      name = IvyFormat.formatTypeName(name);
    }
   JcompType jt = jcomp_typer.findType(name);
   try {
      if (jt == null) {
	 jt = jcomp_typer.findSystemType(name);
       }
    }
   catch (Throwable t) {
      System.err.println("Problem with type " + name);
      throw t;
    }

   return jt;
}


private JcompType getInternalType(IfaceBaseType t)
{
   if (t == null) return null;

   if (t instanceof AstType) {
      AstType at = (AstType) t;
      return at.getJcompType();
    }
   else {
      String nm = t.getName();
      return jcomp_typer.findType(nm);
    }
}



private void updateTypes(JcompTyper typer)
{
   Map<JcompType,AstType> ntypemap = new ConcurrentHashMap<>();
   for (AstType at : type_map.values()) {
      JcompType jt = at.getJcompType();
      JcompType njt = typer.findType(jt.getName());
      if (njt == null) njt = typer.findSystemType(jt.getName());
      if (njt == null) continue;
      at.setJcompType(njt);
      ntypemap.put(njt,at);
    }
   type_map = ntypemap;
   jcomp_typer = typer;
}



private class AstType implements IfaceBaseType {

   private JcompType jcomp_type;

   AstType(JcompType jt) {
      jcomp_type = jt;
    }

   JcompType getJcompType()			{ return jcomp_type; }
   void setJcompType(JcompType jt)		{ jcomp_type = jt; }

   @Override public String getName()		{ return jcomp_type.getName(); }
   @Override public String getSignature()	{ return jcomp_type.getSignature(); }
   @Override public String getJavaTypeName()	{ return jcomp_type.getJavaTypeName(); }

   @Override public boolean isCategory2()	{ return jcomp_type.isCategory2(); }
   @Override public boolean isPrimitiveType()	{ return jcomp_type.isPrimitiveType(); }
   @Override public boolean isFloatingType()	{ return jcomp_type.isFloatingType(); }
   @Override public boolean isVoidType()	{ return jcomp_type.isVoidType(); }
   @Override public boolean isInterfaceType()	{ return jcomp_type.isInterfaceType(); }
   @Override public boolean isArrayType()	{ return jcomp_type.isArrayType(); }
   @Override public boolean isIntType() 	{ return jcomp_type.isIntType(); }
   @Override public boolean isJavaLangObject()	{ return jcomp_type.isJavaLangObject(); }
   @Override public boolean isNumericType()	{ return jcomp_type.isNumericType(); }
   @Override public boolean isStringType()	{ return jcomp_type.isStringType(); }
   @Override public boolean isBooleanType()	{ return jcomp_type.isBooleanType(); }
   @Override public boolean isFunctionRef()	{ return jcomp_type.isFunctionRef(); }

   @Override public boolean isAbstract()	{ return jcomp_type.isAbstract(); }

   @Override public boolean isBroaderType(IfaceBaseType t) {
      return jcomp_type.isBroaderType(getInternalType(t));
    }

   @Override public boolean isDerivedFrom(IfaceBaseType t) {
      return jcomp_type.isDerivedFrom(getInternalType(t));
    }

   @Override public boolean isCompatibleWith(IfaceBaseType t) {
      return jcomp_type.isCompatibleWith(getInternalType(t));
    }

   @Override public IfaceBaseType getBaseType() {
      return getType(jcomp_type.getBaseType());
    }

   @Override public IfaceBaseType getSuperType() {
      return getType(jcomp_type.getSuperType());
    }

   @Override public IfaceBaseType getArrayType() {
      return getType(jcomp_typer.findArrayType(jcomp_type));
    }

   @Override public IfaceBaseType getAssociatedType() {
      return getType(jcomp_type.getAssociatedType());
    }

   @Override public IfaceBaseType getComponentType(int i) {
      List<JcompType> comps = jcomp_type.getComponents();
      if (i < 0 || i >= comps.size()) return null;
      return getType(comps.get(i));
    }

   @Override public IfaceBaseType getRunTimeType() {
      if (jcomp_type.isParameterizedType()) {
	 return getType(jcomp_type.getBaseType());
       }
      else if (jcomp_type.isArrayType() && jcomp_type.getBaseType().isParameterizedType()) {
	 IfaceBaseType t0 = getType(jcomp_type.getBaseType().getBaseType());
	 return t0.getArrayType();
       }
      return this;
    }

   @Override public List<IfaceBaseType> getInterfaces() {
      List<IfaceBaseType> rslt = new ArrayList<>();
      Collection<JcompType> ifc = jcomp_type.getInterfaces();
      if (ifc != null) {
	 for (JcompType jt : ifc) {
	    rslt.add(getType(jt));
	  }
       }
      return rslt;
    }

   @Override public Map<String,IfaceType> getFieldData() {
      Map<String,JcompType> flds = jcomp_type.getFields(jcomp_typer);
      Map<String,IfaceType> rslt = new HashMap<>();
      for (Map.Entry<String,JcompType> ent : flds.entrySet()) {
	 IfaceType t0 = getFullType(ent.getValue());
	 rslt.put(ent.getKey(),t0);
       }
      return rslt;
    }

   @Override public IfaceBaseType findChildForInterface(IfaceBaseType dt) {
      JcompType jt = jcomp_type.findChildForInterface(jcomp_typer,getInternalType(dt));
      return getType(jt);
    }

   @Override public List<IfaceBaseType> getChildTypes() {
      List<IfaceBaseType> rslt = new ArrayList<>();
      for ( ; ; ) {
	 try {
	    Collection<String> typs = jcomp_type.getChildTypes();
	    if (typs != null) {
	       for (String jtnm : typs) {
		  JcompType jt = jcomp_typer.findType(jtnm);
		  if (jt != null) rslt.add(getType(jt));
		}
	     }
	    break;
	  }
	 catch (ConcurrentModificationException e) {
	    rslt.clear();
	  }
       }

      return rslt;
    }

   @Override public IfaceBaseType getCommonParent(IfaceBaseType dt) {
      JcompType jt = JcompType.mergeTypes(jcomp_typer,jcomp_type,getInternalType(dt));
      return getType(jt);
    }

   @Override public boolean isEditable() {
      return fait_control.isEditableClass(this);
    }

   @Override public boolean isInProject() {
      return fait_control.isProjectClass(this);
    }

   @Override public String toString() {
      if (jcomp_type == null) return "???";
      return jcomp_type.toString();
    }

}	// end of inner class AstType




/********************************************************************************/
/*										*/
/*	Method description							*/
/*										*/
/********************************************************************************/

IfaceMethod getMethod(JcompSymbol js)
{
   if (js == null) return null;
   IfaceBaseType typ = getType(js.getClassType());
   if (typ.isEditable()) {
      AstMethod m1 = method_map.get(js);
      if (m1 == null || m1.getJcompSymbol() != js) {
	 m1 = new AstMethod(js);
	 AstMethod m2 = method_map.putIfAbsent(js,m1);
	 if (m2 != null) m1 = m2;
       }
      return m1;
    }

   JcompSymbol jsy = js.getBaseSymbol(jcomp_typer);
   String desc = jsy.getType().getJavaTypeName();
   // TODO: remove generics from desc
   if (desc.contains("<")) {
      StringBuffer buf = new StringBuffer();
      int lvl = 0;
      for (int i = 0; i < desc.length(); ++i) {
	 char c = desc.charAt(i);
	 if (c == '<') ++lvl;
	 else if (c == '>') --lvl;
	 else if (lvl == 0) buf.append(c);
       }
      desc = buf.toString();
    }
   IfaceBaseType rtyp = typ.getRunTimeType();

   IfaceMethod im = fait_control.findMethod(rtyp.getName(),jsy.getName(),desc);
   if (im != null) return im;
   JcompSymbol njs = js.getOriginalMethod();
   if (njs != js) {
      return getMethod(njs);
    }

   FaitLog.logE("METHOD NOT FOUND "  + js + " " + rtyp.getName() + " " + jsy.getName() + " " + desc + " " + typ);

   return null;
}



private void updateMethods()
{
   Map<JcompSymbol,AstMethod> nmethodmap = new ConcurrentHashMap<>();
   for (AstMethod am : method_map.values()) {
      JcompSymbol js = am.getJcompSymbol();
      JcompSymbol js1 = mapMethod(js);
      if (js1 != null) {
	 am.setJcompSymbol(js1);
	 nmethodmap.put(js1,am);
       }
      else {
	 am.setJcompSymbol(js);
       }
    }
   method_map = nmethodmap;
}



private JcompSymbol mapMethod(JcompSymbol js)
{
   JcompType jt = jcomp_typer.findType(js.getClassType().getName());
   if (jt == null) jt = jcomp_typer.findSystemType(js.getClassType().getName());
   if (jt == null) return null;
   String tnm = js.getType().getName();
   JcompType jt2 = jcomp_typer.findType(tnm);
   if (jt2 != null) {
      JcompSymbol js1 = jt.lookupMethod(jcomp_typer,js.getName(),jt2);
      return js1;
    }
   List<JcompSymbol> cands = jt.lookupMethods(jcomp_typer,js.getName(),null);
   for (JcompSymbol njs : cands) {
      String ntnm = njs.getType().getName();
      if (ntnm.equals(tnm)) return njs;
    }
   return null;
}



private class AstMethod implements IfaceMethod {

   private JcompSymbol method_symbol;
   private Map<Object,Integer> local_map;
   private int local_count;
   private List<IfaceMethod> parent_methods;
   private Collection<IfaceMethod> child_methods;
   private Set<JcompSymbol> external_syms;
   private Map<Object,IfaceValue> external_values;

   AstMethod(JcompSymbol js) {
      method_symbol = js;
      local_map = null;
      local_count = 0;
      parent_methods = null;
      child_methods = null;
      external_values = null;
      external_syms = null;
    }

   JcompSymbol getJcompSymbol() 			{ return method_symbol; }
   void setJcompSymbol(JcompSymbol js) {
      method_symbol = js;
      local_map = null;
      local_count = 0;
      parent_methods = null;
      child_methods = null;
    }

   @Override public String getFullName()		{ return method_symbol.getFullName(); }
   @Override public String getName()			{ return method_symbol.getName(); }
   @Override public String getDescription() {
      return method_symbol.getType().getJavaTypeName();
    }
   @Override public String getFile() {
      JcompSource src = JcompAst.getSource(method_symbol.getNameNode());
      return src.getFileName();
    }

   @Override public boolean isStatic()			{ return method_symbol.isStatic(); }
   @Override public boolean isVarArgs() {
      return method_symbol.getType().isVarArgs();
    }
   @Override public boolean isStaticInitializer() {
      return method_symbol.getName().equals("<clinit>") ||
	  method_symbol.getName().equals(INITER_NAME);
    }
   @Override public boolean isConstructor() {
      return method_symbol.isConstructorSymbol();
    }
   @Override public boolean isNative() {
      return (method_symbol.getModifiers() & Modifier.NATIVE) != 0;
    }
   @Override public boolean isPrivate() 		{ return method_symbol.isPrivate(); }
   @Override public boolean isAbstract()		{ return method_symbol.isAbstract(); }
   @Override public boolean isSynchronized() {
      return (method_symbol.getModifiers() & Modifier.SYNCHRONIZED) != 0;
    }
   @Override public boolean isEditable()		{ return true; }
   @Override public boolean hasCode() {
      MethodDeclaration md = (MethodDeclaration) method_symbol.getDefinitionNode();
      if (md == null) return false;
      return md.getBody() != null;
    }

   @Override public IfaceType getReturnType() {
      return fait_control.findDataType(getType(method_symbol.getType().getBaseType()),
	    createAnnotations(method_symbol.getAnnotations()));
    }
   @Override public int getNumArgs() {
      return method_symbol.getType().getComponents().size();
    }
   @Override public IfaceType getArgType(int i) {
      List<JcompType> comps = method_symbol.getType().getComponents();
      if (i < 0 || i >= comps.size()) return null;
      return fait_control.findDataType(getType(comps.get(i)),getArgAnnotations(i));
    }
   @Override public IfaceType getDeclaringClass() {
      return fait_control.findDataType(getType(method_symbol.getClassType()),null);
    }
   @Override public List<IfaceType> getExceptionTypes() {
      MethodDeclaration md = (MethodDeclaration) method_symbol.getDefinitionNode();
      List<IfaceType> rslt = new ArrayList<>();
      for (Object o : md.thrownExceptionTypes()) {
	 Type tn = (Type) o;
	 JcompType jt = JcompAst.getJavaType(tn);
	 if (jt != null) rslt.add(fait_control.findDataType(getType(jt),null));
       }
      return rslt;
    }
   @Override synchronized public List<IfaceMethod> getParentMethods() {
      if (parent_methods == null) {
	 parent_methods = fait_control.findParentMethods(getDeclaringClass(),getName(),getDescription(),false,false,null);
       }
      return parent_methods;
    }

   @Override public Collection<IfaceMethod> getChildMethods() {
      if (child_methods == null) {
	child_methods = fait_control.findChildMethods(getDeclaringClass(),getName(),getDescription(),false,null);
       }
      return child_methods;
    }

   @Override synchronized public List<JcodeTryCatchBlock> getTryCatchBlocks() {
      return null;
    }

   @Override public IfaceProgramPoint getStart() {
      ASTNode n = method_symbol.getDefinitionNode();
      if (n == null) {
	 FaitLog.logE("No AST definition found for " + this);
       }
      return fait_control.getAstReference(n);
    }

   @Override public IfaceProgramPoint getNext(IfaceProgramPoint pt) {
      return null;
    }

   @Override public int getLocalSize() {
      checkLocals();
      return local_count;
    }

   @Override public int getLocalOffset(Object symbol) {
      checkLocals();
      Integer v = local_map.get(symbol);
      if (v == null) return -1;
      return v;
    }

   @Override public Object getItemAtOffset(int off,IfaceProgramPoint pt) {
      checkLocals();
      for (Map.Entry<Object,Integer> ent : local_map.entrySet()) {
	 if (ent.getValue() == off) {
	    return ent.getKey();
	  }
       }
      return null;
    }

   @Override public Collection<Object> getExternalSymbols() {
      checkLocals();
      if (external_syms == null) return null;
      return new ArrayList<>(external_syms);
    }

   @Override public void setExternalValue(Object sym,IfaceValue v) {
      if (v == null) return;
      checkLocals();
      if (external_values != null) {
	 IfaceValue v0 = external_values.get(sym);
	 IfaceValue v1 = v.mergeValue(v0);
	 external_values.put(sym,v1);
       }
    }

   @Override public IfaceValue getExternalValue(Object sym) {
      if (external_values != null) return external_values.get(sym);
      return null;
    }

   private void checkLocals() {
      if (local_map != null) return;
      LocalVisitor lv = new LocalVisitor();
      if (method_symbol.getDefinitionNode() != null) {
         method_symbol.getDefinitionNode().accept(lv);
       }
      local_count = lv.getLocalCount();
      local_map = lv.getLocalMap();
      external_syms = lv.getExternalSymbols();
      if (external_syms != null) external_values = new HashMap<>();
    }

   @Override public IfaceType getLocalType(int slot,IfaceProgramPoint at) {
      checkLocals();
      for (Map.Entry<Object,Integer> ent : local_map.entrySet()) {
	 if (ent.getValue() == slot && ent.getKey() instanceof JcompSymbol) {
	    JcompSymbol js = (JcompSymbol) ent.getKey();
	    return fait_control.findDataType(getType(js.getType()),createAnnotations(js.getAnnotations()));
	  }
       }
      return null;
    }

   @Override public List<IfaceAnnotation> getLocalAnnotations(int slot,IfaceProgramPoint pt) {
      checkLocals();
      for (Map.Entry<Object,Integer> ent : local_map.entrySet()) {
	 if (ent.getValue() == slot && ent.getKey() instanceof JcompSymbol) {
	    JcompSymbol js = (JcompSymbol) ent.getKey();
	    return getAnnotations(js);
	  }
       }
      return null;
    }

   @Override public List<IfaceAnnotation> getReturnAnnotations() {
      return getAnnotations(method_symbol);
    }

   @Override public List<IfaceAnnotation> getArgAnnotations(int i) {
      MethodDeclaration md = (MethodDeclaration) method_symbol.getDefinitionNode();
      if (md == null) return null;
      if (method_symbol.isConstructorSymbol()) {
	 if (method_symbol.getClassType().needsOuterClass()) {
	    if (i == 0) return null;
	    i = i-1;
	  }
       }
      SingleVariableDeclaration arg = (SingleVariableDeclaration) md.parameters().get(i);
      JcompSymbol argsym = JcompAst.getDefinition(arg);
      return getAnnotations(argsym);
    }

   private List<IfaceAnnotation> getAnnotations(JcompSymbol sym) {
      List<JcompAnnotation> jans = sym.getAnnotations();
      if (jans == null) return null;
      List<IfaceAnnotation> rslt = new ArrayList<>();
      for (JcompAnnotation ja : jans) {
	 rslt.add(createAnnotation(ja));
       }
      return rslt;
    }

   // @Override public int hashCode() {
      // return method_symbol.hashCode();
    // }

   // @Override public boolean equals(Object o) {
      // if (o instanceof AstMethod) {
	 // AstMethod am = (AstMethod) o;
	 // return am.method_symbol == method_symbol;
       // }
      // return false;
    // }

   @Override public String toString() {
      return method_symbol.toString();
    }

}	// end of inner class AstMethod




private static class LocalVisitor extends ASTVisitor {

   private Map<Object,Integer> local_map;
   private int local_count;
   private Set<JcompSymbol> external_syms;
   private MethodDeclaration top_method;

   LocalVisitor() {
      local_map = new HashMap<>();
      local_count = 0;
      top_method = null;
      external_syms = null;
    }

   int getLocalCount()				{ return local_count; }
   Map<Object,Integer> getLocalMap()		{ return local_map; }
   Set<JcompSymbol> getExternalSymbols()	{ return external_syms; }

   @Override public boolean visit(MethodDeclaration md) {
      if (top_method == null) top_method = md;
      if (!Modifier.isStatic(md.getModifiers())) {
	 local_map.put("this",local_count++);
	 JcompSymbol js = JcompAst.getDefinition(md);
	 if (js.isConstructorSymbol()) {
	    JcompType jt = js.getClassType();
	    if (jt.needsOuterClass()) {
	       String nm = "this$0";
	       local_map.put(nm,local_count++);
	     }
	  }
       }
      return true;
    }

   @Override public void endVisit(MethodDeclaration md) {
      if (md == top_method) {
	 top_method = null;
       }
    }

   @Override public boolean visit(SingleVariableDeclaration svd) {
      JcompSymbol d = JcompAst.getDefinition(svd);
      if (d != null && local_map.get(d) == null) {
	 local_map.put(d,local_count++);
	 if (d.getType().isCategory2()) ++local_count;
       }
      return true;
    }

   @Override public boolean visit(VariableDeclarationFragment vdf) {
      JcompSymbol d = JcompAst.getDefinition(vdf);
      if (d != null && local_map.get(d) == null) {
	 local_map.put(d,local_count++);
	 if (d.getType().isCategory2()) ++local_count;
       }
      return true;
    }

   @Override public void endVisit(SimpleName v) {
      JcompSymbol js = JcompAst.getReference(v);
      if (js == null || js.isFieldSymbol() || js.isEnumSymbol() ||
	    js.isTypeSymbol() || js.isMethodSymbol())
	 return;
      ASTNode an = js.getDefinitionNode();
      if (an == null) return;
      for (ASTNode pn = an; pn != null; pn = pn.getParent()) {
	 if (pn instanceof MethodDeclaration) {
	    if (pn != top_method) {
	       if (external_syms == null) external_syms = new HashSet<>();
	       external_syms.add(js);
	       local_map.put(js,local_count++);
	       if (js.getType().isCategory2()) ++local_count;
	     }
	    break;
	  }
       }
    }

   @Override public boolean visit(AnonymousClassDeclaration atd) {
      return false;
    }

   @Override public boolean visit(LambdaExpression e) {
      return false;
    }

}	// end of inner class LocalVisitor



/********************************************************************************/
/*										*/
/*	AST-based fields							*/
/*										*/
/********************************************************************************/

IfaceField getField(JcompSymbol js)
{
   if (js == null) return null;

   AstField f1 = field_map.get(js);
   if (f1 == null || f1.getJcompSymbol() != js) {
      f1 = new AstField(js);
      AstField f2 = field_map.putIfAbsent(js,f1);
      if (f2 != null) f1 = f2;
    }
   return f1;
}



void updateFields()
{
   Map<JcompSymbol,AstField> nfieldmap = new ConcurrentHashMap<>();
   for (AstField af : field_map.values()) {
      JcompSymbol js1 = af.getJcompSymbol();
      JcompType jt0 = js1.getClassType();
      if (jt0 == null) {
	 FaitLog.logE("Missing calss type for " + js1);
	 continue;
       }
      JcompType jt = jcomp_typer.findType(jt0.getName());
      if (jt == null) continue;
      JcompSymbol js2 = jt.lookupField(jcomp_typer,js1.getName());
      if (js2 == null) continue;
      af.setJcompSymbol(js2);
      nfieldmap.put(js2,af);
    }
   field_map = nfieldmap;
}



private class AstField implements IfaceField {

   private JcompSymbol field_symbol;

   AstField(JcompSymbol js) {
      field_symbol = js;
    }

   JcompSymbol getJcompSymbol() 			{ return field_symbol; }
   void setJcompSymbol(JcompSymbol js)			{ field_symbol = js; }

   @Override public String getFullName()		{ return field_symbol.getFullName(); }
   @Override public boolean isStatic()			{ return field_symbol.isStatic(); }
   @Override public boolean isFinal()			{ return field_symbol.isFinal(); }
   @Override public boolean isVolatile()		{ return field_symbol.isVolatile(); }
   @Override public String getKey()			{ return field_symbol.getFullName(); }

   @Override public IfaceValue getConstantValue() {
      if (!isStatic() || !isFinal()) return null;
      ASTNode n = field_symbol.getDefinitionNode();
      if (n == null) return null;
      Expression initv = null;
      if (n instanceof VariableDeclarationFragment) {
	 VariableDeclarationFragment vdf = (VariableDeclarationFragment) n;
	 initv = vdf.getInitializer();
       }
      else if (n instanceof FieldDeclaration) {
	 FieldDeclaration fd = (FieldDeclaration) n;
	 for (Object o : fd.fragments()) {
	    VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
	    JcompSymbol js = JcompAst.getDefinition(vdf.getName());
	    if (js == field_symbol) {
	       initv = vdf.getInitializer();
	       break;
	     }
	  }
       }
      if (initv == null) return null;
      IfaceType typ = getType();
      if (initv instanceof NumberLiteral) {
	 if (typ.isFloatingType()) {
	    try {
	       NumberLiteral nl = (NumberLiteral) initv;
	       double v = Double.parseDouble(nl.getToken());
	       return fait_control.findRangeValue(typ,v,v);
	     }
	    catch (NumberFormatException e) { }
	  }
	 else if (typ.isNumericType()) {
	    try {
	       NumberLiteral nl = (NumberLiteral) initv;
	       long v = Long.parseLong(nl.getToken());
	       return fait_control.findConstantValue(typ,v);
	     }
	    catch (NumberFormatException e) { }
	  }
       }
      else if (initv instanceof BooleanLiteral) {
	 if (typ.isBooleanType()) {
	    long v = ((BooleanLiteral) initv).booleanValue() ? 1 : 0;
	    return fait_control.findConstantValue(typ,v);
	  }
       }
      else if (initv instanceof StringLiteral) {
	 if (typ.isStringType()) {
	    return fait_control.findConstantStringValue(((StringLiteral) initv).getLiteralValue());
	  }
       }

      return null;
    }

   public IfaceType getDeclaringClass() {
      IfaceBaseType bt = ControlAstFactory.this.getType(field_symbol.getClassType());
      return fait_control.findDataType(bt,null);
    }

   @Override public IfaceType getType() {
      IfaceBaseType bt = ControlAstFactory.this.getType(field_symbol.getType());
      return fait_control.findDataType(bt,createAnnotations(field_symbol.getAnnotations()));
    }

   @Override public List<IfaceAnnotation> getAnnotations() {
      List<JcompAnnotation> jans = field_symbol.getAnnotations();
      if (jans == null) return null;
      List<IfaceAnnotation> rslt = new ArrayList<>();
      for (JcompAnnotation jan : jans) {
	 rslt.add(createAnnotation(jan));
       }
      return rslt;
    }

   @Override public boolean equals(Object o) {
      if (o instanceof AstField) {
	 AstField af = (AstField) o;
	 return af.field_symbol == field_symbol;
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
      return field_symbol.getFullName().hashCode();
    }

   @Override public String toString() {
      return field_symbol.getFullName();
    }

}	// end of inner class AstField



/********************************************************************************/
/*										*/
/*	Annotations								*/
/*										*/
/********************************************************************************/

AstAnnotation createAnnotation(JcompAnnotation an)
{
   return new AstAnnotation(an);
}


List<IfaceAnnotation> createAnnotations(List<JcompAnnotation> jans)
{
   if (jans == null || jans.isEmpty()) return null;
   List<IfaceAnnotation> rslt = new ArrayList<IfaceAnnotation>();
   for (JcompAnnotation jan : jans) {
      rslt.add(createAnnotation(jan));
    }
   return rslt;
}

IfaceAnnotation [] getAnnotations(ASTNode n)
{
   if (n instanceof AnnotatableType) {
      AnnotatableType at = (AnnotatableType) n;
      List<JcompAnnotation> annots = JcompAnnotation.getAnnotations(at.annotations());
      if (annots == null || annots.size() == 0) return null;
      IfaceAnnotation [] rslt = new IfaceAnnotation[annots.size()];
      for (int i = 0; i < annots.size(); ++i) {
	 rslt[i] = createAnnotation(annots.get(i));
       }
      return rslt;
    }
   else if (n instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) n;
      return getAnnotations(pt.getType());
    }
   else if (n instanceof ArrayType) {
      ArrayType at = (ArrayType) n;
      return getAnnotations(at.getElementType());
    }
   else if (n instanceof UnionType) {
      UnionType ut = (UnionType) n;
      IfaceAnnotation [] rslt = null;
      for (Object o : ut.types()) {
	 IfaceAnnotation [] nrslt = getAnnotations((ASTNode) o);
	 rslt = mergeAnnotations(rslt,nrslt);
       }
      return rslt;
    }
   else if (n instanceof IntersectionType) {
      IntersectionType ut = (IntersectionType) n;
      IfaceAnnotation [] rslt = null;
      for (Object o : ut.types()) {
	 IfaceAnnotation [] nrslt = getAnnotations((ASTNode) o);
	 rslt = mergeAnnotations(rslt,nrslt);
       }
      return rslt;
    }
   return null;
}



private IfaceAnnotation [] mergeAnnotations(IfaceAnnotation [] r1,IfaceAnnotation [] r2)
{
   if (r2 == null) return r1;
   if (r1 == null) return r2;
   List<IfaceAnnotation> add = new ArrayList<>();
   for (IfaceAnnotation a2 : r2) {
      boolean fnd = false;
      for (IfaceAnnotation a1 : r1) {
	 if (a1.equals(a2)) {
	    fnd = true;
	    break;
	  }
       }
      if (!fnd) add.add(a2);
    }
   if (add.isEmpty()) return r1;
   IfaceAnnotation [] rslt = new IfaceAnnotation[r1.length + add.size()];
   for (int i = 0; i < r1.length; ++i) rslt[i] = r1[i];
   int pos = r1.length;
   for (IfaceAnnotation a3 : add) {
      rslt[pos++] = a3;
    }
   return rslt;
}


private class AstAnnotation implements IfaceAnnotation {

   private JcompAnnotation for_annot;

   AstAnnotation(JcompAnnotation an) {
      for_annot = an;
    }

   @Override public String getAnnotationName() {
      JcompType jt = for_annot.getAnnotationType();
      while (jt.isParameterizedType()) jt = jt.getBaseType();
      String nm = jt.getName();
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
      Map<String,Object> map = for_annot.getValues();
      if (map == null) return null;
      return map.get(key);
    }

   @Override public String toString() {
      return for_annot.toString();
    }

}	// end of inner class AstAnnotation




/********************************************************************************/
/*										*/
/*	Program points								*/
/*										*/
/********************************************************************************/

IfaceProgramPoint getPoint(IfaceAstReference ar)
{
   return ar;
}


IfaceAstReference getAstReference(ASTNode n,ASTNode after,IfaceAstStatus sts)
{
   ControlAstReference car = new ControlAstReference(this,n,after,sts);
   ControlAstReference ncar = ref_map.putIfAbsent(car,car);
   if (ncar != null) car = ncar;
   return car;
}



private static class RefComparator implements Comparator<ControlAstReference> {

   @Override public int compare(ControlAstReference r1,ControlAstReference r2) {
      ASTNode e1 = r1.getAstNode();
      ASTNode e2 = r2.getAstNode();
      if (e1 != e2) {
	 if (e1 == null) return -1;
	 if (e2 == null) return 1;
	 int d = e1.hashCode() - e2.hashCode();
	 if (d < 0) return -1;
	 return 1;
       }
      ASTNode a1 = r1.getAfterChild();
      ASTNode a2 = r2.getAfterChild();
      if (a1 != a2) {
	 if (a1 == null) return -1;
	 if (a2 == null) return 1;
	 int d = a1.hashCode() - a2.hashCode();
	 if (d < 0) return -1;
	 return 1;
       }
      IfaceAstStatus s1 = r1.getStatus();
      IfaceAstStatus s2 = r2.getStatus();
      if (s1 == null && s2 == null) return 0;
      if (s1 == null && s2 != null) return -1;
      if (s1 != null && s2 == null) return 1;
      int s = s1.getReason().ordinal() - s2.getReason().ordinal();
      if (s < 0) return -1;
      if (s > 0) return 1;
      if (s1.getValue() == s2.getValue()) return 0;
      if (s1.getValue() == null) return -1;
      if (s2.getValue() == null) return 1;
      s = s1.getValue().hashCode() - s2.getValue().hashCode();
      if (s < 0) return -1;
      return 1;
    }

}	// end of inner class RefComparator




private void updateRefs()
{
   Map<ControlAstReference,ControlAstReference> nrefs = new ConcurrentSkipListMap<>();

   for (ControlAstReference car : ref_map.keySet()) {
      if (car.update()) {
	 nrefs.put(car,car);
       }
    }
   ref_map = nrefs;
}


ASTNode mapAstNode(ASTNode orig)
{
   if (orig == null) return null;

   ASTNode mthd = orig;
   List<ASTNode> children = new LinkedList<>();
   for ( ; mthd != null && mthd.getNodeType() != ASTNode.METHOD_DECLARATION; mthd = mthd.getParent()) {
      children.add(0,mthd);
    }
   if (mthd == null) return null;
   JcompSymbol msym = JcompAst.getDefinition(mthd);
   JcompSymbol nmsym = mapMethod(msym);
   if (nmsym == null) return null;
   ASTNode mnode = msym.getDefinitionNode();
   ASTNode nnode = nmsym.getDefinitionNode();
   if (mnode == nnode) return orig;

   ASTNode rslt = nnode;
   for (ASTNode child : children) {
      rslt = findCorrespondingNode(rslt,mnode,child);
      if (rslt == null) return null;
      mnode = child;
    }

   return rslt;
}



private ASTNode findCorrespondingNode(ASTNode start,ASTNode par,ASTNode child)
{
   if (par.getNodeType() != start.getNodeType()) return null;

   StructuralPropertyDescriptor spd = child.getLocationInParent();
   if (spd.isChildProperty()) {
      return (ASTNode) start.getStructuralProperty(spd);
    }
   else if (spd.isChildListProperty()) {
      List<?> oelts = (List<?>) par.getStructuralProperty(spd);
      int idx = oelts.indexOf(child);
      List<?> elts = (List<?>) start.getStructuralProperty(spd);
      if (idx >= elts.size()) return null;
      return (ASTNode) elts.get(idx);
    }

   return null;
}




}	// end of class ControlAstFactory




/* end of ControlAstFactory.java */
