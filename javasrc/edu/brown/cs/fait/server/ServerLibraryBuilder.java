/********************************************************************************/
/*                                                                              */
/*              ServerLibraryBuilder.java                                       */
/*                                                                              */
/*      Build library description files automatically                           */
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



package edu.brown.cs.fait.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProject;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcode.JcodeClass;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcode.JcodeMethod;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class ServerLibraryBuilder implements ServerConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   ServerLibraryBuilder slb = new ServerLibraryBuilder(args);
   
   slb.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<String> work_queue;
private IfaceProject    fait_project;
private JcodeFactory    jcode_factory;
private String          class_path;
private int             num_threads;
private IvyXmlWriter    output_stream;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ServerLibraryBuilder(String [] args)
{
   work_queue = new ArrayList<>();
   jcode_factory = null;
   fait_project = null;
   num_threads = 1;
   
   class_path = null;
   
   scanArgs(args);
}


/********************************************************************************/
/*                                                                              */
/*      Argument processing methods                                             */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if ((args[i].startsWith("-cp") || args[i].startsWith("-classpath")) &&
               i+1 < args.length) {
            if (class_path == null) class_path = args[++i];
            else class_path += File.pathSeparator + args[++i];
          }
       }
    }
   if (class_path == null) {
      class_path = System.getProperty("java.class.path");
    }
   
   fait_project = IfaceControl.Factory.createSimpleProject(class_path,"$$$");
   jcode_factory = fait_project.getJcodeFactory();
   output_stream = null;
   
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if ((args[i].startsWith("-cp") || args[i].startsWith("-classpath")) &&
               i+1 < args.length) {
            ++i;
          }
         else if (args[i].startsWith("-T")) {
            FaitLog.setTracing(true);
          }
         else if (args[i].startsWith("-D")) {
            FaitLog.setLogLevel(FaitLog.LogLevel.DEBUG);
          }
         else if (args[i].startsWith("-L") && i+1 < args.length) {      
            FaitLog.setLogFile(new File(args[++i]));
          }
         else if (args[i].startsWith("-j") && i+1 < args.length) {
            addJarFile(args[++i]);
          }
         else if (args[i].startsWith("-c") && i+1 < args.length) {
            addClass(args[++i],true);
          }
         else if (args[i].startsWith("-p") && i+1 < args.length) {
            addPackage(args[++i]);
          }
         else if (args[i].startsWith("-m") && i+1 < args.length) {
            addMethod(args[++i]);
          }
         else if (args[i].startsWith("-o") && i+1 < args.length) {
            try {
               FileWriter ots = new FileWriter(args[++i]); 
               output_stream = new IvyXmlWriter(ots);
             }
            catch (IOException e) {
               badArgs();
             }
          }
         else badArgs();
       }
      else {
         addAny(args[i]);
       }
    }
   
   if (output_stream == null) {
      try {
         FileWriter ots = new FileWriter("library.fait");
         output_stream = new IvyXmlWriter(ots);
       }
      catch (IOException e) {
         badArgs();
       }
    }
   
   fait_project = null;
   jcode_factory = null;
}



private void badArgs()
{
   System.err.println("faitlibrarybuilder [-cp classpath] [-c class] [-p package] [-m method] [-o output]");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void addJarFile(String jar)
{
   jcode_factory.addToClassPath(jar);
   
   try {
      JarFile jf = new JarFile(jar);
      for (Enumeration<JarEntry> en = jf.entries(); en.hasMoreElements(); ) {
         JarEntry je = en.nextElement();
         String nm = je.getName();
         if (nm.endsWith(".class")) {
            int idx = nm.lastIndexOf(".class");
            nm = nm.substring(0,idx);
            nm = nm.replace("/",".");
            nm = nm.replace("$",".");
            addClass(nm,false);
          }
       }
      jf.close();
    }
   catch (IOException e) {
      System.err.println("Couldn't load jar file: "  + jar);
    }
}




private void addPackage(String pnm)
{
   if (!pnm.endsWith(".")) pnm += ".";
   for (JcodeClass cls : jcode_factory.getAllPossibleClasses(new PackageFilter(pnm))) {
      if (pnm.startsWith(pnm)) {
         addClass(cls,false);
       }
    } 
}



private static class PackageFilter implements Predicate<String> {
   
   private String package_name;
   
   PackageFilter(String p) {
      package_name = p;
    }
   
   @Override public boolean test(String s) {
      if (s.startsWith(package_name)) return true;
      return false;
    }
}


private void addClass(String cnm,boolean user)
{
   JcodeClass cls = jcode_factory.findClass(cnm);
   if (cls == null) {
      System.err.println("Couldn't load class: " + cnm);
      return;
    }
   addClass(cls,user);
}



private void addClass(JcodeClass cls,boolean user) 
{
   if (!cls.isPublic()) {
      if (user) System.err.println("Class " + cls.getName() + " is not public");
      return;
    }
   for (JcodeMethod jm : cls.getMethods()) {
      if (jm.isPublic() && !jm.isConstructor()) {
         String key = jm.getDeclaringClass().getName();
         key += "@" + jm.getName();
         key += "@" + jm.getDescription();
         work_queue.add(key);
       }
    }
}


private void addMethod(String mnm)
{
   int idx = mnm.lastIndexOf(".");
   if (idx < 0) {
      System.err.println("Must provide full method name: " + mnm);
      return;
    }
   String cnm = mnm.substring(0,idx);
   mnm = mnm.substring(idx+1);
   JcodeClass cls = jcode_factory.findClass(cnm);
   if (cls == null) {
      System.err.println("Couldn't find class: " + cnm);
      return;
    }
   
   for (JcodeMethod jm : cls.findAllMethods(mnm,null)) {
      String key = jm.getDeclaringClass().getName();
      key += "@" + jm.getName();
      key += "@" + jm.getDescription();
      work_queue.add(key);
    }
}


private void addAny(String nm)
{
   boolean fnd = false;
   for (JcodeClass cls : jcode_factory.getAllPossibleClasses(nm)) {
      String cnm = cls.getName();
      if (cnm.startsWith(nm)) {
         addClass(cls,false);
         fnd = true;
       }
    } 
   if (!fnd) addMethod(nm);
}




/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   output_stream.begin("FAIT");
   
   while (!work_queue.isEmpty()) {
      String jm = work_queue.remove(0);
      System.err.println("WORK_ON: " + jm);
      String [] cnts = jm.split("@");
      fait_project = IfaceControl.Factory.createSimpleProject(class_path,"$$$");
      jcode_factory = fait_project.getJcodeFactory();
      IfaceControl control = IfaceControl.Factory.createControl(fait_project);
      IfaceMethod im = control.findMethod(cnts[0],cnts[1],cnts[2]);
      if (!shouldIgnore(control,im)) {
         control.analyze(im,num_threads,ReportOption.NONE);
         for (IfaceCall cc : control.getAllCalls(im)) {
            outputCall(cc);
            System.err.println("RESULT: " + cc);
            System.err.println("EXCEPTIONS: " + cc.getExceptionValue());
          }
       }
     
      control.clearAll();
      jcode_factory.shutDown();
      jcode_factory = null;
      fait_project = null;
    }
   
   output_stream.end("FAIT");
   output_stream.close();
}



private boolean shouldIgnore(IfaceControl ctrl,IfaceMethod im)
{
   IfaceType ctyp = im.getDeclaringClass();
   IfacePrototype ptyp = ctrl.createPrototype(ctyp);
   if (ptyp != null) 
      return true;
   
   // check if there is a method-specific special for this method already, and ignore if so.
   
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

private void outputCall(IfaceCall cc)
{
   output_stream.begin("METHOD");
   output_stream.field("NAME",cc.getMethod().getFullName());
   output_stream.field("SIGNATURE",cc.getMethod().getDescription());
   IfaceValue cv = cc.getResultValue();
   if (cv != null && !cv.getDataType().isVoidType()) {
      if (cv.isMutable()) output_stream.field("MUTABLE",true);
      if (cv.mustBeNull()) output_stream.field("MUSTBENULL",true);
      if (cv.canBeNull()) output_stream.field("CANBENULL",true);
      IfaceType dt = cv.getDataType();
      String retname = dt.getName();
      List<String> annots = dt.getAnnotations();
      if (annots != null) {
         StringBuffer buf = new StringBuffer();
         for (String s : annots) {
            if (buf.length() > 0) buf.append(",");
            buf.append(s);
          }
         output_stream.field("ANNOTATIONS",buf.toString());
       }
      if (cv.getIndexValue() != null) {
         output_stream.field("VALUE",cv.getIndexValue().intValue());
       }
      else if (cv.getMinValue() != null || cv.getMaxValue() != null) {
         if (cv.getMinValue() != null) output_stream.field("MINVALUE",cv.getMinValue());
         if (cv.getMaxValue() != null) output_stream.field("MAXVALUE",cv.getMaxValue());
       }
      if (!cc.getMethod().isStatic() && !cc.getMethod().isConstructor()) {
         IfaceValue varg = null;
         for (IfaceValue cv0 : cc.getParameterValues()) {
            varg = cv0;
            break;
          }
         if (varg != null && varg == cv)
            retname = "0";
       }
      output_stream.field("RETURN",retname);
    }
   else if (cv == null) {
      output_stream.field("NORETURN",true);
    }
   IfaceValue ev = cc.getExceptionValue();
   if (ev != null) {
      Set<String> typs = new HashSet<>();
      for (IfaceEntity ie : ev.getEntities()) {
         IfaceType dt = ie.getDataType();
         typs.add(dt.getName());
       }
      if (typs.size() > 0) {
         StringBuffer buf = new StringBuffer();
         for (String s : typs) {
            if (buf.length() > 0) buf.append(" ");
            buf.append(s);
          }
         output_stream.field("THROWS",buf.toString());
       }
    }
   output_stream.end("METHOD");
}





}       // end of class ServerLibraryBuilder




/* end of ServerLibraryBuilder.java */

