/********************************************************************************/
/*										*/
/*		ProtoBase.java							*/
/*										*/
/*	Basic prototype implementation						*/
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



package edu.brown.cs.fait.proto;

import edu.brown.cs.fait.iface.*;

import java.lang.reflect.Method;
import java.util.*;



abstract class ProtoBase implements IfacePrototype, ProtoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/
protected IfaceControl	fait_control;
private IfaceType	proto_type;
private Map<IfaceMethod,Method> method_map;

private static Class<?> [] call_params = new Class<?> [] {
   IfaceMethod.class, List.class, IfaceLocation.class
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected ProtoBase(IfaceControl fc,IfaceType base)
{
   fait_control = fc;
   proto_type = base;
   method_map = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

IfaceType getDataType()			{ return proto_type; }




/********************************************************************************/
/*										*/
/*	Default methods for fields and array access				*/
/*										*/
/********************************************************************************/

@Override public void setField(IfaceValue v,String fldkey)	        { }

@Override public boolean addToField(IfaceValue v,String fldkey)         { return false; }

@Override public IfaceValue getField(String fldkey)	                { return null; }


@Override public boolean setArrayContents(IfaceValue idx,IfaceValue v)	{ return false; }

@Override public IfaceValue getArrayContents(IfaceValue f)		{ return null; }

@Override public List<IfaceValue> getContents(List<IfaceValue> rslt)    { return rslt; }

@Override public void setAnyValue()                                     { }





/********************************************************************************/
/*										*/
/*	Check for relevant methods						*/
/*										*/
/********************************************************************************/

@Override public boolean isMethodRelevant(IfaceMethod fm)
{
   if (proto_type == null) return true;
   //TODO: needs to handle methods that come from extends to a prototyped class
   return ProtoFactory.isMethodRelevant(fm,proto_type);
}




/********************************************************************************/
/*										*/
/*	Generic call handler							*/
/*										*/
/********************************************************************************/

@Override public IfaceValue handleCall(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   Method mthd = null;
   synchronized (method_map) {
      if (method_map.containsKey(fm)) mthd = method_map.get(fm);
      else {
	 String nm;
	 if (fm.isConstructor()) nm = "prototype__constructor";
	 else nm = "prototype_" + fm.getName();
	 Class<?> c = getClass();
	 try {
	    mthd = c.getMethod(nm,call_params);
	  }
	 catch (NoSuchMethodException e) { }
	 method_map.put(fm,mthd);
       }
    }

   try {
      if (mthd != null) {
	 IfaceValue rslt = (IfaceValue) mthd.invoke(this,fm,args,src);
	 return rslt;
       }
    }
   catch (Exception e) {
      System.err.println("FAIT: Problem with prototype call: " + e);
      e.printStackTrace();
    }

   return returnAny(fm);
}



/********************************************************************************/
/*										*/
/*	Return helpers							       */
/*										*/
/********************************************************************************/

protected IfaceValue returnAny(IfaceMethod fm)
{
   return fait_control.findNativeValue(fm.getReturnType());
}



protected IfaceValue returnNative(IfaceMethod fm)
{
   return fait_control.findNativeValue(fm.getReturnType());
}


protected IfaceValue returnMutable(IfaceMethod fm)
{
   return fait_control.findMutableValue(fm.getReturnType());
}


protected IfaceValue returnTrue()
{
   return fait_control.findConstantValue(true);
}


protected IfaceValue returnFalse()
{
   return fait_control.findConstantValue(false);
}


protected IfaceValue returnInt(int v)
{
   return fait_control.findConstantValue(fait_control.findDataType("int"),v);
}



protected IfaceValue returnInt(int v0,int v1)
{
   Long lv0 = (long) v0;
   Long lv1 = (long) v1;
   return fait_control.findRangeValue(fait_control.findDataType("int"),lv0,lv1);
}


protected IfaceValue returnNull(IfaceMethod fm)
{
   return fait_control.findNullValue(fm.getReturnType());
}


protected IfaceValue returnVoid()
{
   return fait_control.findAnyValue(fait_control.findDataType("void"));
}



/********************************************************************************/
/*										*/
/*	Methods for incrmental update						*/
/*										*/
/********************************************************************************/

@Override public void handleUpdates(IfaceUpdater fu)		   { }




}	// end of abstract class ProtoBase



/* end of ProtoBase.java */
