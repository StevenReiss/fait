/********************************************************************************/
/*										*/
/*		IfaceControl.java						*/
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

import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import org.eclipse.jdt.core.dom.ASTNode;
import java.lang.reflect.Constructor;
import java.io.File;



public interface IfaceControl extends FaitConstants {




/********************************************************************************/
/*										*/
/*	Worker methods								*/
/*										*/
/********************************************************************************/



/**
 *	Do the analysis
 **/

void analyze(int nthread,boolean update,ReportOption opt);
void analyze(IfaceMethod im,int nth,ReportOption opt);




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

List<File> getSystemDescriptionFiles();



/********************************************************************************/
/*										*/
/*	source <-> binary interaction methods					*/
/*										*/
/********************************************************************************/

IfaceType findDataType(String cls,List<IfaceAnnotation> annots);
IfaceType findDataType(String cls,IfaceAnnotation... annots);
IfaceType findDataType(String cls);

IfaceType findConstantType(String cls,Object cnst);
IfaceType findConstantType(IfaceType t,Object cnst);

List<IfaceSubtype> getAllSubtypes();


IfaceMethod findMethod(String cls,String method,String desc);
IfaceMethod findMethod(JcompSymbol js);


IfaceMethod findInheritedMethod(IfaceType cls,String name,String desc,String sgn);

IfaceField findField(IfaceType cls,String fld);
Collection<IfaceMethod> getStartMethods();







/********************************************************************************/
/*										*/
/*	Entity manipulation methods						*/
/*										*/
/********************************************************************************/

IfaceEntity.UserEntity findUserEntity(String id,IfaceLocation loc);
IfaceEntity findFixedEntity(IfaceType typ);
IfaceEntity findMutableEntity(IfaceType typ);
IfaceEntity findStringEntity(String s);
IfaceEntity findArrayEntity(IfaceType base,IfaceValue size);
IfaceEntity findPrototypeEntity(IfaceType base,IfacePrototype from,IfaceLocation src,boolean mutable);
IfaceEntity findFunctionRefEntity(IfaceLocation loc,IfaceType typ,String method);
IfaceEntity findFunctionRefEntity(IfaceLocation loc,IfaceType typ,IfaceMethod mthd,
      Map<Object,IfaceValue> bindings);
IfaceEntity findEntityById(int id);
IfaceEntity findLocalEntity(IfaceLocation loc,IfaceType dt,IfacePrototype ptyp);


IfaceEntitySet createEmptyEntitySet();
IfaceEntitySet createSingletonSet(IfaceEntity fe);



/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

IfaceValue findAnyValue(IfaceType typ);
IfaceValue findRangeValue(IfaceType typ,Long v0,Long v1);

default IfaceValue findRangeValue(IfaceType typ,long v0,long v1)
{
   return findRangeValue(typ,Long.valueOf(v0),Long.valueOf(v1));
}

IfaceValue findConstantValue(IfaceType typ,long v);
IfaceValue findConstantValue(boolean v);
IfaceValue findConstantValue(IfaceType typ,double v);
IfaceValue findRangeValue(IfaceType typ,double v0,double v1);
IfaceValue findObjectValue(IfaceType typ,IfaceEntitySet ss,IfaceAnnotation...	flags);
IfaceValue findEmptyValue(IfaceType typ,IfaceAnnotation... flags);
IfaceValue findConstantStringValue();
IfaceValue findConstantStringValue(String v);
IfaceValue findMainArgsValue();
IfaceValue findNullValue();
IfaceValue findNullValue(IfaceType typ);
IfaceValue findBadValue();
IfaceValue findMarkerValue(IfaceProgramPoint pt,Object data);
IfaceValue findNativeValue(IfaceType typ);
IfaceValue findMutableValue(IfaceType typ);
IfaceValue findAnyObjectValue();
IfaceValue findAnyNewObjectValue();
IfaceValue findRefValue(IfaceType dt,IfaceValue base,IfaceField fld);
IfaceValue findRefValue(IfaceType dt,int slot);
IfaceValue findRefStackValue(IfaceType dt,int slot);
IfaceValue findRefValue(IfaceType dt,IfaceValue base,IfaceValue idx);

IfaceValue findInitialFieldValue(IfaceField fld,boolean nat);

IfaceAstReference getAstReference(ASTNode n);
IfaceAstReference getAstReference(ASTNode n,ASTNode c);
IfaceAstReference getAstReference(ASTNode n,IfaceAstStatus sts);
IfaceProgramPoint getProgramPoint(JcodeInstruction ins);
IfaceAnnotation [] getAnnotations(IfaceProgramPoint pt);




/********************************************************************************/
/*										*/
/*	State methods								*/
/*										*/
/********************************************************************************/

IfaceState createState(int nlocal,IfaceSafetyStatus sts);
IfaceValue getFieldValue(IfaceState st,IfaceField fld,IfaceValue base,boolean thisref);
boolean canClassBeUsed(IfaceType dt);
boolean setFieldValue(IfaceState st,IfaceField fld,IfaceValue v,IfaceValue base,boolean thisref,IfaceLocation src);
IfaceState findStateForLocation(IfaceCall c,IfaceProgramPoint pt);
IfaceState findStateForLocation(IfaceLocation loc);



/********************************************************************************/
/*										*/
/*	Prototype methods							*/
/*										*/
/********************************************************************************/

IfacePrototype createPrototype(IfaceType dt);
IfaceCall findPrototypeMethod(IfaceProgramPoint pt,IfaceMethod fm);



/********************************************************************************/
/*										*/
/*	Call-related methods							*/
/*										*/
/********************************************************************************/

IfaceSpecial getCallSpecial(IfaceProgramPoint pt,IfaceMethod fm);
void clearCallSpecial(IfaceMethod fm);
boolean isSingleAllocation(IfaceType typ,boolean fromast);

IfaceCall findCall(IfaceProgramPoint pt,IfaceMethod fm,List<IfaceValue> args,
      IfaceSafetyStatus sts,InlineType inline);
Collection<IfaceCall> getAllCalls(IfaceMethod fm);
Collection<IfaceCall> getAllCalls();



/********************************************************************************/
/*										*/
/*	Flow methods								*/
/*										*/
/********************************************************************************/

void queueLocation(IfaceLocation loc);
void queueLocation(IfaceCall ic,IfaceProgramPoint pt);
void initialize(IfaceType typ);
void handleCallback(IfaceLocation frm,IfaceMethod fm,List<IfaceValue> args,String cbid);

boolean isProjectClass(IfaceType t);
boolean isInProject(IfaceMethod m);
String getSourceFile(IfaceType c);
String getSourceFile(IfaceMethod m);
boolean isEditableClass(IfaceType t);




IfaceType findCommonParent(IfaceType t1,IfaceType t2);

List<IfaceMethod> findAllMethods(IfaceType cls,String name);

IfaceType createFunctionRefType(String typ,String nstype);
IfaceType createMethodCallType(List<IfaceType> args);

IfaceSafetyStatus getInitialSafetyStatus();
List<IfaceSafetyCheck> getAllSafetyChecks();
String getEventForCall(IfaceMethod fm,List<IfaceValue> args,IfaceLocation from);

void updateAll();
void doUpdate(IfaceUpdateSet what);
void removeCalls(Collection<IfaceCall> call);
Collection<String> getDefaultClasses();

void clearAll();

IfaceBackFlow getBackFlow(IfaceState backfrom,IfaceState backto,IfaceValue endref,boolean conds);
IfaceBackFlow getBackFlow(IfaceState from,IfaceState to,Collection<IfaceAuxReference> refs);
IfaceAuxReference getAuxReference(IfaceLocation loc,IfaceValue ref,IfaceAuxRefType typ);
Collection<IfaceAuxReference> getAuxRefs(IfaceField fld);
Collection<IfaceAuxReference> getAuxArrayRefs(IfaceValue arr);

void processErrorQuery(IfaceCall c,IfaceProgramPoint pt,IfaceError e,IvyXmlWriter xw);
void processVarQuery(String method,int line,int pos,String var,IvyXmlWriter xw) throws FaitException;
void processToQuery(IfaceCall c,IfaceProgramPoint pt,IfaceEntity ent,
      IfaceSubtype styp,IfaceSubtype.Value sval,IfaceValue refval,IvyXmlWriter xw);
void processFlowQuery(IfaceCall c,IfaceProgramPoint pt,IfaceValue refval,
      IfaceValue val,List<IfaceMethod> stack,int depth,int conddepth,
      boolean location,IvyXmlWriter xw);

void processChangeQuery(IfaceCall c,IfaceProgramPoint pt,IvyXmlWriter xw);

void processReflectionQuery(IvyXmlWriter xw);
void processCriticalQuery(String ignores,IvyXmlWriter xw);

void generateTestCase(Element path,IvyXmlWriter xw) throws FaitException;



/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

class Factory {

   public static IfaceControl createControl(IfaceProject proj) {
      try {
         Class<?> c = Class.forName("edu.brown.cs.fait.control.ControlMain");
         Constructor<?> cnst = c.getConstructor(IfaceProject.class);
         Object o = cnst.newInstance(proj);
         return (IfaceControl) o;
       }
      catch (Throwable t) {
         System.err.println("FAIT: Can't create controller: " + t);
         t.printStackTrace();
       }
      return null;
    }

   public static IfaceProject createSimpleProject(String cp,String pfx) {
      try {
	 Class<?> c = Class.forName("edu.brown.cs.fait.control.ControlSimpleProject");
	 Constructor<?> cnst = c.getConstructor(String.class,String.class);
	 Object o = cnst.newInstance(cp,pfx);
	 return (IfaceProject) o;
       }
      catch (Throwable t) {
	 System.err.println("FAIT: Can't create simple project " + t);
	 t.printStackTrace();
       }
      return null;
    }

}	// end if inner class Factory
























}	// end of interface IfaceControl




/* end of IfaceControl.java */
