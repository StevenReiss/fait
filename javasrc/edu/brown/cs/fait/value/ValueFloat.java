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
import edu.brown.cs.ivy.xml.IvyXmlWriter;


class ValueFloat extends ValueNumber
{


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValueFloat(ValueFactory vf,IfaceType dt)
{
   this(vf,dt,null);
}

ValueFloat(ValueFactory vf,IfaceType dt,IfaceEntitySet es)
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
   if (!(fv instanceof ValueFloat)) {
      FaitLog.logD1("Bad float value merge: " + this + " " + fv);
      return value_factory.badValue();
    }

   if (getDataType().isBroaderType(fv.getDataType())) return this;
   if (fv.getDataType().isBroaderType(getDataType())) return fv;

   return this;
}


@Override public IfaceValue restrictByType(IfaceType dt)
{
   IfaceType nt = getDataType().restrictBy(dt);
   if (nt == getDataType()) return this;
   return value_factory.anyValue(nt);
}


@Override public IfaceValue changeType(IfaceType dt)
{
   if (dt == getDataType()) return this;
   return value_factory.anyValue(dt);
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected void outputLocalXml(IvyXmlWriter xw)
{
   xw.field("KIND","FLOAT");
}




}	// end of class ValueFloat




/* end of ValueFloat.java */

