/********************************************************************************/
/*										*/
/*		IfaceEntity.java						*/
/*										*/
/*	Flow Analysis Incremental Tool source point internal definition 	*/
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

import java.util.List;
import java.util.Map;

public interface IfaceEntity extends FaitConstants {

int getId();

IfaceType getDataType();

IfaceLocation getLocation();

boolean isUsedInLock();





/********************************************************************************/
/*										*/
/*	User sources								*/
/*										*/
/********************************************************************************/

UserEntity getUserEntity();

boolean isNative();

boolean isMutable();

boolean isUserEntity();


interface UserEntity {
   
   String getEntityType();
   
}




void setFieldContents(IfaceValue fv,String key);
void setFieldContents(IfaceValue fv,IfaceField fld);
boolean addToFieldContents(IfaceValue fv,String key);
boolean addToFieldContents(IfaceValue fv,IfaceField fld);
IfaceValue getFieldValue(String key);
IfaceValue getFieldValue(IfaceField fld);


void setArrayContents(IfaceValue fv);
boolean replaceArrayContents(IfaceValue fv,IfaceLocation loc);
boolean addToArrayContents(IfaceValue fv,IfaceValue idx,IfaceLocation loc);
IfaceValue getArrayValue(IfaceValue idx,IfaceControl ctl);
boolean setArraySize(IfaceValue sz);

void handleUpdates(IfaceUpdater upd);

IfacePrototype getPrototype();

Map<Object,IfaceValue> getBindings();
String getMethodName();
boolean isFunctionRef();

List<IfaceValue> getContents(List<IfaceValue> rslt);



}	// end of interface IfaceEntity



/* end of IfaceEntity.java */

