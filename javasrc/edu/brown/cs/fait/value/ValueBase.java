/********************************************************************************/
/*										*/
/*		ValueBase.java							*/
/*										*/
/*	Generic representaiton of a value for static checking			*/
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
import edu.brown.cs.fait.type.CheckNullness;

import java.util.*;



abstract class ValueBase implements IfaceValue, ValueConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceType	data_type;
private IfaceEntitySet	entity_set;
protected ValueFactory	value_factory;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected ValueBase(ValueFactory vf,IfaceType dt,IfaceEntitySet eset)
{
   value_factory = vf;
   data_type = dt;
   entity_set = eset;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceType getDataType()		{ return data_type; }

protected IfaceEntitySet getEntitySet() 		{ return entity_set; }

@Override public IfaceEntitySet getModelEntitySet()
{
   if (entity_set == null || entity_set.isEmpty()) return null;
   IfaceEntitySet ms = entity_set.getModelSet();
   if (ms.isEmpty()) return null;
   return ms;
}


@Override public boolean canBeNull()
{
   return !data_type.checkValue(CheckNullness.NullState.NON_NULL);
}


@Override public boolean mustBeNull()
{
   return data_type.checkValue(CheckNullness.NullState.MUST_BE_NULL);
}








@Override public boolean isEmptyEntitySet()
{
   if (entity_set == null) return true;

   return entity_set.isEmpty();
}

@Override public boolean isBad()		{ return false; }


@Override public Iterable<IfaceEntity> getEntities()
{
   if (entity_set == null) return Collections.emptyList();
   return entity_set.getEntities();
}


@Override public boolean containsEntity(IfaceEntity src)
{
   if (entity_set == null) return false;
   return entity_set.contains(src);
}


@Override public boolean isCategory2()			{ return false; }
@Override public boolean isNative()			{ return false; }
@Override public boolean isAllNative()			{ return false; }
@Override public boolean isMutable()                   
{
   if (entity_set != null) {
      for (IfaceEntity ie : entity_set.getEntities()) {
         if (ie.isMutable()) return true;
       }
    }
   return false;
}

@Override public boolean isGoodEntitySet()		{ return true; }



IfaceControl getFaitControl()
{
   return value_factory.getFaitControl();
}




/********************************************************************************/
/*										*/
/*	Change methods								*/
/*										*/
/********************************************************************************/

@Override abstract public IfaceValue mergeValue(IfaceValue v);

@Override public ValueBase forceNonNull()		{ return this; }
@Override public IfaceValue forceInitialized(FaitAnnotation what)
{
   IfaceType t0 = getDataType().getAnnotatedType(what);
   if (t0 == getDataType()) return this;
   
   return restrictByType(t0);
}
@Override public ValueBase allowNull()			{ return this; }







@Override public IfaceValue makeSubtype(IfaceType dt)
{
   return this;
}









/********************************************************************************/
/*										*/
/*	Operation methods							*/
/*										*/
/********************************************************************************/

@Override public final IfaceValue performOperation(IfaceType dt,IfaceValue rhs,FaitOperator op,IfaceLocation loc)
{
   IfaceValue iv = localPerformOperation(dt,rhs,op,loc);
   IfaceType ntyp = dt.getComputedType(iv,op,this,rhs);
   IfaceType xtyp = iv.getDataType();
   if (xtyp != ntyp) {
      iv = iv.changeType(ntyp);
    }
   return iv;
}


@Override public IfaceType checkOperation(FaitOperator op,IfaceValue set)
{
   IfaceType dt = getDataType();
   IfaceType nt = dt.getComputedType(this,op,set);
   if (nt == null || nt == dt) return null;
   
   return nt;
}


protected IfaceValue localPerformOperation(IfaceType dt,IfaceValue rhs,FaitOperator op,IfaceLocation loc)
{
   return value_factory.anyValue(dt);
}

@Override public IfaceImplications getImpliedValues(IfaceValue rhs,FaitOperator op)
{
   return null;
}

@Override public TestBranch branchTest(IfaceValue rhs,FaitOperator op)
{
   return TestBranch.ANY;
}



/********************************************************************************/
/*										*/
/*	Array access methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getArrayContents()		{ return null; }
@Override public IfaceValue getArrayContents(IfaceValue idx) 
{
   return getArrayContents();
}
@Override public IfaceValue getArrayLength()
{
   IfaceType it = value_factory.getFaitControl().findDataType("int");
   return value_factory.anyValue(it);
}

boolean markArrayNonNull()				{ return false; }
boolean markArrayCanBeNull()				{ return false; }




/********************************************************************************/
/*                                                                              */
/*      Reference access methods                                                */
/*                                                                              */
/********************************************************************************/

@Override public boolean isReference()                  { return false; }
@Override public int getRefSlot()                       { return NO_REF; }
@Override public int getRefStack()                      { return NO_REF; }
@Override public IfaceValue getRefBase()                { return null; }
@Override public IfaceField getRefField()               { return null; }
@Override public IfaceValue getRefIndex()               { return null; }

@Override public IfaceValue toFloating()                { return this; }
@Override public Integer getIndexValue()                { return null; }
@Override public String getStringValue()                { return null; }
@Override public Long getMinValue()                     { return null; }
@Override public Long getMaxValue()                     { return null; }




/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected IfaceType findCommonParent(IfaceType t1,IfaceType t2)
{
   return value_factory.getFaitControl().findCommonParent(t1,t2);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("[");
   buf.append(getDataType());
   buf.append("]");
   return buf.toString();
}




}	// end of class ValueBase




/* end of ValueBase.java */

