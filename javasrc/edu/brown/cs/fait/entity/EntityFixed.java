/********************************************************************************/
/*										*/
/*		EntityFixed.java						*/
/*										*/
/*	Representation of fixed (generic) entities				*/
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



package edu.brown.cs.fait.entity;

import edu.brown.cs.fait.iface.*;

import java.util.*;


class EntityFixed extends EntityObject
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	is_mutable;
private IfaceValue	base_value;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

EntityFixed(IfaceType dt,boolean mutable,IfacePrototype ptyp)
{
   super(dt,ptyp);
   base_value = null;
   is_mutable = mutable;
   if (ptyp != null) ptyp.setAnyValue();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isNative()	
{ 
   IfaceType typ = getDataType();
   if (typ != null) {
      if (typ.isInProject()) return false;
    }
   return true; 
}


@Override public boolean isMutable()
{
   return is_mutable;
}


/********************************************************************************/
/*										*/
/*	Array access methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getArrayValue(IfaceValue idx,IfaceControl fc)
{
   IfaceValue fv = super.getArrayValue(idx,fc);
   if (fv != null) return fv;

   if (getDataType().isArrayType() && base_value == null) {
      IfaceType bty = getDataType().getBaseType();
      if (is_mutable || bty.isAbstract()) {
         base_value = fc.findMutableValue(bty);
       }
      else {
         base_value = fc.findNativeValue(bty);
       }
    }
   else if (base_value == null && getPrototype() != null) {
      base_value = getPrototype().getArrayContents(idx);
    }

   return base_value;
}




/********************************************************************************/
/*										*/
/*	Type conversion methods 						*/
/*										*/
/********************************************************************************/

@Override public Collection<IfaceEntity> mutateTo(IfaceType dt,EntityFactory factory)
{
   IfaceEntity eb = null;
   if (is_mutable && dt.isDerivedFrom(getDataType())) {
      if (dt.isInterfaceType() || dt.isAbstract() || dt.getChildTypes().size() > 0) {
	 eb = factory.createMutableEntity(dt);
       }
      else {
	 eb = factory.createFixedEntity(dt);
       }
    }
   else if (getDataType().isInterfaceType() || getDataType().isAbstract()) {
      if (dt.isInterfaceType()) {
	 IfaceType cdt = getDataType().findChildForInterface(dt);
	 if (cdt != null) {
	   eb = (EntityBase) factory.createFixedEntity(cdt);
	  }
       }
      else if (dt.isDerivedFrom(getDataType())) {
	 eb = (EntityBase) factory.createFixedEntity(dt);
       }
    }
   else if (is_mutable && getDataType().isJavaLangObject()) {
      if (dt.isInterfaceType() || dt.isAbstract()) {
	 eb = (EntityBase) factory.createMutableEntity(dt);
       }
    }
   else if (is_mutable && getDataType().isDerivedFrom(dt)) {
      if (FaitLog.isTracing()) 
         FaitLog.logD1("Mutable change " + getDataType() + " => " + dt);
    }
   else if (getDataType().getJavaType() == dt.getJavaType()) {
      eb = (EntityBase) factory.createFixedEntity(dt);
    }
   // else if same Java type then treat as mutable
   
   if (eb == null) return null;
   Collection<IfaceEntity> rslt = new ArrayList<IfaceEntity>();
   rslt.add(eb);
   return rslt;
}






/********************************************************************************/
/*										*/
/*	Output and debugging methods						*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuilder buf = new StringBuilder();
   buf.append("Fixed");
   if (is_mutable) buf.append("*");
   if (getPrototype() != null) buf.append("PROTO");
   buf.append(" ");
   buf.append(getDataType().getName());

   return buf.toString();
}



}	// end of class EntityFixed




/* end of EntityFixed.java */

