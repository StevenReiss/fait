/********************************************************************************/
/*										*/
/*		CallFactory.java						*/
/*										*/
/*	Call creation and management						*/
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



package edu.brown.cs.fait.call;

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.ivy.xml.*;


import java.util.*;
import java.io.*;
import org.w3c.dom.Element;


public class CallFactory implements CallConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private enum AllocMode { LOCAL, BINARY, BINARY_INHERIT, AST, AST_INHERIT };

private IfaceControl	fait_control;
private Map<IfaceMethod,Map<Object,CallBase>> method_map;
private Map<IfaceMethod,CallBase> proto_map;

private Map<IfaceMethod,CallSpecial> special_methods;
private Map<String,List<CallSpecial>> call_methods;
private Map<String,AllocMode> alloc_map;
private Set<String> load_classes;


private final static Object DEFAULT_OBJECT = new Object();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CallFactory(IfaceControl fc)
{
   fait_control = fc;
   method_map = new HashMap<>();
   proto_map = new HashMap<>();
   special_methods = new HashMap<>();
   call_methods = new HashMap<>();
   alloc_map = new HashMap<>();
   load_classes = new HashSet<>();
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

public IfaceCall findCall(IfaceProgramPoint pt,IfaceMethod fm,List<IfaceValue> args,IfaceSafetyStatus sts,InlineType inline)
{
   Object key = null;

   if (inline == InlineType.SPECIAL) {
      key = fait_control.getCallSpecial(pt,fm);
    }
   else if (args == null || args.size() == 0) key = DEFAULT_OBJECT;
   else if (fm.isStatic()) key = DEFAULT_OBJECT;
   else {
      IfaceValue fv = args.get(0);
      switch (inline) {
	 case NONE :
	    key = DEFAULT_OBJECT;
	    break;
	 case DEFAULT :
	    key = getSourceKey(fv);
	    break;
	 case THIS :
	    key = fv;
	    break;
	 case SOURCES :
	    if (args.size() == 1) key = getSourceKey(fv);
	    else {
	       List<Object> kl = new ArrayList<Object>();
	       for (IfaceValue vb : args) {
		  kl.add(getSourceKey(vb));
		}
	       key = kl;
	     }
	    break;
	 case VALUES :
	    if (args.size() == 1) key = getSourceKey(fv);
	    else {
	       List<Object> kl = new ArrayList<Object>();
	       for (IfaceValue vb : args) kl.add(vb);
	       key = kl;
	     }
	    break;
       }
    }
   
   Map<Object,CallBase> mm;
   synchronized (method_map) {
      mm = method_map.get(fm);
      if (mm == null) {
	 mm = new HashMap<>(4);
	 method_map.put(fm,mm);
       }
    }
   
   CallBase cm;
   synchronized (mm) {
      cm = mm.get(key);
      if (cm == null) {
	 if (inline == InlineType.THIS && key instanceof IfaceValue) {
	    for (Object o : mm.keySet()) {
	       if (o instanceof IfaceValue) {
		  if (matchInlineValues((IfaceValue) key,(IfaceValue) o)) {
		     cm = mm.get(o);
		     mm.put(key,cm);
		     break;
		   }
		}
	     }
	  }
       }
      if (cm == null) {
	 cm = new CallBase(fait_control,fm,pt,sts);
	 mm.put(key,cm);
       }
      else {
         cm = cm.getAlternateCall(sts,pt);
       }
    }

   return cm;
}



private Object getSourceKey(IfaceValue iv)
{
   IfaceEntitySet es = iv.getModelEntitySet();
   if (es != null) return es;

   return DEFAULT_OBJECT;
}



private boolean matchInlineValues(IfaceValue v1,IfaceValue v2)
{
   if (v1.getDataType() != v2.getDataType()) return false;
   IfaceValue v3 = v1.mergeValue(v2);
   if (v3 == v1 || v3 == v2) return true;

   IfaceEntity e1 = null;
   for (IfaceEntity ie : v1.getEntities()) {
      if (e1 == null) e1 = ie;
      else return false;
    }
   IfaceEntity e2 = null;
   for (IfaceEntity ie : v2.getEntities()) {
      if (e2 == null) e2 = ie;
      else return false;
    }
   if (e1.getLocation() == null || e2.getLocation() == null) return false;
   if (e1.getLocation().sameBaseLocation(e2.getLocation())) return true;
   return false;
}



/********************************************************************************/
/*										*/
/*	Prototype method management						*/
/*										*/
/********************************************************************************/

public IfaceCall findPrototypeMethod(IfaceProgramPoint pt,IfaceMethod fm)
{
   synchronized (proto_map) {
      CallBase cb = proto_map.get(fm);
      if (cb == null) {
	 cb = new CallBase(fait_control,fm,pt,null);
	 cb.setPrototype();
	 proto_map.put(fm,cb);
       }
      return cb;
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Collection<IfaceCall> getAllCalls(IfaceMethod fm)
{
   Map<Object,CallBase> mm;
   synchronized (method_map) {
      mm = method_map.get(fm);
    }
   if (mm == null) return Collections.emptyList();

   synchronized (mm) {
      return new ArrayList<IfaceCall>(mm.values());
    }
}


public Collection<IfaceCall> getAllCalls()
{
   Collection<IfaceCall> rslt = new HashSet<IfaceCall>();

   synchronized (method_map) {
      for (Map<Object,CallBase> mm : method_map.values()) {
         for (CallBase cb : mm.values()) {
            rslt.addAll(cb.getAlternateCalls());
          }
       }
    }

   return rslt;
}


public void removeCalls(Collection<IfaceCall> calls)
{
   synchronized (method_map) {
      for (IfaceCall call : calls) {
         IfaceMethod im = call.getMethod();
         Map<Object,CallBase> mthds = method_map.get(im);
         if (mthds == null) continue;
         for (Iterator<CallBase> it = mthds.values().iterator(); it.hasNext(); ) {
            CallBase cb = it.next();
            if (cb == call) it.remove();
          }
         if (mthds.isEmpty()) method_map.remove(im);
         if (proto_map.get(im) == call) proto_map.remove(im);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Checks for special method treatment					*/
/*										*/
/********************************************************************************/

public boolean addSpecialFile(File f)
{
   FaitLog.logI("Adding special file " + f);
   
   Element e = IvyXml.loadXmlFromFile(f);
   if (e == null) {
      FaitLog.logE("Problem loading special file " + f);
      return false;
    }
   
   addSpecialFile(e);
   
   return true;
}



public void addSpecialFile(Element xml)
{
   if (xml == null) return;
   for (Element n : IvyXml.children(xml,"PACKAGE")) {
      String pnam = IvyXml.getAttrString(n,"NAME");
      if (!pnam.endsWith(".")) pnam += ".";
      addSpecial(pnam,new CallSpecial(fait_control,n,false));
    }
   for (Element n : IvyXml.children(xml,"CLASS")) {
      String cnam = IvyXml.getAttrString(n,"NAME");
      if (!cnam.endsWith(".")) cnam += ".";
      addSpecial(cnam,new CallSpecial(fait_control,n,false));
    }
   for (Element n : IvyXml.children(xml,"METHOD")) {
      String mnam = IvyXml.getAttrString(n,"NAME");
      String msig = IvyXml.getAttrString(n,"SIGNATURE");
      if (msig != null) mnam += msig;
      addSpecial(mnam,new CallSpecial(fait_control,n,true));
    }
   for (Element n : IvyXml.children(xml,"ALLOC")) {
      addAllocation(n);
    }
   for (Element n : IvyXml.children(xml,"LOAD")) {
      addLoad(n);
    }
}



private void addSpecial(String nm,CallSpecial cs)
{
   List<CallSpecial> lcs = call_methods.get(nm);
   if (lcs == null) {
      lcs = new ArrayList<>();
      call_methods.put(nm,lcs);
    }
   if (cs.match(null)) lcs.add(cs);
   else lcs.add(0,cs);
}


private void addAllocation(Element xml)
{
   String nm = IvyXml.getAttrString(xml,"CLASS");
   if (nm == null) return;
   
   boolean ast = IvyXml.getAttrBool(xml,"AST");
   boolean inherit = IvyXml.getAttrBool(xml,"INHERIT");
   boolean local = IvyXml.getAttrBool(xml,"LOCAL");
   AllocMode am = AllocMode.BINARY;
   if (local) am = AllocMode.LOCAL;
   else if (ast) {
      if (inherit) am = AllocMode.AST_INHERIT;
      else am = AllocMode.AST;
    }
   else if (inherit) am = AllocMode.BINARY_INHERIT;
   alloc_map.put(nm,am);
}



private void addLoad(Element xml)
{
   String nm = IvyXml.getAttrString(xml,"CLASS");
   if (nm == null) nm = IvyXml.getAttrString(xml,"NAME");
   if (nm == null) return;
   load_classes.add(nm);
}



public IfaceSpecial getSpecial(IfaceProgramPoint pt,IfaceMethod fm)
{
   CallSpecial cs = null;
   synchronized (special_methods) {
      cs = special_methods.get(fm);
      if (!special_methods.containsKey(fm)) {
         String fnm = fm.getDeclaringClass().getName() + "." + fm.getName();
         boolean usematch = false;
         List<CallSpecial> lcs = null;
         lcs = call_methods.get(fnm + fm.getDescription());
         cs = findSpecial(lcs,pt);
         if (lcs != null && (cs == null || lcs.size() > 1)) usematch = true;
         if (cs == null) {
            lcs = call_methods.get(fnm);
            cs = findSpecial(lcs,pt);
            if (lcs != null && (cs == null || lcs.size() > 1)) usematch = true;
          }        
         if (cs == null && !fm.getName().equals("<clinit>")) {
            String s = fnm;
            int ln = s.length();
            for ( ; ; ) {
               int idx = s.lastIndexOf(".",ln);
               if (idx < 0) break;
               s = s.substring(0,idx+1);
               ln = idx-1;
               lcs = call_methods.get(s);
               cs = findSpecial(lcs,pt);
               if (lcs != null && (cs == null || lcs.size() > 1)) usematch = true; 
               if (cs != null) break;
             }
          }
         if (!usematch) {
            if (cs == null || cs.match(null)) special_methods.put(fm,cs);
          }
       }
    }

   return cs;
}


private CallSpecial findSpecial(List<CallSpecial> lcs,IfaceProgramPoint pt)
{
   if (lcs == null) return null;
   for (CallSpecial cs : lcs) {
      if (cs.match(pt)) return cs;
    }
   return null;
}

public IfaceSpecial getSpecial(IfaceProgramPoint pt,IfaceCall fc)
{
   return getSpecial(pt,fc.getMethod());
}


public boolean isSingleAllocation(IfaceType typ,boolean fromast)
{
   IfaceBaseType bt = typ.getJavaType();
   AllocMode am = alloc_map.get(bt.getName());
   if (am == null) {
      for (IfaceBaseType nbt = bt.getSuperType(); nbt != null; nbt = nbt.getSuperType()) {
         am = alloc_map.get(nbt.getName());
         if (am != null) {
            switch (am) {
               case AST_INHERIT :
               case BINARY_INHERIT :
                  break;
               default :
                  am = AllocMode.LOCAL;
                  break;
             }
            break;
          }
       }
      if (am == null) am = AllocMode.LOCAL;
      alloc_map.put(bt.getName(),am);
    }
   
   switch (am) {
      default :
      case LOCAL :
         return false;
      case AST_INHERIT :
      case AST :
         return true;
      case BINARY_INHERIT :
      case BINARY :
         if (fromast) return false;
         return true;
    }
}



public void clearSpecial(IfaceMethod im)
{
   special_methods.put(im,null);
}


public void clearAllSpecials()
{
   special_methods.clear();
   call_methods.clear();
}


public boolean canBeCallback(IfaceProgramPoint pt,IfaceMethod fm)
{
   IfaceSpecial is = getSpecial(pt,fm);
   if (is == null) return false;
   
   return is.getCallbackId() != null;
}

public String getCallbackStart(IfaceProgramPoint pt,IfaceMethod fm)
{
   IfaceSpecial is = getSpecial(pt,fm);
   if (is != null && is.getCallbacks() == null) return is.getCallbackId();

   return null;
}




public boolean canBeReplaced(IfaceProgramPoint pt,IfaceMethod fm)
{
   IfaceSpecial is = getSpecial(pt,fm);
   if (is != null) return is.getReplaceName() != null;

   return false;
}



public Collection<String> getDefaultClasses()
{
   return load_classes;
}



}	// end of class CallFactory




/* end of CallFactory.java */

