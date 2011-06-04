/********************************************************************************/
/*										*/
/*		FaitControl.java						*/
/*										*/
/*	Flow Analysis Incremental Tool value definition 			*/
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

import java.io.File;
import java.util.*;


public interface FaitControl extends FaitConstants {




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setProject(FaitProject fp);
File getDescriptionFile();



/********************************************************************************/
/*										*/
/*	source <-> binary interaction methods					*/
/*										*/
/********************************************************************************/

FaitDataType findDataType(String cls);
FaitDataType findClassType(String cls);
FaitMethod findMethod(String cls,String method,String sign);
FaitMethod findInheritedMethod(String cls,String name,String sign);
FaitField findField(String cls,String fld);
Collection<FaitMethod> getStartMethods();
List<FaitMethod> findStaticInitializers(String cls);



// FaitInstruction findCall(FaitMethod fm,int line,String rtn,int idx);
// FaitInstruction findNew(FaitMethod fm,int line,String type,int idx);



/********************************************************************************/
/*										*/
/*	Entity manipulation methods						*/
/*										*/
/********************************************************************************/

IfaceEntity findAllocEntity(FaitLocation loc,FaitDataType typ,boolean unique);
FaitEntity.UserEntity findUserEntity(String id,FaitLocation loc);
IfaceEntity findFixedEntity(FaitDataType typ);
IfaceEntity findMutableEntity(FaitDataType typ);
IfaceEntity findStringEntity(String s);
IfaceEntity findArrayEntity(FaitDataType base,IfaceValue size);
IfaceEntity findPrototypeEntity(FaitDataType base,IfacePrototype from,FaitLocation src);

IfaceEntity findLocalEntity(FaitLocation loc,FaitDataType dt,boolean uniq);

// IfaceEntity findParameterEntity(FaitMethod mthd,int idx);
// IfaceEntity findReturnEntity(FaitMethod method);
// IfaceEntity findArrayEntity(FaitDataType typ,FaitValue size);

IfaceEntitySet createEmptyEntitySet();
IfaceEntitySet createSingletonSet(FaitEntity fe);



/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

IfaceValue findAnyValue(FaitDataType typ);
IfaceValue findRangeValue(FaitDataType typ,long v0,long v1);
IfaceValue findObjectValue(FaitDataType typ,IfaceEntitySet ss,NullFlags flags);
IfaceValue findEmptyValue(FaitDataType typ,NullFlags flags);
IfaceValue findConstantStringValue();
IfaceValue findConstantStringValue(String v);
IfaceValue findMainArgsValue();
IfaceValue findNullValue();
IfaceValue findNullValue(FaitDataType typ);
IfaceValue findBadValue();
IfaceValue findNativeValue(FaitDataType typ);
IfaceValue findMutableValue(FaitDataType typ);
IfaceValue findAnyObjectValue();
IfaceValue findAnyNewObjectValue();
IfaceValue findInitialFieldValue(FaitField fld,boolean isnative);



/********************************************************************************/
/*										*/
/*	State methods								*/
/*										*/
/********************************************************************************/

IfaceState createState(int nlocal);
IfaceValue getFieldValue(IfaceState st,FaitField fld,IfaceValue base,boolean thisref,FaitLocation src);
boolean setFieldValue(IfaceState st,FaitField fld,IfaceValue v,IfaceValue base,boolean thisref,FaitLocation src);



/********************************************************************************/
/*										*/
/*	Prototype methods							*/
/*										*/
/********************************************************************************/

IfacePrototype createPrototype(FaitDataType dt);
IfaceCall findPrototypeMethod(FaitMethod fm);



/********************************************************************************/
/*										*/
/*	Call-related methods							*/
/*										*/
/********************************************************************************/

IfaceSpecial getCallSpecial(FaitMethod fm);
FaitMethodData createMethodData(FaitCall fc);
IfaceCall findCall(FaitMethod fm,List<IfaceValue> args,InlineType inline);
Collection<IfaceCall> getAllCalls(FaitMethod fm);
Collection<IfaceCall> getAllCalls();



/********************************************************************************/
/*										*/
/*	Flow methods								*/
/*										*/
/********************************************************************************/

void analyze(int nthread);
void queueLocation(FaitLocation loc);



/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

class Factory {

   public static FaitControl getControl() {
      try {
	 Class<?> c = Class.forName("edu.brown.cs.fait.control.ControlMain");
	 Object o = c.newInstance();
	 return (FaitControl) o;
       }
      catch (Throwable t) {
	 System.err.println("FAIT: Can't create controller: " + t);
	 t.printStackTrace();
       }
      return null;
    }

}	// end if inner class Factory




}	// end of interface FaitControl




/* end of FaitControl.java */
