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
import edu.brown.cs.ivy.jcode.JcodeDataType;

import java.util.*;


class ValueObject extends ValueBase implements JcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NullFlags	null_flags;
private Map<JcodeDataType,ValueBase> restrict_map;
private Map<JcodeDataType,ValueBase> remove_map;
private ValueBase	nonnull_value;
private ValueBase	testnull_value;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValueObject(ValueFactory vf,JcodeDataType typ,IfaceEntitySet es,NullFlags fgs)
{
   super(vf,typ,es);

   null_flags = fgs;
   restrict_map = null;
   remove_map = null;
   nonnull_value = null;
   testnull_value = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override NullFlags getNullFlags()	
{
   if (null_flags != null) return null_flags;
   return super.getNullFlags();
}



/********************************************************************************/
/*										*/
/*	Methods for operations							*/
/*										*/
/********************************************************************************/

@Override public ValueBase restrictByType(JcodeDataType dt,boolean proj,FaitLocation src)
{
   synchronized (this) {
      if (restrict_map == null) restrict_map = new HashMap<JcodeDataType,ValueBase>(4);
    }

   synchronized (restrict_map) {
      ValueBase nv = restrict_map.get(dt);
      if (nv == null) {
	 IfaceEntitySet ns = getEntitySet().restrictByType(dt,proj,src);
	 if (ns == getEntitySet()) {
	    nv = this;
	    if (ns.isEmpty() && mustBeNull() && dt != getDataType()) {
	       nv = value_factory.nullValue(dt);
	     }
	  }
	 else if (ns.isEmpty()) {
	    if (canBeNull()) nv = value_factory.nullValue(dt);
	    else nv = value_factory.emptyValue(dt,null_flags);
	  }
	 else {
	    JcodeDataType ndt = getSetType(ns);
	    if (ndt != null) nv = value_factory.objectValue(ndt,ns,null_flags);
	    else if (canBeNull()) nv = value_factory.nullValue(dt);
	    else nv = value_factory.objectValue(dt,ns,null_flags);
	  }
	 restrict_map.put(dt,nv);
       }
      return nv;
    }
}



@Override public IfaceValue removeByType(JcodeDataType dt,FaitLocation loc)
{
   synchronized (this) {
      if (remove_map == null) remove_map = new HashMap<JcodeDataType,ValueBase>();
    }

   synchronized (remove_map) {
      ValueBase nv = remove_map.get(dt);
      if (nv == null) {
	 IfaceEntitySet es = getEntitySet().removeByType(dt,loc);
	 if (es == getEntitySet()) nv = this;
	 else if (es.isEmpty()) {
	    if (canBeNull()) nv = value_factory.nullValue();
	    else nv = null;
	  }
	 else {
	    JcodeDataType ndt = getSetType(es);
	    if (ndt != null) nv = value_factory.objectValue(ndt,es,null_flags);
	    else if (canBeNull()) nv = value_factory.nullValue();
	    else nv = null;
	  }
	 remove_map.put(dt,nv);
       }
      return nv;
    }
}



@Override public IfaceValue makeSubtype(JcodeDataType dt)
{
   if (dt != getDataType() && dt.isDerivedFrom(getDataType())) {
      return value_factory.objectValue(dt,getEntitySet(),null_flags);
    }
   return this;
}


@Override public ValueBase forceNonNull()
{
   if (!canBeNull()) return this;

   if (nonnull_value == null) {
      NullFlags nfg = null_flags.forceNonNull();
      nonnull_value = value_factory.objectValue(getDataType(),getEntitySet(),nfg);
    }

   return nonnull_value;
}



@Override public ValueBase allowNull()
{
   if (canBeNull()) return this;

   if (nonnull_value == null) {
      nonnull_value = value_factory.objectValue(getDataType(),getEntitySet(),
	    NullFlags.CAN_BE_NULL);
    }

   return nonnull_value;
}



@Override public ValueBase setTestNull()
{
   if (testForNull()) return this;

   if (testnull_value == null) {
      NullFlags nfg = null_flags.forceTestForNull();
      testnull_value =value_factory.objectValue(getDataType(),
	    getEntitySet(),nfg);
    }

   return testnull_value;
}



@Override public ValueBase mergeValue(IfaceValue cv)
{
   if (cv == this || cv == null) return this;

   if (!(cv instanceof ValueObject)) {
      return value_factory.badValue();
    }

   ValueObject cvo = (ValueObject) cv;
   NullFlags fgs = null_flags.merge(cvo.getNullFlags());
   IfaceEntitySet es = getEntitySet().addToSet(cvo.getEntitySet());

   if (es == getEntitySet() &&
	 (getDataType() == cvo.getDataType() || !es.isEmpty()) &&
	 fgs == null_flags)
      return this;

   if (es == cvo.getEntitySet() &&
	 (getDataType() == cvo.getDataType() || !es.isEmpty()) &&
	 fgs == cvo.getNullFlags())
      return cvo;

   JcodeDataType typ = getSetType(es);
   if (typ == null) typ = getDataType().findCommonParent(cvo.getDataType());

   return value_factory.objectValue(typ,es,fgs);
}



@Override public ValueBase newEntityValue(IfaceEntitySet es)
{
   IfaceEntitySet nes = getEntitySet().addToSet(es);

   return new ValueObject(value_factory,getDataType(),nes,getNullFlags());
}



/********************************************************************************/
/*										*/
/*	Operation handling							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue performOperation(JcodeDataType typ,IfaceValue rhs,int op,FaitLocation src)
{
   switch (op) {
      case JcodeConstants.INSTANCEOF :
	 if (canBeNull()) break;
	 ValueBase ncv = restrictByType(rhs.getDataType(),false,src);
	 if (ncv.isEmptyEntitySet())
	    return value_factory.rangeValue(typ,0,0);
	 if (ncv == this)
	    return value_factory.rangeValue(typ,1,1);
	 break;
    }

   return super.performOperation(typ,rhs,op,src);
}



@Override public TestBranch branchTest(IfaceValue rhs,int op)
{
   if (rhs == null) rhs = this;

   if (!(rhs instanceof ValueObject)) return TestBranch.ANY;

   ValueObject vo = (ValueObject) rhs;
   TestBranch r = TestBranch.ANY;

   switch (op) {
      case IF_ACMPEQ :
	 if (mustBeNull() && vo.mustBeNull()) r = TestBranch.ALWAYS;
	 else if (mustBeNull() && !vo.canBeNull()) r = TestBranch.NEVER;
	 else if (!canBeNull() && vo.mustBeNull()) r = TestBranch.NEVER;
	 break;
      case IF_ACMPNE :
	 if (mustBeNull() && vo.mustBeNull()) r = TestBranch.NEVER;
	 else if (mustBeNull() && !vo.canBeNull()) r = TestBranch.ALWAYS;
	 else if (!canBeNull() && vo.mustBeNull()) r = TestBranch.ALWAYS;
	 break;
      case IFNONNULL :
	 if (mustBeNull()) r = TestBranch.NEVER;
	 else if (!canBeNull()) r = TestBranch.ALWAYS;
	 break;
      case IFNULL :
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

private static JcodeDataType getSetType(IfaceEntitySet es)
{
   JcodeDataType typ = null;

   for (IfaceEntity ie : es.getEntities()) {
      JcodeDataType styp = ie.getDataType();
      if (styp != null) {
	 if (typ == null) typ = styp;
	 else typ = typ.findCommonParent(styp);
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
      IfaceValue cv = (IfaceValue) ent.getArrayValue(null,getFaitControl());
      if (cv != null) {
	 if (cnts == null) cnts = cv;
	 else cnts = cnts.mergeValue(cv);
       }
    }

   return cnts;
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
   rslt.append(getDataType().getName());
   if (mustBeNull()) rslt.append(" =null");
   else if (canBeNull()) rslt.append(" ?null");
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

