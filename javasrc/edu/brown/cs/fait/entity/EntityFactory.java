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
import edu.brown.cs.ivy.jcode.JcodeDataType;

import java.util.*;


public class EntityFactory implements EntityConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl             fait_control;
private Map<BitSet,EntitySet>	set_table;
private EntitySet		empty_set;
private Map<IfaceEntity,EntitySet> single_map;
private Map<JcodeDataType,Map<JcodeDataType,Boolean>> compat_map;

private Map<JcodeDataType,EntityBase> fixed_map;
private Map<JcodeDataType,EntityBase> mutable_map;
private Map<JcodeDataType,List<EntityLocal>> local_map;
private Map<String,EntityBase> string_map;
private Map<JcodeDataType,Set<FaitLocation>> local_updates;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public EntityFactory(FaitControl fc)
{
   fait_control = fc;
   set_table = new WeakHashMap<BitSet,EntitySet>();
   single_map = new HashMap<IfaceEntity,EntitySet>();
   compat_map = new HashMap<JcodeDataType,Map<JcodeDataType,Boolean>>();
   fixed_map = new HashMap<JcodeDataType,EntityBase>();
   mutable_map = new HashMap<JcodeDataType,EntityBase>();
   local_map = new HashMap<JcodeDataType,List<EntityLocal>>();
   string_map = new HashMap<String,EntityBase>();
   local_updates = new HashMap<JcodeDataType,Set<FaitLocation>>();
   BitSet bs = new BitSet(1);
   empty_set = findSetInternal(bs);
}



/********************************************************************************/
/*										*/
/*	Factory Methods 							*/
/*										*/
/********************************************************************************/

public FaitEntity.UserEntity createUserEntity(String id,FaitLocation base)
{
   return new EntityUser(id,base);
}


public IfaceEntity createFixedEntity(JcodeDataType dt)
{
   synchronized (fixed_map) {
      EntityBase fe = fixed_map.get(dt);
      if (fe == null) {
	 // create prototype entity if possible
	 if (dt.isAbstract() || dt.isInterface()) fe = (EntityBase) createMutableEntity(dt);
	 else {
            IfacePrototype ifp = fait_control.createPrototype(dt);
            if (ifp != null) 
               fe = new EntityProto(dt,ifp,null);
            else
               fe = new EntityFixed(dt,false);
          }
	 fixed_map.put(dt,fe);
       }
      return fe;
    }
}


public IfaceEntity createMutableEntity(JcodeDataType dt)
{
   synchronized (mutable_map) {
      EntityBase fe = mutable_map.get(dt);
      if (fe == null) {
         IfacePrototype ifp = fait_control.createPrototype(dt);
         if (ifp != null) { 
            fe = new EntityProto(dt,ifp,null);
          }
	 else {
            fe = new EntityFixed(dt,true);
          }
	 mutable_map.put(dt,fe);
       }
      return fe;
    }
}



public IfaceEntity createLocalEntity(FaitLocation loc,JcodeDataType dt,boolean uniq)
{
   EntityLocal el = new EntityLocal(loc,dt,uniq);

   if (fait_control.isProjectClass(dt)) {
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


public IfaceEntity createStringEntity(FaitControl ctrl,String s)
{
   synchronized (string_map) {
      EntityBase eb = string_map.get(s);
      if (eb == null) {
	 JcodeDataType t = ctrl.findDataType("Ljava/lang/String;");
	 eb = new EntityString(t,s);
	 string_map.put(s,eb);
       }
      return eb;
    }
}




public IfaceEntity createArrayEntity(FaitControl ctrl,JcodeDataType base,IfaceValue size)
{
   return new EntityArray(ctrl,base,size);
}



public IfaceEntity createPrototypeEntity(FaitControl ctrl,JcodeDataType base,IfacePrototype from,
      FaitLocation src)
{ 
   return new EntityProto(base,from,src);
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



public EntitySet createSingletonSet(FaitEntity e1)
{
   if (e1 == null) return createEmptySet();
   IfaceEntity e = (IfaceEntity) e1;

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


Collection<IfaceEntity> getLocalSources(JcodeDataType dt)
{
   synchronized (local_map) {
      List<EntityLocal> l1 = local_map.get(dt);
      List<IfaceEntity> rslt = new ArrayList<IfaceEntity>();
      if (l1 != null) rslt.addAll(l1);
      return rslt;
    }
}


void addLocalReference(JcodeDataType dt,FaitLocation loc)
{
   synchronized (local_updates) {
      Set<FaitLocation> ll = local_updates.get(dt);
      if (ll == null) {
         ll = new HashSet<FaitLocation>(4);
         local_updates.put(dt,ll);
       }
      ll.add(loc);
    }
}

boolean isProjectClass(JcodeDataType dt)
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

   int mid = 0;
   for (IfaceEntity ie : ents) {
      if (ie != null) {
	 int id = ie.getId();
	 if (id > mid) mid = id;
	 single_map.remove(ie);
       }
    }

   BitSet del = new BitSet(mid+1);
   for (IfaceEntity ie : ents) {
      if (ie != null) {
	 int id = ie.getId();
	 del.set(id);
       }
    }

   Collection<BitSet> sets = new ArrayList<BitSet>(set_table.keySet());

   for (BitSet src : sets) {
      if (src.intersects(del)) {
	 BitSet rslt = (BitSet) src.clone();
	 rslt.andNot(del);
	 IfaceEntitySet nset = findSetInternal(rslt);
	 IfaceEntitySet oset = set_table.get(src);
	 upd.addEntitySetMap(oset,nset);
       }
    }
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

JcodeDataType OBJECT = null;



boolean compatibleTypes(JcodeDataType t1,JcodeDataType t2)
{
   Map<JcodeDataType,Boolean> m1;

   synchronized (compat_map) {
      m1 = compat_map.get(t1);
      if (m1 == null) {
	 m1 = new HashMap<JcodeDataType,Boolean>(4);
	 compat_map.put(t1,m1);
       }
    }

   Boolean vl;

   synchronized (m1) {
      vl = m1.get(t2);
      if (vl == null) {
	 if (t1.isDerivedFrom(t2)) vl = Boolean.TRUE;
	 else if (!t1.isPrimitive() && t2.isJavaLangObject()) vl = Boolean.TRUE;
	 else if (t1.isArray() && t2.isArray()) {
	    vl = Boolean.valueOf(compatibleArrayTypes(t1.getBaseDataType(),t2.getBaseDataType()));
	  }
	 else vl = Boolean.FALSE;
	 m1.put(t2,vl);
       }
    }

   return vl.booleanValue();
}



private boolean compatibleArrayTypes(JcodeDataType t1,JcodeDataType t2)
{
   if (t1.isPrimitive() && t2.isPrimitive()) return t1 == t2;
   return compatibleTypes(t1,t2);
}




}	// end of class EntityFactory




/* end of EntityFactory.java */



