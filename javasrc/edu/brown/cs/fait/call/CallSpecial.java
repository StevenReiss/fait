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
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;
import java.util.*;


class CallSpecial implements IfaceSpecial, CallConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl	fait_control;
private String		result_type;
private String		alt_result;
private List<String>	throw_types;
private IfaceAnnotation [] type_annots;
private Map<Integer,IfaceAnnotation []> arg_annots;
private boolean 	canbe_null;
private boolean 	is_mutable;
private boolean 	is_constructor;
private boolean 	is_clone;
private boolean 	return_arg0;
private String		replace_name;
private boolean 	dont_scan;
private boolean 	async_call;
private List<String>	callback_names;
private String		callback_id;
private List<ArgValue>	callback_args;
private boolean 	does_exit;
private boolean 	no_return;
private boolean 	no_virtual;
private boolean 	set_fields;
private boolean 	is_affected;
private Collection<String> load_types;
private List<When>	when_conditions;
private InlineType	inline_type;




 /********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CallSpecial(IfaceControl fc,Element xml,boolean formthd)
{
   fait_control = fc;

   replace_name = IvyXml.getTextElement(xml,"REPLACE");

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
   throw_types = null;
   String thr = IvyXml.getAttrString(xml,"THROWS");
   if (thr != null) {
      StringTokenizer tok = new StringTokenizer(thr);
      while (tok.hasMoreTokens()) {
	 String th = tok.nextToken();
	 if (throw_types == null) throw_types = new ArrayList<>();
	 throw_types.add(th);
       }
    }

   canbe_null = IvyXml.getAttrBool(xml,"NULL",!formthd);
   String annots = IvyXml.getAttrString(xml,"ANNOTATIONS");
   List<String> annotset = new ArrayList<>();
   if (annots != null) {
      StringTokenizer tok = new StringTokenizer(annots," ,");
      while (tok.hasMoreTokens()) {
	 annotset.add(tok.nextToken());
       }
      if (!canbe_null && !annotset.contains("NonNull")) annotset.add("NonNull");
    }
   else {
      if (canbe_null) annotset.add("Nullable");
      else annotset.add("NonNull");
    }
   type_annots = new IfaceAnnotation[annotset.size()];
   for (int i = 0; i < annotset.size(); ++i) {
      type_annots[i] = new FaitAnnotation(annotset.get(i));
    }

   arg_annots = new HashMap<>();
   String argannot = IvyXml.getAttrString(xml,"ARGANNOTATIONS");
   if (argannot != null) {
      StringTokenizer tok = new StringTokenizer(argannot,";+ ");
      while (tok.hasMoreTokens()) {
	 String aan = tok.nextToken();
	 List<String> aaset = new ArrayList<>();
	 int idx = aan.indexOf(":");
	 if (idx < 0) continue;
	 String id = aan.substring(0,idx);
	 aan = aan.substring(idx+1);
	 int ano = -1;
	 try {
	    ano = Integer.parseInt(id);
	  }
	 catch (NumberFormatException e) { continue; }
	 StringTokenizer tok1 = new StringTokenizer(aan,",@");
	 while (tok1.hasMoreTokens()) {
	    aaset.add(tok1.nextToken());
	  }
	 IfaceAnnotation [] aav = new IfaceAnnotation[aaset.size()];
	 for (int i = 0; i < annotset.size(); ++i) {
	    aav[i] = new FaitAnnotation(aaset.get(i));
	  }
	 arg_annots.put(ano,aav);
       }
    }

   is_mutable = IvyXml.getAttrBool(xml,"MUTABLE",!formthd);
   does_exit = IvyXml.getAttrBool(xml,"EXIT");
   async_call = IvyXml.getAttrBool(xml,"ASYNC");
   is_constructor = IvyXml.getAttrBool(xml,"CONSTRUCTOR");
   no_return = IvyXml.getAttrBool(xml,"NORETURN");
   no_virtual = IvyXml.getAttrBool(xml,"NOVIRTUAL");
   set_fields = IvyXml.getAttrBool(xml,"SETFIELDS");
   is_affected = IvyXml.getAttrBool(xml,"AFFECTED");
   inline_type = IvyXml.getAttrEnum(xml,"INLINE",InlineType.NORMAL);
   is_clone = IvyXml.getAttrBool(xml,"CLONE");

   dont_scan = true;
   if (inline_type != InlineType.NORMAL) dont_scan = false;
   dont_scan = !IvyXml.getAttrBool(xml,"SCAN",!dont_scan);

   callback_names = null;
   callback_args = null;
   callback_id = null;
   String cbnm = IvyXml.getTextElement(xml,"CALLBACK");
   if (cbnm != null) {
      callback_names = new ArrayList<String>();
      callback_id = IvyXml.getAttrString(xml,"CBID");
      for (StringTokenizer tok = new StringTokenizer(cbnm); tok.hasMoreTokens(); ) {
	 String cn = tok.nextToken();
	 callback_names.add(cn);
       }
      String args = IvyXml.getAttrString(xml,"CBARGS");
      if (args == null) args = "1";
      callback_args = scanArgs(args);
    }

   if (is_constructor) {
      callback_args = new ArrayList<>();
      String args = IvyXml.getAttrString(xml,"ARGS");
      if (args == null) args = "*";
      callback_args = scanArgs(args);
    }

   if (callback_args == null) {
      String args = IvyXml.getAttrString(xml,"ARGS");
      if (args != null) callback_args = scanArgs(args);
    }

   when_conditions = null;
   for (Element welt : IvyXml.children(xml,"WHEN")) {
      if (when_conditions == null) when_conditions = new ArrayList<>();
      When wh = new When(welt);
      when_conditions.add(wh);
    }

   load_types = null;
   for (Element lelt : IvyXml.children(xml,"LOAD")) {
      if (load_types == null) load_types = new HashSet<>();
      String nm = IvyXml.getAttrString(lelt,"NAME");
      if (nm == null) nm = IvyXml.getText(lelt);
      if (nm != null) load_types.add(nm);
    }

   if (replace_name != null) {
      StringTokenizer tok = new StringTokenizer(replace_name);
      while (tok.hasMoreTokens()) {
	 String mthd = tok.nextToken();
	 int idx = mthd.indexOf("(");
	 if (idx > 0) mthd = mthd.substring(0,idx);
	 if (!is_constructor) {
	    idx = mthd.lastIndexOf(".");
	    if (idx > 0) mthd = mthd.substring(0,idx);
	  }
	 if (load_types == null) load_types = new HashSet<>();
	 load_types.add(mthd);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceValue getReturnValue(IfaceProgramPoint pt,IfaceMethod fm)
{
   IfaceType dt = null;
   if (result_type != null && result_type.equals("void"))
      dt = fait_control.findDataType("void");
   else if (result_type != null) dt = getClassType(result_type);
   if (dt == null && alt_result != null) dt = getClassType(alt_result);
   if (dt == null) {
      dt = fm.getReturnType();
      // if (fm.isConstructor()) dt = fm.getDeclaringClass();
      // else dt = fm.getReturnType();
    }

   if (type_annots != null && dt != null) {
      if (dt.isPrimitiveType()) {
	 List<IfaceAnnotation> ans = null;
	 for (int i = 0; i < type_annots.length; ++i) {
	    IfaceAnnotation ian = type_annots[i];
	    if (ian.getAnnotationName().contains("Null")) continue;
	    if (ian.getAnnotationName().contains("Initial")) continue;
	    if (ans == null) ans = new ArrayList<>();
	    ans.add(ian);
	  }
	 if (ans != null) {
	    IfaceAnnotation [] anarr = new IfaceAnnotation[ans.size()];
	    anarr = ans.toArray(anarr);
	    dt = dt.getAnnotatedType(anarr);
	  }
       }
      else {
	 dt = dt.getAnnotatedType(type_annots);
       }
    }
   else if (type_annots != null) {
      // handle type annot on a constructor here
    }

   IfaceValue rv = null;
   if (is_mutable)
      rv = fait_control.findMutableValue(dt);
   else if (dt.isJavaLangObject() || dt.isAbstract())
      rv = fait_control.findMutableValue(dt);
   else
      rv = fait_control.findNativeValue(dt);

   if (!canbe_null || fm.isConstructor() || dt.isPrimitiveType()) rv = rv.forceNonNull();

   return rv;
}



@Override public IfaceAnnotation [] getArgAnnotations(int idx)
{
   if (arg_annots == null) return null;
   return arg_annots.get(idx);
}


@Override public List<IfaceValue> getExceptions(IfaceProgramPoint pt,IfaceMethod fm)
{
   List<IfaceType> typs = null;

   if (throw_types == null) {
      typs = fm.getExceptionTypes();
    }
   else {
      typs = new ArrayList<>();
      for (String th : throw_types) {
	 IfaceType dt = getClassType(th);
	 if (dt != null) typs.add(dt);
       }
    }
   if (typs == null || typs.size() == 0) return null;

   List<IfaceValue> vals = new ArrayList<IfaceValue>();
   for (IfaceType t : typs) {
      IfaceValue v0 = fait_control.findAnyValue(t);
      v0 = v0.forceNonNull();
      vals.add(v0);
    }

   return vals;
}



@Override public boolean returnsArg0()
{ return return_arg0; }

@Override public boolean isConstructor()
{ return is_constructor; }

@Override public String getReplaceName()
{ return replace_name; }



@Override public Iterable<String> getCallbacks()
{
   if (callback_names == null || callback_names.size() == 0) return null;
   return callback_names;
}

@Override public String getCallbackId()
{ return callback_id; }

@Override public List<IfaceValue> getCallbackArgs(List<IfaceValue> args,IfaceValue newval)
{
   if (callback_args == null) return args;

   List<IfaceValue> rslt = new ArrayList<>();
   for (ArgValue av : callback_args) {
      av.addValues(args,newval,rslt);
    }
   return rslt;
}


@Override public boolean getIsAsync()			{ return async_call; }

@Override public boolean getIsClone()			{ return is_clone; }

@Override public boolean getExits()			{ return does_exit; }

@Override public boolean getNeverReturns()		{ return no_return; }

@Override public boolean getIgnoreVirtualCalls()	{ return no_virtual; }

@Override public boolean getSetFields() 		{ return set_fields; }
@Override public boolean isAffected()			{ return is_affected; }

@Override public boolean getDontScan()			{ return dont_scan; }
@Override public boolean getForceScan() 		{ return !dont_scan; }

@Override public Collection<String> getClassesToLoad()	      { return load_types; }

@Override public InlineType getInlineType()
{
   if (inline_type == null || inline_type == InlineType.NORMAL) return null;
   return inline_type;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw)
{
   xw.begin("SPECIAL");

   if (result_type != null) xw.field("RESULTTYPE",result_type);
   if (alt_result != null) xw.field("ALTRESULT",alt_result);
   if (throw_types != null) {
      StringBuffer buf = new StringBuffer();
      for (String s : throw_types) {
	 buf.append(s);
	 buf.append(" ");
       }
      xw.field("THROWS",buf.toString().trim());
    }
   if (canbe_null) xw.field("CANBENULL",true);
   if (is_mutable) xw.field("MUTABLE",true);
   if (is_constructor) xw.field("CONSTRUCTOR",true);
   if (return_arg0) xw.field("RETURNARG0",true);
   if (replace_name != null) xw.field("REPLACE",replace_name);
   if (!dont_scan) xw.field("SCAN",true);
   if (async_call) xw.field("ASYNC",true);
   if (callback_names != null) {
      StringBuffer buf = new StringBuffer();
      for (String s : callback_names) {
	 buf.append(s);
	 buf.append(" ");
       }
      xw.field("CALLBACKS",buf.toString().trim());
    }
   if (callback_id != null) xw.field("CALLBACKID",callback_id);
   if (does_exit) xw.field("EXITS",true);
   if (no_return) xw.field("NORETURN",true);
   if (no_virtual) xw.field("NOVIRTUAL",true);
   if (set_fields) xw.field("SETFIELDS",true);
   if (is_affected) xw.field("AFFECTED",true);
   if (inline_type != null) xw.field("INLINE",inline_type);

   xw.end("SPECIAL");
}




/********************************************************************************/
/*										*/
/*	Name standardization methods						*/
/*										*/
/********************************************************************************/

private IfaceType getClassType(String name)
{
   if (Character.isLowerCase(name.charAt(0)))
      return fait_control.findDataType(name);

   return fait_control.findDataType(name);
}



/********************************************************************************/
/*										*/
/*	Application conditions							*/
/*										*/
/********************************************************************************/

boolean match(IfaceProgramPoint pt)
{
   if (when_conditions == null) return true;
   if (pt == null) return false;

   for (When wh : when_conditions) {
      if (wh.match(pt)) return true;
    }

   return false;
}



private static class When {

   private String caller_name;
   private String caller_description;
   private int instance_number;

   When(Element xml) {
      caller_name = IvyXml.getAttrString(xml,"CALLER");
      caller_description = IvyXml.getAttrString(xml,"DESCRIPTION");
      instance_number = IvyXml.getAttrInt(xml,"INSTANCE",-1);
    }

   boolean match(IfaceProgramPoint pt) {
      if (pt == null) return false;
      IfaceMethod im = pt.getMethod();
      if (caller_name != null) {
	 String mnm = im.getName();
	 if (!caller_name.equals(mnm)) {
	    String cnm = im.getDeclaringClass().getName();
	    mnm = cnm + "." + mnm;
	    if (!caller_name.equals(mnm)) return false;
	  }
       }
      if (caller_description != null) {
	 String desc = im.getDescription();
	 if (!caller_description.equals(desc)) return false;
       }
      if (instance_number >= 0) {
	 int inno = pt.getInstanceNumber();
	 if (inno != instance_number) return false;
       }

      return true;
    }
}




/********************************************************************************/
/*										*/
/*	Argument Encoding							*/
/*										*/
/********************************************************************************/

private List<ArgValue> scanArgs(String coding)
{
   List<ArgValue> rslt = new ArrayList<>();

   for (StringTokenizer tok = new StringTokenizer(coding," \t,"); tok.hasMoreTokens(); ) {
      String nvl = tok.nextToken();
      ArgValue av = null;
      if (nvl.equalsIgnoreCase("NULL")) {
	 av = new ConstArgValue(fait_control.findNullValue());
       }
      else if (nvl.equalsIgnoreCase("FALSE")) {
	 av = new ConstArgValue(fait_control.findConstantValue(false));
       }
      else if (nvl.equalsIgnoreCase("TRUE")) {
	 av = new ConstArgValue(fait_control.findConstantValue(true));
       }
      else if (nvl.equalsIgnoreCase("THIS") || nvl.equalsIgnoreCase("*")) {
	 av = new NewArgValue();
       }
      else if (nvl.equals("...") || nvl.equalsIgnoreCase("VARARGS")) {
	 av = new VarArgsValue();
       }
      else if (nvl.startsWith("*") && nvl.length() > 1) {
	 String tnm = nvl.substring(1);
	 av = new ConstArgValue(tnm);
       }
      else {
	 try {
	    int i = Integer.parseInt(nvl);
	    av = new OrigArgValue(i);
	  }
	 catch (NumberFormatException e) {
	    FaitLog.logE("ARGS contains bad value: " + coding);
	  }
       }
      if (av != null) rslt.add(av);
    }

   return rslt;
}

private abstract static class ArgValue {

   abstract void addValues(List<IfaceValue> args,IfaceValue newval,List<IfaceValue> rslt);

}


private static class OrigArgValue extends ArgValue {

   private int arg_index;

   OrigArgValue(int idx) {
      arg_index = idx;
    }

   @Override void addValues(List<IfaceValue> args,IfaceValue newval,List<IfaceValue> rslt) {
      rslt.add(args.get(arg_index));
    }

}	// end of inner class OrigArgValue


private class ConstArgValue extends ArgValue {

   private String type_name;
   private IfaceValue const_value;

   ConstArgValue(IfaceValue v) {
      type_name = null;
      const_value = v;
    }
   
   ConstArgValue(String type) {
      type_name = type;
      const_value = null;
    }

   @Override void addValues(List<IfaceValue> args,IfaceValue newval,List<IfaceValue> rslt) {
      if (const_value == null && type_name != null) {
         IfaceType ntyp = fait_control.findDataType(type_name);
         const_value = fait_control.findMutableValue(ntyp);
       }
      rslt.add(const_value);
    }

}	// end of inner class ConstArgValue



private static class NewArgValue extends ArgValue {

   NewArgValue() { }

   @Override void addValues(List<IfaceValue> args,IfaceValue newval,List<IfaceValue> rslt) {
      if (newval != null) rslt.add(newval);
      else rslt.add(args.get(0));
    }

}	// end of inner class NewArgValue


private class VarArgsValue extends ArgValue {

   VarArgsValue() { }

   @Override void addValues(List<IfaceValue> args,IfaceValue newval,List<IfaceValue> rslt) {
      IfaceValue v0 = args.get(args.size()-1);
      IfaceValue nargv = v0.getArrayLength();
      Integer narg = nargv.getIndexValue();
      if (narg == null) return;
      for (int i = 0; i < narg; ++i) {
	 IfaceType ityp = fait_control.findDataType("int");
	 IfaceValue idx = fait_control.findConstantValue(ityp,i);
	 IfaceValue av = v0.getArrayContents(idx);
	 rslt.add(av);
       }
    }

}	// end of inner class VarArgsValue


}	// end of class CallSpecial




/* end of CallSpecial.java */

