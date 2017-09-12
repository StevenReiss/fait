/********************************************************************************/
/*										*/
/*		IfaceCall.java							*/
/*										*/
/*	Representation of a called method					*/
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

import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcode.JcodeMethod;

public interface IfaceCall extends FaitCall
{

JcodeMethod getMethod();
JcodeDataType getMethodClass();
int getInstanceNumber();
IfaceState getStartState();
IfaceValue getResultValue();
IfaceValue getExceptionValue();

boolean isClone();
boolean isReturnArg0();

boolean getCanExit();
void setCanExit();

boolean isPrototype();
void setPrototype();

boolean getIsAsync();

FaitValue getAssociation(AssociationType typ,JcodeInstruction ins);
void setAssociation(AssociationType typ,JcodeInstruction ins,IfaceValue v);

IfaceEntity getArrayEntity(JcodeInstruction ins);
void setArrayEntity(JcodeInstruction ins,IfaceEntity e);
IfaceEntity getBaseEntity(JcodeInstruction ins);
void setBaseEntity(JcodeInstruction ins,IfaceEntity e);
FaitEntity.UserEntity getUserEntity(JcodeInstruction ins);
void setUserEntity(JcodeInstruction ins,FaitEntity.UserEntity e);

IfaceValue getThisValue();
Iterable<IfaceValue> getParameterValues();

boolean addCall(List<IfaceValue> args);
boolean addException(IfaceValue exception);
boolean hasResult();
boolean addResult(IfaceValue v);

Collection<JcodeMethod>	replaceWith(List<IfaceValue> args);
IfaceValue fixReplaceArgs(JcodeMethod fm,LinkedList<IfaceValue> args);

void addCallbacks(FaitLocation loc,List<IfaceValue> args);
JcodeMethod findCallbackMethod(JcodeDataType cls,String mthd,int asz,boolean intf);

void noteCallSite(FaitLocation loc);
Collection<FaitLocation> getCallSites();
void noteMethodCalled(JcodeInstruction ins,JcodeMethod m,IfaceCall called);
IfaceCall getMethodCalled(JcodeInstruction ins,JcodeMethod m);
Collection<IfaceCall> getAllMethodsCalled(JcodeInstruction ins);

void addDeadInstruction(JcodeInstruction ins);
void removeDeadInstruction(JcodeInstruction ins);

void clearForUpdate(IfaceUpdater upd);
void handleUpdates(IfaceUpdater upd);

String getLogName();


}	// end of interface IfaceCall




/* end of IfaceCall.java */

