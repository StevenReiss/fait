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

import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeField;
import edu.brown.cs.ivy.jcode.JcodeMethod;


public interface FaitControl extends FaitMaster {




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

File getDescriptionFile();



/********************************************************************************/
/*										*/
/*	source <-> binary interaction methods					*/
/*										*/
/********************************************************************************/

JcodeDataType findDataType(String cls);
JcodeDataType findClassType(String cls);
JcodeMethod findMethod(String cls,String method,String sign);

Iterable<JcodeMethod> findAllMethods(JcodeDataType typ,String method,String sign);
JcodeMethod findInheritedMethod(String cls,String name,String sign);
JcodeField findField(String cls,String fld);
Collection<JcodeMethod> getStartMethods();
List<JcodeMethod> findStaticInitializers(String cls);



// FaitInstruction findCall(JcodeMethod fm,int line,String rtn,int idx);
// FaitInstruction findNew(JcodeMethod fm,int line,String type,int idx);



/********************************************************************************/
/*										*/
/*	Entity manipulation methods						*/
/*										*/
/********************************************************************************/

IfaceEntity findAllocEntity(FaitLocation loc,JcodeDataType typ,boolean unique);
FaitEntity.UserEntity findUserEntity(String id,FaitLocation loc);
IfaceEntity findFixedEntity(JcodeDataType typ);
IfaceEntity findMutableEntity(JcodeDataType typ);
IfaceEntity findStringEntity(String s);
IfaceEntity findArrayEntity(JcodeDataType base,IfaceValue size);
IfaceEntity findPrototypeEntity(JcodeDataType base,IfacePrototype from,FaitLocation src);

IfaceEntity findLocalEntity(FaitLocation loc,JcodeDataType dt,boolean uniq);

// IfaceEntity findParameterEntity(JcodeMethod mthd,int idx);
// IfaceEntity findReturnEntity(JcodeMethod method);
// IfaceEntity findArrayEntity(JcodeDataType typ,FaitValue size);

IfaceEntitySet createEmptyEntitySet();
IfaceEntitySet createSingletonSet(FaitEntity fe);



/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

IfaceValue findAnyValue(JcodeDataType typ);
IfaceValue findRangeValue(JcodeDataType typ,long v0,long v1);
IfaceValue findObjectValue(JcodeDataType typ,IfaceEntitySet ss,NullFlags flags);
IfaceValue findEmptyValue(JcodeDataType typ,NullFlags flags);
IfaceValue findConstantStringValue();
IfaceValue findConstantStringValue(String v);
IfaceValue findMainArgsValue();
IfaceValue findNullValue();
IfaceValue findNullValue(JcodeDataType typ);
IfaceValue findBadValue();
IfaceValue findNativeValue(JcodeDataType typ);
IfaceValue findMutableValue(JcodeDataType typ);
IfaceValue findAnyObjectValue();
IfaceValue findAnyNewObjectValue();
IfaceValue findInitialFieldValue(JcodeField fld,boolean isnative);



/********************************************************************************/
/*										*/
/*	State methods								*/
/*										*/
/********************************************************************************/

IfaceState createState(int nlocal);
IfaceValue getFieldValue(IfaceState st,JcodeField fld,IfaceValue base,boolean thisref,FaitLocation src);
boolean setFieldValue(IfaceState st,JcodeField fld,IfaceValue v,IfaceValue base,boolean thisref,FaitLocation src);



/********************************************************************************/
/*										*/
/*	Prototype methods							*/
/*										*/
/********************************************************************************/

IfacePrototype createPrototype(JcodeDataType dt);
IfaceCall findPrototypeMethod(JcodeMethod fm);



/********************************************************************************/
/*										*/
/*	Call-related methods							*/
/*										*/
/********************************************************************************/

IfaceSpecial getCallSpecial(JcodeMethod fm);
FaitMethodData createMethodData(FaitCall fc);
IfaceCall findCall(JcodeMethod fm,List<IfaceValue> args,InlineType inline);
Collection<IfaceCall> getAllCalls(JcodeMethod fm);
Collection<IfaceCall> getAllCalls();



/********************************************************************************/
/*										*/
/*	Flow methods								*/
/*										*/
/********************************************************************************/

void queueLocation(FaitLocation loc);
void handleCallback(FaitLocation frm,JcodeMethod fm,List<IfaceValue> args,String cbid);

boolean isProjectClass(JcodeDataType t);
boolean isInProject(JcodeMethod m);


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
