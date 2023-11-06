/********************************************************************************/
/*										*/
/*		ServerUpdateData.java						*/
/*										*/
/*	Handle determining what needs to be updated				*/
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceUpdateSet;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;

class ServerUpdateData implements ServerConstants, IfaceUpdateSet
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<ServerFile>	update_files;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ServerUpdateData(List<ServerFile> files)
{
   update_files = new ArrayList<>(files);
}


/********************************************************************************/
/*										*/
/*	Update checker								*/
/*										*/
/********************************************************************************/

@Override public boolean shouldUpdate(IfaceCall call)
{
   IfaceMethod m = call.getMethod();
   IfaceProgramPoint pt = m.getStart();
   if (pt == null) return false;
   IfaceAstReference ar = pt.getAstReference();
   if (ar == null) {
      // TODO: if method is not AST-based, then update
      String cnm = m.getDeclaringClass().getName();
      if (cnm.startsWith("java.") ||
	    cnm.startsWith("javax.") ||
	    cnm.startsWith("sun.")) return false;
      IfaceType typ = call.getControl().findDataType(m.getDeclaringClass().getName());
      IfaceBaseType btyp = typ.getJavaType();
      if (btyp.isEditable() && !m.isEditable()) 
         return true;
      IfaceMethod m0 = call.getControl().findMethod(m.getDeclaringClass().getName(),
	    m.getName(),m.getDescription());
      if (m0 == m) return false;
      return true;	
    }

   JcompSource src = JcompAst.getSource(ar.getAstNode());
   if (update_files.contains(src)) return true;
   // might want to do method-level checks

   return false;
}


@Override public boolean shouldUpdate(IfaceMethod im)
{
   IfaceProgramPoint pt = im.getStart();
   if (pt == null) return false;
   IfaceAstReference ar = pt.getAstReference();
   if (ar == null) {
      // TODO: if method is not AST-based, then update
      String cnm = im.getDeclaringClass().getName();
      if (cnm.startsWith("java.") ||
	    cnm.startsWith("javax.") ||
	    cnm.startsWith("sun.")) return false;
      return true;
    }
   
   JcompSource src = JcompAst.getSource(ar.getAstNode());
   if (update_files.contains(src)) return true;
   // might want to do method-level checks
   
   return false; 
}



/********************************************************************************/
/*										*/
/*	Field handling								*/
/*										*/
/********************************************************************************/

@Override public Collection<IfaceField> getUpdatedFields(IfaceControl ic)
{
   // this doesn't handle fields of inner types
   List<IfaceField> flds = new ArrayList<>();

   for (ServerFile sf : update_files) {
      CompilationUnit cu = (CompilationUnit) sf.getAstRootNode(); 
      if (cu != null) {
	 for (Object o : cu.types()) {
	    AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
	    JcompType jt = JcompAst.getJavaType(atd);
            if (jt == null) {
               FaitLog.logE("No type defined for " + atd);
               continue;
             }
	    IfaceType ity = ic.findDataType(jt.getName());
	    for (Object bdo : atd.bodyDeclarations()) {
	       if (bdo instanceof FieldDeclaration) {
		  FieldDeclaration fd = (FieldDeclaration) bdo;
                  for (Object vdfo : fd.fragments()) {
                     VariableDeclarationFragment vdf = (VariableDeclarationFragment) vdfo;
                     JcompSymbol js = JcompAst.getDefinition(vdf);
                     if (js == null) js = JcompAst.getDefinition(vdf.getName());
                     if (js != null) {
                        IfaceField fld = ic.findField(ity,js.getName());
                        flds.add(fld);
                      }
                     else {
                        FaitLog.logE("Can't find field for " + fd + " " + vdf);
                      }
                   }
		}
	     }
	  }
       }
    }

   return flds;
}




/********************************************************************************/
/*										*/
/*	Type handling								*/
/*										*/
/********************************************************************************/

@Override public Collection<IfaceType> getUpdatedTypes(IfaceControl ic)
{
   List<IfaceType> typs = new ArrayList<>();
   
   for (ServerFile sf : update_files) {
      CompilationUnit cu = (CompilationUnit) sf.getAstRootNode(); 
      if (cu != null) {
	 for (Object o : cu.types()) {
	    AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
            addUpdatedTypes(ic,atd,typs);
	  }
       }
    }
   
   return typs;
}


private void addUpdatedTypes(IfaceControl ic,AbstractTypeDeclaration atd,Collection<IfaceType> rslt)
{
   JcompType jt = JcompAst.getJavaType(atd);
   if (jt == null) {
      FaitLog.logE("No type defined for " + atd);
    }  
   else {
      IfaceType ity = ic.findDataType(jt.getName());
      rslt.add(ity);
    }
   for (Object o : atd.bodyDeclarations()) {
      if (o instanceof AbstractTypeDeclaration) {
         AbstractTypeDeclaration styp = (AbstractTypeDeclaration) o;
         addUpdatedTypes(ic,styp,rslt);
       }
    }
}


}	// end of class ServerUpdateData




/* end of ServerUpdateData.java */

