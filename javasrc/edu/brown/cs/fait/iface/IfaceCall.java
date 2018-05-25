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


public interface IfaceCall extends FaitConstants
{

IfaceMethod getMethod();
IfaceType getMethodClass();

IfaceState getStartState();
IfaceValue getResultValue();
IfaceValue getExceptionValue();

boolean isClone();
boolean isReturnArg0();

boolean getCanExit();
void setCanExit();

boolean isPrototype();
void setPrototype();

QueueLevel getQueueLevel();
void setQueueLevel(QueueLevel lvl);

boolean getIsAsync();
void loadClasses();

IfaceControl getControl();


IfaceProgramPoint getStartPoint();

IfaceEntity getArrayEntity(IfaceProgramPoint ins);
void setArrayEntity(IfaceProgramPoint ins,IfaceEntity e);
IfaceEntity getBaseEntity(IfaceProgramPoint ins);
void setBaseEntity(IfaceProgramPoint ins,IfaceEntity e);
IfaceEntity.UserEntity getUserEntity(IfaceProgramPoint ins);
void setUserEntity(IfaceProgramPoint ins,IfaceEntity.UserEntity e);

IfaceValue getThisValue();
Iterable<IfaceValue> getParameterValues();

boolean addCall(List<IfaceValue> args);
boolean addException(IfaceValue exception);
boolean hasResult();
boolean addResult(IfaceValue v);

Collection<IfaceMethod> replaceWith(IfaceProgramPoint where,List<IfaceValue> args);
IfaceValue fixReplaceArgs(IfaceMethod fm,LinkedList<IfaceValue> args);

void addCallbacks(IfaceLocation loc,List<IfaceValue> args);
IfaceMethod findCallbackMethod(IfaceType cls,String mthd,int asz,boolean intf);

void noteCallSite(IfaceLocation loc);
Collection<IfaceLocation> getCallSites();
void noteMethodCalled(IfaceProgramPoint ins,IfaceMethod m,IfaceCall called);
IfaceCall getMethodCalled(IfaceProgramPoint ins,IfaceMethod m);
Collection<IfaceCall> getAllMethodsCalled(IfaceProgramPoint ins);

void addError(IfaceProgramPoint ins,IfaceError err);

void removeErrors(IfaceProgramPoint ins);
List<IfaceProgramPoint> getErrorLocations();
Collection<IfaceError> getErrors(IfaceProgramPoint pt);

void removeForUpdate(IfaceUpdater upd);
void handleUpdates(IfaceUpdater upd);

void backFlowParameter(IfaceValue ref,IfaceType settype);
void backFlowReturn(IfaceLocation pt,IfaceType settype);

String getLogName();

void noteScan(int fwd,int bwd);
void outputStatistics();



}	// end of interface IfaceCall




/* end of IfaceCall.java */

