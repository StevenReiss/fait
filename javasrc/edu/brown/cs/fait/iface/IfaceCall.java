/********************************************************************************/
/*                                                                              */
/*              IfaceCall.java                                                  */
/*                                                                              */
/*      Representation of a called method                                       */
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

import java.util.*;

public interface IfaceCall extends FaitCall
{

FaitMethod getMethod();
FaitDataType getMethodClass();
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

FaitValue getAssociation(AssociationType typ,FaitInstruction ins);
void setAssociation(AssociationType typ,FaitInstruction ins,IfaceValue v);

IfaceEntity getArrayEntity(FaitInstruction ins);
void setArrayEntity(FaitInstruction ins,IfaceEntity e);
IfaceEntity getBaseEntity(FaitInstruction ins);
void setBaseEntity(FaitInstruction ins,IfaceEntity e);
FaitEntity.UserEntity getUserEntity(FaitInstruction ins);
void setUserEntity(FaitInstruction ins,FaitEntity.UserEntity e);

IfaceValue getThisValue();
Iterable<IfaceValue> getParameterValues();

boolean addCall(List<IfaceValue> args);
boolean addException(IfaceValue exception);
boolean hasResult();
boolean addResult(IfaceValue v);

Collection<FaitMethod>  replaceWith(List<IfaceValue> args);
IfaceValue fixReplaceArgs(FaitMethod fm,LinkedList<IfaceValue> args);

void addCallbacks(List<IfaceValue> args);
FaitMethod findCallbackMethod(FaitDataType cls,String mthd,int asz,boolean intf);

void noteCallSite(FaitLocation loc);
Collection<FaitLocation> getCallSites();
void noteMethodCalled(FaitInstruction ins,FaitMethod m,IfaceCall called);
IfaceCall getMethodCalled(FaitInstruction ins,FaitMethod m);
Collection<IfaceCall> getAllMethodsCalled(FaitInstruction ins);

void clearForUpdate(IfaceUpdater upd);
void handleUpdates(IfaceUpdater upd);

}       // end of interface IfaceCall




/* end of IfaceCall.java */

