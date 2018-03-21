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




package edu.brown.cs.fait.control;

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.fait.entity.*;
import edu.brown.cs.fait.value.*;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.fait.state.*;
import edu.brown.cs.fait.type.TypeFactory;
import edu.brown.cs.fait.proto.*;
import edu.brown.cs.fait.call.*;
import edu.brown.cs.fait.flow.*;

import java.io.File;
import java.util.*;

import org.eclipse.jdt.core.dom.ASTNode;


public class ControlMain implements IfaceControl {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ControlByteCodeFactory bytecode_factory;
private ControlAstFactory ast_factory;
private EntityFactory	entity_factory;
private ValueFactory	value_factory;
private StateFactory	state_factory;
private ProtoFactory	proto_factory;
private CallFactory	call_factory;
private FlowFactory	flow_factory;
private IfaceProject	user_project;
private TypeFactory     type_factory;
private Map<String,IfaceType> basic_types;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ControlMain(IfaceProject ip)
{
   basic_types = new HashMap<>();
   
   bytecode_factory = new ControlByteCodeFactory(this,ip.getJcodeFactory());
   ast_factory = new ControlAstFactory(this,ip.getTyper());
   
   type_factory = new TypeFactory(this);
   entity_factory = new EntityFactory(this);
   value_factory = new ValueFactory(this);
   state_factory = new StateFactory(this);
   proto_factory = new ProtoFactory(this);
   call_factory = new CallFactory(this);
   flow_factory = new FlowFactory(this);
   
   user_project = ip;
   
   call_factory.addSpecialFile(getDescriptionFile());
   if (ip.getDescriptionFiles() != null) {
      for (File ff : ip.getDescriptionFiles()) {
         call_factory.addSpecialFile(ff);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Access methods 							*/
/*										*/
/********************************************************************************/

@Override public File getDescriptionFile()
{
   return new File("/research/people/spr/fait/lib/faitdata.xml");
}



/********************************************************************************/
/*										*/
/*	source <-> binary interaction methods					*/
/*										*/
/********************************************************************************/

IfaceBaseType findJavaType(String cls)
{
   return ast_factory.getType(cls);
}


@Override public IfaceType findDataType(String cls)
{
   IfaceType rt = basic_types.get(cls);
   if (rt != null) return rt;
   return findDataType(cls,(IfaceAnnotation []) null);
}


@Override public IfaceType findDataType(String cls,List<IfaceAnnotation> an)
{
   if (an == null) {
      IfaceType rt = basic_types.get(cls);
      if (rt == null) {
         rt = type_factory.createType(findJavaType(cls),an);
         basic_types.put(cls,rt);
       }
      return rt;
    }
   
   return type_factory.createType(findJavaType(cls),an);
}


@Override public IfaceType findDataType(String cls,IfaceAnnotation ... ans)
{
   return type_factory.createType(findJavaType(cls),ans);
}


@Override public IfaceType findConstantType(String cls,Object cnst)
{
   return type_factory.createConstantType(findJavaType(cls),cnst);
}


@Override public IfaceType findConstantType(IfaceType t,Object cnst)
{
   return type_factory.createConstantType(t.getJavaType(),cnst);
}








IfaceType findDataType(IfaceBaseType bt,List<IfaceAnnotation> ans)
{
   return type_factory.createType(bt,ans);
}


@Override public IfaceMethod findMethod(String cls,String method,String sign)
{
   IfaceBaseType ctyp = findJavaType(cls);
   if (ctyp.isEditable()) {
      return ast_factory.findMethod(ctyp,method,sign);
    }
   else {
      return bytecode_factory.findMethod(ctyp,method,sign);
    }
}

public List<IfaceMethod> findAllMethods(IfaceBaseType typ,String name)
{
   if (typ.isEditable()) {
      return ast_factory.findAllMethods(typ,name);
    }
   else {
      return bytecode_factory.findAllMethods(typ,name);
    }
}


@Override public List<IfaceMethod> findAllMethods(IfaceType typ,String name)
{
   return findAllMethods(typ.getJavaType(),name);
}


@Override public IfaceMethod findInheritedMethod(IfaceType cls,String nm,String sgn)
{
   IfaceMethod m = findMethod(cls.getName(),nm,sgn);
   if (m != null) return m;
   for (IfaceType ityp : cls.getInterfaces()) {
      m = findInheritedMethod(ityp,nm,sgn);
      if (m != null) return m;
    }
   return null;
}


@Override public IfaceField findField(IfaceType typ,String fld)
{
   if (typ.isEditable()) {
      return ast_factory.findField(typ.getJavaType(),fld);
    }
   else {
      int idx = fld.lastIndexOf(".");
      if (idx > 0) fld = fld.substring(idx+1);
      return bytecode_factory.findField(typ,fld);
    }
}



public Collection<IfaceMethod> getStartMethods()
{
   if (user_project == null) return null;
   Collection<IfaceMethod> rslt = new HashSet<>();
   
   JcompTyper typer = user_project.getTyper();
   for (JcompType jt : typer.getAllTypes()) {
      if (jt.isPrimitiveType()) continue;
      if (jt.isErrorType()) continue;
      if (jt.isAnnotationType()) continue;
      IfaceBaseType it = ast_factory.getType(jt);
      for (IfaceMethod im : findAllMethods(it,"main")) {
         checkStartMethod(im,rslt);
       }
      for (IfaceMethod im : findAllMethods(it,TESTER_NAME)) {
         checkStartMethod(im,rslt);
       }
    }
   
   return rslt;
}


private void checkStartMethod(IfaceMethod im,Collection<IfaceMethod> rslt)
{
   if (!im.isStatic()) return;
   if (im.isStaticInitializer()) return;
   if (im.isConstructor()) return;
   // might want additional checks, e.g. public, main args, ...
   
   rslt.add(im);
}



/********************************************************************************/
/*										*/
/*	Entity manipulation methods						*/
/*										*/
/********************************************************************************/

@Override public IfaceEntity.UserEntity findUserEntity(String id,IfaceLocation loc)
{
   return entity_factory.createUserEntity(id,loc);
}

@Override public IfaceEntity findFixedEntity(IfaceType typ)
{
   return entity_factory.createFixedEntity(typ);
}


@Override public IfaceEntity findMutableEntity(IfaceType typ)
{
   return entity_factory.createMutableEntity(typ);
}


@Override public IfaceEntity findStringEntity(String s)
{
   return entity_factory.createStringEntity(this,s);
}


@Override public IfaceEntity findArrayEntity(IfaceType base,IfaceValue size)
{
   return entity_factory.createArrayEntity(this,base,size);
}


@Override public IfaceEntity findPrototypeEntity(IfaceType base,
      IfacePrototype from,IfaceLocation src)
{
   return entity_factory.createPrototypeEntity(this,base,from,src);
}


@Override public IfaceEntity findLocalEntity(IfaceLocation loc,IfaceType dt)
{
   return entity_factory.createLocalEntity(loc,dt);
}


@Override public IfaceEntity findFunctionRefEntity(IfaceLocation loc,IfaceType dt,String method) 
{
   return entity_factory.createFunctionRefEntity(loc,dt,method);
}


@Override public IfaceEntity findFunctionRefEntity(IfaceLocation loc,IfaceType dt,
      Map<Object,IfaceValue> bindings) 
{
   return entity_factory.createFunctionRefEntity(loc,dt,bindings);
}




/********************************************************************************/
/*										*/
/*	Entity Set manimpulation methods					*/
/*										*/
/********************************************************************************/

@Override public IfaceEntitySet createEmptyEntitySet()
{
   return entity_factory.createEmptySet();
}


@Override public IfaceEntitySet createSingletonSet(IfaceEntity fe)
{
   return entity_factory.createSingletonSet(fe);
}


void updateEntitySets(IfaceUpdater upd)
{
   entity_factory.handleEntitySetUpdates(upd);
}



/********************************************************************************/
/*										*/
/*	State management methods						*/
/*										*/
/********************************************************************************/

@Override public IfaceState createState(int nlocal)
{
   return state_factory.createState(nlocal);
}



@Override public IfaceValue getFieldValue(IfaceState st,IfaceField fld,IfaceValue base,boolean thisref,
					     IfaceLocation src)
{
   return state_factory.getFieldValue(st,fld,base,thisref,src);
}



@Override public boolean setFieldValue(IfaceState st,IfaceField fld,IfaceValue v,
					  IfaceValue base,boolean thisref,IfaceLocation src)
{
   return state_factory.setFieldValue(st,fld,v,base,thisref,src);
}




/********************************************************************************/
/*										*/
/*	Prototype management methods						*/
/*										*/
/********************************************************************************/

@Override public IfacePrototype createPrototype(IfaceType typ)
{
   return proto_factory.createPrototype(typ);
}

@Override public IfaceCall findPrototypeMethod(IfaceProgramPoint pt,IfaceMethod fm)
{
   return call_factory.findPrototypeMethod(pt,fm);
}






/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceValue findAnyValue(IfaceType typ)
{
   return value_factory.anyValue(typ);
}


@Override public IfaceValue findRangeValue(IfaceType typ,long v0,long v1)
{
   return value_factory.rangeValue(typ,v0,v1);
}


@Override public IfaceValue findRangeValue(IfaceType typ,double v0,double v1)
{
   return value_factory.rangeValue(typ,v0,v1);
}


@Override public IfaceValue findObjectValue(IfaceType typ,IfaceEntitySet ss,IfaceAnnotation ... fgs)
{
   return value_factory.objectValue(typ,ss,fgs);
}


@Override public IfaceValue findEmptyValue(IfaceType typ,IfaceAnnotation ... fgs)
{
   return value_factory.emptyValue(typ,fgs);
}



@Override public IfaceValue findConstantStringValue()
{
   return value_factory.constantString();
}


@Override public IfaceValue findConstantStringValue(String v)
{
   return value_factory.constantString(v);
}


@Override public IfaceValue findMainArgsValue()
{
   return value_factory.mainArgs();
}



@Override public IfaceValue findNullValue()
{
   return value_factory.nullValue();
}


@Override public IfaceValue findNullValue(IfaceType typ)
{
   return value_factory.nullValue(typ);
}



@Override public IfaceValue findBadValue()
{
   return value_factory.badValue();
}



@Override public IfaceValue findNativeValue(IfaceType typ)
{
   return value_factory.nativeValue(typ);
}



@Override public IfaceValue findMutableValue(IfaceType typ)
{
   return value_factory.mutableValue(typ);
}


@Override public IfaceValue findAnyObjectValue()
{
   return value_factory.anyObject();
}



@Override public IfaceValue findAnyNewObjectValue()
{
   return value_factory.anyNewObject();
}


@Override public IfaceValue findInitialFieldValue(IfaceField fld,boolean isnative)
{
   return value_factory.initialFieldValue(fld,isnative);
}


@Override public IfaceValue findRefValue(IfaceType typ,IfaceValue base,IfaceField fld)
{
   return value_factory.refValue(typ,base,fld);
}


@Override public IfaceValue findRefValue(IfaceType typ,int slot)
{
   return value_factory.refValue(typ,slot);
}


@Override public IfaceValue findRefValue(IfaceType typ,IfaceValue base,IfaceValue idx)
{
   return value_factory.refValue(typ,base,idx);
}

@Override public IfaceValue findMarkerValue(IfaceProgramPoint pt,Object data)
{
   return value_factory.markerValue(pt,data);
}


void updateValues(IfaceUpdater upd)
{
   value_factory.handleUpdates(upd);
}



/********************************************************************************/
/*										*/
/*	Call-related methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceSpecial getCallSpecial(IfaceProgramPoint pt,IfaceMethod fm)
{
   return call_factory.getSpecial(pt,fm);
}

@Override public IfaceMethodData createMethodData(IfaceCall fc)
{
   if (user_project == null) return null;

   return user_project.createMethodData(fc);
}

@Override public IfaceCall findCall(IfaceProgramPoint pt,IfaceMethod fm,List<IfaceValue> args,InlineType inline)
{
   return call_factory.findCall(pt,fm,args,inline);
}


@Override public Collection<IfaceCall> getAllCalls(IfaceMethod fm)
{
   return call_factory.getAllCalls(fm);
}



@Override public Collection<IfaceCall> getAllCalls()
{
   return call_factory.getAllCalls();
}

@Override public void removeCalls(Collection<IfaceCall> calls)
{
   call_factory.removeCalls(calls);
}


@Override public IfaceAstReference getAstReference(ASTNode n)
{
   return ast_factory.getAstReference(n,null,null);
}

@Override public IfaceAstReference getAstReference(ASTNode n,ASTNode c)
{
   return ast_factory.getAstReference(n,c,null);
}

@Override public IfaceAstReference getAstReference(ASTNode n,IfaceAstStatus sts)
{
   return ast_factory.getAstReference(n,null,sts);
}


@Override public IfaceProgramPoint getProgramPoint(JcodeInstruction ins) 
{
   return bytecode_factory.getPoint(ins);
}




/********************************************************************************/
/*										*/
/*	Flow related methods							*/
/*										*/
/********************************************************************************/

@Override public void analyze(int nthread,boolean update)
{
   flow_factory.analyze(nthread,update);
}


@Override public void queueLocation(IfaceLocation loc)
{
   flow_factory.queueLocation(loc);
}


@Override public void queueLocation(IfaceCall ic,IfaceProgramPoint pt)
{
   flow_factory.queueMethodCall(ic,pt);
}


@Override public void handleCallback(IfaceLocation frm,IfaceMethod fm,List<IfaceValue> args,String cbid)
{
   flow_factory.handleCallback(frm,fm,args,cbid);
}


void updateQueuedStates(IfaceUpdater upd)
{
   flow_factory.handleStateUpdate(upd);
}



/********************************************************************************/
/*                                                                              */
/*      Project methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public boolean isProjectClass(IfaceType dt)
{
   if (user_project == null) return false;
   
   return user_project.isProjectClass(dt.getName());
}


boolean isProjectClass(IfaceBaseType dt)
{
   if (user_project == null) return false;
   
   return user_project.isProjectClass(dt.getName());
}


@Override public boolean isEditableClass(IfaceType dt)
{
   if (user_project == null) return false;
   
   return user_project.isEditableClass(dt.getName());
}


boolean isEditableClass(IfaceBaseType dt)
{
   if (user_project == null) return false;
   
   return user_project.isEditableClass(dt.getName());
}



@Override public boolean isInProject(IfaceMethod jm)
{
   return isProjectClass(jm.getDeclaringClass());
}



public IfaceType findCommonParent(IfaceType t1,IfaceType t2)
{
   return t1.getCommonParent(t2);
}


/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

List<IfaceMethod> findParentMethods(IfaceType cls,String nm,String desc,
      boolean check,boolean first,List<IfaceMethod> rslt)
{
   if (rslt == null) rslt = new ArrayList<>();
   
   if (first && !rslt.isEmpty()) return rslt;
   
   if (check) {
      IfaceMethod fm = findMethod(cls.getName(),nm,desc);
      if (fm != null) {
         rslt.add(fm);
         if (first) return rslt;
       }
    }
   
   if (nm.startsWith("<")) return rslt;
   
   IfaceType sc = cls.getSuperType();
   if (sc != null) findParentMethods(sc,nm,desc,true,first,rslt);
   for (IfaceType it : cls.getInterfaces()) {
      findParentMethods(it,nm,desc,true,first,rslt);
    }
   
   return rslt;
}



Collection<IfaceMethod> findChildMethods(IfaceType cls,String nm,String desc,
      boolean check,Collection<IfaceMethod> rslt)
{
   if (rslt == null) rslt = new HashSet<>();
   
   if (check) {
      IfaceMethod fm = findMethod(cls.getName(),nm,desc);
      if (fm != null) rslt.add(fm);
    }
   
   List<IfaceType> chld = cls.getChildTypes();
   if (chld != null) {
      for (IfaceType ct : chld) {
         findChildMethods(ct,nm,desc,true,rslt);
       }
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Special calls for lambda processing                                     */
/*                                                                              */
/********************************************************************************/

@Override public IfaceType createFunctionRefType(String typ)
{
   IfaceBaseType t1 = bytecode_factory.buildMethodType(typ);
   IfaceBaseType t2 = ast_factory.getFunctionRefType(t1);
   return findDataType(t2,null);
}


IfaceBaseType createMethodType(IfaceType rtn,List<IfaceType> args)
{
   IfaceBaseType bt = ast_factory.getMethodType(rtn,args);
   return bt;
}

/********************************************************************************/
/*                                                                              */
/*      Handle updates after a compilation                                      */
/*                                                                              */
/********************************************************************************/

@Override public void updateAll()
{
   ast_factory.updateAll(user_project.getTyper());
   bytecode_factory.updateAll();
   // entity_factory.updateAll();
   // value_factory.updateAll();
   // state_factory.updateAll();
   // proto_factory.updateAll();
   // call_factory.updateAll();
   // flow_factory.updateAll();
}



@Override public void doUpdate(IfaceUpdateSet updset)
{
   ControlUpdater upd = new ControlUpdater(this,updset); 
   upd.processUpdate();
   
   ast_factory.updateAll(user_project.getTyper());
   bytecode_factory.updateAll();
}



}	// end of class ControlMain




/* end of ControlMain.java */
