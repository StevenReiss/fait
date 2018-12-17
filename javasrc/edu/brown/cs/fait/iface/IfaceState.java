/********************************************************************************/
/*										*/
/*		IfaceState.java 						*/
/*										*/
/*	description of class							*/
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

public interface IfaceState extends FaitConstants
{

IfaceState cloneState();

void pushStack(IfaceValue v);
IfaceValue popStack();
void resetStack(IfaceState tostate);
void copyStackFrom(IfaceState fromstate);
boolean stackIsCategory2();
void handleDup(boolean dbl,int lvl);

IfaceValue getLocal(int idx);
IfaceValue getStack(int idx);
void setStack(int idx,IfaceValue v);
void setLocal(int idx,IfaceValue v);
boolean addToLocal(int idx,IfaceValue v);

IfaceValue getFieldValue(IfaceField fld);
void setFieldValue(IfaceField fnm,IfaceValue v);


Collection<IfaceField> getKnownFields();
boolean hasKnownFields();
void discardFields();




IfaceSafetyStatus getSafetyStatus();
boolean mergeSafetyStatus(IfaceSafetyStatus sts);
void setSafetyStatus(IfaceSafetyStatus sts);
void updateSafetyStatus(String event,IfaceControl ctrl);

IfaceState mergeWith(IfaceState st);


void startInitialization(IfaceType typ);

boolean testDoingInitialization(IfaceType typ);


void handleUpdate(IfaceUpdater upd);

void setLocation(IfaceLocation pt);
IfaceLocation getLocation();

int getNumPriorStates();
IfaceState getPriorState(int idx);

boolean isStartOfMethod();
boolean isMethodCall();







}	// end of interface IfaceState




/* end of IfaceState.java */

