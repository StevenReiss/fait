/********************************************************************************/
/*										*/
/*		ValueObject.java						*/
/*										*/
/*	Class-based values							*/
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
import edu.brown.cs.ivy.jcode.JcodeConstants;

import java.util.*;


class ValueObject extends ValueBase implements JcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<IfaceType,ValueBase> restrict_map;
private Map<IfaceType,ValueBase> change_map;
private ValueBase	nonnull_value;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValueObject(ValueFactory vf,IfaceType typ,IfaceEntitySet es,IfaceAnnotation ... fgs)
{
   super(vf,typ,es);

   restrict_map = null;
   change_map = null;
   nonnull_value = null;
}



/********************************************************************************/
/*										*/
/*	Methods for operations							*/
/*										*/
/********************************************************************************/

@Override public ValueBase restrictByType(IfaceType dt)
{
   synchronized (this) {
      if (restrict_map == null) restrict_map = new HashMap<>(4);
    }

   synchronized (restrict_map) {
      ValueBase nv = restrict_map.get(dt);
      if (nv == null) {
	 IfaceEntitySet ns = getEntitySet().restrictByType(dt);
	 if (ns == getEntitySet()) {
	    nv = this;
	    if (ns.isEmpty() && mustBeNull() && !getDataType().isCompatibleWith(dt)) {
	       nv = value_factory.nullValue(dt);
	     }
            else if (dt != getDataType()) {
               IfaceType ndt = getDataType().restrictBy(dt);
               if (ndt != getDataType()) {
                  nv = value_factory.objectValue(ndt,ns);
                }
             }
	  }
	 else if (ns.isEmpty()) {
            if (dt.isPrimitiveType()) nv = value_factory.anyValue(dt);
	    else if (canBeNull() && dt.isDerivedFrom(getDataType())) {
               nv = value_factory.nullValue(dt);
             }
	    else nv = value_factory.emptyValue(dt);
	  }
	 else {
	    IfaceType ndt = getSetType(ns);
            ndt = ndt.getAnnotatedType(dt);
	    if (ndt != null) nv = value_factory.objectValue(ndt,ns);
	    else if (canBeNull()) nv = value_factory.nullValue(dt);
	    else nv = value_factory.objectValue(dt,ns);
	  }
	 restrict_map.put(dt,nv);
       }
      return nv;
    }
}



@Override public IfaceValue changeType(IfaceType dt)
{
   if (dt == getDataType()) return this;
   
   synchronized (this) {
      if (change_map == null) change_map = new HashMap<>(4);
    }
   
   synchronized (change_map) {
      ValueBase nv = change_map.get(dt);
      if (nv == null) {
         nv = value_factory.objectValue(dt,getEntitySet());
         change_map.put(dt,nv);
       }
      return  nv;
    }
}






@Override public IfaceValue makeSubtype(IfaceType dt)
{
   if (dt != getDataType() && dt.isDerivedFrom(getDataType())) {
      return value_factory.objectValue(dt,getEntitySet());
    }
   return this;
}


@Override public ValueBase forceNonNull()
{
   if (!canBeNull()) return this;

   if (nonnull_value == null) {
      nonnull_value = value_factory.objectValue(getDataType(),getEntitySet(),FaitAnnotation.NON_NULL);
    }

   return nonnull_value;
}


@Override public ValueBase forceInitialized(FaitAnnotation what)
{
   IfaceType t0 = getDataType().getAnnotatedType(what);
   if (t0 == getDataType()) return this;
   
   ValueBase v1 = value_factory.objectValue(t0,getEntitySet());
   
   return v1;
}



@Override public ValueBase allowNull()
{
   if (canBeNull()) return this;

   if (nonnull_value == null) {
      nonnull_value = value_factory.objectValue(getDataType(),getEntitySet(),
	    FaitAnnotation.NULLABLE);
    }

   return nonnull_value;
}







@Override public ValueBase mergeValue(IfaceValue cv)
{
   if (cv == this || cv == null) return this;

   if (!(cv instanceof ValueObject)) {
      FaitLog.logD1("Invalidate variable: Bad value merge: " + this + " " + cv);
      return value_factory.badValue();
    }

   ValueObject cvo = (ValueObject) cv;
   IfaceEntitySet es = getEntitySet().addToSet(cvo.getEntitySet());
   IfaceType t1 = findCommonParent(getDataType(),cvo.getDataType());
   
   if (es == getEntitySet() && getDataType() == t1)
      return this;

   if (es == cvo.getEntitySet() && t1 == cvo.getDataType())
      return cvo;

   IfaceType typ = getSetType(es);
   if (typ == null) typ = t1;
   else typ = typ.getAnnotatedType(t1);

   return value_factory.objectValue(typ,es);
}







/********************************************************************************/
/*										*/
/*	Operation handling							*/
/*										*/
/********************************************************************************/

@Override protected IfaceValue localPerformOperation(IfaceType typ,IfaceValue rhs,
      FaitOperator op,IfaceLocation src)
{
   switch (op) {
      case INSTANCEOF :
	 // if (canBeNull()) break;
	 ValueBase ncv = restrictByType(rhs.getDataType());
	 if (ncv.isEmptyEntitySet())
	    return value_factory.rangeValue(typ,0l,0l);
	 if (ncv == this)
	    return value_factory.rangeValue(typ,1l,1l);
	 break;
      case EQL :
         if (rhs == this) return value_factory.rangeValue(typ,1l,1l);
         if (rhs.mustBeNull() && mustBeNull()) return value_factory.rangeValue(typ,1l,1l);
         if (rhs.mustBeNull() && !canBeNull()) return value_factory.rangeValue(typ,0l,0l);
         if (!rhs.canBeNull() && mustBeNull()) return value_factory.rangeValue(typ,0l,0l);
         break;
      case NEQ :
         if (rhs == this) return value_factory.rangeValue(typ,0l,0l);
         if (rhs.mustBeNull() && mustBeNull()) return value_factory.rangeValue(typ,0l,0l);
         if (rhs.mustBeNull() && !canBeNull()) return value_factory.rangeValue(typ,1l,1l);
         if (!rhs.canBeNull() && mustBeNull()) return value_factory.rangeValue(typ,1l,1l);
         break;
    }

   return super.localPerformOperation(typ,rhs,op,src);
}


@Override public IfaceImplications getImpliedValues(IfaceValue rhsv,FaitOperator op)
{
   ValueBase rhs = (ValueBase) rhsv;
   ValueImplications imp = null;
   ValueBase lt = null;
   ValueBase lf = null;
   ValueBase rt = null;
   ValueBase rf = null;
   IfaceType rtyp = null;
   if (rhs != null) rtyp = rhs.getDataType();
   IfaceTypeImplications timp = getDataType().getImpliedTypes(op,rtyp);
   
   switch (op) {  
      case NULL :
         lt = value_factory.nullValue(timp.getLhsTrueType());
         lf = forceNonNull();
         break;
      case NONNULL :
         lt = forceNonNull();
         lf = value_factory.nullValue(timp.getLhsFalseType());
         break;
      case EQL :
         if (rhs.mustBeNull()) {
            lt = value_factory.nullValue(timp.getLhsTrueType());
            if (!mustBeNull()) lf = forceNonNull();
          }
         else if (mustBeNull()) {
            rt = value_factory.nullValue(timp.getRhsTrueType());
            if (!rhs.mustBeNull()) rf = forceNonNull();
          }
         else {
            // IfaceEntitySet ls = getEntitySet();
            // IfaceEntitySet rs = rhs.getEntitySet();
            // compute ls intersect rs ?
            // but allow generic things to match
          }
         break;
      case NEQ :
         if (rhs.mustBeNull()) {
            lf = value_factory.nullValue(timp.getLhsFalseType());
            lf = forceNonNull();
          }
         else if (mustBeNull()) {
            rf = value_factory.nullValue(timp.getRhsFalseType());
            rt = forceNonNull();
          }
         break;
    }
   
   if (lf == this) lf = null;
   if (lt == this) lt = null;
   if (lf != null || lt != null) {
      imp = new ValueImplications();
      imp.setLhsValues(lt,lf);
    }
   if (rf == rhs) rf = null;
   if (rt == rhs) rt = null;
   if (rt != null || rf != null) {
      if (imp == null) imp = new ValueImplications();
      imp.setRhsValues(rt,rf);
    }
   
   return imp;
}



@Override public TestBranch branchTest(IfaceValue rhs,FaitOperator op)
{
   if (rhs == null) rhs = this;

   if (!(rhs instanceof ValueObject)) return TestBranch.ANY;

   ValueObject vo = (ValueObject) rhs;
   TestBranch r = TestBranch.ANY;

   switch (op) {
      case EQL :
	 if (mustBeNull() && vo.mustBeNull()) r = TestBranch.ALWAYS;
	 else if (mustBeNull() && !vo.canBeNull()) r = TestBranch.NEVER;
	 else if (!canBeNull() && vo.mustBeNull()) r = TestBranch.NEVER;
	 break;
      case NEQ :
	 if (mustBeNull() && vo.mustBeNull()) r = TestBranch.NEVER;
	 else if (mustBeNull() && !vo.canBeNull()) r = TestBranch.ALWAYS;
	 else if (!canBeNull() && vo.mustBeNull()) r = TestBranch.ALWAYS;
	 break;
      case NONNULL :
	 if (mustBeNull()) r = TestBranch.NEVER;
	 else if (!canBeNull()) r = TestBranch.ALWAYS;
	 break;
      case NULL :
	 if (mustBeNull()) r = TestBranch.ALWAYS;
	 else if (!canBeNull()) r = TestBranch.NEVER;
	 break;
    }

   return r;
}

@Override public boolean isNative()
{
   for (IfaceEntity ent : getEntities()) {
      if (ent.isNative()) return true;
    }

   return false;
}


@Override public boolean isAllNative()
{
   int ct = 0;
   for (IfaceEntity ent : getEntities()) {
      if (!ent.isNative()) return false;
      ++ct;
    }

   if (ct == 0) return false;

   return true;
}


@Override public boolean isGoodEntitySet()
{
   if (mustBeNull()) return true;

   for (IfaceEntity ent : getEntities()) {
      if (ent.getDataType() != null) return true;
    }

   return false;
}





/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private IfaceType getSetType(IfaceEntitySet es)
{
   IfaceType typ = null;

   for (IfaceEntity ie : es.getEntities()) {
      IfaceType styp = ie.getDataType();
      if (styp != null) {
	 if (typ == null) typ = styp;
	 else typ = findCommonParent(typ,styp);
       }
    }

   return typ;
}



/********************************************************************************/
/*										*/
/*     Array methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getArrayContents()
{
   IfaceValue cnts = null;

   for (IfaceEntity ent : getEntities()) {
      IfaceValue cv = ent.getArrayValue(null,getFaitControl());
      if (cv != null && !cv.isEmptyEntitySet()) {
	 if (cnts == null) cnts = cv;
	 else cnts = cnts.mergeValue(cv);
       }
    }

   return cnts;
}



@Override public IfaceValue getArrayContents(IfaceValue idx)
{
   IfaceValue cv = null;
   boolean nat = false;
   
   for (IfaceEntity xe : getEntities()) {
      if (xe.getDataType().isArrayType()) {
	 IfaceValue cv1 = xe.getArrayValue(idx,getFaitControl());
	 if (cv == null) cv = cv1;
	 else cv = cv.mergeValue(cv1);
       }
      else if (xe.isNative()) nat = true;
    }
   
   if (cv == null) {
      IfaceType base = getDataType();
      if (base == null || !base.isArrayType()) return null;
      else base = base.getBaseType();
      if (nat) cv = value_factory.nativeValue(base);
      else cv = value_factory.nullValue(base);
    }
   
   return cv;
}


@Override public IfaceValue getArrayLength()
{
   IfaceValue cv = null;
   for (IfaceEntity xe : getEntities()) {
      if (FaitLog.isTracing()) {
         FaitLog.logD1("Array length entity: " + xe + "(" + xe.hashCode() + ") = " + xe.getFieldValue("length"));
       }
      if (xe.getDataType().isArrayType()) {
         IfaceValue cv1 = xe.getFieldValue("length");
         if (cv1 == null) 
            return super.getArrayLength();
         if (cv == null) cv = cv1;
         else cv = cv.mergeValue(cv1);
       }
    }
   if (cv == null) return super.getArrayLength();
   
   return cv;
}



/********************************************************************************/
/*										*/
/*	Output Methods								*/
/*										*/
/********************************************************************************/


@Override public String toString()
{
   StringBuffer rslt = new StringBuffer();

   rslt.append("[");
   rslt.append(getDataType());
   rslt.append(" :: ");
   rslt.append(hashCode());
   rslt.append(" :: ");
   rslt.append(getEntitySet().size());
   if (getEntitySet().size() == 1) {
      rslt.append(" { ");
      for (IfaceEntity ent : getEntities()) {
	 rslt.append(ent.toString());
	 rslt.append(" ");
       }
      rslt.append("} ");
    }
   rslt.append("]");

   return rslt.toString();
}








}	// end of class ValueObject




/* end of ValueObject.java */

