/********************************************************************************/
/*                                                                              */
/*              CheckFileTaint.java                                             */
/*                                                                              */
/*      Handle file path tainting                                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
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

import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

import static edu.brown.cs.fait.type.CheckFileTaint.TaintState.*;

import java.util.List;

class CheckFileTaint extends TypeSubtype
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static CheckFileTaint our_type = new CheckFileTaint();



public enum TaintState implements IfaceSubtype.Value
{
   MAYBE_FILETAINTED, FILETAINTED, UNFILETAINTED;
   
   @Override public IfaceSubtype getSubtype()	{ return our_type; }
}



/********************************************************************************/
/*										*/
/*	Static access								*/
/*										*/
/********************************************************************************/

public static synchronized CheckFileTaint getType()
{
   if (our_type == null) {
      our_type = new CheckFileTaint();
    }
   return our_type;
}


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CheckFileTaint()
{
   super("CheckFileTaint");
   
   FaitError err = new FaitError(this,ErrorLevel.ERROR,
         "Attempt to use tainted data in as a file name");
   FaitError warn = new FaitError(this,ErrorLevel.WARNING,
         "Possible attempt to use tainted data in as a file name");
   
   defineMerge(FILETAINTED,UNFILETAINTED,FILETAINTED);
   defineMerge(MAYBE_FILETAINTED,FILETAINTED,FILETAINTED);
   defineMerge(MAYBE_FILETAINTED,UNFILETAINTED,MAYBE_FILETAINTED);
   
   defineRestrict(FILETAINTED,UNFILETAINTED,err);
   defineRestrict(FILETAINTED,MAYBE_FILETAINTED,FILETAINTED);
   defineRestrict(UNFILETAINTED,FILETAINTED,UNFILETAINTED);
   defineRestrict(UNFILETAINTED,MAYBE_FILETAINTED,UNFILETAINTED);
   defineRestrict(MAYBE_FILETAINTED,UNFILETAINTED,warn);
   defineRestrict(MAYBE_FILETAINTED,FILETAINTED,FILETAINTED);
   
   defineAttribute("FileTainted",FILETAINTED);
   defineAttribute("Tainted",FILETAINTED);
   defineAttribute("FileUntainted",UNFILETAINTED);
   defineAttribute("Untainted",UNFILETAINTED);
}


/********************************************************************************/
/*										*/
/*	Default value methods							*/
/*										*/
/********************************************************************************/

@Override public TaintState getDefaultValue(IfaceBaseType typ)
{
   if (typ.isPrimitiveType()) return UNFILETAINTED;
   switch (typ.getName()) {
      case "java.sql.Date" :
      case "java.lang.Date" :
         return UNFILETAINTED;
      case "java.util.Number" :
      case "java.util.Integer" :
      case "java.util.Real" : 
      case "java.util.Float" : 
      case "java.util.Short" :
      case "java.util.Byte" :
      case "java.util.Long" :
      case "java.util.Character" :
         return UNFILETAINTED;
    }
   
   return MAYBE_FILETAINTED;
}



@Override public IfaceSubtype.Value adjustValueForBase(IfaceSubtype.Value v,IfaceBaseType b)
{
   if (b.isPrimitiveType()) return UNFILETAINTED;
   return v;
}


@Override public TaintState getDefaultConstantValue(IfaceBaseType typ,Object cnst)
{
   return UNFILETAINTED;
}



@Override public TaintState getDefaultUninitializedValue(IfaceType typ)
{
   return UNFILETAINTED;
}


/********************************************************************************/
/*										*/
/*	Computation methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceSubtype.Value getComputedValue(IfaceValue rslt,
      FaitOperator op,IfaceValue v0,IfaceValue v1)
{
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
   
   if (s0 == FILETAINTED || s1 == FILETAINTED || s2 == FILETAINTED) return FILETAINTED;
   if (s0 == UNFILETAINTED && s1 == UNFILETAINTED) return UNFILETAINTED;
   return s2;
}



@Override public IfaceSubtype.Value getCallValue(IfaceCall cm,IfaceValue rslt,
      List<IfaceValue> args)
{
   if (cm.isScanned()) return null;
   
   IfaceSubtype.Value r = rslt.getDataType().getValue(this);
   if (r == FILETAINTED) return r;
   boolean allok = true;
   for (IfaceValue v : args) {
      IfaceSubtype.Value s1 = v.getDataType().getValue(this);
      if (s1 == FILETAINTED) return FILETAINTED;
      else if (s1 != UNFILETAINTED) allok = false;
    }
   if (allok) return UNFILETAINTED;
   
   return r;
}


@Override public IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval)
{
   switch (op) {
      case STARTINIT :
      case DONEINIT :
	 return UNFILETAINTED;
      default :
	 break;
    }
   return super.getComputedValue(op,oval);
}




}       // end of class CheckFileTaint




/* end of CheckFileTaint.java */

