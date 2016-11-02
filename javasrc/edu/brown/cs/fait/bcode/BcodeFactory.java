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

import edu.brown.cs.ivy.xml.*;

import org.objectweb.asm.*;
import org.w3c.dom.*;

import java.util.*;
import java.io.*;
import java.util.jar.*;
import java.util.concurrent.*;


public class BcodeFactory implements BcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl		fait_control;
private Map<String,FileInfo>	class_map;
private FaitProject		for_project;
private Map<String,BcodeClass>	known_classes;
private Map<String,BcodeMethod> known_methods;
private Map<String,BcodeField>	known_fields;
private Queue<String>		work_list;
private LoadExecutor		work_holder;

private Map<Type,BcodeDataType> static_map;
private Map<String,BcodeDataType> name_map;

private int			num_threads;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BcodeFactory(FaitControl fc,int nth)
{
   fait_control = fc;
   class_map = new HashMap<String,FileInfo>();
   known_classes = new HashMap<String,BcodeClass>();
   known_methods = new HashMap<String,BcodeMethod>();
   known_fields = new HashMap<String,BcodeField>();
   static_map = new HashMap<Type,BcodeDataType>();
   name_map = new HashMap<String,BcodeDataType>();

   for_project = null;
   work_list = new LinkedList<String>();
   num_threads = nth;

   name_map.put("Z",new BcodeDataType(Type.BOOLEAN_TYPE,this));
   name_map.put("B",new BcodeDataType(Type.BYTE_TYPE,this));
   name_map.put("C",new BcodeDataType(Type.CHAR_TYPE,this));
   name_map.put("D",new BcodeDataType(Type.DOUBLE_TYPE,this));
   name_map.put("F",new BcodeDataType(Type.FLOAT_TYPE,this));
   name_map.put("I",new BcodeDataType(Type.INT_TYPE,this));
   name_map.put("L",new BcodeDataType(Type.LONG_TYPE,this));
   name_map.put("S",new BcodeDataType(Type.SHORT_TYPE,this));
   name_map.put("V",new BcodeDataType(Type.VOID_TYPE,this));
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

FaitControl getControl()			{ return fait_control; }


	

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
   loadClassesThreaded(num_threads);
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
   File jf3 = new File(jf,"jre");
   if (jf3.exists()) {
      File jf4 = new File(jf,"lib");
      if (jf4.exists()) jf1 = jf4;
   }

   addJavaJars(jf1);
}


private void addJavaJars(File f)
{
   for (File f1 : f.listFiles()) {
      if (f1.isDirectory()) addJavaJars(f1);
      else if (f1.getPath().endsWith(".jar")) addClassPathEntry(f1.getPath());
   }
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
	 FileInputStream fis = new FileInputStream(f);
	 JarInputStream jis = new JarInputStream(fis);
	 for ( ; ; ) {
	    JarEntry je = jis.getNextJarEntry();
	    if (je == null) break;
	    String cn = je.getName();
	    String en = cn;
	    if (cn.endsWith(".class")) en = cn.substring(0,cn.length()-6);
	    else continue;
	    en = en.replace("/",".");
	    if (!class_map.containsKey(en)) {
	       int sz = (int) je.getSize();
	       byte [] buf = null;
	       if (sz > 0) {
		  buf = new byte[sz];
		  int ln = 0;
		  while (ln < sz) {
		     int ct = jis.read(buf,ln,sz-ln);
		     ln += ct;
		   }
		}
	       FileInfo fi = new FileInfo(f,cn,buf);
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
   if (dir == null || !dir.isDirectory() || dir.listFiles() == null) {
      return;
    }

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
      if (t != null) bdt = createDataType(t);
    }

   return bdt;
}


public BcodeDataType findClassType(String s)
{
   if (!s.endsWith(";") && !s.startsWith("[")) {
      s = "L" + s.replace('.','/') + ";";
    }

   return findDataType(s);
}


BcodeDataType findDataType(Type t)
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


public Iterable<FaitMethod> findAllMethods(FaitDataType cls,String mnm,String desc)
{
   BcodeClass fc = known_classes.get(cls.getName());
   return fc.findAllMethods(mnm,desc);
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



public BcodeField findInheritedField(String cls,String fnm)
{
   BcodeClass bc = known_classes.get(cls);
   if (bc == null) return null;

   return bc.findInheritedField(fnm);
}



public Collection<FaitMethod> getStartMethods()
{
   Collection<String> snames = for_project.getStartClasses();
   if (snames == null) snames = for_project.getBaseClasses();

   Collection<FaitMethod> rslt = new HashSet<FaitMethod>();

   for (String s : snames) {
      FaitMethod fm = findMethod(null,s,"main","([Ljava/lang/String;)V");
      if (fm != null) rslt.add(fm);
    }

   return rslt;
}





boolean isProjectClass(String nm)
{
   if (for_project == null) return false;

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

   if (fait_control != null)
      addDescriptionClasses(IvyXml.loadXmlFromFile(fait_control.getDescriptionFile()));

   if (for_project.getDescriptionFile() != null) {
      for (File f : for_project.getDescriptionFile()) {
	 addDescriptionClasses(IvyXml.loadXmlFromFile(f));
       }
    }
}


private void addDescriptionClasses(Element e)
{
   if (e == null) return;

   for (Element me : IvyXml.children(e,"METHOD")) {
      String s = IvyXml.getAttrString(me,"RETURN");
      if (s != null && !s.equals("0")) {
	 while (s.endsWith("[]")) s = s.substring(0,s.length()-2);
	 work_list.add(s);
       }
    }

   for (Element le : IvyXml.children(e,"LOAD")) {
      String s = IvyXml.getAttrString(le,"CLASS");
      if (le != null) work_list.add(s);
    }
}




/********************************************************************************/
/*										*/
/*	Class loading methods							*/
/*										*/
/********************************************************************************/

private synchronized void loadClassesThreaded(int nth)
{
   work_holder = new LoadExecutor(nth);
   for (String s : work_list) work_holder.workOnClass(s);
   work_list.clear();
   synchronized (work_holder) {
      try {
	 work_holder.wait(1000);
       }
      catch (InterruptedException e) { }
      while (!work_holder.isDone()) {
	 try {
	    work_holder.wait(10000);
	  }
	 catch (InterruptedException e) { }
       }
    }
   work_holder.shutdown();
   work_holder = null;
}




private class LoadExecutor extends ThreadPoolExecutor {

   private int num_active;
   private ConcurrentMap<String,Object> work_items;


   LoadExecutor(int nth) {
      super(nth,nth,10,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());
      num_active = 0;
      work_items = new ConcurrentHashMap<String,Object>();
    }

   void workOnClass(String c) {
      if (work_items.putIfAbsent(c,Boolean.TRUE) != null) return;
   
      LoadTask task = new LoadTask(c);
      execute(task);
    }


   @Override synchronized protected void beforeExecute(Thread t,Runnable r) {
      ++num_active;
    }

   @Override synchronized protected void afterExecute(Runnable r,Throwable t) {
      --num_active;
      if (num_active == 0 && getQueue().size() == 0) {
	 notifyAll();
       }
    }

   synchronized boolean isDone() {
      return num_active == 0 && getQueue().size() == 0;
    }

}	// end of class LoadExecutor




private class LoadTask implements Runnable {

   private String load_class;

   LoadTask(String c) {
      load_class = c;
    }

   @Override public void run() {
      FileInfo fi = class_map.get(load_class);
      if (fi == null) {
         IfaceLog.logI("Can't find class " + load_class);
         return;
       }
      IfaceLog.logD("Load class " + load_class);
      InputStream ins = fi.getInputStream();
      if (ins == null) {
         IfaceLog.logE("Can't open file for class " + load_class);
         return;
       }
   
      try {
         boolean pcls = for_project.isProjectClass(load_class);
         BcodeClass bc = null;
         synchronized (known_classes) {
            if (known_classes.get(load_class) == null) {
               bc = new BcodeClass(BcodeFactory.this,pcls);
               known_classes.put(load_class,bc);
               String c1 = load_class.replace('.','/');
               known_classes.put(c1,bc);
               c1 = "L" + c1 + ";";
               known_classes.put(c1,bc);
             }
          }
   
         if (bc != null) {
            ClassReader cr = new ClassReader(ins);
            cr.accept(bc,0);
          }
   
         ins.close();
       }
      catch (IOException e) {
         System.err.println("BCODE: Problem reading class " + load_class);
       }
    }

}	// end of inner class LoadTask








void noteType(String desc)
{
   if (desc.startsWith("L") && desc.endsWith(";")) {
      String nm = desc.substring(1,desc.length()-1);
      nm = nm.replace('/','.');
      noteClass(nm);
    }
   else if (desc.startsWith("[")) {
      noteType(desc.substring(1));
    }
   else if (desc.startsWith("(")) {
      for (Type t : Type.getArgumentTypes(desc)) {
	 switch (t.getSort()) {
	    case Type.ARRAY :
	    case Type.OBJECT :
	       noteType(t.getDescriptor());
	       break;
	  }
       }
    }
   else if (desc.length() > 1) {
      IfaceLog.logW("Type for load not found: '" + desc + "'");
    }
}




void noteClass(String nm)
{
   nm = nm.replace("/",".");
   if (nm.startsWith("[")) {
      noteType(nm);
      return;
    }

   if (work_holder != null) {
      work_holder.workOnClass(nm);
    }
   else {
      if (known_classes.containsKey(nm)) return;
      work_list.add(nm);
      known_classes.put(nm,null);
    }
}






/********************************************************************************/
/*										*/
/*	FileInfo -- information about a class file				*/
/*										*/
/********************************************************************************/

private static class FileInfo {

   private File base_file;
   private String jar_item;
   private byte [] file_data;

   FileInfo(File f) {
      base_file = f;
      jar_item = null;
      file_data = null;
    }

   FileInfo(File jar,String itm,byte [] data) {
      base_file = jar;
      jar_item = itm;
      file_data = data;
    }

   InputStream getInputStream() {
      if (jar_item == null) {
	 try {
	    FileInputStream ins = new FileInputStream(base_file);
	    return ins;
	  }
	 catch (IOException e) { }
       }
      else if (file_data != null) {
	 ByteStream bs = new ByteStream(this,file_data);
	 return bs;
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
	 catch (IOException e) {
	    System.err.println("FAIT: Problem reading jar file: " + e);
	    e.printStackTrace();
	  }
       }
      return null;
    }

   void clear() {
      file_data = null;
    }

}	// end of inner class FileInfo



private static class ByteStream extends ByteArrayInputStream {

   private FileInfo file_info;

   ByteStream(FileInfo fi,byte [] data) {
      super(data);
      file_info = fi;
    }

   @Override public void close() {
      file_info.clear();
    }

}	// end of inner class ByteStream




}	// end of class BcodeFactory




/* end of BcodeFactory.java */
