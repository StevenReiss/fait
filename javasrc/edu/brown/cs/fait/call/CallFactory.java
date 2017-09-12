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
import edu.brown.cs.ivy.jcode.JcodeMethod;
import edu.brown.cs.ivy.xml.*;


import java.util.*;
import java.io.*;
import org.w3c.dom.*;


public class CallFactory implements CallConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl	fait_control;
private Map<JcodeMethod,Map<Object,CallBase>> method_map;
private Map<JcodeMethod,CallBase> proto_map;

private Map<JcodeMethod,CallSpecial> special_methods;
private Map<String,CallSpecial> call_methods;


private final static Object DEFAULT_OBJECT = new Object();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CallFactory(FaitControl fc)
{
   fait_control = fc;
   method_map = new HashMap<>();
   proto_map = new HashMap<JcodeMethod,CallBase>();
   special_methods = new HashMap<JcodeMethod,CallSpecial>();
   call_methods = new HashMap<String,CallSpecial>();
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

public IfaceCall findCall(JcodeMethod fm,List<IfaceValue> args,InlineType inline)
{
   Object key = null;

   if (args == null || args.size() == 0) key = DEFAULT_OBJECT;
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
	 mm = new HashMap<Object,CallBase>(4);
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
	 cm = new CallBase(fait_control,fm,mm.size());
	 mm.put(key,cm);
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

public IfaceCall findPrototypeMethod(JcodeMethod fm)
{
   synchronized (proto_map) {
      CallBase cb = proto_map.get(fm);
      if (cb == null) {
	 cb = new CallBase(fait_control,fm,0);
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

public Collection<IfaceCall> getAllCalls(JcodeMethod fm)
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
   Collection<IfaceCall> rslt = new ArrayList<IfaceCall>();

   synchronized (method_map) {
      for (Map<Object,CallBase> mm : method_map.values()) {
	 rslt.addAll(mm.values());
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Checks for special method treatment					*/
/*										*/
/********************************************************************************/

public void addSpecialFile(File f)
{
   addSpecialFile(IvyXml.loadXmlFromFile(f));
}



public void addSpecialFile(Element xml)
{
   if (xml == null) return;
   for (Element n : IvyXml.children(xml,"PACKAGE")) {
      String pnam = IvyXml.getAttrString(n,"NAME");
      if (!pnam.endsWith(".")) pnam += ".";
      call_methods.put(pnam,new CallSpecial(fait_control,n,false));
    }
   for (Element n : IvyXml.children(xml,"CLASS")) {
      String cnam = IvyXml.getAttrString(n,"NAME");
      if (!cnam.endsWith(".")) cnam += ".";
      call_methods.put(cnam,new CallSpecial(fait_control,n,false));
    }
   for (Element n : IvyXml.children(xml,"METHOD")) {
      String mnam = IvyXml.getAttrString(n,"NAME");
      String msig = IvyXml.getAttrString(n,"SIGNATURE");
      if (msig != null) mnam += msig;
      call_methods.put(mnam,new CallSpecial(fait_control,n,true));
    }
}


public IfaceSpecial getSpecial(JcodeMethod fm)
{
   CallSpecial cs = null;
   synchronized (special_methods) {
      if (special_methods.containsKey(fm)) return special_methods.get(fm);
      String fnm = fm.getDeclaringClass().getName() + "." + fm.getName();
         
      cs = call_methods.get(fnm + fm.getDescription());
      if (cs == null) cs = call_methods.get(fnm);
      if (cs == null) {
	 String s = fnm;
	 int ln = s.length();
	 for ( ; ; ) {
	    int idx = s.lastIndexOf(".",ln);
	    if (idx < 0) break;
	    s = s.substring(0,idx+1);
	    ln = idx-1;
	    cs = call_methods.get(s);
	    if (cs != null) break;
	  }
       }
      special_methods.put(fm,cs);
    }

   return cs;
}

public IfaceSpecial getSpecial(IfaceCall fc)
{
   return getSpecial(fc.getMethod());
}



public boolean canBeCallback(JcodeMethod fm)
{
   IfaceSpecial is = getSpecial(fm);

   return is.getCallbackId() != null;
}

public String getCallbackStart(JcodeMethod fm)
{
   IfaceSpecial is = getSpecial(fm);
   if (is != null && is.getCallbacks() == null) return is.getCallbackId();

   return null;
}

public boolean getIsArrayCopy(JcodeMethod fm)
{
   IfaceSpecial is = getSpecial(fm);
   if (is != null) return is.getIsArrayCopy();

   return false;
}


public boolean canBeReplaced(JcodeMethod fm)
{
   IfaceSpecial is = getSpecial(fm);
   if (is != null) return is.getReplaceName() != null;

   return false;
}


}	// end of class CallFactory




/* end of CallFactory.java */

