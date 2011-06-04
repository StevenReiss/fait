/********************************************************************************/
/*										*/
/*		BcodeField.java 						*/
/*										*/
/*	description of class							*/
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

import java.lang.reflect.*;


class BcodeField extends FieldNode implements BcodeConstants, FaitField
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BcodeClass	in_class;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BcodeField(BcodeClass cls,int a,String n,String d,String s,Object val)
{
   super(a,n,d,s,val);

   in_class = cls;
}



/********************************************************************************/
/*										*/
/*	Access methods							       */
/*										*/
/********************************************************************************/

@Override public String getName()		{ return name; }

@Override public FaitDataType getType() {
   return in_class.getFactory().findDataType(desc);
}

@Override public FaitDataType getDeclaringClass() {
   return in_class.getDataType();
}

@Override public boolean isVolatile()
{
   if (Modifier.isVolatile(this.access)) return true;
   return false;
}

@Override public boolean isStatic()
{
   if (Modifier.isStatic(this.access)) return true;
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Debugging methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return getDeclaringClass().getName() + "." + name;
}




}	// end of class BcodeField




/* end of BcodeField.java */

