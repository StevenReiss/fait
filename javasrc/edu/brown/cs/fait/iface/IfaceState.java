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



public interface IfaceState extends FaitState
{

IfaceState cloneState();

void pushStack(IfaceValue v);
IfaceValue popStack();
void resetStack(IfaceState tostate);
boolean stackIsCategory2();
void handleDup(boolean dbl,int lvl);

IfaceValue getLocal(int idx);
void setLocal(int idx,IfaceValue v);
boolean addToLocal(int idx,IfaceValue v);

IfaceValue getFieldValue(FaitField fld);
void setFieldValue(FaitField fld,IfaceValue v);

Iterable<FaitField> getKnownFields();
boolean hasKnownFields();
void discardFields();

void pushReturn(FaitInstruction ins);
FaitInstruction popReturn();

IfaceState mergeWith(IfaceState st);
boolean compatibleWith(IfaceState st);

void startInitialization(FaitDataType typ);

boolean testDoingInitialization(FaitDataType typ);
boolean addInitializations(IfaceState st);

void handleUpdate(IfaceUpdater upd);


}	// end of interface IfaceState




/* end of IfaceState.java */

