/********************************************************************************/
/*										*/
/*		TypeNullness.java						*/
/*										*/
/*	Nullness checking type							*/
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



package edu.brown.cs.fait.type;

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import java.util.Arrays;
import java.util.Collection;

import static edu.brown.cs.fait.type.CheckNullness.NullState.MUST_BE_NULL;
import static edu.brown.cs.fait.type.CheckNullness.NullState.NON_NULL;
import static edu.brown.cs.fait.type.CheckNullness.NullState.DEREFED;
import static edu.brown.cs.fait.type.CheckNullness.NullState.CAN_BE_NULL;


public final class CheckNullness extends TypeSubtype
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

// add monotonic-non-null

public enum NullState implements IfaceSubtype.Value
{
   MUST_BE_NULL,
   NON_NULL,
   DEREFED,
   CAN_BE_NULL;

   @Override public IfaceSubtype getSubtype()	{ return our_type; }
}

private static CheckNullness  our_type = new CheckNullness();



/********************************************************************************/
/*										*/
/*	Static access								*/
/*										*/
/********************************************************************************/

public static synchronized CheckNullness getType()
{
   if (our_type == null) {
      our_type = new CheckNullness();
    }
   return our_type;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private CheckNullness() 		
{
   super("CheckNullness");
   
   FaitError err = new FaitError(this,ErrorLevel.WARNING,"Attempt to assign null to non-null location");
   FaitError err1 = new FaitError(this,ErrorLevel.WARNING,"Attempt to assign non-null to null location");   
   
   defineMerge(MUST_BE_NULL,NON_NULL,CAN_BE_NULL);
   defineMerge(MUST_BE_NULL,CAN_BE_NULL,CAN_BE_NULL);
   defineMerge(MUST_BE_NULL,DEREFED,DEREFED);
   defineMerge(NON_NULL,CAN_BE_NULL,CAN_BE_NULL);
   defineMerge(NON_NULL,DEREFED,DEREFED);
   defineMerge(CAN_BE_NULL,DEREFED,DEREFED);

   defineRestrict(MUST_BE_NULL,NON_NULL,err);
   defineRestrict(MUST_BE_NULL,CAN_BE_NULL,MUST_BE_NULL);
   defineRestrict(MUST_BE_NULL,DEREFED,err);
   defineRestrict(NON_NULL,MUST_BE_NULL,err1);
   defineRestrict(NON_NULL,CAN_BE_NULL,NON_NULL);
   defineRestrict(NON_NULL,DEREFED,NON_NULL);
   defineRestrict(CAN_BE_NULL,MUST_BE_NULL,MUST_BE_NULL);
   defineRestrict(CAN_BE_NULL,NON_NULL,NON_NULL);
   defineRestrict(CAN_BE_NULL,DEREFED,DEREFED);
   defineRestrict(DEREFED,MUST_BE_NULL,err1);
   defineRestrict(DEREFED,NON_NULL,NON_NULL);
   defineRestrict(DEREFED,CAN_BE_NULL,CAN_BE_NULL);

   defineAttribute("Nullable",CAN_BE_NULL);
   defineAttribute("NonNull",NON_NULL);
   defineAttribute("Nonnull",NON_NULL);
   defineAttribute("NotNull",NON_NULL);
   defineAttribute("MustBeNull",MUST_BE_NULL);
   defineAttribute("Derefed",DEREFED);
}



/********************************************************************************/
/*										*/
/*	Default value methods							*/
/*										*/
/********************************************************************************/

@Override public NullState getDefaultValue(IfaceBaseType typ)
{
   if (typ != null && typ.isPrimitiveType()) return NON_NULL;
   return CAN_BE_NULL;
}


@Override public String getDefaultValues()
{
   return "NON_NULL DEREFED";
}


@Override public Collection<IfaceSubtype.Value> getValues()
{
   return Arrays.asList(NullState.values());
}




@Override public IfaceSubtype.Value adjustValueForBase(IfaceSubtype.Value v,IfaceBaseType b)
{
   if (b.isPrimitiveType()) return NON_NULL;
   return v;
}

@Override public NullState getDefaultConstantValue(IfaceBaseType typ,Object cnst)
{
   if (cnst == null) return MUST_BE_NULL;

   return NON_NULL;
}

@Override public NullState getDefaultUninitializedValue(IfaceType typ)
{
   if (typ.isPrimitiveType()) return NON_NULL;
   return MUST_BE_NULL;
}

@Override public IfaceSubtype.Value getComputedValue(IfaceValue rslt,FaitOperator op,IfaceValue v0,IfaceValue v1)
{
   switch (op) {
      case DEREFERENCE :
         IfaceSubtype.Value oval = rslt.getDataType().getValue(this);
	 if (oval == NON_NULL || oval == DEREFED) return oval;
	 if (oval == MUST_BE_NULL) return oval;
	 return DEREFED;
      case FIELDACCESS :
      case ELEMENTACCESS :
         break;
      case ADD :
         return NON_NULL;
      default :
         break;
    }
   
   IfaceType t0 = rslt.getDataType();
   if (t0.isPrimitiveType()) return NON_NULL;
   return null; 
}



@Override public IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval)
{
   switch (op) {
      default :
	 break;
      case STARTINIT :
      case DONEINIT :
         return NON_NULL;
    }

   return super.getComputedValue(op,oval);
}



@Override void checkImpliedTypes(TypeImplications rslt,FaitOperator op)
{
   IfaceType t0;
   IfaceType t1;

   switch (op) {
      case NULL :
	 t0 = rslt.getLhsTrueType();
	 t0 = t0.getAnnotatedType(FaitAnnotation.MUST_BE_NULL);
	 t1 = rslt.getLhsFalseType();
	 t1 = t1.getAnnotatedType(FaitAnnotation.NON_NULL);
	 rslt.setLhsTypes(t0,t1);
	 break;
      case NONNULL :
	 t0 = rslt.getLhsTrueType();
	 t0 = t0.getAnnotatedType(FaitAnnotation.NON_NULL);
	 t1 = rslt.getLhsFalseType();
	 t1 = t1.getAnnotatedType(FaitAnnotation.MUST_BE_NULL);
	 rslt.setLhsTypes(t0,t1);
	 break;
      case NEQ :
	 if (rslt.getRhsType().getValue(this) == MUST_BE_NULL) {
	    t0 = rslt.getLhsTrueType();
	    t0 = t0.getAnnotatedType(FaitAnnotation.NON_NULL);
	    t1 = rslt.getLhsFalseType();
	    t1 = t0.getAnnotatedType(FaitAnnotation.MUST_BE_NULL);
	    rslt.setLhsTypes(t0,t1);
	  }
	 else if (rslt.getLhsType().getValue(this) == MUST_BE_NULL) {
	    t0 = rslt.getRhsTrueType();
	    t0 = t0.getAnnotatedType(FaitAnnotation.NON_NULL);
	    t1 = rslt.getRhsFalseType();
	    t1 = t0.getAnnotatedType(FaitAnnotation.MUST_BE_NULL);
	    rslt.setRhsTypes(t0,t1);
	  }
	 break;
      default :
	 super.checkImpliedTypes(rslt,op);
    }
}

@Override public NullState getDefaultTypeValue(IfaceType typ)
{
   return NON_NULL;
}



}	// end of class TypeNullness




/* end of TypeNullness.java */

