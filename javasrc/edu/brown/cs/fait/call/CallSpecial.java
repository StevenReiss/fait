/********************************************************************************/
/*										*/
/*		CallSpecial.java						*/
/*										*/
/*	Holder of special instructions for various methods			*/
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



package edu.brown.cs.fait.call;

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeMethod;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;
import java.util.*;


class CallSpecial implements IfaceSpecial, CallConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl	fait_control;
private String		result_type;
private String		alt_result;
private boolean 	canbe_null;
private boolean 	is_mutable;
private boolean         is_constructor;
private boolean 	return_arg0;
private String		replace_name;
private boolean 	dont_scan;
private boolean 	async_call;
private boolean 	array_copy;
private List<String>	callback_names;
private String		callback_id;
private List<Integer>	callback_args;
private boolean 	does_exit;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CallSpecial(FaitControl fc,Element xml,boolean formthd)
{
   fait_control = fc;

   replace_name = IvyXml.getAttrString(xml,"REPLACE");

   return_arg0 = false;
   result_type = null;
   alt_result = null;
   String rtn = IvyXml.getAttrString(xml,"RETURN");
   if (rtn == null || rtn == "" || rtn.equals("*")) ;
   else if (rtn.equals("0")) return_arg0 = true;
   else {
      result_type = rtn;
    }
   alt_result = IvyXml.getAttrString(xml,"ARETURN");

   canbe_null = IvyXml.getAttrBool(xml,"NULL",!formthd);
   is_mutable = IvyXml.getAttrBool(xml,"MUTABLE",!formthd);
   does_exit = IvyXml.getAttrBool(xml,"EXIT");
   array_copy = IvyXml.getAttrBool(xml,"ARRAYCOPY");
   async_call = IvyXml.getAttrBool(xml,"ASYNC");
   is_constructor = IvyXml.getAttrBool(xml,"CONSTRUCTOR");

   dont_scan = !IvyXml.getAttrBool(xml,"SCAN");

   callback_names = null;
   callback_args = null;
   callback_id = null;
   String cbnm = IvyXml.getAttrString(xml,"CALLBACK");
   if (cbnm != null) {
      callback_names = new ArrayList<String>();
      callback_args = new ArrayList<Integer>();
      callback_id = IvyXml.getAttrString(xml,"CBID");
      for (StringTokenizer tok = new StringTokenizer(cbnm); tok.hasMoreTokens(); ) {
	 String cn = tok.nextToken();
	 callback_names.add(cn);
       }
      String args = IvyXml.getAttrString(xml,"CBARGS");
      if (args == null) args = "1";
      for (StringTokenizer tok = new StringTokenizer(args); tok.hasMoreTokens(); ) {
	 try {
	    int i = Integer.parseInt(tok.nextToken());
	    callback_args.add(i);
	  }
	 catch (NumberFormatException e) {
	    System.err.println("FAIT: ARGS contains non-numeric value");
	  }
       }
    }
   if (is_constructor) {
      callback_args = new ArrayList<Integer>();
      String args = IvyXml.getAttrString(xml,"ARGS");
      for (StringTokenizer tok = new StringTokenizer(args); tok.hasMoreTokens(); ) {
         String nvl = tok.nextToken();
         int i = -99;
         if (nvl.equalsIgnoreCase("NULL")) i = -1;
         else if (nvl.equalsIgnoreCase("FALSE")) i = -2;
         else if (nvl.equalsIgnoreCase("TRUE")) i = -3;
         else {
            try {
               i = Integer.parseInt(nvl);
             }
            catch (NumberFormatException e) {
               System.err.println("FAIT: ARGS contains bad value");
             }
          }
         callback_args.add(i);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getReturnValue(JcodeMethod fm)
{
   JcodeDataType dt = null;
   if (result_type != null && result_type.equals("void")) dt = fait_control.findDataType("V");
   else if (result_type != null) dt = getClassType(result_type);
   if (dt == null && alt_result != null) dt = getClassType(alt_result);
   if (dt == null) {
      if (fm.isConstructor()) dt = fm.getDeclaringClass();
      else dt = fm.getReturnType();
    }

   IfaceValue rv = null;
   if (is_mutable)
      rv = fait_control.findMutableValue(dt);
   else if (dt.isJavaLangObject() || dt.isAbstract())
      rv = fait_control.findMutableValue(dt);
   else
      rv = fait_control.findNativeValue(dt);

   if (!canbe_null || fm.isConstructor()) rv = rv.forceNonNull();

   return rv;
}


@Override public boolean returnsArg0()			{ return return_arg0; }

@Override public boolean isConstructor()                { return is_constructor; }

@Override public String getReplaceName()		{ return replace_name; }



@Override public Iterable<String> getCallbacks() {
   if (callback_names == null || callback_names.size() == 0) return null;
   return callback_names;
}

@Override public String getCallbackId() 		{ return callback_id; }

@Override public List<Integer> getCallbackArgs()	{ return callback_args; }

@Override public boolean getIsAsync()			{ return async_call; }

@Override public boolean getIsArrayCopy()		{ return array_copy; }

@Override public boolean getExits()			{ return does_exit; }

@Override public boolean getDontScan()			{ return dont_scan; }




/********************************************************************************/
/*										*/
/*	Name standardization methods						*/
/*										*/
/********************************************************************************/

private JcodeDataType getClassType(String name)
{
   return fait_control.findDataType(getInternalName(name));
}



private String getInternalName(String user)
{
   int act = 0;
   while (user.endsWith("[]")) {
      ++act;
      user = user.substring(0,user.length()-2);
    }

   String rslt = "L" + user.replace(".","/") + ";";
   while (act > 0) {
      rslt = "[" + rslt;
      --act;
    }

   return rslt;
}




}	// end of class CallSpecial




/* end of CallSpecial.java */

