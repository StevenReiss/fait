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

import edu.brown.cs.fait.iface.*;

import java.util.*;


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

private boolean         is_empty;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ProtoMap(FaitControl fc,FaitDataType dt)
{
   super(fc,dt);
   
   is_empty = true;
   key_set = new ProtoCollection(fc,fc.findDataType("Ljava/util/Set;"));
   value_set = new ProtoCollection(fc,fc.findDataType("Ljava/util/Set;"));
   entry_set = new ProtoCollection(fc,fc.findDataType("Ljava/util/Set;"));
   key_entity = fc.findPrototypeEntity(key_set.getDataType(),key_set,null);
   value_entity = fc.findPrototypeEntity(value_set.getDataType(),value_set,null);
   entry_entity = fc.findPrototypeEntity(entry_set.getDataType(),entry_set,null);
   
   MapEntry ent = new MapEntry(fc);
   map_source = fc.findPrototypeEntity(fc.findDataType("Ljava/util/Map$Entry;"),ent,null);
   IfaceEntitySet cset = fc.createSingletonSet(map_source);
   map_value = fc.findObjectValue(map_source.getDataType(),cset,NullFlags.NON_NULL);
}


   

/********************************************************************************/
/*                                                                              */
/*      Map methods                                                             */
/*                                                                              */
/********************************************************************************/

public IfaceValue prototype__constructor(FaitMethod fm,List<IfaceValue> args,FaitLocation src) 
{
   if (args.size() == 2) {
      IfaceValue cv = args.get(1);
      if (!cv.getDataType().isInt()) prototype_putAll(fm,args,src);
    }
   
   return returnAny(fm);
}



public IfaceValue prototype_containsKey(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return key_set.prototype_contains(fm,args,src);
}



public IfaceValue prototype_containsValue(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return value_set.prototype_contains(fm,args,src);
}



public IfaceValue prototype_contains(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return value_set.prototype_contains(fm,args,src);
}


public IfaceValue prototype_elements(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return value_set.prototype_elements(fm,args,src);
}


public IfaceValue prototype_entrySet(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceEntitySet cset = fait_control.createSingletonSet(entry_entity);
   return fait_control.findObjectValue(entry_entity.getDataType(),cset,NullFlags.NON_NULL);
}


public synchronized IfaceValue prototype_get(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   value_set.addElementChange(src);
   
   IfaceValue fv = value_set.getElementValue();
   if (fv == null) return returnNull(fm);
   
   fv = fv.allowNull();
   
   return fv;
}


public IfaceValue prototype_isEmpty(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return key_set.prototype_isEmpty(fm,args,src);
}

   
public IfaceValue prototype_keySet(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceEntitySet cset = fait_control.createSingletonSet(key_entity);
   return fait_control.findObjectValue(key_entity.getDataType(),cset,NullFlags.NON_NULL);
}


public IfaceValue prototype_keys(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return key_set.prototype_elements(fm,args,src);
}


public synchronized IfaceValue prototype_put(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   setNonEmpty();
   
   IfaceValue cv = value_set.getElementValue();
   IfaceValue kv = args.get(1);
   IfaceValue ov = args.get(2);
   
   List<IfaceValue> nargs = new ArrayList<IfaceValue>();
   nargs.add(args.get(0));
   nargs.add(kv);
   key_set.prototype_add(fm,nargs,src);
   
   nargs.set(1,ov);
   value_set.prototype_add(fm,nargs,src);
   
   if (cv == null) return returnNull(fm);
   
   cv = cv.allowNull();
   
   return cv;
}
   

public IfaceValue prototype_putIfAbsent(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_put(fm,args,src);
}


public synchronized IfaceValue prototype_putAll(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   setNonEmpty();
   
   IfaceValue nv = args.get(1);
   boolean addany = false;
   for (IfaceEntity ie : nv.getEntities()) {
      IfacePrototype ip = ie.getPrototype();
      if (ip != null && ip instanceof ProtoMap) {
         ProtoMap pm = (ProtoMap) ip;
         key_set.mergeElementValue(pm.key_set.getElementValue());
         value_set.mergeElementValue(pm.value_set.getElementValue());
       }
      else addany = true;
    }
   
   if (addany) {
      IfaceValue cv = fait_control.findMutableValue(fait_control.findDataType("Ljava/lang/Object;"));
      key_set.mergeElementValue(cv);
      value_set.mergeElementValue(cv);
    }
   
   return returnAny(fm);
}


public IfaceValue prototype_remove(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   key_set.prototype_remove(fm,args,src);
   
   IfaceValue cv = value_set.getElementValue();
   if (cv == null) return returnNull(fm);
   cv = cv.allowNull();
   
   return cv;
}


public IfaceValue protottype_size(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return key_set.prototype_size(fm,args,src);
}



public IfaceValue prototype_values(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceEntitySet cset = fait_control.createSingletonSet(value_entity);
   return fait_control.findObjectValue(value_entity.getDataType(),cset,NullFlags.NON_NULL);
}

public IfaceValue prototype_clone(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceEntity subs = fait_control.findPrototypeEntity(getDataType(),this,src);
   IfaceEntitySet cset = fait_control.createSingletonSet(subs);
   IfaceValue cv = fait_control.findObjectValue(getDataType(),cset,NullFlags.NON_NULL);
   return cv;
}


public synchronized IfaceValue prototype_firstKey(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   key_set.addElementChange(src);
   IfaceValue cv = key_set.getElementValue();
   if (cv == null) return returnNull(fm);
   
   return cv;
}



public IfaceValue prototype_lastKey(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_firstKey(fm,args,src);
}


public IfaceValue prototype_subMap(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   FaitDataType dt = fait_control.findDataType("Ljava/util/Map;");
   IfaceEntity subs = fait_control.findPrototypeEntity(dt,this,src);
   IfaceEntitySet cset = fait_control.createSingletonSet(subs);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,NullFlags.NON_NULL);
   
   return cv;
}


public IfaceValue prototype_headMap(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{ 
   return prototype_headMap(fm,args,src);
}



public IfaceValue prototype_tailMap(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{ 
   return prototype_headMap(fm,args,src);
}




/********************************************************************************/
/*                                                                              */
/*      Helper metohds                                                          */
/*                                                                              */
/********************************************************************************/

private void setNonEmpty()
{
   if (is_empty) {
      entry_set.setElementValue(map_value);
      is_empty = false;
    }
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
}



/********************************************************************************/
/*                                                                              */
/*      Prototype for Map.Entry                                                 */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unused")
private class MapEntry extends ProtoBase {

   MapEntry(FaitControl fc) {
      super(fc,fc.findDataType("Ljava/util/Map$Entry;"));
    }
   
   public IfaceValue prototype_getKey(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoMap.this) {
         key_set.addElementChange(src);
         return key_set.getElementValue();
       }
    }

   public IfaceValue prototype_getValue(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoMap.this) {
         value_set.addElementChange(src);
         return value_set.getElementValue();
       }
    }
   
   
   public IfaceValue prototype_setValue(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      return value_set.prototype_set(fm,args,src);
    }
   
}       // end of inner class MapEntry




}       // end of class ProtoMap




/* end of ProtoMap.java */

