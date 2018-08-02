/********************************************************************************/
/*										*/
/*		IfaceType.java							*/
/*										*/
/*	Type along with subtypes						*/
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


package edu.brown.cs.fait.iface;

import java.util.Collection;
import java.util.List;

import edu.brown.cs.fait.iface.FaitConstants.FaitOperator;

public interface IfaceType
{

String getName();
String getSignature();
String getJavaTypeName();

boolean isCategory2();
boolean isPrimitiveType();
boolean isFloatingType();
boolean isVoidType();
boolean isInterfaceType();
boolean isArrayType();
boolean isIntType();
boolean isJavaLangObject();
boolean isNumericType();
boolean isStringType();
boolean isBooleanType();
boolean isFunctionRef();

boolean isAbstract();

boolean isBroaderType(IfaceType t);
boolean isDerivedFrom(IfaceType t);
boolean isCompatibleWith(IfaceType t);
boolean checkCompatibility(IfaceType t,IfaceLocation loc);

IfaceType getArrayType();
List<IfaceType> getInterfaces();
IfaceType findChildForInterface(IfaceType dt);
IfaceType getCommonParent(IfaceType t2);
IfaceType restrictBy(IfaceType tr);
List<IfaceType> getChildTypes();
IfaceType getAssociatedType();
IfaceType getRunTimeType();
IfaceType getBaseType();
IfaceType getSuperType();

boolean isEditable();
boolean isInProject();

IfaceSubtype.Value getValue(IfaceSubtype styp);
boolean checkValue(IfaceSubtype.Value v);
boolean checkValue(IfaceAnnotation ... annots);
IfaceBaseType getJavaType();
IfaceType getAnnotatedType(IfaceAnnotation ... an);
IfaceType getAnnotatedType(Collection<IfaceAnnotation> ans);
IfaceType getAnnotatedType(IfaceType tannot);

IfaceType getComputedType(IfaceValue r,FaitOperator op,IfaceValue lv,IfaceValue rv);
IfaceType getComputedType(FaitOperator op);

IfaceTypeImplications getImpliedTypes(FaitOperator op,IfaceType tr);
List<IfaceType> getBackTypes(FaitOperator op,IfaceValue ... v);
List<String> getAnnotations();







}	// end of interface IfaceType




/* end of IfaceType.java */

