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
import edu.brown.cs.ivy.jcode.JcodeDataType;

import java.util.*;


abstract class ValueBase implements IfaceValue, ValueConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JcodeDataType	data_type;
private IfaceEntitySet	entity_set;
protected ValueFactory	value_factory;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected ValueBase(ValueFactory vf,JcodeDataType dt,IfaceEntitySet eset)
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

@Override public JcodeDataType getDataType()		{ return data_type; }

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
   return getNullFlags().canBeNull();
}


@Override public boolean mustBeNull()
{
   return getNullFlags().mustBeNull();
}


@Override public boolean testForNull()
{
   return getNullFlags().testForNull();
}


NullFlags getNullFlags()		      { return NullFlags.NON_NULL; }


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


@Override public boolean containsEntity(FaitEntity src)
{
   if (entity_set == null) return false;
   return entity_set.contains(src);
}


@Override public boolean isCategory2()			{ return false; }
@Override public boolean isNative()			{ return false; }
@Override public boolean isAllNative()			{ return false; }

@Override public boolean isGoodEntitySet()		{ return true; }



FaitControl getFaitControl()
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
@Override public ValueBase allowNull()			{ return this; }
@Override public ValueBase setTestNull()		{ return this; }


@Override public IfaceValue restrictByType(JcodeDataType dt,boolean proj,FaitLocation src)
{
   return this;
}

@Override public IfaceValue removeByType(JcodeDataType dt,FaitLocation src)
{
   return this;
}

@Override public IfaceValue makeSubtype(JcodeDataType dt)
{
   return this;
}



@Override public IfaceValue addEntity(IfaceEntitySet es)
{
   if (entity_set != null && entity_set.contains(es)) return this;
   if (es == null || es.isEmpty()) return this;

   return newEntityValue(es);
}

abstract protected IfaceValue newEntityValue(IfaceEntitySet es);



/********************************************************************************/
/*										*/
/*	Operation methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue performOperation(JcodeDataType dt,IfaceValue rhs,int op,FaitLocation loc)
{
   return value_factory.anyValue(dt);
}

@Override public TestBranch branchTest(IfaceValue rhs,int op)
{
   return TestBranch.ANY;
}



/********************************************************************************/
/*										*/
/*	Array access methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getArrayContents()				{ return null; }
boolean markArrayNonNull()				{ return false; }
boolean markArrayCanBeNull()				{ return false; }




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("[");
   buf.append(getDataType().getName());
   buf.append("]");
   return buf.toString();
}




}	// end of class ValueBase




/* end of ValueBase.java */

