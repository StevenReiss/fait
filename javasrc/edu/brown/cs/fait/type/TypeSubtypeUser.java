/********************************************************************************/
/*                                                                              */
/*              TypeSubtypeUser.java                                            */
/*                                                                              */
/*      User-define subtype                                                     */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.fait.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.xml.IvyXml;

class TypeSubtypeUser extends TypeSubtype
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,UserValue> value_set;
private UserValue default_constant;
private UserValue default_uninit;
private UserValue default_value;
private List<InstanceCheck> const_checks;
private List<InstanceCheck> uninit_checks;
private List<InstanceCheck> default_checks;
private List<InstanceCheck> base_checks;
private List<PredecessorCheck> predecessor_checks;
private List<OpCheck> operator_checks;
private Set<UserValue> default_values;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TypeSubtypeUser(Element xml)
{
   super(IvyXml.getAttrString(xml,"NAME"));
      
   value_set = new HashMap<>();
   default_constant = null;
   default_uninit = null;
   default_value = null;
   default_values = new HashSet<>();
   
   UserValue first = null;
   for (Element velt : IvyXml.children(xml,"VALUE")) {
      String nm = IvyXml.getAttrString(velt,"NAME");
      if (nm == null) FaitLog.logE("Missing name for subtype value definition");
      UserValue vl = new UserValue(nm);
      value_set.put(nm,vl);
      if (first == null) first = vl;
      if (IvyXml.getAttrBool(velt,"CONSTANT")) default_constant = vl;
      if (IvyXml.getAttrBool(velt,"UNINIT")) default_uninit = vl;
      if (IvyXml.getAttrBool(velt,"DEFAULT")) default_value = vl;
      String ats = IvyXml.getAttrString(velt,"ATTRIBUTES");
      if (ats != null) {
         StringTokenizer tok = new StringTokenizer(ats," ,;");
         while (tok.hasMoreTokens()) {
            String atr = tok.nextToken();
            defineAttribute(atr,vl);
          }
       }
    }
   if (default_value == null) default_value = first;
   if (default_uninit == null) default_uninit = default_value;
   if (default_constant == null) default_constant = default_value;
   default_values.add(default_value);
   default_values.add(default_uninit);
   default_values.add(default_constant);
   
   for (Element velt : IvyXml.children(xml,"MERGE")) {
      String fnm = IvyXml.getAttrString(velt,"VALUE");
      UserValue fvl = value_set.get(fnm);
      String tnm = IvyXml.getAttrString(velt,"WITH");
      UserValue tvl = value_set.get(tnm);
      String rnm = IvyXml.getAttrString(velt,"YIELDS");
      UserValue rvl = value_set.get(rnm);
      if (fvl != null && tvl != null && rvl != null) {
         defineMerge(fvl,tvl,rvl);
       }
      else {
         FaitLog.logE("Bad MERGE Definition for subtype " + getName());
       }
    }
   
   for (Element velt : IvyXml.children(xml,"RESTRICT")) {
      String fnm = IvyXml.getAttrString(velt,"VALUE");
      UserValue fvl = value_set.get(fnm);
      String tnm = IvyXml.getAttrString(velt,"WITH");
      UserValue tvl = value_set.get(tnm);
      String rnm = IvyXml.getAttrString(velt,"YIELDS");
      if (fnm == null || tnm == null) {
         FaitLog.logE("Bad RESTRICT definition for subtype " + getName());
       }
      else if (rnm != null) {
         UserValue rvl = value_set.get(rnm);
         if (fvl != null && tvl != null && rvl != null) {
            defineRestrict(fvl,tvl,rvl);
          }
       }
      else if (fvl != null && tvl != null) {
         FaitError fe = null;
         String enm = IvyXml.getAttrString(velt,"ERROR");
         String wnm = IvyXml.getAttrString(velt,"WARNING");
         String nnm = IvyXml.getAttrString(velt,"NOTE");
         if (enm != null) fe = new FaitError(this,FaitError.ErrorLevel.ERROR,enm);
         else if (wnm != null) fe = new FaitError(this,FaitError.ErrorLevel.WARNING,wnm);   
         else if (nnm != null) fe = new FaitError(this,FaitError.ErrorLevel.NOTE,nnm);   
         if (fe != null) defineRestrict(fvl,tvl,fe);
       }
      else {
         FaitLog.logE("Bad RESTRICT yields for subtype " + getName());
       }
    }
   
   const_checks = null;
   for (Element cchk : IvyXml.children(xml,"CONSTANT")) {
      if (const_checks == null) const_checks = new ArrayList<>();
      InstanceCheck ck = new InstanceCheck(cchk);
      const_checks.add(ck);
      default_values.add(ck.getBaseValue());
    }
   
   uninit_checks = null;
   for (Element cchk : IvyXml.children(xml,"UNINITIALIZED")) {
      if (uninit_checks == null) uninit_checks = new ArrayList<>();
      InstanceCheck ck = new InstanceCheck(cchk);
      uninit_checks.add(ck);
      default_values.add(ck.getBaseValue());
    }
   
   default_checks = null;
   for (Element cchk : IvyXml.children(xml,"DEFAULT")) {
      if (default_checks == null) default_checks = new ArrayList<>();
      InstanceCheck ck = new InstanceCheck(cchk);
      default_checks.add(ck);
      default_values.add(ck.getBaseValue());
    }
   
   base_checks = null;
   for (Element cchk : IvyXml.children(xml,"BASE")) {
      if (base_checks == null) base_checks = new ArrayList<>();
      InstanceCheck ck = new InstanceCheck(cchk);
      base_checks.add(ck);
      default_values.add(ck.getBaseValue());
    }
   
   operator_checks = null;
   for (Element cchk : IvyXml.children(xml,"OPERATION")) {
      if (operator_checks == null) operator_checks = new ArrayList<>();
      OpCheck ck = new OpCheck(cchk);
      operator_checks.add(ck);
    }
   
   predecessor_checks = null;
   for (Element cchk : IvyXml.children(xml,"PREDECESSOR")) {
      if (predecessor_checks == null) predecessor_checks = new ArrayList<>();
      PredecessorCheck ck = new PredecessorCheck(cchk);
      predecessor_checks.add(ck);
    }
   
   int ct = 0;
   Set<UserValue> igns = new HashSet<>();
   for (Element ichk : IvyXml.children(xml,"IGNORE")) {
      ++ct;
      String nm = IvyXml.getAttrString(ichk,"VALUE");
      if (nm != null) {
         UserValue val = value_set.get(nm);
         if (val != null) igns.add(val);
       }
    }
   if (ct > 0) default_values = igns;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public UserValue getDefaultConstantValue(IfaceBaseType typ,Object cnst)
{
   if (const_checks != null) {
      for (InstanceCheck ck : const_checks) {
         UserValue uv = ck.getValue(typ,cnst);
         if (uv != null) return uv;
       }
    }
   return default_constant;
}


@Override public UserValue getDefaultUninitializedValue(IfaceType typ)
{
   if (uninit_checks != null) {
      for (InstanceCheck ck : uninit_checks) {
         UserValue uv = ck.getValue(typ.getJavaType(),null);
         if (uv != null) return uv;
       }
    }
   return default_uninit;
}


@Override public UserValue getDefaultValue(IfaceBaseType typ)
{
   if (default_checks != null) {
      for (InstanceCheck ck : default_checks) {
         UserValue uv = ck.getValue(typ,null);
         if (uv != null) return uv;
       }
    }
   return default_value;
}



/********************************************************************************/
/*                                                                              */
/*      Operator methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSubtype.Value getComputedValue(IfaceValue rslt,
      FaitOperator op,IfaceValue v0,IfaceValue v1)
{
   if (operator_checks != null) {
      IfaceType t0 = v0.getDataType();
      IfaceType t1 = v1.getDataType();
      IfaceType t2 = rslt.getDataType();
      IfaceSubtype.Value s0 = t0.getValue(this);
      IfaceSubtype.Value s1 = t1.getValue(this);
      IfaceSubtype.Value s2 = t2.getValue(this);
      switch (op) {
         case DEREFERENCE :
         case ELEMENTACCESS :
         case FIELDACCESS :
            t1 = t2;
            s1 = s2;
            break;
         default :
            break;
       }
      for (OpCheck ck : operator_checks) {
         UserValue uv = ck.getOperatorValue(op,s0,s1,s2);
         if (uv != null) return uv;
       }
    }
   return super.getComputedValue(rslt,op,v0,v1);
}



@Override public IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval)
{
   if (operator_checks != null) {
      for (OpCheck ck : operator_checks) {
         UserValue uv = ck.getOperatorValue(op,oval);
         if (uv != null) return uv;
       }
    }
   return super.getComputedValue(op,oval);
}


@Override public IfaceSubtype.Value getCallValue(IfaceCall cm,IfaceValue rslt,List<IfaceValue> args)
{
   if (operator_checks != null) {
      IfaceSubtype.Value [] atyps = new IfaceSubtype.Value[args.size()];
      int i = 0;
      for (IfaceValue vl : args) {
         atyps[i++] = vl.getDataType().getValue(this);
       }
      IfaceSubtype.Value rtyp = rslt.getDataType().getValue(this);
      for (OpCheck ck : operator_checks) {
         UserValue uv = ck.computeCallValue(cm.getMethod(),rtyp,atyps);
         if (uv != null) return uv;
       }
    }
   return super.getCallValue(cm,rslt,args);
}


@Override public IfaceSubtype.Value adjustValueForBase(IfaceSubtype.Value v,IfaceBaseType b)
{
   if (base_checks != null) {
      for (InstanceCheck ck : base_checks) {
         UserValue uv = ck.getValue(b,null);
         if (uv != null) return uv;
       }
    }
   
   return super.adjustValueForBase(v,b);
}



@Override public IfaceSubtype.Value getImpliedValue(FaitOperator op,IfaceValue v0,IfaceValue v1,boolean branch)
{
   return super.getImpliedValue(op,v0,v1,branch);
}


@Override public IfaceAnnotation getArgumentAnnotation(FaitOperator op,int opnd,IfaceValue [] vals)
{
   return super.getArgumentAnnotation(op,opnd,vals);
}



@Override public boolean isPredecessorRelevant(IfaceSubtype.Value pred,IfaceSubtype.Value cur)
{
   if (predecessor_checks != null) {
      for (PredecessorCheck pk : predecessor_checks) {
         Boolean r = pk.checkValue(pred,cur);
         if (r != null) return r;
       }
    }
   return super.isPredecessorRelevant(pred,cur);
}


@Override public String getDefaultValues()
{
   StringBuffer buf = new StringBuffer();
   int ct = 0;
   for (UserValue v : default_values) {
      if (ct++ != 0) buf.append(" ");
      buf.append(v);
    }
   return buf.toString();
}


@Override public Collection<IfaceSubtype.Value> getValues()
{
   return new ArrayList<>(value_set.values());
}



/********************************************************************************/
/*                                                                              */
/*      Value representation                                                    */
/*                                                                              */
/********************************************************************************/

private class UserValue implements Value {
   
   private String value_name;
   
   UserValue(String nm) {
      value_name = nm;
    }
   
   @Override public String toString()           { return value_name; }
   
   @Override public IfaceSubtype getSubtype()   { return TypeSubtypeUser.this; }
   
}       // end of inner class UserValue



/********************************************************************************/
/*                                                                              */
/*      Class for handling special cases for default values                     */
/*                                                                              */
/********************************************************************************/

private class InstanceCheck {
   
   private boolean is_primitive;
   private boolean is_null;
   private String for_value;
   private String type_class;
   private String value_class;
   private UserValue result_value;
   
   InstanceCheck(Element xml) {
      is_primitive = IvyXml.getAttrBool(xml,"PRIMITIVE");
      is_null = IvyXml.getAttrBool(xml,"NULL");
      for_value = IvyXml.getAttrString(xml,"EQUALS");
      type_class = IvyXml.getAttrString(xml,"CLASS");
      value_class = IvyXml.getAttrString(xml,"CONSTCLASS");
      result_value = value_set.get(IvyXml.getAttrString(xml,"RESULT"));
      if (result_value == null) FaitLog.logE("Unknown result value for constant definition: " + xml);
    }
   
   UserValue getBaseValue()                             { return result_value; }
   
   UserValue getValue(IfaceBaseType typ,Object cnst) {
      if (is_primitive) {
         if (typ ==  null || !typ.isPrimitiveType()) return null;
       }
      if (is_null) {
         if (cnst != null) return null;
       }
      if (for_value != null) {
         if (!for_value.equals(String.valueOf(cnst))) return null;
       }
      if (type_class != null) {
         if (!type_class.equals(typ.toString())) return null;
       }
      if (value_class != null && cnst != null) {
         if (!value_class.equals(cnst.getClass().toString())) {
            try {
               Class<?> c0 = Class.forName(value_class);
               if (!c0.isAssignableFrom(cnst.getClass())) return null;
             }
            catch (ClassNotFoundException e) { }
          }
       }
      
      return result_value;
    }
   
}       // end of inner class ConstantCheck




/********************************************************************************/
/*                                                                              */
/*      Class for handling special cases for default values                     */
/*                                                                              */
/********************************************************************************/

private class PredecessorCheck {

   private UserValue pred_value;
   private UserValue cur_value;
   private Boolean result_value;
   
   PredecessorCheck(Element xml) {
      result_value = IvyXml.getAttrBool(xml,"RESULT");
      pred_value = value_set.get(IvyXml.getAttrString(xml,"PREDECESSOR"));
      cur_value = value_set.get(IvyXml.getAttrString(xml,"CURRENT"));
    }
   
   Boolean checkValue(IfaceSubtype.Value pred,IfaceSubtype.Value cur) {
       if (pred_value != null && pred != pred_value) return null;
       if (cur_value != null && cur != cur_value) return null;
       return result_value;
    }
   
}       // end of inner class ConstantCheck





/********************************************************************************/
/*                                                                              */
/*      Class for handling operator cases                                       */
/*                                                                              */
/********************************************************************************/

private class OpCheck {
   
   private Set<String> operator_values;
   private UserValue result_value;
   private UserValue [] arg_values;
   private boolean and_check;
   private UserValue return_value;
   private int return_arg;
   private String call_name;
   
   OpCheck(Element xml) {
      String ops = IvyXml.getAttrString(xml,"OPERATOR");
      if (ops == null) {
         operator_values = null;
       }
      else {
         operator_values = new HashSet<>();
         StringTokenizer tok = new StringTokenizer(ops,", \t;");
         while (tok.hasMoreTokens()) {
            operator_values.add(tok.nextToken());
          }
         if (operator_values.isEmpty()) operator_values = null;
       }
      and_check = IvyXml.getAttrBool(xml,"AND");
      result_value = getUserValue(xml,"RESULT");
      call_name = IvyXml.getAttrString(xml,"METHOD");      
      String atyps = IvyXml.getAttrString(xml,"ARGS");
      if (atyps == null) {
         arg_values = null;
       }
      else {
         StringTokenizer tok = new StringTokenizer(atyps," \t,;");
         arg_values = new UserValue[tok.countTokens()];
         int i = 0;
         while (tok.hasMoreTokens()) {
           String t = tok.nextToken();
           UserValue uv = null;
           if (t.equals("*") || t.equals("ANY")) ;
           else {
              uv = value_set.get(t);
            }
           arg_values[i++] = uv;
          }
         if (arg_values.length == 0) arg_values = null;
       }
      
      UserValue any = getUserValue(xml,"VALUE");
      if (any != null) {
         if (result_value == null) result_value = any;
         if (arg_values == null) {
            arg_values = new UserValue[] { any };
          }
         else {
            for (int i = 0; i < arg_values.length; ++i) {
               if (arg_values[i] == null) arg_values[i]= any;
             }
          }
       }
      String rvl = IvyXml.getAttrString(xml,"RETURN");
      if (rvl != null) {
         return_value = value_set.get(rvl);
         return_arg = -1;
         if (return_value == null) {
            if (rvl.equals("RESULT")) return_arg = 0;
            else if (rvl.equals("LHS")) return_arg = 1;
            else if (rvl.equals("RHS")) return_arg = 2;
            try {
               return_arg = Integer.parseInt(rvl);
             }
            catch (NumberFormatException e) { 
               return_arg = 0;
             }
          }
       }
    }
   
   UserValue getOperatorValue(FaitTypeOperator op,IfaceSubtype.Value val) {
      return computeValue(op.toString(),val,val,val);
    }
   
   UserValue getOperatorValue(FaitOperator op,IfaceSubtype.Value lhs,IfaceSubtype.Value rhs,IfaceSubtype.Value rslt) {
      return computeValue(op.toString(),rslt,lhs,rhs);
    }
   
   private UserValue computeValue(String op,IfaceSubtype.Value rslt,IfaceSubtype.Value... args) {
      if (operator_values != null && !operator_values.contains(op)) return null;
      return checkValues(rslt,args);
    }
   
   UserValue computeCallValue(IfaceMethod method,IfaceSubtype.Value rslt,IfaceSubtype.Value [] args) {
     
      if (call_name != null && !call_name.equals(method.getName()) &&
            !call_name.equals(method.getFullName()) && 
            !call_name.equals("*"))
         return null;
      return checkValues(rslt,args);
    }
   
   private UserValue getUserValue(Element xml,String key) {
      String v = IvyXml.getAttrString(xml,key);
      if (v == null) return null;
      return value_set.get(v);
    }
   
   private UserValue checkValues(IfaceSubtype.Value rslt,IfaceSubtype.Value [] args) {
      boolean fg;
      if (and_check) {
         fg = true;
         if (result_value != null && rslt != result_value) fg = false;
         if (arg_values != null) {
            int ct = arg_values.length;
            for (int i = 0; i < args.length; ++i) {
               UserValue match;
               if (i >= ct) match = arg_values[ct-1];
               else match = arg_values[i];
               if (match != null && args[i] != match) fg = false;
             }
          }
       }
      else {
         fg = false;
         if (result_value != null && rslt == result_value) fg = true;
         if (arg_values != null) {
            int ct = arg_values.length;
            for (int i = 0; i < args.length; ++i) {
               UserValue match;
               if (i >= ct) match = arg_values[ct-1];
               else match = arg_values[i];
               if (match != null && args[i] == match) fg = true;
             }
          }
         else if (result_value == null) fg = true;
       }
      if (!fg) return null;
      if (return_value != null) return return_value;
      if (return_arg == 0) return (UserValue) rslt;
      if (return_arg > 0 && return_arg <= args.length) {
         return (UserValue) args[return_arg-1];
       }
      
      return null;
    }

}       // end of inner class OpCheck




}       // end of class TypeSubtypeUser




/* end of TypeSubtypeUser.java */

