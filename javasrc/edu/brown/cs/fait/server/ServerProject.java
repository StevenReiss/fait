/********************************************************************************/
/*										*/
/*		ServerProject.java						*/
/*										*/
/*	Representation of a bubbles project					*/
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



package edu.brown.cs.fait.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceProject;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceUpdateSet;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.control.ControlDescriptionFile;
import edu.brown.cs.fait.iface.FaitException;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.FaitStatistics;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceDescriptionFile;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSemantics;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.project.IvyProjectManager;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;

public class ServerProject implements ServerConstants, IfaceProject
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ServerMain	server_main;
private List<String>	project_names;
private List<String>	class_paths;
private Set<ServerFile> active_files;
private Set<ServerFile> changed_files;
private JcompProject	base_project;
private JcodeFactory	binary_control;
private ReadWriteLock	project_lock;
private Set<IfaceDescriptionFile> description_files;
private Map<String,Boolean> project_packages;
private ServerRunner	current_runner;
private Set<String>	editable_classes;
private List<File>	source_paths;
private List<String>	project_classes;
private boolean 	test_built;
private ServerFile	test_file;

private static final String DEFAULT_PACKAGE = "*DEFAULT*";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ServerProject(ServerMain sm,String name)
{
   server_main = sm;
   project_names = new ArrayList<>();
   project_names.add(name);
   base_project = null;
   class_paths = new ArrayList<>();
   current_runner = null;
   source_paths = new ArrayList<>();
   project_classes = new ArrayList<>();

   active_files = new HashSet<>();
   changed_files = new HashSet<>();
   description_files = new HashSet<>();
   project_packages = new HashMap<>();
   editable_classes = new HashSet<>();

   project_lock = new ReentrantReadWriteLock();

   // compute class path for project
   CommandArgs args = new CommandArgs("PATHS",true,"CLASSES",true);
   Element xml = sm.getXmlReply("OPENPROJECT",name,args,null,0);
   if (xml != null) setupFromXml(xml);
   else setupFromIvy(name);
   project_packages.put("fait.test",true);

   test_built = false;
   test_file = null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addProject(String pnm)
{
   CommandArgs args = new CommandArgs("PATHS",true,"CLASSES",true);
   Element xml = server_main.getXmlReply("OPENPROJECT",pnm,args,null,0);
   if (xml != null) setupFromXml(xml);
   else setupFromIvy(pnm);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean addFile(ServerFile sf)
{
   boolean added = false;
   
   if (sf != null && active_files.add(sf)) {
      noteFileChanged(sf,false);
      added = true;
    }
   
   return added;
}


void removeFile(ServerFile sf)
{
   if (sf == null) return;

   if (active_files.remove(sf)) {
      noteFileChanged(sf,true);
    }
}



Collection<ServerFile> getActiveFiles()
{
   return active_files;
}


boolean isErrorFree()
{
   return server_main.isErrorFree();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupFromXml(Element xml)
{
   if (IvyXml.isElement(xml,"RESULT")) xml = IvyXml.getChild(xml,"PROJECT");

   boolean isused = false;
   if (IvyXml.getChild(xml,"USEDBY") != null) isused = true;

   String wsdir = IvyXml.getAttrString(xml,"WORKSPACE");
   if (wsdir != null) {
      File wsf = new File(wsdir);
      File bdir = new File(wsf,".bubbles");
      File fait = new File(bdir,"fait.xml");
      if (fait.exists() && fait.canRead()) {
	 int p = IfaceDescriptionFile.PRIORITY_BASE_PROJECT;
	 if (isused) p = IfaceDescriptionFile.PRIORITY_DEPENDENT_PROJECT;
	 ControlDescriptionFile dd = new ControlDescriptionFile(fait,p);
	 description_files.add(dd);
       }
    }
   Element cp = IvyXml.getChild(xml,"CLASSPATH");
   String ignore = null;
   for (Element rpe : IvyXml.children(cp,"PATH")) {
      String bn = null;
      String ptyp = IvyXml.getAttrString(rpe,"TYPE");
      if (ptyp != null && ptyp.equals("SOURCE")) {
	 bn = IvyXml.getTextElement(rpe,"OUTPUT");
	 String sdir = IvyXml.getTextElement(rpe,"SOURCE");
	 if (sdir != null) {
	    File sdirf = new File(sdir);
	    source_paths.add(sdirf);
	    File fait = new File(sdirf,"fait.xml");
	    if (fait.exists() && fait.canRead()) {
	       int p = IfaceDescriptionFile.PRIORITY_BASE_PROJECT;
	       if (isused) p = IfaceDescriptionFile.PRIORITY_DEPENDENT_PROJECT;
	       ControlDescriptionFile dd = new ControlDescriptionFile(fait,p);
	       description_files.add(dd);
	     }
	  }
       }
      else {
	 bn = IvyXml.getTextElement(rpe,"BINARY");
       }
      if (bn == null) continue;
      if (bn.endsWith("/lib/rt.jar")) {
	 int idx = bn.lastIndexOf("rt.jar");
	 ignore = bn.substring(0,idx);
       }
      if (bn.endsWith("/lib/jrt-fs.jar")) {
	 int idx = bn.lastIndexOf("/lib/jrt-fs.jar");
	 ignore = bn.substring(0,idx);
       }
      if (IvyXml.getAttrBool(rpe,"SYSTEM")) continue;
      if (!class_paths.contains(bn)) {
	 class_paths.add(bn);
	 checkForDescriptionFile(bn);
       }
    }
   if (ignore != null) {
      for (Iterator<String> it = class_paths.iterator(); it.hasNext(); ) {
	 String nm = it.next();
	 if (nm.startsWith(ignore)) it.remove();
       }
    }

   Element clss = IvyXml.getChild(xml,"CLASSES");
   for (Element typ : IvyXml.children(clss,"TYPE")) {
      String cnm = IvyXml.getAttrString(typ,"NAME");
      int idx = cnm.lastIndexOf(".");
      String pnm = null;
      if (idx > 0) {
	 pnm = cnm.substring(0,idx).trim();
       }
      if (pnm == null) continue;
      project_packages.put(pnm,true);
      project_classes.add(cnm);
    }
   if (project_packages.isEmpty()) project_packages.put(DEFAULT_PACKAGE,true);
}



private void setupFromIvy(String name)
{
   IvyProjectManager pm = IvyProjectManager.getManager();
   IvyProject ip = pm.findProject(name);
   if (ip == null) return;
   boolean havedep = false;

   File wsf = ip.getWorkspace();
   File bdir = new File(wsf,".bubbles");
   File fait = new File(bdir,"fait.xml");
   if (fait.exists() && fait.canRead()) {
      int p = IfaceDescriptionFile.PRIORITY_BASE_PROJECT;
      if (!havedep) p = IfaceDescriptionFile.PRIORITY_DEPENDENT_PROJECT;
      ControlDescriptionFile dd = new ControlDescriptionFile(fait,p);
      description_files.add(dd);
    }

   for (String s : ip.getClassPath()) {
      if (!class_paths.contains(s)) {
	 class_paths.add(s);
	 checkForDescriptionFile(s);
       }
    }

   for (String sdir : ip.getSourcePath()) {
      File sdirf = new File(sdir);
      source_paths.add(sdirf);
      File faitf = new File(sdirf,"fait.xml");
      if (faitf.exists() && faitf.canRead()) {
	 int p = IfaceDescriptionFile.PRIORITY_BASE_PROJECT;
	 if (!havedep) p = IfaceDescriptionFile.PRIORITY_DEPENDENT_PROJECT;
	 ControlDescriptionFile dd = new ControlDescriptionFile(faitf,p);
	 description_files.add(dd);
       }
    }

   for (String s : ip.getUserPackages()) {
      project_packages.put(s,true);
    }

   // could add start classes from IvyProject, but if not done for Eclipse, don't bother
}



private void checkForDescriptionFile(String s)
{
   File f1 = new File(s);
   if (!f1.exists() || !f1.canRead()) return;
   if (f1.getName().endsWith(".jar")) {
      try {
	 JarFile jf = new JarFile(f1);
	 JarEntry je = jf.getJarEntry("fait.xml");
	 if (je == null) {
	    je = jf.getJarEntry("./fait.xml");
	  }
	 if (je != null) {
	    File ftemp = File.createTempFile("fait",".xml");
	    InputStream fis = jf.getInputStream(je);
	    IvyFile.copyFile(fis,ftemp);
	    ftemp.deleteOnExit();
	    ControlDescriptionFile dd = new ControlDescriptionFile(ftemp,f1);
	    description_files.add(dd);
	    FaitLog.logD("Create file " + ftemp + " for fait.xml in " + s);
	  }
	 jf.close();
       }
      catch (IOException e) { }
    }
}


/********************************************************************************/
/*										*/
/*	Compilation related methods						*/
/*										*/
/********************************************************************************/

boolean noteFileChanged(ServerFile sf,boolean force)
{
   if (!force && !active_files.contains(sf)) return false;

   project_lock.readLock().lock();
   try {
      if (force || active_files.contains(sf)) {
	 synchronized (changed_files) {
	    if (sf != null) {
	       if (changed_files.add(sf)) {
		  resumeAnalysis();
		}
	     }
	  }
	 return true;
       }
    }
   finally {
      project_lock.readLock().unlock();
    }

   return false;
}



boolean anyChangedFiles()
{
   project_lock.readLock().lock();
   try {
      return changed_files.size() == 0;
    }
   finally {
      project_lock.readLock().unlock();
    }
}



IfaceUpdateSet compileProject()
{
   IfaceUpdateSet rslt = null;

   if (!test_built) {
      test_built = true;
      ServerBuildTestDriver bt = new ServerBuildTestDriver(this);
      ServerFile testf = bt.process(project_classes);
      if (testf != null) {
	 if (test_file == null) {
	    test_file = testf;
	    active_files.add(test_file);
	    changed_files.add(test_file);
	  }
	 else {
	    test_file.editFile(0,0,testf.getFileContents(),true);
	    changed_files.add(test_file);
	  }
       }
      else {
	 if (test_file != null) {
	    active_files.remove(test_file);
	    test_file = null;
	  }
       }
    }


   List<ServerFile> newfiles = null;
   synchronized (changed_files) {
      newfiles = new ArrayList<>(changed_files);
      changed_files.clear();
    }

   if (!newfiles.isEmpty()) {
      project_lock.writeLock().lock();
      try {
	 ServerFile.setCurrentProject(this);
	 for (ServerFile sf : newfiles) {
	    FaitLog.logI("Update file " + sf.getFileName());
	  }

	 clearProject();
	 getJcompProject();
	 getEditableClasses();		// this resolves the project
	 rslt = new ServerUpdateData(newfiles);
       }
      finally {
	 ServerFile.setCurrentProject(null);
	 project_lock.writeLock().unlock();
       }
    }
   else {
      // force resolution if necessary
      if (!getJcompProject().isResolved()) getTyper();
    }

   return rslt;
}



synchronized void clearProject()
{
   if (base_project != null) {
      JcompControl jc = ServerMain.getJcompBase();
      jc.freeProject(base_project);
      base_project = null;
    }
}


public synchronized JcompProject getJcompProject()
{
   if (base_project != null) return base_project;

   for (ServerFile sf : active_files) {
      ASTNode an = sf.getAstRootNode();
      if (an != null) JcompAst.clearAll(an);
    }

   JcompControl jc = ServerMain.getJcompBase();
   Collection<JcompSource> srcs = new ArrayList<>(active_files);

   base_project = jc.getProject(getJcodeFactory(),srcs);

   return base_project;
}




public synchronized JcodeFactory getJcodeFactory()
{
   if (binary_control != null) return binary_control;

   int ct = Runtime.getRuntime().availableProcessors();
   ct = Math.max(1,ct/2);
   // ct = 1;			// for debugging only
   JcodeFactory jf = new JcodeFactory(ct);
   for (String s : class_paths) {
      jf.addToClassPath(s);
    }
   jf.load();
   binary_control = jf;

   return binary_control;
}



/********************************************************************************/
/*										*/
/*	Context methods 							*/
/*										*/
/********************************************************************************/

public JcompTyper getTyper()
{
   getJcompProject();
   if (base_project == null) return null;
   base_project.resolve();
   Collection<JcompSemantics> srcs = base_project.getSources();
   for (JcompSemantics js : srcs) {
      ASTNode an = js.getRootNode();
      JcompTyper jt = JcompAst.getTyper(an);
      if (jt != null) return jt;
    }

   return base_project.getResolveTyper();
}



/********************************************************************************/
/*										*/
/*	Methods to find all editable classes					*/
/*										*/
/********************************************************************************/

private void getEditableClasses()
{
   getTyper();		// resolve the project

   editable_classes.clear();
   Collection<JcompSemantics> srcs = base_project.getSources();
   ClassFinder cf = new ClassFinder();
   for (JcompSemantics js : srcs) {
      ASTNode an = js.getRootNode();
      an.accept(cf);
    }
}


private class ClassFinder extends ASTVisitor {

   ClassFinder() { }

   @Override public void endVisit(TypeDeclaration td) {
      JcompType jt = JcompAst.getJavaType(td);
      if (jt != null) editable_classes.add(jt.getName());
    }

   @Override public void endVisit(EnumDeclaration td) {
      JcompType jt = JcompAst.getJavaType(td);
      if (jt != null) editable_classes.add(jt.getName());
    }

   @Override public void endVisit(AnnotationTypeDeclaration td) {
      JcompType jt = JcompAst.getJavaType(td);
      if (jt != null) editable_classes.add(jt.getName());
    }


}	// end of inner class ClassFinder





/********************************************************************************/
/*										*/
/*	Interface for FAIT							*/
/*										*/
/********************************************************************************/

public Collection<IfaceDescriptionFile> getDescriptionFiles()
{
   return description_files;
}

public boolean isProjectClass(String cls)
{
   if (cls == null) return false;

   if (cls.startsWith("L") && cls.endsWith(";")) {
      cls = cls.substring(1,cls.length()-1);
      cls = cls.replace("/",".");
    }
   int idx = cls.lastIndexOf(".");
   if (idx < 0) return project_packages.containsKey(DEFAULT_PACKAGE);

   String pkg = cls.substring(0,idx);
   Boolean fg = project_packages.get(pkg);
   if (fg != null) return fg;

   String xpkg = pkg;
   for (int idx1 = xpkg.lastIndexOf("."); idx1 > 0; ) {
      xpkg = xpkg.substring(0,idx1);
      fg = project_packages.get(xpkg);
      if (fg != null) return fg;
      idx1 = xpkg.lastIndexOf(".");
    }
   project_packages.put(pkg,false);

   return false;
}


public boolean isEditableClass(String cls)
{
   return editable_classes.contains(cls);
}


public String getSourceFileForClass(String cls)
{
   return findSourceFile(cls);
}



public Collection<String> getClasspath()
{
   return class_paths;
}



/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

synchronized void beginAnalysis(int nth,String retid,ReportOption opt)
{
   if (current_runner == null) {
      current_runner = new ServerRunner(this,nth,retid,opt);
      current_runner.start();
    }
   else {
      current_runner.resumeAnalysis(nth,retid,opt);
    }
}


synchronized void resumeAnalysis()
{
   if (current_runner != null) beginAnalysis(0,null,null);
}

synchronized void pauseAnalysis()
{
   if (current_runner != null) current_runner.pauseAnalysis();
}



void sendAborted(String rid,long analt,long compt,long updt)
{
   CommandArgs args = new CommandArgs("ID",rid,"ABORTED",true,"COMPILETIME",compt,
					 "ANALYSISTIME",analt,"UPDATETIME",updt);
   server_main.response("ANALYSIS",args,null,null);
}


void sendStarted(String rid)
{
   CommandArgs args = new CommandArgs("ID",rid,"STARTED",true);
   server_main.response("ANALYSIS",args,null,null);
}


void sendAnalysis(String rid,IfaceControl ifc,ReportOption opt,long analt,long compt,
      long updt,int nthread,boolean upd)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("DATA");

   switch (opt) {
      case NONE :
	 break;
      case SOURCE :
      case SOURCE_STATS :
	 outputErrors(ifc,xw,true,false);
	 outputErrors(ifc,xw,false,true);
	 break;
      case FULL :
      case FULL_STATS :
	 outputErrors(ifc,xw,true,false);
	 outputErrors(ifc,xw,false,false);
	 break;
    }

   xw.end("DATA");

   CommandArgs args = new CommandArgs("ID",rid,"ABORTED",false,
	 "COMPILETIME",compt,"ANALYSISTIME",analt,"NTHREAD",nthread,
	 "UPDATETIME",updt,"UPDATE",upd);
   server_main.response("ANALYSIS",args,xw.toString(),null);

   xw.close();
}



private void outputErrors(IfaceControl ifc,IvyXmlWriter xw,boolean editable,boolean erronly)
{
   for (IfaceCall ic0 : ifc.getAllCalls()) {
      for (IfaceCall ic : ic0.getAlternateCalls()) {
	 List<IfaceProgramPoint> ppts = ic.getErrorLocations();
	 if (ppts == null || ppts.isEmpty()) continue;
	 IfaceProgramPoint ppt0 = ppts.get(0);
	 if (editable && ppt0.getAstReference() == null) continue;
	 else if (!editable && ppt0.getAstReference() != null) continue;

	 String file = ppt0.getSourceFile();
	 String cls = ic.getMethod().getDeclaringClass().getName();
	 if (!isProjectClass(cls)) file = null;
	 else if (file == null || !file.contains(File.separator)) {
	    file = findSourceFile(cls);
	  }
	 // TODO : this gives us the .class file, need to find corresponding source file

	 if (erronly) {
	    boolean haveerr = false;
	    for (IfaceProgramPoint ppt : ppts) {
	       for (IfaceError ie : ic.getErrors(ppt)) {
		  haveerr |= ie.getErrorLevel() == ErrorLevel.ERROR;
		}
	     }
	    if (!haveerr) continue;
	  }

	 xw.begin("CALL");
	 xw.field("METHOD",ic.getMethod().getName());
	 xw.field("INPROJECT",ifc.isInProject(ic.getMethod()));
	 xw.field("CLASS",ic.getMethod().getDeclaringClass().getName());
	 xw.field("SIGNATURE",ic.getMethod().getDescription());
	 if (file != null) xw.field("FILE",file);
	 xw.field("HASHCODE",ic.hashCode());
	 for (IfaceProgramPoint ppt : ppts) {
	    for (IfaceError ie : ic.getErrors(ppt)) {
	       if (erronly && ie.getErrorLevel() != ErrorLevel.ERROR) continue;
	       ie.outputXml(ppt,xw);
	     }
	  }
	 xw.end("CALL");
       }
    }
}



private String findSourceFile(String cls)
{
   int idx = cls.indexOf(".$");
   if (idx > 0) cls = cls.substring(0,idx);
   idx = cls.indexOf("$");
   if (idx > 0) cls = cls.substring(0,idx);
   String [] segs = cls.split("\\.");

   for (File f : source_paths) {
      File f1 = f;
      for (int i = 0; i < segs.length; ++i) {
	 File f2 = new File(f1,segs[i]);
	 if (f2.exists() && f2.isDirectory()) f1 = f2;
	 else {
	    File f3 = new File(f1,segs[i] + ".java");
	    if (f3.exists() && f3.canRead()) return f3.getAbsolutePath();
	    else break;
	  }
       }
    }
   return null;
}




/********************************************************************************/
/*										*/
/*	Query Processing							*/
/*										*/
/********************************************************************************/

void handleQuery(Element qxml,IvyXmlWriter xw) throws FaitException
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
       throw new FaitException("Analysis not run");
    }

   String methodinfo = IvyXml.getAttrString(qxml,"METHOD");
   int idx = methodinfo.indexOf("@");
   String mnm = methodinfo.substring(0,idx);
   String callid = methodinfo.substring(idx+1);
   int callhc = 0;
   try {
      if (callid != null) callhc = Integer.parseInt(callid);
    }
   catch (NumberFormatException e) { }

   String msg = null;
   String mcl = null;
   int idx1 = mnm.indexOf("(");
   if (idx1 > 0) {
      msg = mnm.substring(idx1);
      mnm = mnm.substring(0,idx1);
    }
   int idx2 = mnm.lastIndexOf(".");
   if (idx2 > 0) {
      mcl = mnm.substring(0,idx2);
      mnm = mnm.substring(idx2+1);
    }
   IfaceMethod m = ctrl.findMethod(mcl,mnm,msg);
   IfaceCall call = null;
   for (IfaceCall c : ctrl.getAllCalls(m)) {
      for (IfaceCall c1 : c.getAlternateCalls()) {
	 if (callhc == 0 || callhc == c1.hashCode()) {
	    call = c1;
	    break;
	  }
	 else if (call == null) call = c1;
       }
    }
   if (call == null) throw new FaitException("Call not found");
   
   String qtype = IvyXml.getAttrString(qxml,"QTYPE");
   switch (qtype) {
      case "ERROR" :
         handleErrorQuery(ctrl,qxml,call,xw);
         break;
      case "TO" :
      case "EXPLAIN" :
         handleToQuery(ctrl,qxml,call,xw);
         break;
      case "VALUE" :
         break;
    }
}


private void handleErrorQuery(IfaceControl ctrl,Element qxml,IfaceCall call,IvyXmlWriter xw) 
        throws FaitException
{
   Set<Integer> errids = new HashSet<>();
   StringTokenizer tok = new StringTokenizer(IvyXml.getAttrString(qxml,"ERROR"));
   while (tok.hasMoreTokens()) {
      try {
	 int hc = Integer.parseInt(tok.nextToken());
	 errids.add(hc);
       }
      catch (NumberFormatException e) { }
    }
   int spos = IvyXml.getAttrInt(qxml,"START");
   int lno = IvyXml.getAttrInt(qxml,"LINE");
   int loc = IvyXml.getAttrInt(qxml,"LOCATION");

   Map<IfaceError,IfaceProgramPoint> errs = new HashMap<>();
   List<IfaceProgramPoint> ppts = call.getErrorLocations();
   for (IfaceProgramPoint pt : ppts) {
      if (lno >= 0 && pt.getLineNumber() != lno) continue;
      IfaceAstReference ar = pt.getAstReference();
      if (ar != null) {
	 ASTNode an = ar.getAstNode();
	 if (an == null) continue;
	 if (spos >= 0 && an.getStartPosition() != spos) continue;
       }
      else if (pt.getInstruction() != null && loc > 0) {
	 int iidx = pt.getInstruction().getIndex();
	 if (iidx != loc) continue;
       }
      for (IfaceError ie : call.getErrors(pt)) {
	 if (errids.size() == 0 || errids.contains(ie.hashCode()))
	    errs.put(ie,pt);
       }
    }
   if (errs.size() == 0) throw new FaitException("Error not found");

   xw.begin("RESULTSET");
   for (Map.Entry<IfaceError,IfaceProgramPoint> ent : errs.entrySet()) {
      IfaceError ie = ent.getKey();
      IfaceProgramPoint ppt = ent.getValue();
      xw.begin("QUERY");
      xw.field("METHOD",call.getMethod().getFullName());
      xw.field("SIGNATURE",call.getMethod().getDescription());
      xw.field("CALL",call.hashCode());
      ie.outputXml(ppt,xw);
      ctrl.processErrorQuery(call,ppt,ie,xw);
      xw.end("QUERY");
    }
   xw.end("RESULTSET");
}



private void handleToQuery(IfaceControl ctrl,Element qxml,IfaceCall call,IvyXmlWriter xw)
        throws FaitException
{
   int spos = IvyXml.getAttrInt(qxml,"START");
   int loc = IvyXml.getAttrInt(qxml,"LOCATION");
   
   IfaceProgramPoint pt0 = call.getMethod().getStart();
   IfaceAstReference r0 = pt0.getAstReference();
   ASTNode an0 = getReferredNode(r0.getAstNode(),spos,loc);
   if (an0 == null) throw new FaitException("Program point not found");
   
   int apos = IvyXml.getAttrInt(qxml,"AFTER");
   int aloc = IvyXml.getAttrInt(qxml,"AFTERLOCATION");
   ASTNode aft = null;
   if (apos >= 0) {
      aft = getReferredNode(r0.getAstNode(),apos,aloc);
      if (aft == null) throw new FaitException("Program after point not found");
    }
   IfaceProgramPoint ppt = ctrl.getAstReference(an0,aft);
   
   IfaceEntity ent = null;
   int entid = IvyXml.getAttrInt(qxml,"ENTITY");
   if (entid >= 0) {
      ent = ctrl.findEntityById(entid);
      if (ent == null) throw new FaitException("Entity " + entid + " not found");
    }
   
   String stn = IvyXml.getAttrString(qxml,"SUBTYPE");
   String stv = IvyXml.getAttrString(qxml,"SUBTYPEVALUE");
   IfaceSubtype styp = null;
   IfaceSubtype.Value sval = null;
   if (stn != null) {
      for (IfaceSubtype st0 : ctrl.getAllSubtypes()) {
         if (st0.getName().equals(stn)) {
            styp = st0;
            break;
          }
       }
      if (styp == null) throw new FaitException("Can't find subtype " + stn);
      for (IfaceSubtype.Value sv0 : styp.getValues()) {
         if (sv0.toString().equals(stv)) {
            sval = sv0;
            break;
          }
       }
      if (sval == null) throw new FaitException("Can't find subtype value " + stv);
    }
   
   String vtypnm = IvyXml.getAttrString(qxml,"TYPE");
   IfaceType vtyp = null;
   if (vtypnm != null) vtyp = ctrl.findDataType(vtypnm);
   
   Element refxml = IvyXml.getChild(qxml,"REFERENCE");
   IfaceValue ref = getReference(ctrl,call,vtyp,ppt,refxml);
   if (ref == null) throw new FaitException("Reference not found");
   
   xw.begin("QUERY");
   xw.field("METHOD",call.getMethod().getFullName());
   xw.field("SIGNATURE",call.getMethod().getDescription());
   xw.field("ENTITY",ent.getId());
   
   if (styp != null) {
      xw.field("SUBTYPE",styp.getName());
      xw.field("SUBTYPEVALUE",sval.toString());
    }
   xw.field("CALL",call.hashCode());
   ppt.outputXml(xw);
   ctrl.processToQuery(call,ppt,ent,styp,sval,ref,xw);
   xw.end("QUERY");
}



private IfaceValue getReference(IfaceControl ctrl,IfaceCall call,IfaceType vtyp,
      IfaceProgramPoint ppt,Element refxml) throws FaitException
{
   IfaceValue ref = null;
   if (vtyp == null) {
      Element telt = IvyXml.getChild(refxml,"TYPE");
      String vtypnm = IvyXml.getAttrString(telt,"BASE");
      if (vtypnm != null) vtyp = ctrl.findDataType(vtypnm);
    }
   
   int slot = IvyXml.getAttrInt(refxml,"LOCAL");
   if (slot < 0) slot = IvyXml.getAttrInt(refxml,"BASELOCAL");
   int stk = IvyXml.getAttrInt(refxml,"STACK");
   if (stk < 0) stk = IvyXml.getAttrInt(refxml,"BASESTACK");
   if (slot >= 0) {
      ref = ctrl.findRefValue(vtyp,slot);
    }
   else if (stk >= 0) {
      ref = ctrl.findRefStackValue(vtyp,stk);
    }
   String fldnm = IvyXml.getAttrString(refxml,"FIELD");
   if (fldnm != null) {
      if (ref == null) {
         IfaceState state = ctrl.findStateForLocation(call,ppt);
         int bid = IvyXml.getAttrInt(refxml,"BASEID");
         for (int i = 0; ; ++i) {
            IfaceValue sv = state.getStack(i);
            if (sv == null) break;
            if (sv.hashCode() == bid) {
               ref = sv;
               break;
             }
          }
         if (ref == null) {
            for (int i = 0; ; ++i) {
               IfaceValue sv = state.getLocal(i);
               if (sv == null) break;
               if (sv.hashCode() == bid) {
                  ref = sv;
                  break;
                }
             }
          }
       }
      if (ref == null) {
         throw new FaitException("Can't find base value for field " + fldnm);
       }
      int idx1 = fldnm.lastIndexOf(".");
      String cnm = fldnm.substring(0,idx1);
      String fnm = fldnm.substring(idx1+1);
      IfaceType ftyp = ctrl.findDataType(cnm);
      IfaceField fld = ctrl.findField(ftyp,fnm);
      if (fld == null) throw new FaitException("Field " + fldnm + " not found");
      ref = ctrl.findRefValue(vtyp,ref,fld);
    }
   return ref;
}


private ASTNode getReferredNode(ASTNode root,int spos,int ntyp)
{
   if (root == null) return null;
   
   ASTNode an0 = JcompAst.findNodeAtOffset(root,spos);
   while (an0 != null && an0.getNodeType() != ntyp) {
      an0 = an0.getParent();
    }
   return an0;
}


void handleVarQuery(Element qxml,IvyXmlWriter xw) throws FaitException
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }

   String mnm = IvyXml.getAttrString(qxml,"METHOD");
   int pos = IvyXml.getAttrInt(qxml,"START");
   int line = IvyXml.getAttrInt(qxml,"LINE");
   String var = IvyXml.getAttrString(qxml,"TOKEN");

   ctrl.processVarQuery(mnm,line,pos,var,xw);
}



/********************************************************************************/
/*                                                                              */
/*      Handle queries for ROSE                                                 */
/*                                                                              */
/********************************************************************************/

void handleFlowQuery(Element qxml,IvyXmlWriter xw) throws FaitException
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }
   
   String mnm = IvyXml.getAttrString(qxml,"METHOD");
   String msg = null;
   String mcl = null;
   int idx1 = mnm.indexOf("(");
   if (idx1 > 0) {
      msg = mnm.substring(idx1);
      mnm = mnm.substring(0,idx1);
    }
   int idx2 = mnm.lastIndexOf(".");
   if (idx2 > 0) {
      mcl = mnm.substring(0,idx2);
      mnm = mnm.substring(idx2+1);
    }
   
   String qtyp = IvyXml.getAttrString(qxml,"QTYPE");
   
   Map<Integer,Element> calls = null;
   Element vloc = null;
 
   switch (qtyp) {
      case "EXPRESSION" :
         calls = null;
         vloc = IvyXml.getChild(qxml,"EXPR");
         break;
      case "VARIABLE" :
         calls = new HashMap<>();
         for (Element loc : IvyXml.children(qxml,"LOCATION")) {
            int cid = IvyXml.getAttrInt(loc,"CALLID"); 
            if (cid != -1) {
               Element ploc = IvyXml.getChild(loc,"POINT");
               calls.put(cid,ploc);
             }
          }
         break;
      case "LOCATION" :
         calls = null;
         vloc = IvyXml.getChild(qxml,"LOCATION");
         break;
    }
   
   IfaceMethod m = ctrl.findMethod(mcl,mnm,msg);
   for (IfaceCall c : ctrl.getAllCalls(m)) {
      for (IfaceCall c1 : c.getAlternateCalls()) {
         Element loc = vloc;
         if (loc == null &&  calls != null) loc = calls.get(c1.hashCode());
         if (loc != null) {
            handleFlowQueryForCall(ctrl,qxml,c1,loc,xw);
          }
       }
    }
}



void handleChangeQuery(Element qxml,IvyXmlWriter xw) throws FaitException
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }
   
   String mnm = IvyXml.getAttrString(qxml,"METHOD");
   String msg = null;
   String mcl = null;
   int idx1 = mnm.indexOf("(");
   if (idx1 > 0) {
      msg = mnm.substring(idx1);
      mnm = mnm.substring(0,idx1);
    }
   int idx2 = mnm.lastIndexOf(".");
   if (idx2 > 0) {
      mcl = mnm.substring(0,idx2);
      mnm = mnm.substring(idx2+1);
    }
   
   Element vloc = IvyXml.getChild(qxml,"LOCATION");
   
   IfaceMethod m = ctrl.findMethod(mcl,mnm,msg);
   for (IfaceCall c : ctrl.getAllCalls(m)) {
      for (IfaceCall c1 : c.getAlternateCalls()) {
         if (vloc != null) {
            handleChangeQueryForCall(ctrl,qxml,c1,vloc,xw);
          }
       }
    }
}


void handleFileQuery(Element qxml,IvyXmlWriter xw) throws FaitException 
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }
   
   
   Set<String> classes = new HashSet<>();
   Set<IfaceCall> done = new HashSet<>();
   
   for (Element melt : IvyXml.children(qxml,"METHOD")){
      String mnm = IvyXml.getText(melt);
      String msg = null;
      String mcl = null;
      int idx1 = mnm.indexOf("(");
      if (idx1 > 0) {
         msg = mnm.substring(idx1);
         mnm = mnm.substring(0,idx1);
       }
      int idx2 = mnm.lastIndexOf(".");
      if (idx2 > 0) {
         mcl = mnm.substring(0,idx2);
         mnm = mnm.substring(idx2+1);
       }
      IfaceMethod m = ctrl.findMethod(mcl,mnm,msg);
      for (IfaceCall c : ctrl.getAllCalls(m)) {
         for (IfaceCall c1 : c.getAlternateCalls()) {
            handleFileQueryForCall(ctrl,c1,classes,done);
          }
       }
    }
   
   for (String s : classes) {
      xw.textElement("CLASS",s);
    }
}



private void handleFlowQueryForCall(IfaceControl ctrl,Element qxml,IfaceCall call,
      Element loc,IvyXmlWriter xw) throws FaitException
{
   int spos = IvyXml.getAttrInt(loc,"START");
   int ltyp = IvyXml.getAttrInt(loc,"NODETYPEID");
   IfaceProgramPoint pt0 = call.getMethod().getStart();
   IfaceAstReference r0 = pt0.getAstReference();
   if (r0 == null) {
      FaitLog.logE("Can't find AST node " + pt0 + " " + r0 + " " + spos + " " +
            ltyp + " " + call.getMethod());
      return;
    }
   ASTNode an0 = getReferredNode(r0.getAstNode(),spos,ltyp);
   int apos = IvyXml.getAttrInt(loc,"AFTERSTART");
   int atyp = IvyXml.getAttrInt(loc,"AFTERTYPEID");
   ASTNode aft = null;
   if (apos >= 0) {
      aft = getReferredNode(r0.getAstNode(),apos,atyp);
    }
   if (aft != null) an0 = aft.getParent();
   IfaceProgramPoint ppt = ctrl.getAstReference(an0,aft);
   
   List<IfaceMethod> stack = null;
   Element selt = IvyXml.getChild(qxml,"STACK");
   if (selt != null) {
      stack = new ArrayList<>();
      boolean fndstart = false; 
      IfaceMethod callm = call.getMethod();
      boolean istest = false;
      for (Element felt : IvyXml.children(selt,"FRAME")) {
         String cnm = IvyXml.getAttrString(felt,"CLASS");
         if (cnm.startsWith("org.junit.runners")) istest = true;
       }
      for (Element felt : IvyXml.children(selt,"FRAME")) {
         String cnm = IvyXml.getAttrString(felt,"CLASS");
         String mnm = IvyXml.getAttrString(felt,"METHOD");
         String msg = IvyXml.getAttrString(felt,"SIGNATURE");
         IfaceMethod im = ctrl.findMethod(cnm,mnm,msg);
         if (im == null && cnm.contains("$")) continue;
         if (im == null && cnm.contains(".junit.")) continue;
         if (im == null && cnm.contains(".junit4.")) continue;
         if (cnm.startsWith("jdk.internal.reflect.")) {
            if (istest) break;
            else continue;
          }
         if (im == null) stack = null;
         else if (!fndstart) {
            if (im == callm) fndstart = true;
            else continue;
          }
         if (stack != null) stack.add(im);
       }
      if (stack != null && stack.isEmpty()) stack = null;
      if (!fndstart) stack = null;
    }
   
   IfaceValue ref = null;
   if (IvyXml.isElement(loc,"EXPR")) {
      IfaceState st0 = ctrl.findStateForLocation(call,ppt);
      if (st0 == null) return;
      IfaceValue refv = st0.getStack(0);
      ref = ctrl.findRefStackValue(refv.getDataType(),0);
    }
   else {
      Element refxml = IvyXml.getChild(qxml,"VALUE");
      if (refxml != null) {
         ref = getReference(ctrl,call,null,ppt,refxml);
       }
      else {
         // create dummy local reference
         IfaceMethod m = call.getMethod();
         int sz = m.getLocalSize();
         IfaceType t0 = ctrl.findDataType("int");
         ref = ctrl.findRefValue(t0,sz+10);
       }
    }
   
   if (ref == null) return;
   
   String strval = IvyXml.getAttrString(qxml,"CURRENT");
   IfaceType valtyp = ref.getDataType();
   IfaceValue curval = getCurrentValue(ctrl,strval,valtyp);
  
   ctrl.processFlowQuery(call,ppt,ref,curval,stack,xw);
}



private void handleChangeQueryForCall(IfaceControl ctrl,Element qxml,IfaceCall call,
      Element loc,IvyXmlWriter xw) throws FaitException
{
   int spos = IvyXml.getAttrInt(loc,"START");
   int ltyp = IvyXml.getAttrInt(loc,"NODETYPEID");
   IfaceProgramPoint pt0 = call.getMethod().getStart();
   IfaceAstReference r0 = pt0.getAstReference();
   if (r0 == null) {
      FaitLog.logE("Can't find AST node " + pt0 + " " + r0 + " " + spos + " " +
            ltyp + " " + call.getMethod());
      return;
    }
   ASTNode an0 = getReferredNode(r0.getAstNode(),spos,ltyp);
   int apos = IvyXml.getAttrInt(loc,"AFTERSTART");
   int atyp = IvyXml.getAttrInt(loc,"AFTERTYPEID");
   ASTNode aft = null;
   if (apos >= 0) {
      aft = getReferredNode(r0.getAstNode(),apos,atyp);
    }
   IfaceProgramPoint ppt = ctrl.getAstReference(an0,aft);
   
   List<IfaceMethod> stack = null;
   Element selt = IvyXml.getChild(qxml,"STACK");
   if (selt != null) {
      stack = new ArrayList<>();
      for (Element felt : IvyXml.children(selt,"FRAME")) {
         String cnm = IvyXml.getAttrString(felt,"CLASS");
         String mnm = IvyXml.getAttrString(felt,"METHOD");
         String msg = IvyXml.getAttrString(felt,"SIGNATURE");
         IfaceMethod im = ctrl.findMethod(cnm,mnm,msg);
         if (im == null && cnm.contains("$")) continue;
         if (im == null && cnm.contains(".junit.")) continue;
         if (im == null && cnm.contains(".junit4.")) continue;
         if (im == null) stack = null;
         if (stack != null) stack.add(im);
       }
      if (stack != null && stack.isEmpty()) stack = null;
    }
   
   ctrl.processChangeQuery(call,ppt,xw);
} 



private IfaceValue getCurrentValue(IfaceControl ctrl,String vstr,IfaceType typ)
{
   if (vstr == null) return null;
   if (typ == null || typ.isVoidType()) return null;
   if (typ.isIntType()) {
      try {
         long i = Long.parseLong(vstr);
         return ctrl.findRangeValue(typ,i,i);
       }
      catch (NumberFormatException e) { }
    }
   else if (typ.isStringType()) {
      
    }
   else if (typ.isPrimitiveType()) ;
   else {
      if (vstr.equals("null")) {
         return ctrl.findNullValue(typ);
       }
      else {
         // might want to get type from vstr
         IfaceValue v = ctrl.findAnyValue(typ);
         v = v.forceNonNull();
         return v;
       }
    }
   
   return null;
}



private void handleFileQueryForCall(IfaceControl ctrl,IfaceCall call,
      Set<String> rslt,Set<IfaceCall> done)
{
   if (done.add(call)) {
      String cnm = call.getMethod().getDeclaringClass().getName();
      if (isProjectClass(cnm)) {
         if (cnm != null) rslt.add(cnm);
         for (IfaceCall c : call.getAllMethodsCalled(null)) {
            handleFileQueryForCall(ctrl,c,rslt,done);
          }
       }
    } 
}
   
   
   
/********************************************************************************/
/*										*/
/*	Handle Reflection queries						*/
/*										*/
/********************************************************************************/

void handleReflection(Element xml,IvyXmlWriter xw) throws FaitException
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }

   ctrl.processReflectionQuery(xw);
}



/********************************************************************************/
/*										*/
/*	Handle determining the set of critical routines 			*/
/*										*/
/********************************************************************************/

void handleFindCritical(Element xml,IvyXmlWriter xw) throws FaitException
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }

   String ignores = IvyXml.getAttrString(xml,"IGNORES");
   ctrl.processCriticalQuery(ignores,xw);
}



/********************************************************************************/
/*										*/
/*	Handle performance queries						*/
/*										*/
/********************************************************************************/

void handlePerformance(Element xml,IvyXmlWriter xw) throws FaitException
{
   double cutoff = IvyXml.getAttrDouble(xml,"CUTOFF",0);
   double scancutoff = IvyXml.getAttrDouble(xml,"SCANCUTOFF",100.0);
   Map<IfaceMethod,FaitStatistics> basecounts = new HashMap<>();
   Map<IfaceMethod,FaitStatistics> totalcounts = new HashMap<>();

   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }

   FaitStatistics totals = new FaitStatistics();
   for (IfaceCall c : ctrl.getAllCalls()) {
      FaitStatistics stats = c.getStatistics();
      totals.add(stats);
    }
   cutoff = totals.getNumForward() * cutoff / 100.0;
   scancutoff = totals.getNumScans() * scancutoff / 100.0;

   for (IfaceCall c : ctrl.getAllCalls()) {
      FaitStatistics stats = c.getStatistics();
      if (stats.accept(cutoff,scancutoff)) {
	 addCounts(basecounts,c.getMethod(),stats,1);
	 addCounts(totalcounts,c.getMethod(),stats,1);
	 updateParents(c,stats,totalcounts,1.0,new HashSet<>());
       }
    }

   xw.begin("PERFORMANCE");
   totals.outputXml("TOTALS",xw);
   for (Map.Entry<IfaceMethod,FaitStatistics> ent : totalcounts.entrySet()) {
      IfaceMethod im = ent.getKey();
      FaitStatistics stats = ent.getValue();
      if (!stats.accept(cutoff,scancutoff)) continue;

      xw.begin("METHOD");
      xw.field("NAME",im.getFullName());
      xw.field("INPROJECT",ctrl.isInProject(im));
      xw.field("DESCRIPTION",im.getDescription());
      xw.field("FILE",im.getFile());
      stats.outputXml("TOTAL",xw);
      FaitStatistics base = basecounts.get(im);
      if (base != null) base.outputXml("BASE",xw);
      xw.end("METHOD");
    }
   xw.end("PERFORMANCE");
}

		

private void updateParents(IfaceCall c,FaitStatistics stats,Map<IfaceMethod,FaitStatistics> totals,
			      double fract,HashSet<IfaceCall> done)
{
   if (!done.add(c)) return;

   List<IfaceLocation> callers = getAllCallers(c);
   for (IfaceLocation loc : callers) {
      double nfract = fract / callers.size();
      if (nfract < 0.0001) continue;
      addCounts(totals,loc.getMethod(),stats,nfract);
      updateParents(loc.getCall(),stats,totals,nfract,done);
    }
}



private void addCounts(Map<IfaceMethod,FaitStatistics> cnts,IfaceMethod m,
      FaitStatistics s,double fract)
{
   FaitStatistics oldstat = cnts.get(m);
   if (oldstat == null) {
      oldstat = new FaitStatistics();
      cnts.put(m,oldstat);
    }
   oldstat.add(s,fract);
}



private List<IfaceLocation> getAllCallers(IfaceCall c)
{
   List<IfaceLocation> rslt = new ArrayList<>();
   for (IfaceCall cc : c.getAlternateCalls()) {
      for (IfaceLocation loc : cc.getCallSites()) {
	 rslt.add(loc);
       }
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Handle test case generation						*/
/*										*/
/********************************************************************************/

void handleTestCase(Element path,IvyXmlWriter xw) throws FaitException
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) {
      throw new FaitException("Analysis not run");
    }

   ctrl.generateTestCase(path,xw);
}




/********************************************************************************/
/*										*/
/*	Handle description file queries 					*/
/*										*/
/********************************************************************************/

List<File> getBaseDescriptionFiles()
{
   IfaceControl ctrl = null;
   if (current_runner != null) {
      ctrl = current_runner.getControl();
    }
   if (ctrl == null) return null;
   return ctrl.getSystemDescriptionFiles();
}









}	// end of class ServerProject




/* end of ServerProject.java */

