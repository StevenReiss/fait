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
import edu.brown.cs.ivy.jcode.JcodeClass;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.fait.state.*;
import edu.brown.cs.fait.testgen.TestgenFactory;
import edu.brown.cs.fait.type.TypeFactory;
import edu.brown.cs.fait.proto.*;
import edu.brown.cs.fait.query.QueryFactory;
import edu.brown.cs.fait.safety.SafetyFactory;
import edu.brown.cs.fait.call.*;
import edu.brown.cs.fait.flow.*;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

import org.eclipse.jdt.core.dom.ASTNode;
import org.w3c.dom.Element;


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
private SafetyFactory   safety_factory;
private QueryFactory    query_factory;
private TestgenFactory  testgen_factory;
private Map<String,IfaceType> basic_types;
private Map<File,Long>  fait_files;



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
   safety_factory = new SafetyFactory(this);
   query_factory = new QueryFactory(this);
   testgen_factory = new TestgenFactory(this);
   
   user_project = ip;
   
   fait_files = new LinkedHashMap<>();
   SortedSet<IfaceDescriptionFile> files = new TreeSet<>();
   List<File> sysfiles = getSystemDescriptionFiles();
   if (sysfiles != null) {
      for (File f : sysfiles) {
         files.add(new ControlDescriptionFile(f,IfaceDescriptionFile.PRIORITY_BASE));
       }
    }
   if (ip.getDescriptionFiles() != null) {
      for (IfaceDescriptionFile ff : ip.getDescriptionFiles()) {
         files.add(ff);
       }
    }
   for (IfaceDescriptionFile f : files) {
      addSpecialFile(f.getFile());
    }
   
   flow_factory = new FlowFactory(this);
}




/********************************************************************************/
/*										*/
/*	Description File methods                				*/
/*										*/
/********************************************************************************/

@Override public List<File> getSystemDescriptionFiles()
{
   List<File> rslt = new ArrayList<>();
   File base = new File("*FAIT*");
   
   File f1 = new File(base,"faitdata.xml");
   rslt.add(f1);
   File f2 = new File(base,"faitsecurity.xml");
   rslt.add(f2);
   
   return rslt;
}


void addSpecialFile(File f)
{
   Element e = null;
   if (f.exists() && f.canRead()) {
      FaitLog.logI("Adding special file " + f);
      fait_files.put(f,f.lastModified());
      e = IvyXml.loadXmlFromFile(f);
    }
   else if (f.getPath().startsWith("*FAIT*")) {
      String name = "/" + f.getName();
      InputStream ins = this.getClass().getResourceAsStream(name);
      if (ins != null) {
         URL nm = this.getClass().getResource(name);
         FaitLog.logI("Loading system resource " + nm);
         e = IvyXml.loadXmlFromStream(ins);
       }
      else {
         File f1 = new File("/research/people/spr/fait/lib",name);
         if (!f1.exists()) f1 = new File("/pro/fait/lib",name);
         if (f1.exists() && f1.canRead()) {
            e = IvyXml.loadXmlFromFile(f1);
          }
       }
    }
   
   if (e != null) {
      type_factory.addSpecialFile(e);
      call_factory.addSpecialFile(e);
      safety_factory.addSpecialFile(e);
    }
   else {
      FaitLog.logE("Bad XML in special file " + f);
    }
}


boolean checkSpecialFiles()
{
   boolean chng = false;
   for (Map.Entry<File,Long> ent : fait_files.entrySet()) {
      File f1 = ent.getKey();
      if (f1.exists() && f1.canRead()) {
         if (f1.lastModified() > ent.getValue()) chng = true;
       }
      else chng = true;
    }
   
   return chng;
}



void reloadSpecialFiles()
{
   call_factory.clearAllSpecials();
   safety_factory.clearAllSpecials();
   type_factory.clearAllSpecials();
   fait_files.clear();
   SortedSet<IfaceDescriptionFile> files = new TreeSet<>();
   List<File> sysfiles = getSystemDescriptionFiles();
   if (sysfiles != null && !sysfiles.isEmpty()) {
      for (File f : sysfiles) {
         files.add(new ControlDescriptionFile(f,IfaceDescriptionFile.PRIORITY_BASE));
       }
    }
   if (user_project.getDescriptionFiles() != null) {
      for (IfaceDescriptionFile ff : user_project.getDescriptionFiles()) {
         files.add(ff);
       }
    }
   for (IfaceDescriptionFile f : files) {
      addSpecialFile(f.getFile());
    }
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
   if (t == null) return null;
   
   return type_factory.createConstantType(t.getJavaType(),cnst);
}



IfaceType findDataType(IfaceBaseType bt,List<IfaceAnnotation> ans)
{
   return type_factory.createType(bt,ans);
}


public IfaceType findCommonParent(IfaceType t1,IfaceType t2)
{
   return t1.getCommonParent(t2);
}



@Override public IfaceMethod findMethod(String cls,String method,String sign)
{
   IfaceBaseType ctyp = findJavaType(cls);
   if (ctyp == null) return null;
   else if (ctyp.isEditable()) {
      return ast_factory.findMethod(ctyp,method,sign);
    }
   else {
      return bytecode_factory.findMethod(ctyp,method,sign);
    }
}


@Override public IfaceMethod findMethod(JcompSymbol js) 
{
   return ast_factory.getMethod(js);
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
   
   JcodeFactory jf = user_project.getJcodeFactory();
   for (JcodeClass jc : jf.getAllPossibleClasses(new ProjectFilter())) { 
      String nm = jc.getName();
      nm = nm.replace("$",".");
      findJavaType(nm);
    }
   Collection<IfaceMethod> rslt = new HashSet<>();
   
   JcompTyper typer = user_project.getTyper();
   for (JcompType jt : typer.getAllTypes()) {
      if (!user_project.isProjectClass(jt.getName())) continue;
      if (jt.isPrimitiveType()) continue;
      if (jt.isErrorType()) continue;
      if (jt.isAnnotationType()) continue;
      if (jt.isMethodType()) continue;
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



private class ProjectFilter implements Predicate<String> {

   @Override public boolean test(String t) {
      return user_project.isProjectClass(t);
    }
   
}       // end of inner class ProjectFilter


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
      IfacePrototype from,IfaceLocation src,boolean mutable)
{
   return entity_factory.createPrototypeEntity(base,from,src,mutable);
}


@Override public IfaceEntity findLocalEntity(IfaceLocation loc,IfaceType dt,IfacePrototype ptyp)
{
   return entity_factory.createLocalEntity(loc,dt,ptyp);
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



@Override public IfaceEntity findEntityById(int id)
{
   return entity_factory.getEntity(id);
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

@Override public IfaceState createState(int nlocal,IfaceSafetyStatus sts)
{
   return state_factory.createState(nlocal,sts);
}



@Override public IfaceValue getFieldValue(IfaceState st,IfaceField fld,IfaceValue base,boolean thisref)
{
   return flow_factory.getFieldValue(st,fld,base,thisref);
}


@Override public boolean canClassBeUsed(IfaceType dt)
{
   return flow_factory.canClassBeUsed(dt);
}



@Override public boolean setFieldValue(IfaceState st,IfaceField fld,IfaceValue v,
					  IfaceValue base,boolean thisref,IfaceLocation src)
{
   return state_factory.setFieldValue(st,fld,v,base,thisref,src);
}


@Override public IfaceState findStateForLocation(IfaceLocation loc)
{
   return findStateForLocation(loc.getCall(),loc.getProgramPoint());
}


@Override public IfaceState findStateForLocation(IfaceCall c,IfaceProgramPoint pt)
{
   return flow_factory.findStateForLocation(c,pt);
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
   if (typ == null) return value_factory.anyObject();
   return value_factory.anyValue(typ);
}


@Override public IfaceValue findConstantValue(IfaceType typ,long v)
{
   return findRangeValue(typ,(Long) v,(Long) v);
}


@Override public IfaceValue findConstantValue(IfaceType typ,double v)
{
   return findRangeValue(typ,v,v);
}


@Override public IfaceValue findConstantValue(boolean v)
{
   IfaceType bt = findDataType("boolean");
   if (v) return findConstantValue(bt,1);
   else return findConstantValue(bt,0);
}

@Override public IfaceValue findRangeValue(IfaceType typ,Long v0,Long v1)
{
   return value_factory.rangeValue(typ,v0,v1);
}


@Override public IfaceValue findRangeValue(IfaceType typ,double v0,double v1)
{
   return value_factory.floatRangeValue(typ,v0,v1);
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

@Override public IfaceValue findRefStackValue(IfaceType typ,int slot)
{
   return value_factory.refValue(typ,-slot-2);
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


void updateStates(IfaceUpdater upd)
{
   state_factory.handleUpdates(upd);
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


@Override public boolean isSingleAllocation(IfaceType typ,boolean fromast)
{ 
   return call_factory.isSingleAllocation(typ,fromast);
}


@Override public void clearCallSpecial(IfaceMethod fm)
{
   call_factory.clearSpecial(fm);
}



@Override public IfaceCall findCall(IfaceProgramPoint pt,IfaceMethod fm,List<IfaceValue> args,
      IfaceSafetyStatus sts,InlineType inline)
{
   return call_factory.findCall(pt,fm,args,sts,inline);
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


@Override public Collection<String> getDefaultClasses()
{
   return call_factory.getDefaultClasses();
}

/********************************************************************************/
/*                                                                              */
/*      Location methods                                                        */
/*                                                                              */
/********************************************************************************/


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

@Override public IfaceAnnotation [] getAnnotations(IfaceProgramPoint pt)
{
   IfaceAstReference ar = pt.getAstReference();
   if (ar != null) {
      return ast_factory.getAnnotations(ar.getAstNode());
    }
   return bytecode_factory.getAnnotations(pt.getInstruction());
}




/********************************************************************************/
/*										*/
/*	Flow related methods							*/
/*										*/
/********************************************************************************/

@Override public void analyze(int nthread,boolean update,ReportOption reportopt)
{
   flow_factory.analyze(nthread,update,reportopt);
}


@Override public void analyze(IfaceMethod im,int nth,ReportOption rptopt)
{
   flow_factory.analyze(im,nth,rptopt);
}


@Override public void queueLocation(IfaceLocation loc)
{
   flow_factory.queueLocation(loc);
}


@Override public void queueLocation(IfaceCall ic,IfaceProgramPoint pt)
{
   flow_factory.queueMethodCall(ic,pt);
}


@Override public void initialize(IfaceType typ)
{
   flow_factory.initialize(typ);
}


@Override public void handleCallback(IfaceLocation frm,IfaceMethod fm,List<IfaceValue> args,String cbid)
{
   flow_factory.handleCallback(frm,fm,args,cbid);
}


void updateFlow(IfaceUpdater upd)
{
   flow_factory.handleUpdate(upd);
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
   
   String name = dt.getName();
   
   if (user_project.isEditableClass(name)) return true;
   
   int idx = name.indexOf(".$");;
   if (idx > 0) {
      String onm = name.substring(0,idx);
      return user_project.isEditableClass(onm);
    }
   
   return false;
}



@Override public boolean isInProject(IfaceMethod jm)
{
   return isProjectClass(jm.getDeclaringClass());
}


@Override public String getSourceFile(IfaceType c)
{
   if (user_project == null) return null;
   return user_project.getSourceFileForClass(c.getName());
}


@Override public String getSourceFile(IfaceMethod im) 
{
   return getSourceFile(im.getDeclaringClass());
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

@Override public IfaceType createFunctionRefType(String typ,String nstype)
{
   IfaceBaseType t1 = bytecode_factory.buildMethodType(typ);
   IfaceBaseType tns = null;
   if (nstype != null) {
      tns = bytecode_factory.buildMethodType(nstype);
    }
   IfaceBaseType t2 = ast_factory.getFunctionRefType(t1,tns);
   return findDataType(t2,null);
}


IfaceBaseType createMethodType(IfaceType rtn,List<IfaceType> args)
{
   IfaceBaseType bt = ast_factory.getMethodType(rtn,args);
   return bt;
}



/********************************************************************************/
/*                                                                              */
/*      Safety methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceSafetyStatus getInitialSafetyStatus()
{
   return safety_factory.getInitialStatus();
}


@Override public List<IfaceSafetyCheck> getAllSafetyChecks()
{
   return safety_factory.getAllSafetyChecks();
}


@Override public List<IfaceSubtype> getAllSubtypes()
{
   return type_factory.getAllSubtypes();
}


@Override public String getEventForCall(IfaceMethod fm,List<IfaceValue> args,IfaceLocation loc)
{
   if (fm.getDeclaringClass().getName().equals("edu.brown.cs.karma.KarmaUtils")) {
      if (fm.getName().equals("KarmaEvent")) {
         if (args == null) return "ANY";
	 IfaceValue v0 = args.get(0);
	 String sv = v0.getStringValue();
	 if (sv != null) return sv;
       }
    }
   
   return null;
}

/********************************************************************************/
/*                                                                              */
/*      Query methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void processErrorQuery(IfaceCall c,IfaceProgramPoint pt,IfaceError e,IvyXmlWriter xw)
{
   query_factory.processErrorQuery(c,pt,e,xw);
}


@Override public void processToQuery(IfaceCall c,IfaceProgramPoint pt,IfaceEntity ent,
      IfaceSubtype styp,IfaceSubtype.Value sval,IfaceValue refval,IvyXmlWriter xw) 
{
   query_factory.processToQuery(c,pt,ent,styp,sval,refval,xw);
}

@Override public void processFlowQuery(IfaceCall c,IfaceProgramPoint pt,IfaceValue refval,
      IfaceValue val,List<IfaceMethod> stack,IvyXmlWriter xw)
{
   query_factory.processFlowQuery(c,pt,refval,val,stack,xw);
}





@Override public void processVarQuery(String method,int line,int pos,String var,IvyXmlWriter xw)
        throws FaitException
{
   query_factory.processVarQuery(method,line,pos,var,xw);
}


@Override public void processReflectionQuery(IvyXmlWriter xw)
{
   query_factory.processReflectionQuery(this,xw);
}


@Override public void processCriticalQuery(String ignores,IvyXmlWriter xw)
{
   query_factory.processCriticalQuery(this,ignores,xw);
}



/********************************************************************************/
/*                                                                              */
/*      Test case generation                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void generateTestCase(Element path,IvyXmlWriter xw) throws FaitException
{
   testgen_factory.generateTestCase(path,xw);
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



/********************************************************************************/
/*                                                                              */
/*      Back flow methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public IfaceBackFlow getBackFlow(IfaceState backfrom,IfaceState backto,IfaceValue endref,boolean conds)
{
   ControlBackFlow cbf = new ControlBackFlow(this,backfrom,backto,endref);
   cbf.computeBackFlow(conds);
   return cbf;
}


@Override public IfaceBackFlow getBackFlow(IfaceState from,IfaceState to,
      Collection<IfaceAuxReference> refs)
{
   return new ControlBackFlow(this,from,to,refs);
}



@Override public IfaceAuxReference getAuxReference(IfaceLocation loc,IfaceValue ref)
{
   return QueryFactory.getAuxReference(loc,ref);
}



@Override public Collection<IfaceAuxReference> getAuxRefs(IfaceField fld)
{
   return flow_factory.getAuxRefs(fld);
}


@Override public Collection<IfaceAuxReference> getAuxArrayRefs(IfaceValue arr)
{
   return flow_factory.getAuxArrayRefs(arr);
}
/********************************************************************************/
/*                                                                              */
/*      Handle clean up                                                         */
/*                                                                              */
/********************************************************************************/

@Override public void clearAll()
{
   entity_factory.clearAll();
}


}	// end of class ControlMain




/* end of ControlMain.java */
