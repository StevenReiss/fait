/********************************************************************************/
/*										*/
/*		IfaceUpdater.java						*/
/*										*/
/*	Internal access to update information					*/
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



public interface IfaceUpdater extends FaitConstants
{

IfaceValue getNewValue(IfaceValue v);
IfaceEntitySet getNewEntitySet(IfaceEntitySet s);

void addEntityToRemove(IfaceEntity ie);
Collection<IfaceEntity> getEntitiesToRemove();
void addToEntitySetMap(IfaceEntitySet oset,IfaceEntitySet nset);
void addToValueMap(IfaceValue oval,IfaceValue nval);

boolean shouldUpdate(IfaceCall ic);
void removeCall(IfaceCall ic);

boolean isLocationRemoved(IfaceLocation loc);
boolean isTypeRemoved(IfaceType typ);
boolean isTypeRemoved(IfaceBaseType typ);
boolean isCallRemoved(IfaceCall ic);

Collection<IfaceType> getTypesRemoved();

void addToWorkQueue(IfaceCall ic,IfaceProgramPoint pt);
   
}	// end of interface IfaceUpdater




/* end of IfaceUpdater.java */

