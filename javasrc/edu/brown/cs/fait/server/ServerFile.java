/********************************************************************************/
/*                                                                              */
/*              ServerFile.java                                                 */
/*                                                                              */
/*      Representation of a single editable file                                */
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
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompAstCleaner;
import edu.brown.cs.ivy.jcomp.JcompExtendedSource;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSemantics;

class ServerFile implements ServerConstants, JcompAstCleaner, JcompExtendedSource 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IDocument		edit_document;
private File			for_file;
private ASTNode	        project_root;
private ASTNode                 general_root;

private static ServerProject	current_project;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ServerFile(File f,String cnts,String linesep)
{
   for_file = f;
   edit_document = new Document(cnts);
   general_root = null;
   project_root = null;
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void editFile(int len,int off,String txt,boolean complete)
{
   if (complete) len = edit_document.getLength();
   try {
      edit_document.replace(off,len,txt);
    }
   catch (BadLocationException e) {
      FaitLog.logE("Problem doing file edit",e);
    }
   
   synchronized (this) {
      general_root = null;
    }
}


void resetSemantics()
{
   project_root = null;
}







/********************************************************************************/
/*										*/
/*	Methods to provide ASTs to jcomp for current project			*/
/*										*/
/********************************************************************************/

static void setCurrentProject(ServerProject sp)
{
   current_project = sp;
}





public ASTNode cleanupAst(ASTNode n)
{
   cleanupAstRoot(n);
   
   return n;
}



/********************************************************************************/
/*										*/
/*	Abstract syntax tree methods						*/
/*										*/
/********************************************************************************/

ASTNode getAst()
{
   ASTNode an = null;
   synchronized (this) {
      an = general_root;
      if (an != null) return an;
    }
   
   an = buildAst();
   
   synchronized (this) {
      ASTNode nan = general_root;
      if (nan != null) an = nan;
      else general_root = an;
    }
   
   return an;
}


private ASTNode buildAst()
{
   ASTParser parser = ASTParser.newParser(AST.JLS8);
   parser.setKind(ASTParser.K_COMPILATION_UNIT);
   parser.setSource(edit_document.get().toCharArray());
   Map<String,String> options = JavaCore.getOptions();
   JavaCore.setComplianceOptions(JavaCore.VERSION_1_6,options);
   parser.setCompilerOptions(options);
   parser.setResolveBindings(false);
   parser.setStatementsRecovery(true);
   ASTNode an = parser.createAST(null);
   JcompAst.setSource(an,this);
   cleanupAstRoot(an);
   
   return an;
}



private void cleanupAstRoot(ASTNode an)
{
   ServerAstCleaner sac = new ServerAstCleaner(current_project,(CompilationUnit) an);
   sac.precleanAst();
}



ASTNode getResolvedAst(ServerProject sp)
{
   ASTNode an = null;
   synchronized (this) {
      an = project_root;
      if (an != null && JcompAst.isResolved(an)) return an;
    }
   
   JcompProject proj = sp.getJcompProject();
   proj.resolve();
   // calling compileProject causes random updates with intermediate edits
   JcompSemantics semdata = ServerMain.getJcompBase().getSemanticData(this);
   an = semdata.getAstNode();
   
   synchronized (this) {
      ASTNode nan = project_root;
      if (nan != null) an = nan;
      else project_root = an;
    }
   
   return an;
}

void resetProject(ServerProject sp)
{
   synchronized (this) {
      project_root = null;
    }
}



Position createPosition(int pos)
{
   Position p = new Position(pos);
   try {
      edit_document.addPosition(p);
    }
   catch (BadLocationException e) {
      return null;
    }
   
   return p;
}




/********************************************************************************/
/*										*/
/*	JcompSource methods							*/
/*										*/
/********************************************************************************/

@Override public String getFileContents()
{
   return edit_document.get();
}



@Override public String getFileName()
{
   return for_file.getPath();
}


@Override public ASTNode getAstRootNode()                
{
   return project_root;
}


public File getFile()
{
   return for_file;
}




/********************************************************************************/
/*                                                                              */
/*      Debugging methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return getFile().toString();
}



}       // end of class ServerFile




/* end of ServerFile.java */

