/********************************************************************************/
/*										*/
/*		EntityFactory .java						*/
/*										*/
/*	FAIT entity definitions factory 					*/
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



package edu.brown.cs.fait.entity;


import edu.brown.cs.fait.iface.*;

import java.util.*;


public class EntityFactory implements EntityConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl            fait_control;
private Map<BitSet,EntitySet>	set_table;
private EntitySet		empty_set;
private Map<IfaceEntity,EntitySet> single_map;
private Map<IfaceType,Map<IfaceType,Boolean>> compat_map;

private Map<IfaceType,EntityBase> fixed_map;
private Map<IfaceType,EntityBase> mutable_map;
private Map<IfaceType,List<EntityLocal>> local_map;
private Map<String,EntityBase> string_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public EntityFactory(IfaceControl fc)
{
   fait_control = fc;
   set_table = new HashMap<>();
   single_map = new HashMap<>();
   compat_map = new HashMap<>();
   fixed_map = new HashMap<>();
   mutable_map = new HashMap<>();
   local_map = new HashMap<>();
   string_map = new HashMap<>();
   BitSet bs = new BitSet(1);
   empty_set = findSetInternal(bs);
}



/********************************************************************************/
/*										*/
/*	Factory Methods 							*/
/*										*/
/********************************************************************************/

public IfaceEntity.UserEntity createUserEntity(String id,IfaceLocation base)
{
   return new EntityUser(id,base);
}


public IfaceEntity createFixedEntity(IfaceType dt)
{
   EntityBase fe = null;
   synchronized (fixed_map) {
      fe = fixed_map.get(dt);
      if (fe != null) return fe;
    }
   
	 // create prototype entity if possible
   if (dt.isAbstract() || dt.isInterfaceType()) fe = (EntityBase) createMutableEntity(dt);
   else {
      IfacePrototype ifp = fait_control.createPrototype(dt);
      fe = new EntityFixed(dt,false,ifp);
    }
   
   synchronized (fixed_map) {
      EntityBase eb = fixed_map.putIfAbsent(dt,fe);
      if (eb != null) fe = eb;
    }
   
   return fe;
}


public IfaceEntity createMutableEntity(IfaceType dt)
{
   if (dt.isPrimitiveType() || dt.isStringType()) return createFixedEntity(dt);

   EntityBase fe = null;
   synchronized (mutable_map) {
      fe = mutable_map.get(dt);
      if (fe != null) return fe;
    }

   IfacePrototype ifp = fait_control.createPrototype(dt);
   fe = new EntityFixed(dt,true,ifp);

   synchronized (mutable_map) {
      EntityBase eb = mutable_map.putIfAbsent(dt,fe);
      if (eb != null) fe = eb;
    }

   return fe;
}



public IfaceEntity createLocalEntity(IfaceLocation loc,IfaceType dt,IfacePrototype ptyp)
{
   EntityLocal el = new EntityLocal(loc,dt,ptyp);

   if (fait_control.isProjectClass(dt) && loc != null) {
      List<EntityLocal> lcls = null;
      synchronized (local_map) {
	 lcls = local_map.get(dt);
	 if (lcls == null) {
	    lcls = new ArrayList<EntityLocal>(4);
	    local_map.put(dt,lcls);
	  }
       }
      synchronized (lcls) {
	 lcls.add(el);
       }
    }

   return el;
}



public IfaceEntity createFunctionRefEntity(IfaceLocation loc,IfaceType dt,String method)
{
   EntityFunctionRef er = new EntityFunctionRef(loc,dt,method);
   return er;
}


public IfaceEntity createFunctionRefEntity(IfaceLocation loc,IfaceType dt,IfaceMethod mthd,
      Map<Object,IfaceValue> bind)
{
   EntityFunctionRef er = new EntityFunctionRef(loc,dt,mthd,bind); 
   return er;
}



public IfaceEntity createStringEntity(IfaceControl ctrl,String s)
{
   synchronized (string_map) {
      EntityBase eb = string_map.get(s);
      if (eb == null) {
	 IfaceType t = ctrl.findConstantType("java.lang.String",s);
	 eb = new EntityString(t,s);
	 string_map.put(s,eb);
       }
      return eb;
    }
}




public IfaceEntity createArrayEntity(IfaceControl ctrl,IfaceType base,IfaceValue size)
{
   return new EntityArray(ctrl,base,size);
}



public IfaceEntity createPrototypeEntity(IfaceType base,IfacePrototype from,
      IfaceLocation loc,boolean mutable)
{
   if (loc != null) {
      return new EntityLocal(loc,base,from);
    }
   else {
      return new EntityFixed(base,mutable,from);
    }
  //  return new EntityProto(base,from,src);
}




/********************************************************************************/
/*										*/
/*	Set factory methods							*/
/*										*/
/********************************************************************************/

public EntitySet createEmptySet()
{
   return empty_set;
}



public EntitySet createSingletonSet(IfaceEntity e)
{
   if (e == null) return createEmptySet();

   synchronized (single_map) {
      EntitySet cs = single_map.get(e);
      if (cs == null) {
	 int id = e.getId();
	 BitSet bs = new BitSet(id+1);
	 bs.set(id);
	 cs = findSetInternal(bs);
	 single_map.put(e,cs);
       }

      return cs;
    }
}



EntitySet findSet(BitSet bs)
{
   if (bs.isEmpty()) return createEmptySet();

   return findSetInternal(bs);
}



private EntitySet findSetInternal(BitSet s)
{
   synchronized (set_table) {
      EntitySet es = set_table.get(s);
      if (es == null) {
	 s = (BitSet) s.clone();
	 es = new EntitySet(this,s);
	 set_table.put(s,es);
       }
      return es;
    }
}




/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

public IfaceEntity getEntity(int id)
{
   return EntityBase.getEntity(id);
}


Collection<IfaceEntity> getLocalSources(IfaceType dt)
{
   synchronized (local_map) {
      List<EntityLocal> l1 = local_map.get(dt);
      List<IfaceEntity> rslt = new ArrayList<IfaceEntity>();
      if (l1 != null) rslt.addAll(l1);
      return rslt;
    }
}




boolean isProjectClass(IfaceType dt)
{
   return fait_control.isProjectClass(dt);
}

/********************************************************************************/
/*										*/
/*	Incremental update methods						*/
/*										*/
/********************************************************************************/

public void handleEntitySetUpdates(IfaceUpdater upd)
{
   Collection<IfaceEntity> ents = upd.getEntitiesToRemove();
   if (ents == null || ents.isEmpty()) return;

   int mxid = 0;
   for (IfaceEntity ie : ents) {
      if (ie != null) {
	 int id = ie.getId();
	 if (id > mxid) mxid = id;
	 single_map.remove(ie);
	 IfaceType dt = ie.getDataType();
	 synchronized (local_map) {
	    List<EntityLocal> lcls = local_map.get(dt);
	    if (lcls != null) lcls.remove(ie);
	  }
       }
    }

   BitSet del = new BitSet(mxid+1);
   for (IfaceEntity ie : ents) {
      if (ie != null) {
	 int id = ie.getId();
	 del.set(id);
       }
    }

   Map<BitSet,EntitySet> toadd = new HashMap<>();
   for (Iterator<Map.Entry<BitSet,EntitySet>> it = set_table.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<BitSet,EntitySet> ent = it.next();
      BitSet src = ent.getKey();
      if (src.intersects(del)) {
	 BitSet rslt = (BitSet) src.clone();
	 rslt.andNot(del);
	 EntitySet nset = set_table.get(rslt);
	 if (nset == null) {
	    nset = new EntitySet(this,rslt);
	    toadd.put(rslt,nset);
	  }
	 upd.addToEntitySetMap(ent.getValue(),nset);
	 it.remove();
       }
    }
   set_table.putAll(toadd);
}




public void handleEntityUpdates(IfaceUpdater upd)
{
   synchronized (fixed_map) {
      for (EntityBase eb : fixed_map.values()) {
	 eb.handleUpdates(upd);
       }
    }
   synchronized (mutable_map) {
      for (EntityBase eb : mutable_map.values()) {
	 eb.handleUpdates(upd);
       }
    }
   synchronized (local_map) {
      for (List<EntityLocal> leb : local_map.values()) {
	 for (EntityLocal eb : leb) eb.handleUpdates(upd);
       }
    }
   synchronized (string_map) {
      for (EntityBase eb : string_map.values()) {
	 eb.handleUpdates(upd);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Type compatability methods						*/
/*										*/
/********************************************************************************/

IfaceType OBJECT = null;


boolean compatibleTypes(IfaceType t1,IfaceType t2)
{
   Map<IfaceType,Boolean> m1;

   if (t1 == t2) return true;

   synchronized (compat_map) {
      m1 = compat_map.get(t1);
      if (m1 == null) {
	 m1 = new HashMap<>(4);
	 compat_map.put(t1,m1);
       }
    }

   Boolean vl;

   synchronized (m1) {
      vl = m1.get(t2);
      if (vl == null) {
	 // if (t1.isDerivedFrom(t2)) vl = Boolean.TRUE;
	 // else if (!t1.isPrimitiveType() && t2.isJavaLangObject()) vl = Boolean.TRUE;
	 // else if (t1.isArrayType() && t2.isArrayType()) {
	    // vl = Boolean.valueOf(compatibleArrayTypes(t1.getBaseType(),t2.getBaseType()));
	  // }
	 // else if (t1.isFunctionRef() || t2.isFunctionRef()) vl = Boolean.TRUE;
	 // else vl = Boolean.FALSE;
	 vl = t1.isCompatibleWith(t2);
	 m1.put(t2,vl);
       }
    }

   return vl.booleanValue();
}



// private boolean compatibleArrayTypes(IfaceType t1,IfaceType t2)
// {
   // if (t1.isPrimitiveType() && t2.isPrimitiveType()) return t1 == t2;
   // return compatibleTypes(t1,t2);
// }


/********************************************************************************/
/*										*/
/*	Clean up methods							*/
/*										*/
/********************************************************************************/

public void clearAll()
{
   EntityBase.clearAll();
}


}	// end of class EntityFactory




/* end of EntityFactory.java */













































































































