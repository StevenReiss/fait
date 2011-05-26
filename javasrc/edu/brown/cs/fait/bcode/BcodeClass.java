/********************************************************************************/
/*										*/
/*		BcodeClass.java 						*/
/*										*/
/*	Internal representation of a Java class 				*/
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



package edu.brown.cs.fait.bcode;

import edu.brown.cs.fait.iface.*;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.*;

import java.util.*;



class BcodeClass extends ClassNode implements FaitClass
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Type	base_type;
private BcodeFactory bcode_factory;
private boolean in_project;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BcodeClass(BcodeFactory bf,boolean proj)
{
   super();
   bcode_factory = bf;
   base_type = null;
   in_project = proj;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()
{
   return base_type.getClassName();
}

FaitDataType getDataType()
{
   return bcode_factory.findDataType(base_type);
}


FaitDataType getDataType(Type t)
{
   return bcode_factory.findDataType(t);
}


@Override public boolean isInProject()
{
   return in_project;
}


BcodeFactory getFactory()			{ return bcode_factory; }


/********************************************************************************/
/*										*/
/*	Member access methods							*/
/*										*/
/********************************************************************************/

BcodeMethod findMethod(String nm,String desc)
{
   for (Object o : methods) {
      BcodeMethod bm = (BcodeMethod) o;
      if (bm.getName().equals(nm)) {
	 if (desc == null || bm.getDescription().equals(desc)) return bm;
       }
    }

   return null;
}



List<FaitMethod> findStaticInitializers()
{
   List<FaitMethod> rslt = new ArrayList<FaitMethod>();
   for (Object o : methods) {
      BcodeMethod bm = (BcodeMethod) o;
      if (bm.getName().equals("<clinit>")) rslt.add(bm);
    }
   return rslt;
}


BcodeField findField(String nm)
{
   for (Object o : fields) {
      BcodeField bf = (BcodeField) o;
      if (bf.getName().equals(nm)) return bf;
    }

   return null;
}



Collection<FaitMethod> findParentMethods(String nm,String desc,boolean check,boolean first,Collection<FaitMethod> rslt)
{
   if (rslt == null) rslt = new HashSet<FaitMethod>();
   
   if (first && !rslt.isEmpty()) return rslt;

   if (check) {
      BcodeMethod bm = findMethod(nm,desc);
      if (bm != null) { 
         rslt.add(bm);
         if (first) return rslt;
       }
    }

   BcodeClass bc = bcode_factory.findClassNode(superName);
   if (bc != null) bc.findParentMethods(nm,desc,true,first,rslt);

   for (Object o : interfaces) {
      bc = bcode_factory.findClassNode(o.toString());
      if (bc != null) bc.findParentMethods(nm,desc,true,first,rslt);
    }

   return rslt;
}



Collection<FaitMethod> findChildMethods(String nm,String desc,boolean check,Collection<FaitMethod> rslt)
{
   if (rslt == null) rslt = new HashSet<FaitMethod>();

   if (check) {
      BcodeMethod bm = findMethod(nm,desc);
      if (bm != null) rslt.add(bm);
    }

   BcodeDataType xdt = (BcodeDataType) getDataType();
   Collection<BcodeDataType> ctyps = xdt.getChildTypes();
   if (ctyps == null) return rslt;

   for (BcodeDataType dt : ctyps) {
      BcodeClass bc = bcode_factory.findClassNode(dt.getDescriptor());
      if (bc != null) bc.findChildMethods(nm,desc,true,rslt);
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Visit methods								*/
/*										*/
/********************************************************************************/

@Override public void visit(int v,int a,String name,String sgn,String sup,String [] ifaces)
{
   super.visit(v,a,name,sgn,sup,ifaces);
   base_type = Type.getObjectType(name);
   if (sup != null) bcode_factory.noteClass(sup);
   if (ifaces != null)
      for (String f : ifaces) bcode_factory.noteClass(f);
}


@Override public void visitEnd()
{
   BcodeDataType bdt = bcode_factory.findObjectType(name);
   bdt.noteSuperType(superName);
   bdt.noteInterfaces(interfaces);
   bdt.noteModifiers(access);
}


@SuppressWarnings("unchecked")
@Override public MethodVisitor visitMethod(int a,String n,String d,String sgn,String [] ex)
{
   if (ex != null) for (String s : ex) bcode_factory.noteClass(s);
   for (Type t : Type.getArgumentTypes(d)) bcode_factory.noteType(t.getDescriptor());
   bcode_factory.noteType(Type.getReturnType(d).getDescriptor());
   BcodeMethod bm = new BcodeMethod(bcode_factory,this,a,n,d,sgn,ex);
   methods.add(bm);
   return bm;
}


@SuppressWarnings("unchecked")
@Override public FieldVisitor visitField(int a,String n,String d,String sgn,Object val)
{
   bcode_factory.noteType(d);
   BcodeField bf = new BcodeField(this,a,n,d,sgn,val);
   fields.add(bf);
   return bf;
}




@Override public void visitInnerClass(String name,String oname,String iname,int acc)
{
   bcode_factory.noteClass(name);
   super.visitInnerClass(name,oname,iname,acc);
}



}	// end of class BcodeClass



/* end of BcodeClass.java */





