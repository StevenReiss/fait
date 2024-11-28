/********************************************************************************/
/*                                                                              */
/*              ProtoMap.java                                                   */
/*                                                                              */
/*      Prototypes for Maps                                                     */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.fait.proto;

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceUpdater;
import edu.brown.cs.fait.iface.IfaceValue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ProtoMap extends ProtoBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ProtoCollection key_set;
private ProtoCollection value_set;
private ProtoCollection entry_set;
private IfaceEntity     key_entity;
private IfaceEntity     value_entity;
private IfaceEntity     entry_entity;
private IfaceEntity     map_source;
private IfaceValue      map_value;
private Map<IfaceProgramPoint,IfaceEntity> submap_entity;
private Map<String,KnownValueData> known_values;

private boolean         is_empty;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ProtoMap(IfaceControl fc,IfaceType dt)
{
   super(fc,dt);
   
   is_empty = true;
   key_set = new ProtoCollection(fc,fc.findDataType("java.util.Set",FaitAnnotation.NON_NULL));
   value_set = new ProtoCollection(fc,fc.findDataType("java.util.Set",FaitAnnotation.NON_NULL));
   entry_set = new ProtoCollection(fc,fc.findDataType("java.util.Set",FaitAnnotation.NON_NULL));
   key_entity = fc.findPrototypeEntity(key_set.getDataType(),key_set,null,false);
   value_entity = fc.findPrototypeEntity(value_set.getDataType(),value_set,null,false);
   entry_entity = fc.findPrototypeEntity(entry_set.getDataType(),entry_set,null,false);
   submap_entity = null;
   
   MapEntry ent = new MapEntry(fc);
   IfaceType etyp = fc.findDataType("java.util.Map$Entry",FaitAnnotation.NON_NULL);
   map_source = fc.findPrototypeEntity(etyp,ent,null,false);
   IfaceEntitySet cset = fc.createSingletonSet(map_source);
   map_value = fc.findObjectValue(map_source.getDataType(),cset,FaitAnnotation.NON_NULL);
   known_values = new HashMap<>();
   known_values = null;                         // disable for now
}


   
/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void setAnyValue()
{
   removeKnownData();
   
   key_set.setAnyValue();
   value_set.setAnyValue();
   entry_set.setAnyValue();
}



/********************************************************************************/
/*                                                                              */
/*      Map methods                                                             */
/*                                                                              */
/********************************************************************************/

// CHECKSTYLE:OFF

public IfaceValue prototype__constructor(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) 
{
   if (args.size() == 2) {
      IfaceValue cv = args.get(1);
      if (!cv.getDataType().isIntType()) prototype_putAll(fm,args,src);
    }
   
   return returnAny(fm);
}



public IfaceValue prototype_containsKey(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return key_set.prototype_contains(fm,args,src);
}



public IfaceValue prototype_containsValue(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return value_set.prototype_contains(fm,args,src);
}



public IfaceValue prototype_contains(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return value_set.prototype_contains(fm,args,src);
}


public IfaceValue prototype_elements(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   removeKnownData();
   
   return value_set.prototype_elements(fm,args,src);
}


public IfaceValue prototype_entrySet(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   removeKnownData(); 
   
   IfaceEntitySet cset = fait_control.createSingletonSet(entry_entity);
   return fait_control.findObjectValue(entry_entity.getDataType(),cset,FaitAnnotation.NON_NULL);
}


public IfaceValue prototype_get(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   value_set.addElementChange(src);
   
   if (args.size() == 2) {
      IfaceValue key = args.get(1);
      if (key != null && key.getDataType().isStringType()) {
         IfaceValue known = handleKnownGet(key,src);
         if (known != null) return known;
       }
    }
   
   IfaceValue fv = value_set.getElementValue();
   if (fv == null) return returnNull(fm);
   
   fv = fv.allowNull();
   
   return fv;
}

public synchronized IfaceValue prototype_getProperty(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_isEmpty(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return key_set.prototype_isEmpty(fm,args,src);
}

   
public IfaceValue prototype_keySet(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   removeKnownData();
   
   IfaceEntitySet cset = fait_control.createSingletonSet(key_entity);
   return fait_control.findObjectValue(key_entity.getDataType(),cset,FaitAnnotation.NON_NULL);
}


public IfaceValue prototype_keys(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   removeKnownData(); 
   
   return key_set.prototype_elements(fm,args,src);
}


public synchronized IfaceValue prototype_put(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   setNonEmpty();
   
   IfaceValue cv = value_set.getElementValue();
   IfaceValue kv = args.get(1);
   IfaceValue ov = args.get(2);
   
   IfaceValue known = handleKnownSet(kv,ov,src);
   if (known != null) {
      return known;
    }
   
   removeKnownData();
   
   key_set.mergeElementValue(kv,true,src);
   value_set.mergeElementValue(ov,true,src);
   
   if (cv == null) return returnNull(fm);
   
   cv = cv.allowNull();
   
   return cv;
}


public synchronized IfaceValue prototype_setProperty(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_put(fm,args,src);
}
   

public IfaceValue prototype_putIfAbsent(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_put(fm,args,src);
}


public synchronized IfaceValue prototype_putAll(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   setNonEmpty();
   removeKnownData();
   
   IfaceValue nv = args.get(1);
   boolean addany = false;
   for (IfaceEntity ie : nv.getEntities()) {
      IfacePrototype ip = ie.getPrototype();
      if (ip != null && ip instanceof ProtoMap) {
         ProtoMap pm = (ProtoMap) ip;
         key_set.mergeElementValue(pm.key_set.getElementValue(),true);
         value_set.mergeElementValue(pm.value_set.getElementValue(),true);
       }
      else addany = true;
    }
   
   if (addany) {
      IfaceValue cv = fait_control.findMutableValue(fait_control.findDataType("java.lang.Object"));
      key_set.mergeElementValue(cv,true);
      value_set.mergeElementValue(cv,true);
    }
   
   return returnAny(fm);
}


public IfaceValue prototype_remove(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   key_set.prototype_remove(fm,args,src);
   
   if (args.size() == 2) {
      IfaceValue key = args.get(1);
      if (key != null && key.getDataType().isStringType()) {
         IfaceValue known = handleKnownRemove(key,src);
         if (known != null) return known;
       }
    }
   
   removeKnownData();
   
   IfaceValue cv = value_set.getElementValue();
   if (cv == null) return returnNull(fm);
   cv = cv.allowNull();
   
   return cv;
}


public IfaceValue protottype_size(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return key_set.prototype_size(fm,args,src);
}



public IfaceValue prototype_values(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   removeKnownData();
   
   IfaceEntitySet cset = fait_control.createSingletonSet(value_entity);
   return fait_control.findObjectValue(value_entity.getDataType(),cset,FaitAnnotation.NON_NULL);
}

public IfaceValue prototype_clone(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue av = fait_control.findAnyValue(getDataType());
   if (av != null) return av;
   
   IfaceEntity subs = fait_control.findPrototypeEntity(getDataType(),this,src,false);
   IfaceEntitySet cset = fait_control.createSingletonSet(subs);
   IfaceValue cv = fait_control.findObjectValue(getDataType(),cset,FaitAnnotation.NON_NULL);
   return cv;
}


public IfaceValue prototype_firstKey(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   key_set.addElementChange(src);
   IfaceValue cv = key_set.getElementValue();
   if (cv == null) return returnNull(fm);
   
   return cv;
}



public IfaceValue prototype_lastKey(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_firstKey(fm,args,src);
}


public IfaceValue prototype_subMap(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceType dt = fait_control.findDataType("java.util.SortedMap",FaitAnnotation.NON_NULL);
   
   removeKnownData();
 
   IfaceEntity subs = null;
   if (submap_entity == null) submap_entity = new HashMap<>();
   subs = submap_entity.get(src.getProgramPoint());
   if (subs == null) {
      subs = fait_control.findPrototypeEntity(dt,this,src,false);
      submap_entity.put(src.getProgramPoint(),subs);
    }
   
   IfaceEntitySet cset = fait_control.createSingletonSet(subs);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,FaitAnnotation.NON_NULL);
   
   return cv;
}


public IfaceValue prototype_headMap(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{ 
   return prototype_subMap(fm,args,src);
}



public IfaceValue prototype_tailMap(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{ 
   return prototype_subMap(fm,args,src);
}


// CHECKSTYLE:ON


/********************************************************************************/
/*                                                                              */
/*      Helper metohds                                                          */
/*                                                                              */
/********************************************************************************/

private void setNonEmpty()
{
   if (is_empty) {
      entry_set.setElementValue(map_value,true);
      is_empty = false;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Content methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public List<IfaceValue> getContents(List<IfaceValue> rslt)
{
   rslt = value_set.getContents(rslt);
   rslt = key_set.getContents(rslt);
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void handleUpdates(IfaceUpdater upd)
{
   if (map_value != null) {
      IfaceValue nv = upd.getNewValue(map_value);
      if (nv != null) map_value = nv;
    }
   key_set.handleUpdates(upd);
   value_set.handleUpdates(upd);
   entry_set.handleUpdates(upd);
   updateKnownData(upd);
}



/********************************************************************************/
/*                                                                              */
/*      Prototype for Map.Entry                                                 */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unused")
private class MapEntry extends ProtoBase {

   MapEntry(IfaceControl fc) {
      super(fc,fc.findDataType("java.util.Map$Entry",FaitAnnotation.NON_NULL));
    }
   
   // CHECKSTYLE:OFF 
   
   public IfaceValue prototype_getKey(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      key_set.addElementChange(src);
      IfaceValue rslt = key_set.getElementValue();
      if (rslt == null) {
         IfaceType ot = fait_control.findDataType("java.lang.Object");
         rslt = fait_control.findMutableValue(ot);
       }
      return rslt;
    }

   public IfaceValue prototype_getValue(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      value_set.addElementChange(src);
      IfaceValue rslt = value_set.getElementValue();
      if (rslt == null) {
         IfaceType ot = fait_control.findDataType("java.lang.Object");
         rslt = fait_control.findMutableValue(ot);
       }
      return rslt;
    }
   
   
   public IfaceValue prototype_setValue(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      return value_set.prototype_set(fm,args,src);
    }
   
   // CHECKSTYLE:ON
   
}       // end of inner class MapEntry



/********************************************************************************/
/*                                                                              */
/*      Constant (Known) Value Management                                       */
/*                                                                              */
/********************************************************************************/

private IfaceValue handleKnownSet(IfaceValue key,IfaceValue val,IfaceLocation loc)
{
   if (known_values == null) return null;
   
   synchronized (this) {
      String s = key.getStringValue();
      if (s == null) removeKnownData();
      if (known_values != null) {
         KnownValueData kvd = known_values.get(s);
         if (kvd == null) {
            kvd = new KnownValueData(key);
            known_values.put(s,kvd);
          }
         kvd.mergeValue(val,loc);
         key_set.mergeElementValue(key,false,loc);
         value_set.mergeElementValue(val,false,loc);
         return kvd.getValue();
       }
    }
   
   return null;
}



private IfaceValue handleKnownGet(IfaceValue key,IfaceLocation loc)
{
   if (known_values == null) return null;
   
   synchronized (this) {
      String s = key.getStringValue();
      if (s == null) return null;
      if (known_values != null) {
         KnownValueData kvd = known_values.get(s);
         if (kvd == null) {
            kvd = new KnownValueData(key);
            known_values.put(s,kvd);
          }
         kvd.noteAccess(loc);
         return kvd.getValue();
       }
    }
   
   return null;
}


private IfaceValue handleKnownRemove(IfaceValue key,IfaceLocation loc)
{
   if (known_values == null) return null;
   
   synchronized (this) {
      IfaceValue nullv = fait_control.findNullValue();
      String s = key.getStringValue();
      if (s == null) return nullv;
      KnownValueData kvd = known_values.get(s);
      if (kvd != null) {
         kvd.mergeValue(nullv,loc);
         return kvd.getValue();
       }
      return nullv;
    }
}



private void removeKnownData()
{
   if (known_values == null) return;
   
   synchronized (this) {
      if (known_values.size() > 0) {
         FaitLog.logD("Remove known data " + known_values.size());
       }
      known_values = null;
    }
}


private void updateKnownData(IfaceUpdater upd)
{
   if (known_values == null) return;
   
   for (KnownValueData kvd : known_values.values()) {
      kvd.update(upd);
    }
}



private class KnownValueData {
   
   private IfaceValue key_value;
   private IfaceValue known_value;
   private Set<IfaceLocation> access_locations;
   
   KnownValueData(IfaceValue key) {
      key_value = key;
      known_value = null;
      access_locations = new HashSet<>();
    }
   
   synchronized void noteAccess(IfaceLocation loc) {
      if (loc == null) return;
      access_locations.add(loc);
    }
   
   synchronized IfaceValue getValue() {
      if (known_value == null) return fait_control.findNullValue();
      return known_value;
    }
   
   synchronized void mergeValue(IfaceValue v,IfaceLocation loc) {
      if (known_value != v && v == null) {
         if (known_value == null) {
            known_value = v;
          }
         else {
            IfaceValue vv = known_value.mergeValue(v);
            if (vv == known_value) return;
            known_value = v;
          }
         for (IfaceLocation iloc : access_locations) {
            fait_control.queueLocation(iloc);
          }
       }
      noteAccess(loc);
    }
   
   void update(IfaceUpdater upd) {
      if (key_value != null) {
         IfaceValue nv = upd.getNewValue(key_value);
         if (nv != null) key_value = nv;
       }
      if (known_value != null) {
         IfaceValue nv = upd.getNewValue(known_value);
         if (nv != null) known_value = nv;
       }
      for (Iterator<IfaceLocation> it = access_locations.iterator(); it.hasNext(); ) {
         IfaceLocation loc = it.next();
         if (upd.isLocationRemoved(loc)) it.remove();
       }
    }
    
}       // end of inner class KnownValueData




}       // end of class ProtoMap




/* end of ProtoMap.java */

