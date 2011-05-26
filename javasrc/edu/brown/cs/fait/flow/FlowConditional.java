/********************************************************************************/
/*										*/
/*		FlowConditional.java						*/
/*										*/
/*	Handle implications of evaluation of values				*/
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



package edu.brown.cs.fait.flow;

import edu.brown.cs.fait.iface.*;


/**
 *	This class handles implications of running assuming that the code does
 *	not abort.  For example, if an access is not going to result in a null
 *	pointer exception, then one can assume that the value is not-null.  This
 *	code takes that information and works backwards from the access to
 *	set the actual value (local variable or local field) to be non-null.
 *
 *	It also handles branch implications, setting the value of the field
 *	or local based on what branch is taken.
 **/

class FlowConditional implements FlowConstants, FaitOpcodes
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl		fait_control;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowConditional(FaitControl fc)
{
   fait_control = fc;
}



/********************************************************************************/
/*										*/
/*	Handle branch implications						*/
/*										*/
/********************************************************************************/

IfaceState handleImplications(FlowQueueInstance wq,FaitInstruction ins,
      IfaceState st0,TestBranch brslt)
{
   int act = 1;
   switch (ins.getOpcode()) {
      case IF_ICMPEQ : case IF_ICMPNE : case IF_ICMPLT :
      case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
	 act = 2;
	 break;
    }

   boolean inst = !ins.getMethod().isStatic();
   WhereItem where = getWhere(ins.getPrevious(),act);

   if (brslt != TestBranch.NEVER) {
      IfaceState st1 = st0;
      if (where != null && brslt != TestBranch.ALWAYS) {
	 switch (ins.getOpcode()) {
	    case IFNULL :
	       st1 = where.setNull(st0,true,inst);
	       break;
	    case IFNONNULL :
	       st1 = where.setNonNull(st0,true,false,inst);
	       break;
	    case IFEQ :
	    case IF_ICMPEQ :
	       st1 = where.setEqual(st0,true,inst);
	       break;
	    case IFNE :
	    case IF_ICMPNE :
	       st1 = where.setNonEqual(st0,true,inst);
	       break;
	  }
       }
      FaitInstruction tins = ins.getTargetInstruction();
      wq.mergeState(st1,tins);
    }

   if (brslt == TestBranch.ALWAYS) return null;

   if (where != null && brslt != TestBranch.NEVER) {
      switch (ins.getOpcode()) {
	 case IFNULL :
	    st0 = where.setNonNull(st0,false,false,inst);
	    break;
	 case IFNONNULL :
	    st0 = where.setNull(st0,false,inst);
	    break;
	 case IFEQ :
	 case IF_ICMPEQ :
	    st0 = where.setNonEqual(st0,false,inst);
	    break;
	 case IFNE :
	 case IF_ICMPNE :
	    st0 = where.setEqual(st0,false,inst);
	    break;
       }
    }

   return st0;
}




/********************************************************************************/
/*										*/
/*	Handle access (if access works, the reference must be non-null) 	*/
/*										*/
/********************************************************************************/

IfaceState handleAccess(FaitLocation loc,int act,IfaceState st0)
{
   FaitInstruction ins = loc.getInstruction();
   boolean inst = !ins.getMethod().isStatic();

   FaitInstruction sino = skipWhere(ins.getPrevious(),act);

   WhereItem where = getWhere(sino,1);

   if (where != null) {
      st0 = where.setNonNull(st0,false,true,inst);
    }

   return st0;
}




/********************************************************************************/
/*										*/
/*	Methods to find the source for a value					*/
/*										*/
/********************************************************************************/

private WhereItem getWhere(FaitInstruction ins,int act)
{
   if (ins == null) return null;

   WhereItem where = null;
   FaitField fld = null;

   while (where == null && ins != null) {
      switch (ins.getOpcode()) {
	 case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
	 case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
	 case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
	    where = new WhereItem(ins.getLocalVariable(),fld);
	    break;
	 case DUP : case DUP_X1 : case DUP_X2 : case DUP2 : case DUP2_X1 : case DUP2_X2 :
	    break;
	 case GETFIELD :
	    if (fld != null) return null;
	    fld = ins.getFieldReference();
	    break;
	 case GETSTATIC :
	    if (fld != null) return null;
	    fld = ins.getFieldReference();
	    where = new WhereItem(-1,fld);
	    break;
	 case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
	 case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :
	 case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
	    FaitInstruction pins = ins.getPrevious();
	    if (pins != null) {
	       switch (pins.getOpcode()) {
		  case DUP : case DUP_X1 : case DUP_X2 : case DUP2 :
		  case DUP2_X1 : case DUP2_X2 :
		     where = new WhereItem(ins.getLocalVariable(),fld);
		     break;
		  case ISUB :
		     // check for iconst 1, dup preceding theis and use value-1
		     return null;
		  default :
		     return null;
		}
	     }
	    break;
	 default :
	    return null;
       }
      ins = ins.getPrevious();
    }

   if (where == null) return null;

   if (act > 1) {
      switch (ins.getOpcode()) {
	 case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
	 case ICONST_5 : case ICONST_M1 :
	 case LCONST_0 : case LCONST_1 :
	    where.setValue(ins.getIntValue());
	    break;
	 case LDC : case LDC_W :
	    Object o = ins.getObjectValue();
	    if (o instanceof Number) {
	       where.setValue(((Number) o).longValue());
	     }
	    else where = null;
	    break;
	 default :
	    where = null;
	    break;
       }
    }

   return where;
}



private FaitInstruction skipWhere(FaitInstruction ins,int act)
{
   int tot = 0;
   while (ins != null && tot < act) {
      int diff = ins.getStackDiff();
      tot += diff;
      ins = ins.getPrevious();
    }

   return ins;
}








/********************************************************************************/
/*										*/
/*	Class representing a value source					*/
/*										*/
/********************************************************************************/

private class WhereItem {

   private int var_number;
   private FaitField field_name;
   private long int_value;

   WhereItem(int var,FaitField fld) {
      var_number = var;
      field_name = fld;
      int_value = 0;
    }

   void setValue(long v)			{ int_value = v; }

   IfaceState setNull(IfaceState cs,boolean clone,boolean inst) {
      if (field_name == null) {
	 if (var_number >= 0) {
	    IfaceValue v = cs.getLocal(var_number);
	    if (!v.mustBeNull()) {
	       if (clone) cs = cs.cloneState();
	       cs.setLocal(var_number,fait_control.findNullValue());
	     }
	  }
       }
      else {
	 if (var_number == 0 && inst) {
	    IfaceValue v = cs.getFieldValue(field_name);
	    if (v == null || !v.mustBeNull()) {
	       if (clone) cs = cs.cloneState();
	       cs.setFieldValue(field_name,fait_control.findNullValue());
	     }
	  }
       }
      return cs;
    }

   IfaceState setNonNull(IfaceState cs,boolean clone,boolean force,boolean inst) {
      if (field_name == null) {
	 if (var_number >= 0) {
	    IfaceValue cv = cs.getLocal(var_number);
	    IfaceValue cv0 = cv.forceNonNull();
	    if (cv != cv0) {
	       if (clone) cs = cs.cloneState();
	       cs.setLocal(var_number,cv0);
	     }
	  }
       }
      else {
	 if (var_number == 0 && inst && !field_name.isStatic()) {
	    IfaceValue cv = cs.getFieldValue(field_name);
	    if (cv != null && cv.canBeNull()) {
	       cv = cv.forceNonNull();
	       if (clone) cs = cs.cloneState();
	       cs.setFieldValue(field_name,cv);
	     }
	  }
       }
      return cs;
    }

   IfaceState setEqual(IfaceState cs,boolean clone,boolean inst) {
      FaitDataType dt = fait_control.findDataType("I");
      IfaceValue v = fait_control.findRangeValue(dt,int_value,int_value);
      if (field_name == null) {
	 if (var_number >= 0) {
	    if (clone) cs = cs.cloneState();
	    cs.setLocal(var_number,v);
	  }
       }
      else if (var_number == 0 && inst) {
	 if (cs.getFieldValue(field_name) != v) {
	    if (clone) cs = cs.cloneState();
	    cs.setFieldValue(field_name,v);
	  }
       }
      return cs;
    }

   IfaceState setNonEqual(IfaceState cs,boolean cln,boolean inst) {
      return cs;
    }

}	// end of inner class WhereItem



}	// end of class FlowConditional



/* end of FlowConditional.java */

