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

import edu.brown.cs.ivy.jcode.JcodeDataType;

public interface IfaceValue extends FaitValue {


/********************************************************************************/
/*										*/
/*	Computation methods							*/
/*										*/
/********************************************************************************/

IfaceValue mergeValue(IfaceValue v);
IfaceValue restrictByType(JcodeDataType dt,boolean pfg,FaitLocation src);
IfaceValue removeByType(JcodeDataType dt,FaitLocation src);
IfaceValue makeSubtype(JcodeDataType dt);
IfaceValue forceNonNull();
IfaceValue allowNull();
IfaceValue setTestNull();
IfaceValue addEntity(IfaceEntitySet e);

IfaceValue performOperation(JcodeDataType dt,IfaceValue rhs,int op,FaitLocation src);
TestBranch branchTest(IfaceValue rhs,int op);
 
IfaceEntitySet getModelEntitySet();
boolean isGoodEntitySet();

IfaceValue getArrayContents();

boolean isAllNative();




}	// end of interface IfaceValue




/* end of IfaceValue.java */
