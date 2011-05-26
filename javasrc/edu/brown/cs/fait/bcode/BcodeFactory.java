/********************************************************************************/
/*										*/
/*		BcodeFactory.java						*/
/*										*/
/*	Byte code definitions factory						*/
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



package edu.brown.cs.fait.bcode;

import edu.brown.cs.fait.iface.*;

import org.objectweb.asm.*;

import java.util.*;
import java.io.*;
import java.util.jar.*;


public class BcodeFactory implements BcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,FileInfo>	class_map;
private FaitProject		for_project;
private Map<String,BcodeClass>	known_classes;
private Map<String,BcodeMethod> known_methods;
private Map<String,BcodeField>	known_fields;
private Queue<String>		work_list;

private Map<Type,BcodeDataType> static_map;
private Map<String,BcodeDataType> name_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BcodeFactory(FaitControl fc)
{
   class_map = new HashMap<String,FileInfo>();
   known_classes = new HashMap<String,BcodeClass>();
   known_methods = new HashMap<String,BcodeMethod>();
   known_fields = new HashMap<String,BcodeField>();
   static_map = new HashMap<Type,BcodeDataType>();
   name_map = new HashMap<String,BcodeDataType>();

   for_project = null;
   work_list = new LinkedList<String>();

   //TODO: need to get special file to add extra classes that aren't
   // linked directory from start classes
}





/********************************************************************************/
/*										*/
/*	Class setup methods							*/
/*										*/
/********************************************************************************/

public void setProject(FaitProject fp)
{
   for_project = fp;
   class_map.clear();
   work_list.clear();
   setupClassPath();
   setupInitialClasses();
   loadClasses();
}



private void setupClassPath()
{
   String cp = for_project.getClasspath();
   StringTokenizer tok = new StringTokenizer(cp,File.pathSeparator);
   while (tok.hasMoreTokens()) {
      String cpe = tok.nextToken();
      addClassPathEntry(cpe);
    }

   String jh = System.getProperty("java.home");
   File jf = new File(jh);
   File jf1 = new File(jf,"lib");
   File jf2 = new File(jf1,"dt.jar");
   if (!jf2.exists()) jf2 = new File(jf1,"rt.jar");
   addClassPathEntry(jf2.getPath());
}


private void addClassPathEntry(String cpe)
{
   File f = new File(cpe);
   if (!f.exists()) return;
   if (f.isDirectory()) {
      addClassFiles(f,null);
    }
   else {
      try {
	 JarInputStream jis = new JarInputStream(new FileInputStream(f));
	 for ( ; ; ) {
	    JarEntry je = jis.getNextJarEntry();
	    if (je == null) break;
	    String cn = je.getName();
	    String en = cn;
	    if (cn.endsWith(".class")) en = cn.substring(0,cn.length()-6);
	    en = en.replace("/",".");
	    if (!class_map.containsKey(en)) {
	       FileInfo fi = new FileInfo(f,cn);
	       class_map.put(en,fi);
	     }
	  }
	 jis.close();
       }
      catch (IOException e) { }
    }
}


private void addClassFiles(File dir,String pfx)
{
   for (File f : dir.listFiles()) {
      if (f.isDirectory()) {
	 String sfx = f.getName();
	 if (pfx != null) sfx = pfx + "." + sfx;
	 addClassFiles(f,sfx);
       }
      else if (f.getName().endsWith(".class")) {
	 String sfx = f.getName();
	 int idx = sfx.lastIndexOf(".");
	 sfx = sfx.substring(0,idx);
	 if (pfx != null) sfx = pfx + "." + sfx;
	 if (!class_map.containsKey(sfx)) {
	    FileInfo fi = new FileInfo(f);
	    class_map.put(sfx,fi);
	  }
       }
    }
}





/********************************************************************************/
/*										*/
/*	Type access methods							*/
/*										*/
/********************************************************************************/

public BcodeDataType findDataType(String s)
{
   BcodeDataType bdt = null;

   synchronized (static_map) {
      bdt = name_map.get(s);
      if (bdt != null) return bdt;
      Type t = Type.getType(s);
      if (t == null) {
	 noteClass(s);		// try loading dynamically
	 loadClasses();
	 t = Type.getType(s);
       }
      if (t != null) bdt = createDataType(t);
    }

   return bdt;
}


BcodeDataType findObjectType(String s)
{
   if (!s.endsWith(";")) {
      s = "L" + s.replace('.','/') + ";";
    }
   return findDataType(s);
}



public BcodeDataType findDataType(Type t)
{
   BcodeDataType bdt = null;

   synchronized (static_map) {
      bdt = static_map.get(t);
      if (bdt == null) {
	 bdt = createDataType(t);
       }
    }

   return bdt;
}



private BcodeDataType createDataType(Type t)
{
   synchronized (static_map) {
      BcodeDataType bdt = new BcodeDataType(t,this);
      static_map.put(t,bdt);
      name_map.put(t.getDescriptor(),bdt);
      name_map.put(t.getClassName(),bdt);
      return bdt;
    }
}



BcodeClass findClassNode(String nm)
{
   if (nm == null) return null;

   return known_classes.get(nm);
}



/********************************************************************************/
/*										*/
/*	Member access methods							*/
/*										*/
/********************************************************************************/

public BcodeMethod findMethod(String nm,String cls,String mnm,String desc)
{
   if (nm == null) {
      if (desc != null) nm = cls + "." + mnm + desc;
      else nm = cls + "." + mnm;
    }

   synchronized (known_methods) {
      BcodeMethod bm = known_methods.get(nm);
      if (bm == null) {
	 if (cls == null) {
	    int idx0 = nm.indexOf("(");
	    int idx1 = 0;
	    if (idx0 < 0) idx1 = nm.lastIndexOf(".");
	    else idx1 = nm.lastIndexOf(".",idx0);
	    cls = nm.substring(0,idx1);
	    if (idx0 >= 0) {
	       mnm = nm.substring(idx1+1,idx0);
	       desc = nm.substring(idx0);
	     }
	    else {
	       mnm = nm.substring(idx1+1);
	       desc = null;
	     }
	  }
	 BcodeClass bc = known_classes.get(cls);
	 if (bc == null) return null;
	 bm = bc.findMethod(mnm,desc);
	 known_methods.put(nm,bm);
       }
      return bm;
    }
}



public BcodeMethod findInheritedMethod(String cls,String nm,String desc)
{
   BcodeClass bc = known_classes.get(cls);
   if (bc == null) return null;
   
   List<FaitMethod> rslt = new ArrayList<FaitMethod>();
   bc.findParentMethods(nm,desc,true,true,rslt);
   
   if (rslt.isEmpty()) return null;
   
   return (BcodeMethod) rslt.get(0);
}
   
      



public List<FaitMethod> findStaticInitializers(String cls)
{
   synchronized (known_methods) {
      BcodeClass bc = known_classes.get(cls);
      if (bc == null) return null;
      return bc.findStaticInitializers();
    }
}

public BcodeField findField(String nm,String cls,String fnm)
{
   if (nm == null) nm = cls + "." + fnm;

   synchronized (known_fields) {
      BcodeField bf = known_fields.get(nm);
      if (bf == null) {
	 if (cls == null) {
	    int idx = nm.lastIndexOf(".");
	    cls = nm.substring(0,idx);
	    fnm = nm.substring(idx+1);
	  }
	 BcodeClass bc = known_classes.get(cls);
	 if (bc != null) bf = bc.findField(fnm);
	 if (bf != null) known_fields.put(nm,bf);
       }
      return bf;
    }
}





boolean isProjectClass(String nm)
{
   return for_project.isProjectClass(nm);
}



/********************************************************************************/
/*										*/
/*	Get classes needed for the project and redirection			*/
/*										*/
/********************************************************************************/

private void setupInitialClasses()
{
   work_list.addAll(for_project.getBaseClasses());
}



/********************************************************************************/
/*										*/
/*	Class loading methods							*/
/*										*/
/********************************************************************************/

private synchronized void loadClasses()
{
   while (!work_list.isEmpty()) {
      String c = work_list.remove();
      if (known_classes.get(c) != null) continue;
      FileInfo fi = class_map.get(c);
      if (fi == null) {
	 System.err.println("BCODE: Can't find class " + c);
	 continue;
       }
      InputStream ins = fi.getInputStream();
      if (ins == null) {
	 System.err.println("BCODE: Can't open file for class " + c);
	 continue;
       }

      try {
	 boolean pcls = for_project.isProjectClass(c);
	 BcodeClass bc = new BcodeClass(this,pcls);
	 known_classes.put(c,bc);
	 ClassReader cr = new ClassReader(ins);
	 cr.accept(bc,0);
	 ins.close();
	 // System.err.println("BCODE: Read class " + c);
       }
      catch (IOException e) {
	 System.err.println("BCODE: Problem reading class " + c);
       }
    }
}




void noteType(String desc)
{
   if (desc.startsWith("L") && desc.endsWith(";")) {
      String nm = desc.substring(1,desc.length()-1);
      noteClass(nm);
    }
   else if (desc.startsWith("[")) {
      noteType(desc.substring(1));
    }
}




void noteClass(String nm)
{
   nm = nm.replace("/",".");
   if (nm.endsWith(";") && nm.startsWith("L")) {
      nm = nm.substring(1,nm.length()-1);
    }
   if (known_classes.containsKey(nm)) return;
   work_list.add(nm);
   known_classes.put(nm,null);
}






/********************************************************************************/
/*										*/
/*	FileInfo -- information about a class file				*/
/*										*/
/********************************************************************************/

private static class FileInfo {

   private File base_file;
   private String jar_item;

   FileInfo(File f) {
      base_file = f;
      jar_item = null;
    }

   FileInfo(File jar,String itm) {
      base_file = jar;
      jar_item = itm;
    }

   InputStream getInputStream() {
      if (jar_item == null) {
	 try {
	    FileInputStream ins = new FileInputStream(base_file);
	    return ins;
	  }
	 catch (IOException e) { }
       }
      else {
	 try {
	    FileInputStream ins = new FileInputStream(base_file);
	    JarInputStream jins = new JarInputStream(ins);
	    for ( ; ; ) {
	       JarEntry je = jins.getNextJarEntry();
	       if (je == null) break;
	       if (je.getName().equals(jar_item)) {
		  return jins;
		}
	     }
	    ins.close();
	  }
	 catch (IOException e) { }
       }
      return null;
    }

}	// end of inner class FileInfo


}	// end of class BcodeFactory




/* end of BcodeFactory.java */

