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
private List<String>    throw_types;
private IfaceAnnotation [] type_annots;
private Map<Integer,IfaceAnnotation []> arg_annots;
private boolean 	canbe_null;
private boolean 	is_mutable;
private boolean         is_constructor;
private boolean 	return_arg0;
private String		replace_name;
private boolean 	dont_scan;
private boolean 	async_call;
private List<String>	callback_names;
private String		callback_id;
private List<ArgValue>	callback_args;
private boolean 	does_exit;
private boolean         no_return;
private List<String>    load_types;
private List<When>      when_conditions;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CallSpecial(IfaceControl fc,Element xml,boolean formthd)
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

   dont_scan = !IvyXml.getAttrBool(xml,"SCAN");

   callback_names = null;
   callback_args = null;
   callback_id = null;
   String cbnm = IvyXml.getAttrString(xml,"CALLBACK");
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
   
   when_conditions = null;
   for (Element welt : IvyXml.children(xml,"WHEN")) {
      if (when_conditions == null) when_conditions = new ArrayList<>();
      When wh = new When(welt);
      when_conditions.add(wh);
    }
   
   load_types = null;
   for (Element lelt : IvyXml.children(xml,"LOAD")) {
      if (load_types == null) load_types = new ArrayList<>();
      String nm = IvyXml.getAttrString(lelt,"NAME");
      if (nm == null) nm = IvyXml.getText(lelt);
      if (nm != null) load_types.add(nm);
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
  
   if (type_annots != null) {
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


@Override public boolean getIsAsync()		        { return async_call; }

@Override public boolean getExits()	                { return does_exit; }

@Override public boolean getNeverReturns()              { return no_return; }

@Override public boolean getDontScan()	                { return dont_scan; }
@Override public boolean getForceScan()                 { return !dont_scan; }

@Override public List<String> getClassesToLoad()        { return load_types; }


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
/*                                                                              */
/*      Application conditions                                                  */
/*                                                                              */
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
         // TODO: check for instance in method
       }
      
      return true;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Argument Encoding                                                       */
/*                                                                              */
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
         IfaceType ntyp = fait_control.findDataType(tnm);
         IfaceValue v = fait_control.findMutableValue(ntyp);
         av = new ConstArgValue(v);
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
   
}       // end of inner class OrigArgValue


private static class ConstArgValue extends ArgValue {
  
   private IfaceValue const_value;
   
   ConstArgValue(IfaceValue v) {
      const_value = v;
    }
   
   @Override void addValues(List<IfaceValue> args,IfaceValue newval,List<IfaceValue> rslt) {
      rslt.add(const_value);
    }
   
}       // end of inner class ConstArgValue



private static class NewArgValue extends ArgValue {

   NewArgValue() { }
   
   @Override void addValues(List<IfaceValue> args,IfaceValue newval,List<IfaceValue> rslt) {
      if (newval != null) rslt.add(newval);
      else rslt.add(args.get(0));
    }
   
}       // end of inner class NewArgValue


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
   
}       // end of inner class VarArgsValue


}	// end of class CallSpecial




/* end of CallSpecial.java */

