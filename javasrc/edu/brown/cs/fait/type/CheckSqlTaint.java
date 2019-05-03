/********************************************************************************/
/*										*/
/*		CheckSqlTaint.java						*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
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



package edu.brown.cs.fait.type;

import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

import static edu.brown.cs.fait.type.CheckSqlTaint.TaintState.*;

import java.util.List;


class CheckSqlTaint extends TypeSubtype
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static CheckSqlTaint our_type = new CheckSqlTaint();

public enum TaintState implements IfaceSubtype.Value
{
   MAYBE_SQLTAINTED, SQLTAINTED, FULLY_SQLTAINTED, UNSQLTAINTED;

   @Override public IfaceSubtype getSubtype()	{ return our_type; }
}



/********************************************************************************/
/*										*/
/*	Static access								*/
/*										*/
/********************************************************************************/

public static synchronized CheckSqlTaint getType()
{
   if (our_type == null) {
      our_type = new CheckSqlTaint();
    }
   return our_type;
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private CheckSqlTaint()
{
   super("CheckSqlTaint");

   FaitError err = new FaitError(this,ErrorLevel.ERROR,
	 "Attempt to use tainted SQL data in a non-tainted location");
   FaitError warn = new FaitError(this,ErrorLevel.WARNING,
	 "Possible attempt to use tainted SQL data in a non-tainted location");
   
   defineMerge(SQLTAINTED,UNSQLTAINTED,SQLTAINTED);
   defineMerge(MAYBE_SQLTAINTED,SQLTAINTED,SQLTAINTED);
   defineMerge(MAYBE_SQLTAINTED,UNSQLTAINTED,MAYBE_SQLTAINTED);
   defineMerge(MAYBE_SQLTAINTED,UNSQLTAINTED,MAYBE_SQLTAINTED);
   defineMerge(FULLY_SQLTAINTED,SQLTAINTED,FULLY_SQLTAINTED);
   defineMerge(FULLY_SQLTAINTED,UNSQLTAINTED,FULLY_SQLTAINTED);
   defineMerge(FULLY_SQLTAINTED,UNSQLTAINTED,FULLY_SQLTAINTED);
   defineMerge(FULLY_SQLTAINTED,MAYBE_SQLTAINTED,FULLY_SQLTAINTED);

   defineRestrict(SQLTAINTED,UNSQLTAINTED,err);
   defineRestrict(SQLTAINTED,MAYBE_SQLTAINTED,SQLTAINTED);
   defineRestrict(FULLY_SQLTAINTED,UNSQLTAINTED,err);
   defineRestrict(FULLY_SQLTAINTED,MAYBE_SQLTAINTED,FULLY_SQLTAINTED);
   defineRestrict(FULLY_SQLTAINTED,SQLTAINTED,FULLY_SQLTAINTED);
   defineRestrict(UNSQLTAINTED,SQLTAINTED,UNSQLTAINTED);
   defineRestrict(UNSQLTAINTED,MAYBE_SQLTAINTED,UNSQLTAINTED);
   defineRestrict(MAYBE_SQLTAINTED,UNSQLTAINTED,warn);		
   defineRestrict(MAYBE_SQLTAINTED,SQLTAINTED,SQLTAINTED);

   defineAttribute("SqlTainted",SQLTAINTED);
   defineAttribute("Tainted",SQLTAINTED);
   defineAttribute("SqlUntainted",UNSQLTAINTED);
   defineAttribute("Untainted",UNSQLTAINTED);
}


/********************************************************************************/
/*										*/
/*	Default value methods							*/
/*										*/
/********************************************************************************/

@Override public TaintState getDefaultValue(IfaceBaseType typ)
{
   if (typ != null && typ.isPrimitiveType()) return UNSQLTAINTED;

   return MAYBE_SQLTAINTED;
}



@Override public IfaceSubtype.Value adjustValueForBase(IfaceSubtype.Value v,IfaceBaseType b)
{
   if (b.isPrimitiveType()) return UNSQLTAINTED;
   return v;
}


@Override public TaintState getDefaultConstantValue(IfaceBaseType typ,Object cnst)
{
   return UNSQLTAINTED;
}



@Override public TaintState getDefaultUninitializedValue(IfaceType typ)
{
   return UNSQLTAINTED;
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
      case ADD :
	 if (s0 == UNSQLTAINTED && s1 == UNSQLTAINTED) return UNSQLTAINTED;
	 if (s0 == MAYBE_SQLTAINTED && s1 == MAYBE_SQLTAINTED) return MAYBE_SQLTAINTED;
	 if (s0 == MAYBE_SQLTAINTED && s1 == UNSQLTAINTED) return MAYBE_SQLTAINTED;
	 if (s0 == UNSQLTAINTED && s1 == MAYBE_SQLTAINTED) return MAYBE_SQLTAINTED;
	 return FULLY_SQLTAINTED;
      default :
	 break;
    }

   if (s0 == SQLTAINTED || s1 == SQLTAINTED || s2 == SQLTAINTED) return SQLTAINTED;
   if (s0 == UNSQLTAINTED && s1 == UNSQLTAINTED) return UNSQLTAINTED;
   return s2;
}



@Override public IfaceSubtype.Value getCallValue(IfaceCall cm,IfaceValue rslt,
      List<IfaceValue> args)
{
   if (cm.isScanned()) return null;

   IfaceSubtype.Value r = rslt.getDataType().getValue(this);
   if (r == SQLTAINTED) return r;
   boolean allok = true;
   for (IfaceValue v : args) {
      IfaceSubtype.Value s1 = v.getDataType().getValue(this);
      if (s1 == SQLTAINTED) return SQLTAINTED;
      else if (s1 != UNSQLTAINTED) allok = false;
    }
   if (allok) return UNSQLTAINTED;

   return r;
}


@Override public IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval)
{
   switch (op) {
      case STARTINIT :
      case DONEINIT :
	 return UNSQLTAINTED;
      default :
	 break;
    }
   return super.getComputedValue(op,oval);
}


@Override public boolean isPredecessorRelevant(IfaceSubtype.Value pred,IfaceSubtype.Value cur)
{
   if (pred == cur) return true;
   if (cur == FULLY_SQLTAINTED) {
      if (pred == SQLTAINTED || pred == FULLY_SQLTAINTED) return true;
    }

   return super.isPredecessorRelevant(pred,cur);
}


}	// end of class CheckSqlTaint




/* end of CheckSqlTaint.java */

