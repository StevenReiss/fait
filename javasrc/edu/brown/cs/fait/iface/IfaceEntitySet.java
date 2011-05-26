/********************************************************************************/
/*										*/
/*		IfaceEntitySet.java						*/
/*										*/
/*	Flow Analysis Incremental Tool source point internal definition 	*/
/*										*/
/*	Entity sets are unique and immutable.  Two entity sets can be		*/
/*	compared for equality using == rather than equals.			*/
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




public interface IfaceEntitySet extends FaitConstants {



boolean contains(FaitEntity s);
boolean contains(IfaceEntitySet es);
boolean isEmpty();
int size();
boolean overlaps(IfaceEntitySet s);

Iterable<IfaceEntity> getEntities();

IfaceEntitySet restrictByType(FaitDataType dt,boolean proj,FaitLocation loc);
IfaceEntitySet removeByType(FaitDataType dt,FaitLocation loc);

IfaceEntitySet addToSet(IfaceEntitySet es);

IfaceEntitySet getModelSet();



}	// end of interface IfaceEntitySet



/* end of IfaceEntitySet.java */

