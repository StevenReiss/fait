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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.IfaceBaseType;
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


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TypeSubtypeUser(Element xml)
{
   value_set = new HashMap<>();
   default_constant = null;
   default_uninit = null;
   default_value = null;
   
   UserValue first = null;
   for (Element velt : IvyXml.children(xml,"VALUE")) {
      String nm = IvyXml.getAttrString(velt,"NAME");
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
    }
   
   for (Element velt : IvyXml.children(xml,"RESTRICT")) {
      String fnm = IvyXml.getAttrString(velt,"VALUE");
      UserValue fvl = value_set.get(fnm);
      String tnm = IvyXml.getAttrString(velt,"WITH");
      UserValue tvl = value_set.get(tnm);
      String rnm = IvyXml.getAttrString(velt,"YIELDS");
      if (rnm != null) {
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
         if (enm != null) fe = new FaitError(FaitError.ErrorLevel.ERROR,enm);
         else if (wnm != null) fe = new FaitError(FaitError.ErrorLevel.WARNING,wnm);   
         else if (nnm != null) fe = new FaitError(FaitError.ErrorLevel.NOTE,nnm);   
         if (fe != null) defineRestrict(fvl,tvl,fe);
       }
    }
   
   //TODO: need to specify operations and implied values here
   // might also want to specify constant based on constant type and value
   // might also want to specify uninit based on data type
   // might also want to specify default based on data type
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public UserValue getDefaultConstantValue(IfaceBaseType typ,Object cnst)
{
   return default_constant;
}


@Override public UserValue getDefaultUninitializedValue(IfaceType typ)
{
   return default_uninit;
}


@Override public UserValue getDefaultValue(IfaceBaseType typ)
{
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
   return super.getComputedValue(rslt,op,v0,v1);
}



@Override public IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval)
{
   return super.getComputedValue(op,oval);
}



@Override public IfaceSubtype.Value getImpliedValue(FaitOperator op,IfaceValue v0,IfaceValue v1,boolean branch)
{
   return super.getImpliedValue(op,v0,v1,branch);
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



}       // end of class TypeSubtypeUser




/* end of TypeSubtypeUser.java */

