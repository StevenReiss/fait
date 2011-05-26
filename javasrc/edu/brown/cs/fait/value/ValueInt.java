/********************************************************************************/
/*										*/
/*		ValueInt.java							*/
/*										*/
/*	Integer value representation						*/
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



package edu.brown.cs.fait.value;

import edu.brown.cs.fait.iface.*;


class ValueInt extends ValueNumber
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	have_range;
private long		min_value;
private long		max_value;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValueInt(ValueFactory vf,FaitDataType dt)
{
   this(vf,dt,null);
}


ValueInt(ValueFactory vf,FaitDataType dt,IfaceEntitySet es)
{
   super(vf,dt,es);
   have_range = false;
   min_value = 0;
   max_value = 0;
}


ValueInt(ValueFactory vf,FaitDataType dt,long minv,long maxv)
{
   this(vf,dt,minv,maxv,null);
}


ValueInt(ValueFactory vf,FaitDataType dt,long minv,long maxv,IfaceEntitySet es)
{
   super(vf,dt,es);
   have_range = true;
   min_value = minv;
   max_value = maxv;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

long getMinValue()			{ return min_value; }
long getMaxValue()			{ return max_value; }




/********************************************************************************/
/*										*/
/*	Methods for handling arithmetic 					*/
/*										*/
/********************************************************************************/

@Override 
public IfaceValue performOperation(FaitDataType typ,IfaceValue rhsv,int op,FaitLocation src)
{
   if (rhsv == null) rhsv = this;

   ValueInt rhs = (ValueInt) rhsv;

   boolean valok = true;
   long v0 = 0;
   long v1 = 0;
   if (min_value == max_value) v0 = min_value;
   else valok = false;
   if (rhs.min_value == rhs.max_value) v1 = rhs.min_value;
   else valok = false;

   boolean rngok = (have_range && rhs.have_range);
   if (!rngok) valok = false;
   long mnv = min_value;
   long mxv = max_value;

   switch (op) {
      case FaitOpcodes.IINC :
      case FaitOpcodes.IADD :
      case FaitOpcodes.LADD :
	 v0 += v1;
	 if (rngok) {
	    mnv += rhs.min_value;
	    mxv += rhs.max_value;
	  }
	 break;
      case FaitOpcodes.IDIV :
      case FaitOpcodes.LDIV :
	 if (valok && v1 == 0) valok = false;
	 else if (valok) v0 /= v1;
	 if (rngok) {
	    if (rhs.min_value <= 0 && rhs.max_value >= 0) rngok = false;
	    else {
	       mnv = Math.min(mnv/rhs.max_value,mnv/rhs.min_value);
	       mxv = Math.max(mxv/rhs.max_value,mxv/rhs.min_value);
	     }
	  }
	 break;
      case FaitOpcodes.IMUL :
      case FaitOpcodes.LMUL :
	 v0 *= v1;
	 if (rngok) {
	    mnv = min_value*rhs.min_value;
	    mnv = Math.min(mnv,min_value*rhs.max_value);
	    mnv = Math.min(mnv,max_value*rhs.min_value);
	    mnv = Math.min(mnv,max_value*rhs.max_value);
	    mxv = min_value*rhs.min_value;
	    mxv = Math.max(mxv,min_value*rhs.max_value);
	    mxv = Math.max(mxv,max_value*rhs.min_value);
	    mxv = Math.max(mxv,max_value*rhs.max_value);
	  }
	 break;
      case FaitOpcodes.IREM :
      case FaitOpcodes.LREM :
	 if (v1 == 0) return value_factory.anyValue(typ);
	 if (rngok) {
	    if (rhs.min_value <= mxv) rngok = false;
	  }
	 break;
      case FaitOpcodes.ISUB :
      case FaitOpcodes.LSUB :
	 v0 -= v1;
	 if (rngok) {
	    mnv -= rhs.max_value;
	    mxv -= rhs.min_value;
	  }
	 break;
      case FaitOpcodes.IAND :
      case FaitOpcodes.LAND :
	 v0 &= v1;
	 rngok = false;
	 break;
      case FaitOpcodes.IOR :
      case FaitOpcodes.LOR :
	 v0 |= v1;
	 rngok = false;
	 break;
      case FaitOpcodes.IXOR :
      case FaitOpcodes.LXOR :
	 v0 ^= v1;
	 rngok = false;
	 break;
      case FaitOpcodes.ISHL :
      case FaitOpcodes.LSHL :
	 v0 <<= v1;
	 rngok = false;
	 break;
      case FaitOpcodes.ISHR :
      case FaitOpcodes.LSHR :
	 v0 >>= v1;
	 rngok = false;
	 break;
      case FaitOpcodes.IUSHR :
      case FaitOpcodes.LUSHR :
	 v0 >>>= v1;
	 rngok = false;
	 break;
      case FaitOpcodes.I2B :
      case FaitOpcodes.I2C :
      case FaitOpcodes.I2S :
      case FaitOpcodes.I2L :
      case FaitOpcodes.L2I :
	 break;
      case FaitOpcodes.INEG :
      case FaitOpcodes.LNEG :
	 v0 = -v0;
	 mnv = -mxv;
	 mxv = -mnv;
	 break;
      case FaitOpcodes.I2D :
      case FaitOpcodes.L2D :
      default :
	 valok = false;
	 rngok = false;
	 break;
    }

   ValueBase rslt;

   if (valok) {
      rslt = value_factory.rangeValue(typ,v0,v0);
    }
   else if (rngok) {
      rslt = value_factory.rangeValue(typ,mnv,mxv);
    }
   else rslt = value_factory.anyValue(typ);

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Methods to handle merging values					*/
/*										*/
/********************************************************************************/

@Override public IfaceValue mergeValue(IfaceValue vb)
{
   if (vb == this || vb == null) return this;

   if (!(vb instanceof ValueInt)) return value_factory.badValue();

   ValueInt cvi = (ValueInt) vb;
   FaitDataType fdt = getDataType();
   FaitDataType mdt = cvi.getDataType();

   if (!have_range || (cvi.have_range && min_value <= cvi.min_value &&
	 max_value >= cvi.max_value)) {
      if (fdt.isBroaderType(mdt)) return this;
    }

   if (!cvi.have_range || (have_range && cvi.min_value < min_value &&
	 cvi.max_value >= max_value)) {
      if (mdt.isBroaderType(fdt)) return cvi;
    }

   if (have_range && cvi.have_range && getDataType() == cvi.getDataType()) {
      return value_factory.rangeValue(getDataType(),
	    Math.min(min_value,cvi.min_value),
	    Math.max(max_value,cvi.max_value));
    }

   if (!fdt.isBroaderType(mdt)) fdt = mdt;

   return value_factory.anyValue(fdt);
}



@Override protected IfaceValue newEntityValue(IfaceEntitySet cs)
{
   cs = cs.addToSet(getEntitySet());

   ValueInt nv = this;

   if (have_range) {
      nv = new ValueInt(value_factory,getDataType(),min_value,max_value,cs);
    }
   else {
      nv = new ValueInt(value_factory,getDataType(),cs);
    }

   return nv;
}




/********************************************************************************/
/*										*/
/*	Methods to handle branches						*/
/*										*/
/********************************************************************************/

@Override public TestBranch branchTest(IfaceValue rhs,int op)
{
   if (rhs == null) rhs = this;

   ValueInt rvi = (ValueInt) rhs;

   TestBranch rslt = TestBranch.ANY;

   if (have_range && rvi.have_range) {
      switch (op) {
	 case FaitOpcodes.IF_ICMPEQ :
	    if (min_value == max_value && min_value == rvi.min_value &&
		  max_value == rvi.max_value)
	       rslt = TestBranch.ALWAYS;
	    else if (min_value > rvi.max_value || max_value < rvi.min_value)
	       rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IF_ICMPNE :
	    if (min_value == max_value && min_value == rvi.min_value &&
		  max_value == rvi.max_value)
	       rslt = TestBranch.NEVER;
	    else if (min_value > rvi.max_value || max_value < rvi.min_value)
	       rslt = TestBranch.ALWAYS;
	    break;
	 case FaitOpcodes.IF_ICMPLT :
	    if (max_value < rvi.min_value) rslt = TestBranch.ALWAYS;
	    else if (min_value >= rvi.max_value) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IF_ICMPGE :
	    if (min_value >= rvi.max_value) rslt = TestBranch.ALWAYS;
	    else if (max_value < rvi.min_value) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IF_ICMPGT :
	    if (min_value > rvi.max_value) rslt = TestBranch.ALWAYS;
	    else if (max_value <= rvi.min_value) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IF_ICMPLE :
	    if (max_value <= rvi.min_value) rslt = TestBranch.ALWAYS;
	    else if (min_value > rvi.max_value) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IFEQ :
	    if (min_value == max_value && min_value == 0) rslt = TestBranch.ALWAYS;
	    else if (min_value > 0 || max_value < 0) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IFNE :
	    if (min_value == max_value && min_value == 0) rslt = TestBranch.NEVER;
	    else if (min_value > 0 || max_value < 0) rslt = TestBranch.ALWAYS;
	    break;
	 case FaitOpcodes.IFLT :
	    if (max_value < 0) rslt = TestBranch.ALWAYS;
	    else if (min_value >= 0) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IFLE :
	    if (max_value < 0) rslt = TestBranch.ALWAYS;
	    else if (min_value > 0) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IFGT :
	    if (min_value > 0) rslt = TestBranch.ALWAYS;
	    else if (max_value <= 0) rslt = TestBranch.NEVER;
	    break;
	 case FaitOpcodes.IFGE :
	    if (min_value >= 0) rslt = TestBranch.ALWAYS;
	    else if (max_value < 0) rslt = TestBranch.NEVER;
	    break;
	 default :
	    rslt = TestBranch.ANY;
	    break;
       }
    }

   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   StringBuffer rslt = new StringBuffer();
   rslt.append("[");
   rslt.append(getDataType().getName());
   if (have_range) {
      rslt.append(" :: ");
      rslt.append(min_value);
      rslt.append(" -> ");
      rslt.append(max_value);
    }
   rslt.append("]");
   return rslt.toString();
}



}	// end of class ValueInt




/* end of ValueInt.java */

