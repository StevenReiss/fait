/********************************************************************************/
/*										*/
/*		BcodeDataType.java						*/
/*										*/
/*	Representaiton of java data types					*/
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

import org.objectweb.asm.*;

import java.util.*;


class BcodeDataType implements FaitDataType
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Type	base_type;
private BcodeFactory bcode_factory;
private BcodeDataType super_type;
private Collection<BcodeDataType> iface_types;
private Collection<BcodeDataType> child_types;
private int	modifier_values;
private boolean is_project;
private Map<FaitDataType,FaitDataType> parent_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BcodeDataType(Type t,BcodeFactory factory)
{
   base_type = t;
   bcode_factory = factory;
   super_type = null;
   iface_types = null;
   child_types = null;
   is_project = factory.isProjectClass(t.getClassName());
   parent_map = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()		{ return base_type.getClassName(); }
@Override public String getDescriptor() 	{ return base_type.getDescriptor(); }

@Override public boolean isArray()
{
   return base_type.getSort() == Type.ARRAY;
}
@Override public boolean isAbstract()
{
   return (modifier_values & FaitOpcodes.ACC_ABSTRACT) != 0;
}

@Override public boolean isCategory2()
{
   switch (base_type.getSort()) {
      case Type.LONG :
      case Type.DOUBLE :
	 return true;
    }
   return false;
}

@Override public boolean isBroaderType(FaitDataType c1)
{
   BcodeDataType c = (BcodeDataType) c1;
   if (this == c1) return true;
   if (base_type.getSort() == Type.BOOLEAN) return false;
   else if (c.base_type.getSort() == Type.BOOLEAN) return true;

   if (base_type.getSort() == Type.CHAR) return false;
   else if (c.base_type.getSort() == Type.CHAR) return true;

   int ln0 = classLength();
   int ln1 = c.classLength();

   return ln0 >= ln1;
}



private int classLength()
{
   switch (base_type.getSort()) {
      case Type.BOOLEAN :
      case Type.BYTE :
	 return 1;
      case Type.SHORT :
      case Type.CHAR :
	 return 2;
      case Type.INT :
      case Type.FLOAT :
	 return 4;
      case Type.LONG :
      case Type.DOUBLE :
	 return 8;
      default :
	 return 4;
    }
}




@Override public boolean isInterface()
{
   return (modifier_values & FaitOpcodes.ACC_INTERFACE) != 0;
}

@Override public boolean isProjectClass()
{
   return is_project;
}

@Override public FaitDataType getSuperType()		{ return super_type; }


@Override public FaitDataType getBaseDataType()
{
   if (!isArray()) return null;

   Type t = base_type.getElementType();
   return bcode_factory.findDataType(t);
}



@Override public FaitDataType getArrayType()
{
   String adesc = "[" + base_type.getDescriptor();
   Type t = Type.getType(adesc);
   return bcode_factory.findDataType(t);
}



@Override public boolean isJavaLangObject()
{
   return base_type.getClassName().equals("java.lang.Object");
}



@Override public boolean isInt()
{
   return base_type == Type.INT_TYPE;
}

@Override public boolean isVoid()
{
   return base_type.getSort() == Type.VOID;
}

@Override public boolean isFloating()
{
   switch (base_type.getSort()) {
      case Type.FLOAT :
      case Type.DOUBLE :
	 return true;
    }
   return false;
}


@Override public boolean isPrimitive()
{
   switch (base_type.getSort()) {
      case Type.VOID :
      case Type.BOOLEAN :
      case Type.CHAR :
      case Type.BYTE :
      case Type.SHORT :
      case Type.INT :
      case Type.FLOAT :
      case Type.LONG :
      case Type.DOUBLE :
	 return true;
    }
   return false;
}




/********************************************************************************/
/*										*/
/*	Type comparison methods 						*/
/*										*/
/********************************************************************************/

void noteModifiers(int mods)
{
   modifier_values = mods;
}



void noteSuperType(String st)
{
   if (st != null) {
      super_type = bcode_factory.findClassType(st);
      super_type.addChild(this);
    }
}


void noteInterfaces(Collection<?> ifs)
{
   if (ifs == null || ifs.size() == 0) return;
   if (iface_types == null) iface_types = new ArrayList<BcodeDataType>();
   for (Object o : ifs) {
      String iname = o.toString();
      BcodeDataType bdt = bcode_factory.findClassType(iname);
      iface_types.add(bdt);
      bdt.addChild(this);
    }
}


private synchronized void addChild(BcodeDataType bdt)
{
   if (child_types == null) child_types = new ArrayList<BcodeDataType>();
   child_types.add(bdt);
}




@Override public boolean isDerivedFrom(FaitDataType fdt)
{
   if (fdt == null) return false;

   BcodeDataType bdt = (BcodeDataType) fdt;

   if (bdt == this) return true;
   if (isPrimitive()) {
      if (!bdt.isPrimitive()) return false;
      int st = base_type.getSort();
      switch (bdt.base_type.getSort()) {
	 case Type.CHAR :
	 case Type.SHORT :
	    if (st == Type.CHAR || st == Type.SHORT || st == Type.BYTE) return true;
	    break;
	 case Type.INT :
	    if (st == Type.CHAR || st == Type.SHORT || st == Type.BYTE) return true;
	    break;
	 case Type.LONG :
	    if (st == Type.CHAR || st == Type.SHORT || st == Type.BYTE || st == Type.INT) return true;
	    break;
	 case Type.FLOAT :
	    if (st == Type.CHAR || st == Type.SHORT || st == Type.BYTE || st == Type.INT) return true;
	    break;
	 case Type.DOUBLE :
	    if (st == Type.CHAR || st == Type.SHORT || st == Type.BYTE || st == Type.INT ||
		   st == Type.FLOAT)
	       return true;
	    break;
       }
      return false;
    }

   if (super_type != null && super_type.isDerivedFrom(fdt)) return true;
   if (iface_types != null) {
      for (BcodeDataType ift : iface_types)
	 if (ift.isDerivedFrom(fdt)) return true;
    }

   return false;
}



@Override public FaitDataType findChildForInterface(FaitDataType ity)
{
   FaitDataType fdt0 = null;

   if (child_types != null) {
      for (BcodeDataType k : child_types) {
	 FaitDataType r = k.findChildForInterface(ity);
	 if (r != null) {
	    if (fdt0 == null) fdt0 = r;
	    else return ity;	// if more than one candidate, return the interface type
	  }
       }
    }

   if (fdt0 == null) {
      if (!isAbstract() && isDerivedFrom(ity)) fdt0 = this;
    }

   return fdt0;
}



Collection<BcodeDataType> getChildTypes()
{
   return child_types;
}



/********************************************************************************/
/*										*/
/*	Compute the common parent of two types					*/
/*										*/
/********************************************************************************/

@Override public FaitDataType findCommonParent(FaitDataType t2)
{
   synchronized (this) {
      if (parent_map == null) parent_map = new HashMap<FaitDataType,FaitDataType>();
    }

   boolean setother = false;

   FaitDataType bc = null;
   synchronized (parent_map) {
      bc = parent_map.get(t2);
      if (bc == null) {
	 bc = computeCommonParent(t2);
	 parent_map.put(t2,bc);
	 setother = true;
       }
    }

   if (setother) {
      BcodeDataType b2 = (BcodeDataType) t2;
      b2.setCommonParent(this,bc);
    }

   return bc;
}



private void setCommonParent(FaitDataType dt,FaitDataType rslt)
{
   synchronized (this) {
      if (parent_map == null) parent_map = new HashMap<FaitDataType,FaitDataType>();
    }
   synchronized (parent_map) {
      parent_map.put(dt,rslt);
    }
}



private FaitDataType computeCommonParent(FaitDataType t2a)
{
   if (this == t2a) return this;
   BcodeDataType t2 = (BcodeDataType) t2a;

   if (isInterface() && t2.isInterface()) return findCommonInterface(t2,null);
   else if (isInterface()) return t2.findCommonClassInterface(this);
   else if (t2.isInterface()) return findCommonClassInterface(t2);

   if (isArray() && t2.isArray()) return findCommonArray(t2);
   else if (isArray() || t2.isArray()) return bcode_factory.findDataType("Ljava/lang/Object;");

   if (isDerivedFrom(t2)) return t2;
   else if (t2.isDerivedFrom(this)) return this;

   for (BcodeDataType bdt = super_type; bdt != null; bdt = bdt.super_type) {
      if (bdt == t2 || t2.isDerivedFrom(bdt)) return bdt;
    }

   for (BcodeDataType bdt = t2.super_type; bdt != null; bdt = bdt.super_type) {
      if (bdt == this || t2.isDerivedFrom(bdt)) return bdt;
    }

   return bcode_factory.findDataType("Ljava/lang/Object;");
}



private FaitDataType findCommonInterface(BcodeDataType i2,Set<FaitDataType> done)
{
   if (isDerivedFrom(i2)) return i2;
   else if (i2.isDerivedFrom(this)) return this;

   if (done == null) done = new HashSet<FaitDataType>();
   if (super_type != null) {
      if (!done.contains(super_type)) {
	 done.add(super_type);
	 FaitDataType c = super_type.findCommonInterface(i2,done);
	 if (c.isInterface()) return c;
       }
    }
   for (BcodeDataType typ : iface_types) {
      if (done.contains(typ)) continue;
      done.add(typ);
      FaitDataType c = typ.findCommonInterface(i2,done);
      if (c.isInterface()) return c;
    }

   return bcode_factory.findDataType("Ljava/lang/Object;");
}



private FaitDataType findCommonClassInterface(BcodeDataType typ)
{
   return this;
}



private FaitDataType findCommonArray(BcodeDataType t2)
{
   if (this == t2) return this;

   FaitDataType rslt = null;

   FaitDataType ac1 = getBaseDataType();
   FaitDataType ac2 = t2.getBaseDataType();

   if (isDerivedFrom(t2)) rslt = t2;
   else if (t2.isDerivedFrom(this)) rslt = this;
   else if (ac1.isDerivedFrom(ac2)) rslt = t2;
   else if (ac2.isDerivedFrom(ac1)) rslt = this;
   else {
      FaitDataType fdt = ac1.findCommonParent(ac2);
      rslt = bcode_factory.findDataType("[" + fdt.getDescriptor());
    }

   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Debugging methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return base_type.toString();
}



}	// end of class BcodeDataType




/* end of BcodeDataType.java */

