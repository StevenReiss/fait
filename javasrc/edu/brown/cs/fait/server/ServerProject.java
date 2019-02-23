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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceProject;
import edu.brown.cs.fait.iface.IfaceUpdateSet;
import edu.brown.cs.fait.iface.FaitException;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceMethod;
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
private Set<File>	description_files;
private Map<String,Boolean> project_packages;
private ServerRunner	current_runner;
private Set<String>	editable_classes;

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

void addFile(ServerFile sf)
{
   if (sf == null) return;

   if (active_files.add(sf)) {
      noteFileChanged(sf,false);
    }
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
   String wsdir = IvyXml.getAttrString(xml,"WORKSPACE");
   if (wsdir != null) {
      File wsf = new File(wsdir);
      File bdir = new File(wsf,".bubbles");
      File fait = new File(bdir,"fait.xml");
      if (fait.exists() && fait.canRead()) description_files.add(fait);
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
	    File fait = new File(sdirf,"fait.xml");
	    if (fait.exists() && fait.canRead()) description_files.add(fait);
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
      if (!class_paths.contains(bn)) class_paths.add(bn);
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
    }
   if (project_packages.isEmpty()) project_packages.put(DEFAULT_PACKAGE,true);
}



private void setupFromIvy(String name)
{
   IvyProjectManager pm = IvyProjectManager.getManager();
   IvyProject ip = pm.findProject(name);
   if (ip == null) return;

   File wsf = ip.getWorkspace();
   File bdir = new File(wsf,".bubbles");
   File fait = new File(bdir,"fait.xml");
   if (fait.exists() && fait.canRead()) description_files.add(fait);

   for (String s : ip.getClassPath()) {
      if (!class_paths.contains(s)) class_paths.add(s);
    }

   for (String sdir : ip.getSourcePath()) {
      File sdirf = new File(sdir);
      File faitf = new File(sdirf,"fait.xml");
      if (faitf.exists() && faitf.canRead()) description_files.add(faitf);
    }

   for (String s : ip.getUserPackages()) {
      project_packages.put(s,true);
    }

   // could add start classes from IvyProject, but if not done for Eclipse, don't bother
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
	    sf.resetSemantics();
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
      for (ServerFile sf : active_files) {
	 sf.resetProject(this);
       }
    }
}


public synchronized JcompProject getJcompProject()
{
   if (base_project != null) return base_project;

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
   getTyper();				// resolve the project

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

public Collection<File> getDescriptionFiles()		 { return description_files; }

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



void sendAborted(String rid,long analt,long compt)
{
   CommandArgs args = new CommandArgs("ID",rid,"ABORTED",true,"COMPILETIME",compt,
					 "ANALYSISTIME",analt);
   server_main.response("ANALYSIS",args,null,null);
}


void sendAnalysis(String rid,IfaceControl ifc,ReportOption opt,long analt,long compt,
      int nthread,boolean upd)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("DATA");

   switch (opt) {
      case NONE :
	 break;
      case SOURCE :
	 outputErrors(ifc,xw,true);
	 break;
      case FULL :
	 outputErrors(ifc,xw,true);
	 outputErrors(ifc,xw,false);
	 break;
    }

   xw.end("DATA");

   CommandArgs args = new CommandArgs("ID",rid,"ABORTED",false,
	 "COMPILETIME",compt,"ANALYSISTIME",analt,"NTHREAD",nthread,"UPDATE",upd);
   server_main.response("ANALYSIS",args,xw.toString(),null);

   xw.close();
}



private void outputErrors(IfaceControl ifc,IvyXmlWriter xw,boolean editable)
{
   for (IfaceCall ic0 : ifc.getAllCalls()) {
      for (IfaceCall ic : ic0.getAlternateCalls()) {
	 List<IfaceProgramPoint> ppts = ic.getErrorLocations();
	 if (ppts == null || ppts.isEmpty()) continue;
	 IfaceProgramPoint ppt0 = ppts.get(0);
	 if (editable && ppt0.getAstReference() == null) continue;
	 else if (!editable && ppt0.getAstReference() != null) continue;

	 String file = null;
	 if (ppt0.getAstReference() != null) {
	    ASTNode n = ppt0.getAstReference().getAstNode();
	    JcompSource src = JcompAst.getSource(n);
	    if (src != null) {
	       file = src.getFileName();
	     }
	  }

	 xw.begin("CALL");
	 xw.field("METHOD",ic.getMethod().getName());
	 xw.field("CLASS",ic.getMethod().getDeclaringClass().getName());
	 xw.field("SIGNATURE",ic.getMethod().getDescription());
	 if (file != null) xw.field("FILE",file);
	 xw.field("HASHCODE",ic.hashCode());
	 if (ppts != null && !ppts.isEmpty()) {
	    for (IfaceProgramPoint ppt : ppts) {
	       for (IfaceError ie : ic.getErrors(ppt)) {
                  ie.outputXml(ppt,xw);
		}
	     }
	  }
	 xw.end("CALL");
       }
    }
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

   Map<IfaceError,IfaceProgramPoint> errs = new HashMap<>();
   List<IfaceProgramPoint> ppts = call.getErrorLocations();
   for (IfaceProgramPoint pt : ppts) {
      IfaceAstReference ar = pt.getAstReference();
      if (ar == null) continue;
      ASTNode an = ar.getAstNode();
      if (an == null) continue;
      if (spos >= 0 && an.getStartPosition() != spos) continue;
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
      ie.outputXml(ppt,xw);
      ctrl.processErrorQuery(call,ppt,ie,xw);
      xw.end("QUERY");
    }
   xw.end("RESULTSET");
}




}	// end of class ServerProject




/* end of ServerProject.java */
