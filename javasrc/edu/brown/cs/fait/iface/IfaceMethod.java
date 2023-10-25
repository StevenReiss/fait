/********************************************************************************/
/*										*/
/*		IfaceMethod.java						*/
/*										*/
/*	Common representation of a method					*/
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

import edu.brown.cs.ivy.jcode.JcodeTryCatchBlock;


public interface IfaceMethod
{


String getFullName();
String getName();
String getDescription();
String getFile();
String getSignature();

boolean isStatic();
boolean isVarArgs();
boolean isStaticInitializer();
boolean isConstructor();
boolean isNative();
boolean isPrivate();
boolean isAbstract();
boolean isSynchronized();
boolean isEditable();
boolean hasCode();
IfaceType getReturnType();
int getNumArgs();

default int getArgSize() 
{
   int base = 0;
   if (!isStatic()) {
      base = 1;
      if (isConstructor() && getDeclaringClass().getSuperType() != null) base= 2;
    }
   for (int i = 0; i < getNumArgs(); ++i) {
      IfaceType argtyp = getArgType(i);
      if (argtyp.isCategory2()) base += 2;
      else base += 1;
    }
   
   return base;
}

IfaceType getArgType(int i);
IfaceType getDeclaringClass();
List<IfaceType> getExceptionTypes();
List<IfaceMethod> getParentMethods();
Collection<IfaceMethod> getChildMethods();
Collection<JcodeTryCatchBlock> getTryCatchBlocks();

IfaceProgramPoint getStart();
IfaceProgramPoint getNext(IfaceProgramPoint pt);

List<IfaceAnnotation> getReturnAnnotations();
List<IfaceAnnotation> getArgAnnotations(int i);
IfaceType getLocalType(int slot,IfaceProgramPoint pt);
List<IfaceAnnotation> getLocalAnnotations(int slot,IfaceProgramPoint pt);

int getLocalSize();
int getLocalOffset(Object symbol);
Object getItemAtOffset(int offset,IfaceProgramPoint pt);
Collection<Object> getExternalSymbols();
void setExternalValue(Object sym,IfaceValue v);
IfaceValue getExternalValue(Object sym);


}	// end of interface IfaceMethod




/* end of IfaceMethod.java */

