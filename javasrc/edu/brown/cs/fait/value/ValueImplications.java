/********************************************************************************/
/*                                                                              */
/*              ValueImplications.java                                          */
/*                                                                              */
/*      Holder for implied values                                               */
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



package edu.brown.cs.fait.value;

import edu.brown.cs.fait.iface.IfaceImplications;
import edu.brown.cs.fait.iface.IfaceValue;

class ValueImplications implements IfaceImplications, ValueConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceValue      lhs_true;
private IfaceValue      lhs_false;
private IfaceValue      rhs_true;
private IfaceValue      rhs_false;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValueImplications()
{
   lhs_true = null;
   lhs_false = null;
   rhs_true = null;
   rhs_false = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceValue getLhsTrueValue()
{
   return lhs_true;
}

@Override public IfaceValue getLhsFalseValue()
{
   return lhs_false;
}

@Override public IfaceValue getRhsTrueValue()
{
   return rhs_true;
}

@Override public IfaceValue getRhsFalseValue()
{
   return rhs_false; 
}



@Override public void setLhsValues(IfaceValue t,IfaceValue f)
{
   if (t != null) lhs_true = t;
   if (f != null) lhs_false = f;
}


@Override public void setRhsValues(IfaceValue t,IfaceValue f)
{
   if (t != null) rhs_true = t;
   if (f != null) rhs_false = f;
}




}       // end of class ValueImplications




/* end of ValueImplications.java */

