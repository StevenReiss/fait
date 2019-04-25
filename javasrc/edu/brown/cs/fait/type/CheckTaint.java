/********************************************************************************/
/*                                                                              */
/*              CheckTaint.java                                                 */
/*                                                                              */
/*      Handle tainting subtypes                                                */
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

import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

import static edu.brown.cs.fait.type.CheckTaint.TaintState.*;

import java.util.List;

public class CheckTaint extends TypeSubtype
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static CheckTaint our_type = new CheckTaint();

public enum TaintState implements IfaceSubtype.Value
{
   MAYBE_TAINTED, TAINTED, UNTAINTED;

   @Override public IfaceSubtype getSubtype()   { return our_type; }
}



/********************************************************************************/
/*                                                                              */
/*      Static access                                                           */
/*                                                                              */
/********************************************************************************/

public static synchronized CheckTaint getType()
{
   if (our_type == null) {
      our_type = new CheckTaint();
    }
   return our_type;
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CheckTaint()
{
   super("CheckTaint");
   
   FaitError err = new FaitError(this,ErrorLevel.ERROR,
         "Attempt to use tainted data in a non-tainted location");
   
   defineMerge(TAINTED,UNTAINTED,TAINTED);
   defineMerge(MAYBE_TAINTED,TAINTED,TAINTED);
   defineMerge(MAYBE_TAINTED,UNTAINTED,MAYBE_TAINTED);
   
   defineRestrict(TAINTED,UNTAINTED,err);
   defineRestrict(TAINTED,MAYBE_TAINTED,TAINTED);
   defineRestrict(UNTAINTED,TAINTED,UNTAINTED);
   defineRestrict(UNTAINTED,MAYBE_TAINTED,UNTAINTED);
   defineRestrict(MAYBE_TAINTED,UNTAINTED,err);
   defineRestrict(MAYBE_TAINTED,TAINTED,TAINTED);
   
   defineAttribute("Tainted",TAINTED);
   defineAttribute("Untainted",UNTAINTED);
}


/********************************************************************************/
/*                                                                              */
/*      Default value methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public TaintState getDefaultValue(IfaceBaseType typ)
{
   if (typ.isPrimitiveType()) return UNTAINTED;
   
   return MAYBE_TAINTED;
}



@Override public IfaceSubtype.Value adjustValueForBase(IfaceSubtype.Value v,IfaceBaseType b)
{
   if (b.isPrimitiveType()) return UNTAINTED;
   return v;
}


@Override public TaintState getDefaultConstantValue(IfaceBaseType typ,Object cnst)
{
   return UNTAINTED;
}



@Override public TaintState getDefaultUninitializedValue(IfaceType typ)
{
   return UNTAINTED;
}


/********************************************************************************/
/*                                                                              */
/*      Computation methods                                                     */
/*                                                                              */
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
   
   if (s0 == TAINTED || s1 == TAINTED || s2 == TAINTED) return TAINTED;
   if (s0 == UNTAINTED && s1 == UNTAINTED) return UNTAINTED;
   return s2;
}



@Override public IfaceSubtype.Value getCallValue(IfaceCall cm,IfaceValue rslt,
      List<IfaceValue> args)
{
   if (cm.isScanned()) return null;
   
   IfaceSubtype.Value r = rslt.getDataType().getValue(this);
   if (r == TAINTED) return r;
   boolean allok = true;
   for (IfaceValue v : args) {
      IfaceSubtype.Value s1 = v.getDataType().getValue(this);
      if (s1 == TAINTED) return TAINTED;
      else if (s1 != UNTAINTED) allok = false;
    }
   if (allok) return UNTAINTED;
   
   return r;
}


@Override public IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval)
{
   switch (op) {
      case STARTINIT :
      case DONEINIT :
         return UNTAINTED;
      default :
         break;
    }
   return super.getComputedValue(op,oval);
}
   


}       // end of class CheckTaint




/* end of CheckTaint.java */

