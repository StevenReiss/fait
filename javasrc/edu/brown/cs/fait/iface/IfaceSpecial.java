/********************************************************************************/
/*										*/
/*		IfaceSpecial.java						*/
/*										*/
/*	Information about methods requiring special treatment			*/
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


public interface IfaceSpecial extends FaitConstants
{

IfaceValue getReturnValue(IfaceProgramPoint pt,IfaceMethod mthd);
List<IfaceValue> getExceptions(IfaceProgramPoint pt,IfaceMethod mthd);
boolean returnsArg0();
boolean isConstructor();
String getReplaceName();

Iterable<String> getCallbacks();
String getCallbackId();
List<IfaceValue> getCallbackArgs(List<IfaceValue> args,IfaceValue newval);
boolean getIsAsync();
boolean getExits();
boolean getNeverReturns();
boolean getIgnoreVirtualCalls();
boolean getSetFields();
boolean isAffected();
boolean getDontScan();
boolean getForceScan();
Collection<String> getClassesToLoad();
IfaceAnnotation [] getArgAnnotations(int idx);


}	// end of interface IfaceSpecial




/* end of IfaceSpecial.java */

