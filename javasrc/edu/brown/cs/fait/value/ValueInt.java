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
import edu.brown.cs.ivy.jcode.JcodeConstants;


class ValueInt extends ValueNumber implements JcodeConstants
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

ValueInt(ValueFactory vf,IfaceType dt)
{
   this(vf,dt,null);
}


ValueInt(ValueFactory vf,IfaceType dt,IfaceEntitySet es)
{
   super(vf,dt,es);
   have_range = false;
   min_value = 0;
   max_value = 0;
}


ValueInt(ValueFactory vf,IfaceType dt,long minv,long maxv)
{
   this(vf,dt,minv,maxv,null);
}


ValueInt(ValueFactory vf,IfaceType dt,long minv,long maxv,IfaceEntitySet es)
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

@Override protected IfaceValue localPerformOperation(IfaceType typ,IfaceValue rhsv,
        FaitOperator op,IfaceLocation src)
{
   if (rhsv == null) rhsv = this;
   
   if (rhsv instanceof ValueBad) return value_factory.anyValue(typ);
   if (!(rhsv instanceof ValueInt)) return value_factory.anyValue(typ);
   
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
      case INCR :
      case ADD :
	 v0 += v1;
	 if (rngok) {
	    mnv += rhs.min_value;
	    mxv += rhs.max_value;
	  }
	 break;
      case DIV :
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
      case MUL :
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
      case MOD :
	 if (v1 == 0) return value_factory.anyValue(typ);
	 if (rngok) {
	    if (rhs.min_value <= mxv) rngok = false;
	  }
	 break;
      case SUB :
	 v0 -= v1;
	 if (rngok) {
	    mnv -= rhs.max_value;
	    mxv -= rhs.min_value;
	  }
	 break;
      case AND :
	 v0 &= v1;
	 rngok = false;
	 break;
      case OR :
	 v0 |= v1;
	 rngok = false;
	 break;
      case XOR :
	 v0 ^= v1;
	 rngok = false;
	 break;
      case LSH :
	 v0 <<= v1;
	 rngok = false;
	 break;
      case RSH :
	 v0 >>= v1;
	 rngok = false;
	 break;
      case RSHU :
	 v0 >>>= v1;
	 rngok = false;
	 break;
      case TOBYTE :
      case TOCHAR :
      case TOSHORT :
      case TOLONG :
      case TOINT :
	 break;
      case NEG :
	 v0 = -v0;
	 mnv = -mxv;
	 mxv = -mnv;
	 break;
      case LSS :
         if (rngok) {
            if (max_value < rhs.min_value) {
               v0 = 1;
               valok = true;
             }
            else if (min_value >= rhs.max_value) {
               v0 = 0;
               valok = true;
             }
          }
         break;
      case LEQ :
         if (rngok) {
            if (max_value <= rhs.min_value) {
               v0 = 1;
               valok = true;
             }
            else if (min_value > rhs.max_value) {
               v0 = 0;
               valok = true;
             }
          }
         break;
      case GTR :
         if (rngok) {
            if (min_value > rhs.max_value) {
               v0 = 1;
               valok = true;
             }
            else if (max_value <= rhs.min_value) {
               v0 = 0;
               valok = true;
             }
          }
         break;
      case GEQ :
         if (rngok) {
            if (min_value >= rhs.max_value) {
               v0 = 1;
               valok = true;
             }
            else if (max_value < rhs.min_value) {
               v0 = 0;
               valok = true;
             }
          }
         break;
      case EQL :
         if (v0 == v1) v0 = 1;
         else v0 = 0;
         break;
      case NEQ :
         if (v0 != v1) v0 = 1;
         else v0 = 0;
         break;
         
      case TODOUBLE :
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





@Override public IfaceImplications getImpliedValues(IfaceValue rhsv,FaitOperator op)
{
   ValueInt rhs = null;
   IfaceType rtyp = null;
   if (rhsv != null && rhsv instanceof ValueInt) {
      rhs = (ValueInt) rhsv;
      rtyp = rhs.getDataType();
    }
   
   ValueImplications imp = null;
   ValueBase lt = null;
   ValueBase lf = null;
   ValueBase rt = null;
   ValueBase rf = null;
   boolean flip = false;
   
   switch (op) {
      case NEQ :
         flip = true;
         //$FALL-THROUGH$
      case EQL :
         if (have_range && rhs.have_range) {
            long min = Math.max(min_value,rhs.min_value);
            long max = Math.min(max_value,rhs.max_value);
            lt = value_factory.rangeValue(getDataType(),min,max);
            rt = value_factory.rangeValue(rhs.getDataType(),min,max);
            if (rhs.min_value == rhs.max_value) {
               if (min_value == rhs.min_value) {
                  lf = value_factory.rangeValue(getDataType(),min_value+1,max_value);
                }
               else if (max_value == rhs.max_value) {
                  lf = value_factory.rangeValue(getDataType(),min_value,max_value-1);
                }
             }
            if (min_value == max_value) {
               if (rhs.min_value == min_value) {
                  rf = value_factory.rangeValue(rhs.getDataType(),rhs.min_value+1,rhs.max_value);
                }
               else if (rhs.max_value == max_value) {
                  rf = value_factory.rangeValue(rhs.getDataType(),rhs.min_value,rhs.max_value-1);
                }
             }
          }
         break;
      case GTR :
         flip = true;
	 //$FALL-THROUGH$
      case LEQ :
         if (have_range && rhs.have_range) {
            lt = value_factory.rangeValue(getDataType(),min_value,Math.min(max_value,rhs.max_value));
            lf = value_factory.rangeValue(getDataType(),Math.max(min_value,rhs.min_value+1),max_value);
            rt = value_factory.rangeValue(getDataType(),Math.max(rhs.min_value,min_value),rhs.max_value);
            rf = value_factory.rangeValue(getDataType(),rhs.min_value,Math.min(rhs.max_value,max_value-1));
          }
         break;
      case GEQ :
         flip = true;
	 //$FALL-THROUGH$
      case LSS :
         if (have_range && rhs.have_range) {
            lt = value_factory.rangeValue(getDataType(),min_value,Math.min(max_value,rhs.max_value-1));
            lf = value_factory.rangeValue(getDataType(),Math.max(min_value,rhs.min_value),max_value);
            rt = value_factory.rangeValue(rhs.getDataType(),Math.max(rhs.min_value,min_value+1),rhs.max_value);
            rf = value_factory.rangeValue(rhs.getDataType(),rhs.min_value,Math.min(rhs.max_value,max_value));
          }
         break;
      case NEQ_ZERO :
         flip = true;
	 //$FALL-THROUGH$
      case EQL_ZERO :
         lt = value_factory.rangeValue(getDataType(),0,0);
         if (have_range && min_value == 0 && max_value > 0) {
            lf = value_factory.rangeValue(getDataType(),1,max_value);
          }
         break;
      case GEQ_ZERO :
         flip = true;
         //$FALL-THROUGH$
      case LSS_ZERO :
         if (have_range && min_value < 0) {
            lt = value_factory.rangeValue(getDataType(),min_value,-1);
            lf = value_factory.rangeValue(getDataType(),0,max_value);
          }
         break;
      case GTR_ZERO :
         flip = true;
         //$FALL-THROUGH$
      case LEQ_ZERO :
         if (have_range && min_value <= 0) {
            lt = value_factory.rangeValue(getDataType(),min_value,0);
            lf = value_factory.rangeValue(getDataType(),1,max_value); 
          }
         break; 
      default :
         return null;
    }
   
   if (flip) {
      ValueBase t = lf;
      lf = lt;
      lt = t;
      t = rf;
      rf = rt;
      rt = t;
    }
   
   IfaceTypeImplications timp = null;
   if (lf == this) lf = null;
   if (lt == this) lt = null;
   if (lf != null || lt != null) {
      timp = getDataType().getImpliedTypes(op,rtyp);
      if (lt != null) lt = (ValueBase) lt.restrictByType(timp.getLhsTrueType());
      if (lf != null) lf = (ValueBase) lf.restrictByType(timp.getLhsFalseType());
      imp = new ValueImplications();
      if (timp == null) timp = getDataType().getImpliedTypes(op,rhs.getDataType());
      imp.setLhsValues(lt,lf);
    }
   if (rf == rhs) rf = null;
   if (rt == rhs) rt = null;
   if (rt != null || rf != null) {
      if (timp == null) timp = getDataType().getImpliedTypes(op,rtyp);
      if (imp == null) imp = new ValueImplications();
      if (rt != null) rt = (ValueBase) rt.restrictByType(timp.getRhsTrueType());
      if (rf != null) rf = (ValueBase) rf.restrictByType(timp.getRhsFalseType());
      imp.setRhsValues(rt,rf);
    }
   
   return imp;
   
}


@Override public IfaceValue toFloating()
{
   IfaceType dt = value_factory.getFaitControl().findDataType("double");
   if (have_range) {
      return value_factory.rangeValue(dt,(double) min_value,(double) max_value);
    }
   return value_factory.anyValue(dt);
}

@Override public Integer getIndexValue()
{
   if (have_range && min_value == max_value) return (int) min_value;
   return null;
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
   IfaceType fdt = getDataType();
   IfaceType mdt = cvi.getDataType();

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

@Override public IfaceValue restrictByType(IfaceType dt) 
{
   IfaceType nt = getDataType().restrictBy(dt);
   if (nt == getDataType()) return this;
   if (have_range) return value_factory.rangeValue(nt,min_value,max_value);
   return value_factory.anyValue(nt);
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

@Override public TestBranch branchTest(IfaceValue rhs,FaitOperator op)
{
   if (rhs == null) rhs = this;

   ValueInt rvi = (ValueInt) rhs;

   TestBranch rslt = TestBranch.ANY;

   if (have_range && rvi.have_range) {
      switch (op) {
	 case EQL :
	    if (min_value == max_value && min_value == rvi.min_value &&
		  max_value == rvi.max_value)
	       rslt = TestBranch.ALWAYS;
	    else if (min_value > rvi.max_value || max_value < rvi.min_value)
	       rslt = TestBranch.NEVER;
	    break;
	 case NEQ :
	    if (min_value == max_value && min_value == rvi.min_value &&
		  max_value == rvi.max_value)
	       rslt = TestBranch.NEVER;
	    else if (min_value > rvi.max_value || max_value < rvi.min_value)
	       rslt = TestBranch.ALWAYS;
	    break;
	 case LSS :
	    if (max_value < rvi.min_value) rslt = TestBranch.ALWAYS;
	    else if (min_value >= rvi.max_value) rslt = TestBranch.NEVER;
	    break;
	 case GEQ :
	    if (min_value >= rvi.max_value) rslt = TestBranch.ALWAYS;
	    else if (max_value < rvi.min_value) rslt = TestBranch.NEVER;
	    break;
	 case GTR :
	    if (min_value > rvi.max_value) rslt = TestBranch.ALWAYS;
	    else if (max_value <= rvi.min_value) rslt = TestBranch.NEVER;
	    break;
	 case LEQ :
	    if (max_value <= rvi.min_value) rslt = TestBranch.ALWAYS;
	    else if (min_value > rvi.max_value) rslt = TestBranch.NEVER;
	    break;
	 case EQL_ZERO :
	    if (min_value == max_value && min_value == 0) rslt = TestBranch.ALWAYS;
	    else if (min_value > 0 || max_value < 0) rslt = TestBranch.NEVER;
	    break;
	 case NEQ_ZERO :
	    if (min_value == max_value && min_value == 0) rslt = TestBranch.NEVER;
	    else if (min_value > 0 || max_value < 0) rslt = TestBranch.ALWAYS;
	    break;
	 case LSS_ZERO :
	    if (max_value < 0) rslt = TestBranch.ALWAYS;
	    else if (min_value >= 0) rslt = TestBranch.NEVER;
	    break;
	 case LEQ_ZERO :
	    if (max_value < 0) rslt = TestBranch.ALWAYS;
	    else if (min_value > 0) rslt = TestBranch.NEVER;
	    break;
	 case GTR_ZERO :
	    if (min_value > 0) rslt = TestBranch.ALWAYS;
	    else if (max_value <= 0) rslt = TestBranch.NEVER;
	    break;
	 case GEQ_ZERO :
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
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer rslt = new StringBuffer();
   rslt.append("[");
   rslt.append(getDataType());
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

