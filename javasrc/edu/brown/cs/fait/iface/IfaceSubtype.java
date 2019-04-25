/********************************************************************************/
/*                                                                              */
/*              IfaceSubtype.java                                               */
/*                                                                              */
/*      Description of a subtype for annotation-based checking                  */
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



package edu.brown.cs.fait.iface;

import java.util.Collection;
import java.util.List;

public interface IfaceSubtype extends FaitConstants
{

String getName();

IfaceSubtype.Value getDefaultValue(IfaceBaseType base);
IfaceSubtype.Value getValueFor(IfaceSubtype.Attr attribute);
IfaceSubtype.Value getMergeValue(IfaceSubtype.Value v1,IfaceSubtype.Value v2);
IfaceSubtype.Value getRestrictValue(IfaceSubtype.Value v1,IfaceSubtype.Value v2);
Collection<IfaceSubtype.Attr> getAttributes();

boolean isCompatibleWith(IfaceSubtype.Value v1,IfaceSubtype.Value v2);
IfaceError checkCompatabilityWith(IfaceSubtype.Value v1,IfaceSubtype.Value v2);

IfaceSubtype.Value getDefaultConstantValue(IfaceBaseType base,Object cnst);
IfaceSubtype.Value getDefaultUninitializedValue(IfaceType type);
IfaceSubtype.Value getDefaultTypeValue(IfaceType type);                 // x.class

IfaceSubtype.Value getDefaultValue(IfaceValue v);
IfaceSubtype.Value adjustValueForBase(IfaceSubtype.Value v,IfaceBaseType base);

IfaceSubtype.Value getComputedValue(IfaceValue rslt,FaitOperator op,IfaceValue lv,IfaceValue rv);
IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval);
IfaceSubtype.Value getCallValue(IfaceCall cm,IfaceValue rslt,List<IfaceValue> args);
IfaceSubtype.Value getImpliedValue(FaitOperator op,IfaceValue v0,IfaceValue v1,boolean branch);
IfaceAnnotation getArgumentAnnotation(FaitOperator op,int opnd,IfaceValue [] vals);

boolean isPredecessorRelevant(IfaceSubtype.Value pred,IfaceSubtype.Value cur);



interface Value {
   IfaceSubtype getSubtype();
}

interface Attr {
   String getName();
   IfaceSubtype getSubtype();
}


}       // end of interface IfaceSubtype




/* end of IfaceSubtype.java */

