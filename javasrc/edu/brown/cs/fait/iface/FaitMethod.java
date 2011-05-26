/********************************************************************************/
/*										*/
/*		FaitMethod.java 						*/
/*										*/
/*	Flow Analysis Incremental Tool method representation			*/
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



import java.util.*;



public interface FaitMethod extends FaitConstants {



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

FaitDataType getDeclaringClass();
String getName();
String getDescription();

Collection<FaitMethod> getAllCalls(FaitInstruction ins);
Collection<FaitMethod> getParentMethods();
Collection<FaitMethod> getChildMethods();

Collection<FaitTryCatchBlock> getTryCatchBlocks();


FaitValue getParameterValues(int idx);
FaitValue getThisValue();

boolean isInProject();
boolean isStaticInitializer();
boolean isStatic();
boolean isAbstract();
boolean isConstructor();
boolean isNative();
boolean isPrivate();
boolean isSynchronized();

FaitDataType getReturnType();
List<FaitDataType> getExceptionTypes();
FaitDataType getArgType(int idx);
int getNumArguments();
int getLocalSize();

int getNumInstructions();
FaitInstruction getInstruction(int i);
int getIndexOf(FaitInstruction ins);




}	// end of interface FaitMethod




/* end of FaitMethod.java */
