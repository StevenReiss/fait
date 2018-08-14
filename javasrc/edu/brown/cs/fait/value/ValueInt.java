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

private Long		min_value;
private Long		max_value;



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
   min_value = null;
   max_value = null;
}


ValueInt(ValueFactory vf,IfaceType dt,Long minv,Long maxv)
{
   this(vf,dt,minv,maxv,null);
}


ValueInt(ValueFactory vf,IfaceType dt,Long minv,Long maxv,IfaceEntitySet es)
{
   super(vf,dt,es);
   min_value = minv;
   max_value = maxv;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Long getMinValue()		{ return min_value; }
@Override public Long getMaxValue()     	{ return max_value; }









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

   Long mnv = null;
   Long mxv = null;
   Long v0 = null;
   Long v1 = null;
   if (min_value != null && max_value != null && min_value.equals(max_value))
      v0 = min_value;
   if (rhs.min_value != null && rhs.max_value != null && rhs.min_value.equals(rhs.max_value))
      v1 = rhs.min_value;

   switch (op) {
      case INCR :
      case ADD :
	 if (min_value != null && rhs.min_value != null) {
	    mnv = min_value + rhs.min_value;
	  }
	 if (max_value != null && rhs.max_value != null) {
	    mxv = max_value + rhs.max_value;
	  }
	 break;
      case DIV :
	 if (min_value != null && rhs.max_value != null && rhs.max_value.longValue() != 0) {
	    mnv = min_value / rhs.max_value;
	  }
	 if (max_value != null && rhs.min_value != null && rhs.min_value.longValue() != 0) {
	    mxv = max_value / rhs.min_value;
	  }
	 break;
      case MUL :
	 if (min_value != null && rhs.min_value != null) {
	    mnv = min_value * rhs.min_value;
	  }
	 if (max_value != null && rhs.max_value != null) {
	    mxv = max_value * rhs.max_value;
            if (mxv < mnv) mxv = null;
	  }
	 break;
      case MOD :
	 if (v0 != null && v1 != null && v1.longValue() != 0) {
	    mnv = v0 % v1;
	    mxv = mnv;
	  }
	 else if (rhs.max_value != null && rhs.max_value > 0) {
	    mnv = 0l;
	    mxv = rhs.max_value;
	  }
	 break;
      case SUB :
	 if (min_value != null && rhs.max_value != null && rhs.max_value.longValue() != 0) {
	    mnv = min_value - rhs.max_value;
	  }
	 if (max_value != null && rhs.min_value != null && rhs.min_value.longValue() != 0) {
	    mxv = max_value - rhs.min_value;
	  }
	 break;
      case AND :
	 if (v0 != null && v1 != null) {
	    mnv = v0 & v1;
	    mxv = mnv;
	  }
	 break;
      case OR :
	 if (v0 != null && v1 != null) {
	    mnv = v0 | v1;
	    mxv = mnv;
	  }
	 else if (min_value != null && rhs.min_value != null && max_value != null && rhs.max_value != null) {
	    long x0 = Math.min(min_value,rhs.min_value);
	    long x1 = Math.max(max_value,rhs.max_value);
	    if (x0 >= -1 && x1 <= 1) {
	       mnv = x0;
	       mxv = x1;
	     }
	  }
	 break;
      case XOR :
	 if (v0 != null && v1 != null) {
	    mnv = v0 ^ v1;
	    mxv = mnv;
	  }
	 break;
      case LSH :
	 if (v0 != null && v1 != null) {
	    mnv = v0 << v1;
	    mxv = mnv;
	  }
	 break;
      case RSH :
	 if (v0 != null && v1 != null) {
	    mnv = v0 >> v1;
	    mxv = mnv;
	  }
	 else if (v1 != null && v1 == 63) {
	    mnv = -1L;
	    mxv = 1L;
	  }
	 break;
      case RSHU :
	 if (v0 != null && v1 != null) {
	    mnv = v0 >>> v1;
	    mxv = mnv;
	  }
	 else if (v1 != null && v1 == 63) {
	    mnv = 0L;
	    mxv = 1L;
	  }
	 break;
      case TOBYTE :
	 if (v0 != null) {
	    mnv = (long) v0.byteValue();
	    mxv = mnv;
	  }
	 else {
	    if (min_value == null) mnv = (long) Byte.MIN_VALUE;
	    else mnv = Math.max(min_value.longValue(),Byte.MIN_VALUE);
	    if (max_value == null) mxv = (long) Byte.MAX_VALUE;
	    else mxv = Math.min(max_value.longValue(),Byte.MAX_VALUE);
	  }
	 break;
      case TOCHAR :
      case TOSHORT :
	 if (v0 != null) {
	    mnv = (long) v0.shortValue();
	    mxv = mnv;
	  }
	 else {
	    if (min_value == null) mnv = (long) Short.MIN_VALUE;
	    else mnv = Math.max(min_value.longValue(),Short.MIN_VALUE);
	    if (max_value == null) mxv = (long) Short.MAX_VALUE;
	    else mxv = Math.min(max_value.longValue(),Short.MAX_VALUE);
	  }
	 break;
      case TOINT :
	 if (v0 != null) {
	    mnv = (long) v0.intValue();
	    mxv = mnv;
	  }
	 else {
	    if (min_value == null) mnv = null;
	    else mnv = Math.max(min_value.longValue(),Integer.MIN_VALUE);
	    if (max_value == null) mxv = null;
	    else mxv = Math.min(max_value.longValue(),Integer.MAX_VALUE);
	  }
	 break;
      case TOLONG :
	 if (v0 != null) {
	    mnv = v0;
	    mxv = mnv;
	  }
	 break;
      case NEG :
	 if (max_value != null) mnv = -max_value;
	 if (min_value != null) mxv = -min_value;
	 break;
      case LSS :
	 if (max_value != null && rhs.min_value != null && max_value < rhs.min_value) {
	    mnv = 1l;
	    mxv = 1l;
	  }
	 else if (min_value != null && rhs.max_value != null && min_value >= rhs.max_value) {
	    mnv = 0l;
	    mxv = mnv;
	  }
	 break;
      case LEQ :
	 if (max_value != null && rhs.min_value != null && max_value <= rhs.min_value) {
	    mnv = 1l;
	    mxv = 1l;
	  }
	 else if (min_value != null && rhs.max_value != null && min_value > rhs.max_value) {
	    mnv = 0l;
	    mxv = mnv;
	  }
	 break;
      case GTR :
	 if (min_value != null && rhs.max_value != null && min_value > rhs.max_value) {
	    mnv = 1l;
	    mxv = 1l;
	  }
	 else if (max_value != null && rhs.min_value != null && max_value <= rhs.min_value) {
	    mnv = 0l;
	    mxv = mnv;
	  }
	 break;
      case GEQ :
	 if (min_value != null && rhs.max_value != null && min_value >= rhs.max_value) {
	    mnv = 1l;
	    mxv = 1l;
	  }
	 else if (max_value != null && rhs.min_value != null && max_value < rhs.min_value) {
	    mnv = 0l;
	    mxv = mnv;
	  }
	 break;
      case EQL :
	 if (v0 != null && v1 != null) {
	    if (v0.equals(v1)) mnv = 1l;
	    else mnv = 0l;
	    mxv = mnv;
	  }
	 else if (max_value != null && rhs.min_value != null && max_value < rhs.min_value) {
	    mnv = 0l;
	    mxv = mnv;
	  }
	 else if (min_value != null && rhs.max_value != null && min_value > rhs.max_value) {
	    mnv = 0l;
	    mxv = mnv;
	  }
	 break;
      case NEQ :
	 if (v0 != null && v1 != null) {
	    if (v0.equals(v1)) mnv = 0l;
	    else mnv = 1l;
	    mxv = mnv;
	  }
	 else if (max_value != null && rhs.min_value != null && max_value < rhs.min_value) {
	    mnv = 1l;
	    mxv = mnv;
	  }
	 else if (min_value != null && rhs.max_value != null && min_value > rhs.max_value) {
	    mnv = 1l;
	    mxv = mnv;
	  }
	 break;

      case TODOUBLE :
      default :
	 break;
    }

   ValueBase rslt;
   if (mnv == null && mxv == null) {
      rslt = value_factory.anyValue(typ);
    }
   else {
      if (mnv != null && mxv != null && mnv > mxv) {
         if (mxv > 0) mnv = null;
         else mxv = null;
       }
      rslt = value_factory.rangeValue(typ,mnv,mxv);
    }

   return rslt;
}





@SuppressWarnings("fallthrough")
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
   Long mnv,mxv;
   Long v0 = null;
   Long v1 = null;
   if (min_value != null && min_value.equals(max_value)) v0 = min_value;
   if (rhs != null && rhs.min_value != null && rhs.min_value.equals(rhs.max_value)) v1 = rhs.min_value;

   switch (op) {
      case NEQ :
	 flip = true;
	 // fall through
      case EQL :
	 if (rhs == null) return null;
	 mnv = null;
	 if (min_value != null && rhs.min_value != null) {
	    mnv = Math.max(min_value.longValue(),rhs.min_value.longValue());
	  }
	 mxv = null;
	 if (max_value != null && rhs.max_value != null) {
	    mxv = Math.min(max_value.longValue(),rhs.max_value.longValue());
	  }
	 if (mnv != null && mxv != null && mnv > mxv) {
	    mnv = mxv = null;
	  }
	 lt = value_factory.rangeValue(getDataType(),
	       (mnv == null ? min_value : mnv),
	       (mxv == null ? max_value : mxv));
	 rt = value_factory.rangeValue(rhs.getDataType(),
	       (mnv == null ? rhs.min_value : mnv),
	       (mxv == null ? rhs.max_value : mxv));
	 if (v1 != null && max_value != null) {
	    if (v1.equals(min_value) && min_value+1 <= max_value) {
	       lf = value_factory.rangeValue(getDataType(),min_value+1,max_value);
	     }
	    else if (v1.equals(max_value) && min_value <= max_value-1) {
	       lf = value_factory.rangeValue(getDataType(),min_value,max_value-1);
	     }
	  }
	 if (v0 != null) {
	    if (v0.equals(rhs.min_value)) {
               if (rhs.max_value == null || rhs.min_value+1 <= rhs.max_value) {
                  rf = value_factory.rangeValue(rhs.getDataType(),rhs.min_value+1,rhs.max_value);
                }
	     }
	    else if (v0.equals(rhs.max_value)) {
               if (rhs.min_value == null || rhs.min_value <= rhs.max_value-1) {
                  rf = value_factory.rangeValue(rhs.getDataType(),rhs.min_value,rhs.max_value-1);
                }
	     }
	  }
	 break;
      case GTR :
	 flip = true;
	 // fall through
      case LEQ :
	 if (rhs == null) return null;
	 if (max_value != null && rhs.max_value != null) {
            long x1 = Math.min(max_value.longValue(),rhs.max_value.longValue());
            if (min_value == null || x1 >= min_value) {
               lt = value_factory.rangeValue(getDataType(),min_value,x1);
             }
            long x2 = Math.min(rhs.max_value.longValue(),max_value-1);
            if (rhs.min_value == null || x2 >= rhs.min_value) {
               rf = value_factory.rangeValue(rhs.getDataType(),rhs.min_value,x2);
             }
	  }
	 if (min_value != null && rhs.min_value != null) {
            long x1 = Math.max(min_value.longValue(),rhs.min_value + 1);
            if (max_value == null || x1 <= max_value) {
               lf = value_factory.rangeValue(getDataType(),x1,max_value);
             }
            long x2 = Math.max(rhs.min_value.longValue(),min_value.longValue());
            if (rhs.max_value == null || x2 <= rhs.max_value) {
               rt = value_factory.rangeValue(rhs.getDataType(),x2,rhs.max_value);
             }
	  }
	 break;
      case GEQ :
	 flip = true;
	 // fall through
      case LSS :
	 if (rhs == null) return null;
	 if (max_value != null && rhs.max_value != null) {
            long x1 = Math.min(max_value.longValue(),rhs.max_value-1);
            if (min_value == null || x1 >= min_value) {
               lt= value_factory.rangeValue(getDataType(),min_value,x1);
             }
            long x2 = Math.min(rhs.max_value.longValue(),max_value.longValue());
            if (rhs.min_value == null || rhs.min_value <= x2) {
               rf = value_factory.rangeValue(rhs.getDataType(),rhs.min_value,x2);
             }
	  }
	 if (min_value != null && rhs.min_value != null) {
            long x1 = Math.max(min_value.longValue(),rhs.min_value.longValue());
            if (max_value == null || x1 <= max_value) {
               lf = value_factory.rangeValue(getDataType(),x1,max_value);
             }
            long x2 = Math.max(rhs.min_value.longValue(),min_value+1);
            if (rhs.max_value == null || x2 <= rhs.max_value) {
               rt = value_factory.rangeValue(rhs.getDataType(),x2,rhs.max_value);
             }
	  }
	 break;
      case NEQ_ZERO :
	 flip = true;
	 // fall through
      case EQL_ZERO :
	 lt = value_factory.rangeValue(getDataType(),0l,0l);
	 if (min_value != null && min_value.longValue() == 0) {
            if (max_value == null || max_value >= 1) {
               lf = value_factory.rangeValue(getDataType(),1l,max_value);
             }
	  }
	 else if (max_value != null && max_value.longValue() == 0) {
            if (min_value == null || min_value <= -1) {
               lf = value_factory.rangeValue(getDataType(),min_value,-1l);
             }
	  }
	 break;
      case GEQ_ZERO :
	 flip = true;
	 // fall through
      case LSS_ZERO :
	 if (max_value == null || max_value >= 0) {
            if (min_value == null || min_value <= -1) {
               lt = value_factory.rangeValue(getDataType(),min_value,-1l);
             }
	  }
	 if (min_value == null || min_value < 0) {
            if (max_value == null || max_value >= 0) {
               lf = value_factory.rangeValue(getDataType(),0l,max_value);
             }
	  }
	 break;
      case GTR_ZERO :
	 flip = true;
	 // fall through
      case LEQ_ZERO :
	 if (max_value == null || max_value > 0) {
            if (min_value == null || min_value <= 0) {
               lt = value_factory.rangeValue(getDataType(),min_value,0l);
             }
	  }
	 if (min_value == null || min_value <= 0) {
            if (max_value == null || max_value >= 1) {
               lf = value_factory.rangeValue(getDataType(),1l,max_value);
             }
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
   if (min_value != null && max_value != null) {
      return value_factory.floatRangeValue(dt,(double) min_value,(double) max_value);
    }
   return value_factory.anyValue(dt);
}

@Override public Integer getIndexValue()
{
   if (min_value != null && min_value.equals(max_value)) return min_value.intValue();
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

   if (!(vb instanceof ValueInt)) {
      FaitLog.logD1("Invalidate variable: Bad int value merge: " + this + " " + vb);
      return value_factory.badValue();
    }

   ValueInt cvi = (ValueInt) vb;
   IfaceType fdt = getDataType();
   IfaceType mdt = cvi.getDataType();

   if (fdt == mdt) {
      Long mnv = null;
      Long mxv = null;
      if (min_value != null && cvi.min_value != null)
	 mnv = Math.min(min_value,cvi.min_value);
      if (max_value != null && cvi.max_value != null)
	 mxv = Math.max(max_value,cvi.max_value);
      if (mnv != null && mxv != null && mxv - mnv >= VALUE_MAX_RANGE) {
	 if (Math.abs(min_value-cvi.min_value) <= 1 && mnv == 0) mxv = null;
	 else if (Math.abs(max_value-cvi.max_value) <= 0) mnv = null;
	 else mnv = mxv = null;
       }
      if (mnv != null && min_value != cvi.min_value && mxv == null && 
            mnv < -VALUE_MAX_RANGE) 
         mnv = null;
      // FaitLog.logD1("MergeInt " + min_value + " " + cvi.min_value + " " +
	    // max_value + " " + cvi.max_value + " " + mnv + " " + mxv);
      return value_factory.rangeValue(getDataType(),mnv,mxv);
    }

   if (fdt.isBroaderType(mdt)) {
      // see if we can return old value directly
      if (min_value == null && max_value == null) return this;
      if (min_value == null || (cvi.min_value != null && min_value <= cvi.min_value)) {
	 if (max_value == null || (cvi.max_value != null && max_value >= cvi.max_value))
	    return this;
       }
    }
   if (mdt.isBroaderType(fdt)) {
      // see if we can use new value directly
      if (cvi.min_value == null && cvi.max_value == null) return cvi;
      if (cvi.min_value == null || (min_value != null && cvi.min_value <= min_value)) {
	 if (cvi.max_value == null || (max_value != null && cvi.max_value >= max_value))
	    return cvi;
       }
    }

   if (!fdt.isBroaderType(mdt)) fdt = mdt;

   return value_factory.anyValue(fdt);
}

@Override public IfaceValue restrictByType(IfaceType dt)
{
   IfaceType nt = getDataType().restrictBy(dt);
   if (nt == getDataType()) return this;
   if (hasRange()) return value_factory.rangeValue(nt,min_value,max_value);
   return value_factory.anyValue(nt);
}


@Override public IfaceValue changeType(IfaceType dt)
{
   if (dt == getDataType()) return this;
   if (hasRange()) return value_factory.rangeValue(dt,min_value,max_value);
   return value_factory.anyValue(dt);
}







/********************************************************************************/
/*										*/
/*	Methods to handle branches						*/
/*										*/
/********************************************************************************/

@Override public TestBranch branchTest(IfaceValue rhs,FaitOperator op)
{
   if (rhs == null) rhs = this;

   if (!(rhs instanceof ValueInt)) return TestBranch.ANY;

   ValueInt rvi = (ValueInt) rhs;

   TestBranch rslt = TestBranch.ANY;

   switch (op) {
      case EQL :
	  if (min_value != null && min_value.equals(max_value) &&
		rvi.min_value != null && rvi.min_value.equals(rvi.max_value) &&
		min_value.equals(rvi.min_value))
	     rslt = TestBranch.ALWAYS;
	  else if (min_value != null && rvi.max_value != null && min_value > rvi.max_value)
	     rslt = TestBranch.NEVER;
	  else if (max_value != null && rvi.min_value != null && max_value < rvi.min_value)
	     rslt = TestBranch.NEVER;
	  break;
      case NEQ :
	 if (min_value != null && min_value.equals(max_value) &&
	       rvi.min_value != null && rvi.min_value.equals(rvi.max_value) &&
	       min_value.equals(rvi.min_value))
	    rslt = TestBranch.NEVER;
	 else if (min_value != null && rvi.max_value != null && min_value > rvi.max_value)
	    rslt = TestBranch.ALWAYS;
	 else if (max_value != null && rvi.min_value != null && max_value < rvi.min_value)
	    rslt = TestBranch.ALWAYS;
	 break;
      case LSS :
	 if (max_value != null && rvi.min_value != null && max_value < rvi.min_value)
	    rslt = TestBranch.ALWAYS;
	 else if (min_value != null && rvi.max_value != null && min_value >= rvi.max_value)
	    rslt = TestBranch.NEVER;
	 break;
      case GEQ :
	 if (max_value != null && rvi.min_value != null && max_value < rvi.min_value)
	    rslt = TestBranch.NEVER;
	 else if (min_value != null && rvi.max_value != null && min_value >= rvi.max_value)
	    rslt = TestBranch.ALWAYS;
	 break;
      case LEQ :
	 if (max_value != null && rvi.min_value != null && max_value <= rvi.min_value)
	    rslt = TestBranch.ALWAYS;
	 else if (min_value != null && rvi.max_value != null && min_value > rvi.max_value)
	    rslt = TestBranch.NEVER;
	 break;
      case GTR :
	 if (max_value != null && rvi.min_value != null && max_value <= rvi.min_value)
	    rslt = TestBranch.NEVER;
	 else if (min_value != null && rvi.max_value != null && min_value > rvi.max_value)
	    rslt = TestBranch.ALWAYS;
	 break;
      case EQL_ZERO :
	 if (min_value != null && min_value.equals(max_value) && min_value.longValue() == 0)
	    rslt = TestBranch.ALWAYS;
	 else if (min_value != null && min_value > 0)
	    rslt = TestBranch.NEVER;
	 else if (max_value != null && max_value < 0)
	    rslt = TestBranch.NEVER;
	 break;
      case NEQ_ZERO :
	 if (min_value != null && min_value.equals(max_value) && min_value.longValue() == 0)
	    rslt = TestBranch.NEVER;
	 else if (min_value != null && min_value > 0)
	    rslt = TestBranch.ALWAYS;
	 else if (max_value != null && max_value < 0)
	    rslt = TestBranch.ALWAYS;
	 break;
      case LSS_ZERO :
	 if (max_value != null && max_value < 0) rslt = TestBranch.ALWAYS;
	 else if (min_value != null && min_value >= 0) rslt = TestBranch.NEVER;
	 break;
      case GEQ_ZERO :
	 if (max_value != null && max_value < 0) rslt = TestBranch.NEVER;
	 else if (min_value != null && min_value >= 0) rslt = TestBranch.ALWAYS;
	 break;
      case LEQ_ZERO :
	 if (max_value != null && max_value <= 0) rslt = TestBranch.ALWAYS;
	 else if (min_value != null && min_value > 0) rslt = TestBranch.NEVER;
	 break;
      case GTR_ZERO :
	 if (max_value != null && max_value <= 0) rslt = TestBranch.NEVER;
	 else if (min_value != null && min_value > 0) rslt = TestBranch.ALWAYS;
	 break;
      default :
	 break;
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private boolean hasRange()
{
   return min_value != null && max_value != null;
}



/********************************************************************************/
/*										*/
/*	Comparison methods for ensuring uniqueness				*/
/*										*/
/********************************************************************************/

@Override public int hashCode()
{
   int hc = getDataType().hashCode();
   if (min_value != null) hc += min_value.hashCode();
   if (max_value != null) hc += max_value.hashCode();
   IfaceEntitySet es = getEntitySet();
   if (es != null) hc += es.hashCode();
   return hc;
}



@Override public boolean equals(Object o)
{
   if (o == this) return true;
   if (o instanceof ValueInt) {
      ValueInt vi = (ValueInt) o;
      if (getDataType() != vi.getDataType()) return false;
      if (!same(min_value,vi.min_value)) return false;
      if (!same(max_value,vi.max_value)) return false;
      if (getEntitySet() != vi.getEntitySet()) return false;
      return true;
    }
   return false;
}



private static boolean same(Long l0,Long l1)
{
   if (l0 == null && l1 == null) return true;
   if (l0 == null || l1 == null) return false;
   return l0.longValue() == l1.longValue();
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
   if (min_value != null || max_value != null) {
      rslt.append(" :: ");
      if (min_value != null) rslt.append(min_value);
      else rslt.append("ANY");
      rslt.append(" -> ");
      if (max_value != null) rslt.append(max_value);
      else rslt.append("ANY");
    }
   rslt.append("]");
   return rslt.toString();
}



}	// end of class ValueInt




/* end of ValueInt.java */

