/********************************************************************************/
/*                                                                              */
/*              TypeFactory.java                                                */
/*                                                                              */
/*      Factor creating and managing extended types                             */
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



package edu.brown.cs.fait.type;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.ivy.xml.IvyXml;

public class TypeFactory implements TypeConstants, FaitConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private TypeMap type_map;
private List<TypeSubtype> all_subtypes;
private Set<TypeSubtype> base_subtypes;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public TypeFactory(IfaceControl ic)
{
   type_map = new TypeMap();
   all_subtypes = new ArrayList<>();
   all_subtypes.add(CheckNullness.getType());
   // all_subtypes.add(CheckInitialization.getType());
   // all_subtypes.add(CheckTaint.getType());
   
   base_subtypes= new HashSet<>(all_subtypes);
   
   for (int i = 0; i < all_subtypes.size(); ++i) {
      TypeSubtype tst = all_subtypes.get(i);
      tst.setIndex(i);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Type creation methods                                                   */
/*                                                                              */
/********************************************************************************/

public IfaceType createType(IfaceBaseType base)
{
   if (base == null) return null;
   
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];
   for (int i = 0; i < getNumSubtypes(); ++i) {
      TypeSubtype st = getSubtype(i);
      IfaceSubtype.Value v = st.getDefaultValue(base);
      vals[i] = v;
    }
   
   return createActualType(base,vals);
}


public IfaceType createType(IfaceBaseType base,Collection<IfaceAnnotation> annots)
{
   if (base == null) return null;
   
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];
   for (int i = 0; i < getNumSubtypes(); ++i) {
      TypeSubtype st = getSubtype(i);
      vals[i] = st.getDefaultValue(base,annots);
    }
   return createActualType(base,vals);
}


public IfaceType createType(IfaceBaseType base,IfaceAnnotation ... annots)
{
   if (base == null) return null;
   
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];
   for (int i = 0; i < getNumSubtypes(); ++i) {
      TypeSubtype st = getSubtype(i);
      vals[i] = st.getDefaultValue(base,annots);
    }
   return createActualType(base,vals);
}



IfaceType createType(IfaceBaseType base,IfaceSubtype.Value [] vals)
{
   if (base == null) return null;
   
   return createActualType(base,vals);
}


public IfaceType createType(IfaceType base,Map<IfaceSubtype,IfaceSubtype.Value> subs)
{
   if (base == null) return null;
   
   Map<IfaceSubtype,IfaceSubtype.Value> nsubs = new HashMap<>();
   for (IfaceSubtype ist : all_subtypes) {
      IfaceSubtype.Value val = subs.get(ist);
      if (val == null) {
         val = base.getValue(ist);
       }
      if (val != null) nsubs.put(ist,val);
    }
   
   return createActualType(base.getJavaType(),nsubs);
}



public IfaceType createType(IfaceType base,IfaceAnnotation ... annots)
{
   if (base == null) return null;
   
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];  
   for (int i = 0; i < getNumSubtypes(); ++i) {
      TypeSubtype st = getSubtype(i);
      vals[i] = st.getDefaultValue(annots,base.getValue(st));
    }
   
   return createActualType(base.getJavaType(),vals);
}



public IfaceType createType(IfaceType base,Collection<IfaceAnnotation> annots)
{
   if (base == null) return null;
   
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];  
   for (int i = 0; i < getNumSubtypes(); ++i) {
      TypeSubtype st = getSubtype(i);
      vals[i] = st.getDefaultValue(annots,base.getValue(st));
    }
   
   return createActualType(base.getJavaType(),vals);
}


public IfaceType createType(IfaceBaseType base,IfaceType orig)
{
   if (base == null) return null;
   
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];
   for (int i = 0; i < getNumSubtypes(); ++i) {
      IfaceSubtype ist = getSubtype(i);
      IfaceSubtype.Value v = orig.getValue(ist);
      vals[i] = v;
    }
   
   return createActualType(base,vals);
}



public IfaceType createConstantType(IfaceBaseType base,Object cnst)
{
   IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];
   for (TypeSubtype st : all_subtypes) {
      int idx = st.getIndex();
      vals[idx] = st.getDefaultConstantValue(base,cnst);
    }
   return createActualType(base,vals);
}



private IfaceType createActualType(IfaceBaseType base,Map<IfaceSubtype,IfaceSubtype.Value> subs)
{
   if (base == null) return null;
   
   IfaceType t0 = type_map.findType(base,subs);
   if (t0 == null) {
      IfaceSubtype.Value [] vals = new IfaceSubtype.Value[getNumSubtypes()];
      for (int i = 0; i < getNumSubtypes(); ++i) {
         TypeSubtype st = getSubtype(i);
         IfaceSubtype.Value v = null;
         if (subs != null) v = subs.get(st);
         if (v == null) v = st.getDefaultValue(base);
         vals[i] = v;
       }
      t0 = new TypeBase(this,base,vals);
      t0 = type_map.defineType(t0);
    }
   return t0;
}


private IfaceType createActualType(IfaceBaseType base,IfaceSubtype.Value [] subs)
{
   IfaceType t0 = type_map.findType(base,subs);
   if (t0 == null) {
      t0 = new TypeBase(this,base,subs);
      t0 = type_map.defineType(t0);
    }
   return t0;
}




/********************************************************************************/
/*                                                                              */
/*      Handle user defined subtypes                                            */
/*                                                                              */
/********************************************************************************/

public void addSpecialFile(File f)
{
   addSpecialFile(IvyXml.loadXmlFromFile(f));
}


public synchronized void addSpecialFile(Element xml)
{
   for (Element selt : IvyXml.children(xml,"SUBTYPE")) {
      String cnm = IvyXml.getAttrString(selt,"CLASS");
      if (cnm != null) {
         if (cnm.startsWith("Check")) cnm = "edu.brown.cs.fait.type." + cnm;
         try {
            Class<?> c = Class.forName(cnm);
            Method m = c.getMethod("getType");
            TypeSubtype tst = (TypeSubtype) m.invoke(null);
            if (!all_subtypes.contains(tst)) {
               int ct = all_subtypes.size();
               all_subtypes.add(tst);
               tst.setIndex(ct);
             }
            continue;
          }
         catch (Throwable e) { 
            FaitLog.logE("Problem getting subtype by class",e);
          }
         FaitLog.logE("Subtype class " + cnm + " not found or invalid");
         continue;
       }
      TypeSubtypeUser tsu = new TypeSubtypeUser(selt);
      int ct = all_subtypes.size();
      all_subtypes.add(tsu);
      tsu.setIndex(ct);
    }
}


public void clearAllSpecials()
{
   for (Iterator<TypeSubtype> it = all_subtypes.iterator(); it.hasNext(); ) {
      TypeSubtype tst = it.next();
      if (!base_subtypes.contains(tst)) it.remove();
    }
   for (int i = 0; i < all_subtypes.size(); ++i) {
      TypeSubtype tst = all_subtypes.get(i);
      tst.setIndex(i);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

int getNumSubtypes()            { return all_subtypes.size(); }

TypeSubtype getSubtype(int i)   { return all_subtypes.get(i); }



/********************************************************************************/
/*                                                                              */
/*      Efficient mapping to get unique types                                   */
/*                                                                              */
/********************************************************************************/

private class TypeMap {
   
   private Map<IfaceBaseType,Object> base_map;
   
   TypeMap() {
      base_map = new ConcurrentHashMap<>();
    }
   
   @SuppressWarnings("unchecked") 
   IfaceType defineType(IfaceType t) {
      int ct = all_subtypes.size();
      if (ct == 0) {
         IfaceType t0 = (IfaceType) base_map.putIfAbsent(t.getJavaType(),t);
         if (t0 != null) return t0;
         return t;
       }
       
      Map<Object,Object> map = (Map<Object,Object>) base_map.get(t.getJavaType());
      if (map == null) {
         map = new HashMap<>();
         Map<Object,Object> m1 = (Map<Object,Object>) base_map.putIfAbsent(t.getJavaType(),map);
         if (m1 != null) map = m1;
       }
      
      synchronized (map) {
         for (int i = 0; i < ct-1; ++i) {
            IfaceSubtype.Value val = t.getValue(all_subtypes.get(i));
            Map<Object,Object> nmap = (Map<Object,Object>) map.get(val);
            if (nmap == null) {
               nmap = new HashMap<>();
               map.put(val,nmap);
             }
            map = nmap;
          }
         IfaceSubtype.Value val = t.getValue(all_subtypes.get(ct-1));
         IfaceType t0 = (IfaceType) map.putIfAbsent(val,t);
         if (t0 != null) return t0;
       }
      
      return t;
    }
   
   IfaceType findType(IfaceBaseType bt,IfaceSubtype.Value [] vals) {
      int ct = all_subtypes.size(); 
      if (ct == 0) {
         return (IfaceType) base_map.get(bt);
       }
      Map<?,?> map = (Map<?,?>) base_map.get(bt);
      if (map == null) return null;
      for (int i = 0; i < ct-1; ++i) {
         Map<?,?> nmap = (Map<?,?>) map.get(vals[i]);
         if (nmap == null) return null;
         map = nmap;
       }
      return (IfaceType) map.get(vals[ct-1]);
    }
   
   IfaceType findType(IfaceBaseType bt,Map<IfaceSubtype,IfaceSubtype.Value> valmap) {
      int ct = all_subtypes.size(); 
      if (ct == 0) {
         return (IfaceType) base_map.get(bt);
       }
      Map<?,?> map = (Map<?,?>) base_map.get(bt);
      if (map == null) return null;
      for (int i = 0; i < ct-1; ++i) {
         Map<?,?> nmap = (Map<?,?>) map.get(getValue(bt,i,valmap));
         if (nmap == null) return null;
         map = nmap;
       }
      return (IfaceType) map.get(getValue(bt,ct-1,valmap));
    }
   
   private IfaceSubtype.Value getValue(IfaceBaseType bt,
         int idx,Map<IfaceSubtype,IfaceSubtype.Value> valmap) {
      IfaceSubtype.Value v = null;
      if (valmap != null) {
         v = valmap.get(all_subtypes.get(idx));
       }
      if (v == null) {
         v = all_subtypes.get(idx).getDefaultValue(bt);
       }
      return v;
    }
   
}       // end of inner class TypeMap


}       // end of class TypeFactory




/* end of TypeFactory.java */

