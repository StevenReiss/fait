/********************************************************************************/
/*										*/
/*		ValueFloat.java 						*/
/*										*/
/*	Representation of a floating point numberic value			*/
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


class ValueFloat extends ValueNumber
{


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValueFloat(ValueFactory vf,FaitDataType dt)
{
   this(vf,dt,null);
}

ValueFloat(ValueFactory vf,FaitDataType dt,IfaceEntitySet es)
{
   super(vf,dt,es);
}




/********************************************************************************/
/*										*/
/*	Methods for merging values						*/
/*										*/
/********************************************************************************/

@Override public IfaceValue mergeValue(IfaceValue fv)
{
   if (fv == this || fv == null) return this;
   if (!(fv instanceof ValueFloat)) return value_factory.badValue();

   if (getDataType().isBroaderType(fv.getDataType())) return this;
   if (fv.getDataType().isBroaderType(getDataType())) return fv;

   return this;
}


@Override protected IfaceValue newEntityValue(IfaceEntitySet cs)
{
   cs = cs.addToSet(getEntitySet());

   IfaceValue nv = this;

   nv = new ValueFloat(value_factory,getDataType(),cs);

   return nv;
}



}	// end of class ValueFloat




/* end of ValueFloat.java */

