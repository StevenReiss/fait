/********************************************************************************/
/*										*/
/*		IfaceValue.java 						*/
/*										*/
/*	Flow Analysis Incremental Tool flow value internal definition		*/
/*										*/
/*	IfaceValues are unique and immutable.  Two IfaceValues are the		*/
/*	same if and only if they are the same object (i.e. == is usable).	*/
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

public interface IfaceValue extends FaitConstants {



/********************************************************************************/
/*                                                                              */
/*      Methods for external use                                                */
/*                                                                              */
/********************************************************************************/

boolean canBeNull();
boolean mustBeNull();

boolean isEmptyEntitySet();
boolean isBad();
boolean isCategory2();
boolean isNative();
boolean isMutable();

IfaceType getDataType();

Iterable<IfaceEntity> getEntities();

boolean containsEntity(IfaceEntity src);




/********************************************************************************/
/*										*/
/*	Computation methods							*/
/*										*/
/********************************************************************************/

IfaceValue mergeValue(IfaceValue v);
IfaceValue restrictByType(IfaceType dt);
IfaceValue changeType(IfaceType dt);

IfaceValue makeSubtype(IfaceType dt);
IfaceValue forceNonNull();
IfaceValue forceInitialized(FaitAnnotation what);
IfaceValue allowNull();


IfaceValue toFloating();
Integer getIndexValue();
String getStringValue();
Long getMinValue();
Long getMaxValue();


IfaceValue performOperation(IfaceType dt,IfaceValue rhs,FaitOperator op,IfaceLocation src);
IfaceType checkOperation(FaitOperator op,IfaceValue set);
TestBranch branchTest(IfaceValue rhs,FaitOperator op);
IfaceImplications getImpliedValues(IfaceValue rhs,FaitOperator op);
 
IfaceEntitySet getModelEntitySet();
boolean isGoodEntitySet();

IfaceValue getArrayContents();
IfaceValue getArrayLength();
IfaceValue getArrayContents(IfaceValue idx);

boolean isAllNative();

boolean isReference();
int getRefSlot();
int getRefStack();
IfaceValue getRefBase();
IfaceField getRefField();
IfaceValue getRefIndex();




}	// end of interface IfaceValue




/* end of IfaceValue.java */
