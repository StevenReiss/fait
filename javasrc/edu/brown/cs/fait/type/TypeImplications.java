/********************************************************************************/
/*                                                                              */
/*              TypeImplications.java                                           */
/*                                                                              */
/*      Holder of operator implication results                                  */
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

import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceTypeImplications;

class TypeImplications implements IfaceTypeImplications, TypeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceType       lhs_type;
private IfaceType       rhs_type;
private IfaceType       lhs_true;
private IfaceType       lhs_false;
private IfaceType       rhs_true;
private IfaceType       rhs_false;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TypeImplications(IfaceType tl,IfaceType tr)
{
   lhs_type = tl;
   rhs_type = tr;
   lhs_true = tl;
   lhs_false = tl;
   rhs_true = tr;
   rhs_false = tr;
}




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

IfaceType getLhsType()                          { return lhs_type; }
IfaceType getRhsType()                          { return rhs_type; }


@Override public IfaceType getRhsTrueType()
{
   return rhs_true;
}


@Override public IfaceType getRhsFalseType()
{
   return rhs_false;
}


@Override public IfaceType getLhsTrueType()
{
   return lhs_true;
}


@Override public IfaceType getLhsFalseType()
{
   return lhs_false;
}




@Override public void setLhsTypes(IfaceType t,IfaceType f)
{
   if (t != null) lhs_true = t;
   if (f != null) lhs_false = f;
}


@Override public void setRhsTypes(IfaceType t,IfaceType f)
{
   if (t != null) rhs_true = t;
   if (f != null) rhs_false = f;
}


}       // end of class TypeImplications




/* end of TypeImplications.java */

