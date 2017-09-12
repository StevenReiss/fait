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
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcode.JcodeField;
import edu.brown.cs.ivy.jcode.JcodeMethod;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.fait.state.*;
import edu.brown.cs.fait.proto.*;
import edu.brown.cs.fait.call.*;
import edu.brown.cs.fait.flow.*;

import java.io.File;
import java.util.*;


public class ControlMain implements FaitControl {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JcodeFactory	bcode_factory;
private EntityFactory	entity_factory;
private ValueFactory	value_factory;
private StateFactory	state_factory;
private ProtoFactory	proto_factory;
private CallFactory	call_factory;
private FlowFactory	flow_factory;
private FaitProject	user_project;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ControlMain()
{
   bcode_factory = new JcodeFactory(10);
   entity_factory = new EntityFactory(this);
   value_factory = new ValueFactory(this);
   state_factory = new StateFactory(this);
   proto_factory = new ProtoFactory(this);
   call_factory = new CallFactory(this);
   flow_factory = new FlowFactory(this);
   user_project = null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

@Override public void setProject(FaitProject fp)
{
   user_project = fp;
   call_factory.addSpecialFile(getDescriptionFile());
   if (fp.getDescriptionFile() != null) {
      for (File ff : fp.getDescriptionFile()) {
	 call_factory.addSpecialFile(ff);
       }
    }
}



@Override public File getDescriptionFile()
{
   return new File("/research/people/spr/fait/lib/faitdata.xml");
}



/********************************************************************************/
/*										*/
/*	source <-> binary interaction methods					*/
/*										*/
/********************************************************************************/

@Override public JcodeDataType findDataType(String cls)
{
   return bcode_factory.findNamedType(cls);
}


@Override public JcodeDataType findClassType(String cls)
{
   return bcode_factory.findNamedType(cls);
}


@Override public JcodeMethod findMethod(String cls,String method,String sign)
{
   return bcode_factory.findMethod(null,cls,method,sign);
}


@Override public Iterable<JcodeMethod> findAllMethods(JcodeDataType dt,String mthd,String sgn)
{
   return bcode_factory.findAllMethods(dt,mthd,sgn);
}

@Override public JcodeMethod findInheritedMethod(String cls,String nm,String sgn)
{
   return bcode_factory.findInheritedMethod(cls,nm,sgn);
}


@Override public List<JcodeMethod> findStaticInitializers(String cls)
{
   return bcode_factory.findStaticInitializers(cls);
}

@Override public JcodeField findField(String cls,String fld)
{
   return bcode_factory.findField(null,cls,fld);
}



public Collection<JcodeMethod> getStartMethods()
{
   if (user_project == null) return null;
   
   Collection<String> snames = user_project.getStartClasses();
   if (snames == null) snames = user_project.getBaseClasses();
   
   Collection<JcodeMethod> rslt = new HashSet<>();
   
   for (String s : snames) {
      JcodeMethod jm = findMethod(s,"main","([Ljava/lang/String;)V");
      if (jm != null) rslt.add(jm);      
    }
   
   return rslt;
}

// FaitInstruction findCall(JcodeMethod fm,int line,String rtn,int idx);
// FaitInstruction findNew(JcodeMethod fm,int line,String type,int idx);



/********************************************************************************/
/*										*/
/*	Entity manipulation methods						*/
/*										*/
/********************************************************************************/

@Override public IfaceEntity findAllocEntity(FaitLocation loc,JcodeDataType typ,boolean uniq)
{
   return entity_factory.createLocalEntity(loc,typ,uniq);
}


@Override public FaitEntity.UserEntity findUserEntity(String id,FaitLocation loc)
{
   return entity_factory.createUserEntity(id,loc);
}

@Override public IfaceEntity findFixedEntity(JcodeDataType typ)
{
   return entity_factory.createFixedEntity(typ);
}


@Override public IfaceEntity findMutableEntity(JcodeDataType typ)
{
   return entity_factory.createMutableEntity(typ);
}


@Override public IfaceEntity findStringEntity(String s)
{
   return entity_factory.createStringEntity(this,s);
}


@Override public IfaceEntity findArrayEntity(JcodeDataType base,IfaceValue size)
{
   return entity_factory.createArrayEntity(this,base,size);
}


@Override public IfaceEntity findPrototypeEntity(JcodeDataType base,
      IfacePrototype from,FaitLocation src)
{
   return entity_factory.createPrototypeEntity(this,base,from,src);
}


@Override public IfaceEntity findLocalEntity(FaitLocation loc,JcodeDataType dt,boolean uniq)
{
   return entity_factory.createLocalEntity(loc,dt,uniq);
}

// IfaceEntity findParameterEntity(JcodeMethod mthd,int idx);
// IfaceEntity findReturnEntity(JcodeMethod method);



/********************************************************************************/
/*										*/
/*	Entity Set manimpulation methods					*/
/*										*/
/********************************************************************************/

@Override public IfaceEntitySet createEmptyEntitySet()
{
   return entity_factory.createEmptySet();
}


@Override public IfaceEntitySet createSingletonSet(FaitEntity fe)
{
   return entity_factory.createSingletonSet(fe);
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



@Override public IfaceValue getFieldValue(IfaceState st,JcodeField fld,IfaceValue base,boolean thisref,
					     FaitLocation src)
{
   return state_factory.getFieldValue(st,fld,base,thisref,src);
}



@Override public boolean setFieldValue(IfaceState st,JcodeField fld,IfaceValue v,
					  IfaceValue base,boolean thisref,FaitLocation src)
{
   return state_factory.setFieldValue(st,fld,v,base,thisref,src);
}




/********************************************************************************/
/*										*/
/*	Prototype management methods						*/
/*										*/
/********************************************************************************/

@Override public IfacePrototype createPrototype(JcodeDataType typ)
{
   return proto_factory.createPrototype(typ);
}

@Override public IfaceCall findPrototypeMethod(JcodeMethod fm)
{
   return call_factory.findPrototypeMethod(fm);
}




/********************************************************************************/
/*										*/
/*	Data access methods							*/
/*										*/
/********************************************************************************/

// Collection<FaitInstruction> getAllUses(FaitEntity src);
// Collection<FaitInstruction> getAllUses(JcodeMethod mthd);
// FaitValue getValueAtInstruction(FaitInstruction ins,int idx);



/********************************************************************************/
/*										*/
/*	Value methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceValue findAnyValue(JcodeDataType typ)
{
   return value_factory.anyValue(typ);
}


@Override public IfaceValue findRangeValue(JcodeDataType typ,long v0,long v1)
{
   return value_factory.rangeValue(typ,v0,v1);
}


@Override public IfaceValue findObjectValue(JcodeDataType typ,IfaceEntitySet ss,NullFlags fgs)
{
   return value_factory.objectValue(typ,ss,fgs);
}


@Override public IfaceValue findEmptyValue(JcodeDataType typ,NullFlags fgs)
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


@Override public IfaceValue findNullValue(JcodeDataType typ)
{
   return value_factory.nullValue(typ);
}



@Override public IfaceValue findBadValue()
{
   return value_factory.badValue();
}



@Override public IfaceValue findNativeValue(JcodeDataType typ)
{
   return value_factory.nativeValue(typ);
}



@Override public IfaceValue findMutableValue(JcodeDataType typ)
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


@Override public IfaceValue findInitialFieldValue(JcodeField fld,boolean isnative)
{
   return value_factory.initialFieldValue(fld,isnative);
}



/********************************************************************************/
/*										*/
/*	Call-related methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceSpecial getCallSpecial(JcodeMethod fm)
{
   return call_factory.getSpecial(fm);
}

@Override public FaitMethodData createMethodData(FaitCall fc)
{
   if (user_project == null) return null;

   return user_project.createMethodData(fc);
}

@Override public IfaceCall findCall(JcodeMethod fm,List<IfaceValue> args,InlineType inline)
{
   return call_factory.findCall(fm,args,inline);
}


@Override public Collection<IfaceCall> getAllCalls(JcodeMethod fm)
{
   return call_factory.getAllCalls(fm);
}



@Override public Collection<IfaceCall> getAllCalls()
{
   return call_factory.getAllCalls();
}




/********************************************************************************/
/*										*/
/*	Flow related methods							*/
/*										*/
/********************************************************************************/

@Override public void analyze(int nthread)
{
   flow_factory.analyze(nthread);
}


@Override public void queueLocation(FaitLocation loc)
{
   flow_factory.queueLocation(loc);
}


@Override public void handleCallback(FaitLocation frm,JcodeMethod fm,List<IfaceValue> args,String cbid)
{
   flow_factory.handleCallback(frm,fm,args,cbid);
}



/********************************************************************************/
/*                                                                              */
/*      Project methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public boolean isProjectClass(JcodeDataType dt)
{
   if (user_project == null) return false;
   
   return user_project.isProjectClass(dt.getName());
}



@Override public boolean isInProject(JcodeMethod jm)
{
   return isProjectClass(jm.getDeclaringClass());
}




}	// end of class ControlMain




/* end of ControlMain.java */
