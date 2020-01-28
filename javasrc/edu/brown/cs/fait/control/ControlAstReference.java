/********************************************************************************/
/*										*/
/*		ControlAstReference.java					*/
/*										*/
/*	Reference to an execution location in an AST				*/
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



package edu.brown.cs.fait.control;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAstStatus;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import edu.brown.cs.fait.iface.FaitConstants;

class ControlAstReference implements IfaceAstReference, FaitConstants, Comparable<ControlAstReference>
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ASTNode eval_node;
private ASTNode after_child;
private IfaceAstStatus run_status;
private ControlAstFactory ast_factory;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ControlAstReference(ControlAstFactory af,ASTNode n,ASTNode c,IfaceAstStatus sts)
{
   ast_factory = af;
   eval_node = n;
   after_child = c;
   run_status = sts;

   if (n == null) {
      FaitLog.logX("Eval node shouldn't be null");
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public ASTNode getAstNode()			{ return eval_node; }

@Override public ASTNode getAfterChild()		{ return after_child; }

@Override public IfaceAstStatus getStatus()		{ return run_status; }

@Override public JcodeInstruction getInstruction()	{ return null; }

@Override public IfaceAstReference getAstReference()	{ return this; }

@Override public boolean isByteCode()			{ return false; }

@Override public IfaceMethod getMethod()
{
   if (eval_node == null) return null;
   for (ASTNode n = eval_node; n != null; n = n.getParent()) {
      if (n instanceof MethodDeclaration) {
	 MethodDeclaration md = (MethodDeclaration) n;
	 JcompSymbol js = JcompAst.getDefinition(md);
	 if (js == null) js = JcompAst.getDefinition(md.getName());
	 if (js != null) return ast_factory.getMethod(js);
       }
    }
   return null;
}

@Override public IfaceProgramPoint getNext()		{ return null; }

@Override public IfaceProgramPoint getPrevious()	{ return null; }

@Override public IfaceMethod getReferencedMethod()
{
   ASTNode n = eval_node;
   JcompSymbol js = JcompAst.getDefinition(n);
   if (js == null) {
      js = JcompAst.getReference(n);
    }
   if (js != null && js.isMethodSymbol()) {
      return ast_factory.getMethod(js);
    }

   return null;
}


@Override public IfaceMethod getCalledMethod() 
{
   if (eval_node instanceof MethodInvocation) {
      MethodInvocation mi = (MethodInvocation) eval_node;
      int ct = mi.arguments().size();
      if (ct > 0) {
         if (after_child != mi.arguments().get(ct-1)) return null;
       }
      else if (mi.getExpression() != null && after_child != mi.getExpression()) return null;
      return getReferencedMethod();
    }
   return null;
}



@Override public IfaceField getReferencedField()
{
   ASTNode n = eval_node;
   if (n instanceof ArrayAccess) {
      n = ((ArrayAccess) n).getArray();
    }
   JcompSymbol js = JcompAst.getDefinition(n);
   if (js == null) js = JcompAst.getReference(n);
   if (js != null) {
      return ast_factory.getField(js);
    }
   return null;
}

@Override public IfaceType getReferencedType()
{
   ASTNode n = eval_node;
   JcompType jt = JcompAst.getJavaType(n);
   if (jt == null) {
      jt = JcompAst.getExprType(n);
    }
   if (jt != null) {
      return ast_factory.getFullType(jt);
    }

   return null;
}

@Override public IfaceProgramPoint getReferencedTarget()
{
   return null;
}

@Override public List<IfaceProgramPoint> getReferencedTargets()
{
   return null;
}

@Override public boolean isInterfaceCall()
{
   JcompSymbol js = JcompAst.getReference(eval_node);
   if (js == null) js = JcompAst.getDefinition(eval_node);
   if (js == null) return true;

   return js.getClassType().isInterfaceType();
}

@Override public boolean isVirtualCall()
{
   JcompSymbol js = JcompAst.getReference(eval_node);

   if (js == null) js = JcompAst.getDefinition(eval_node);
   if (js == null) return true;
   if (js.isConstructorSymbol()) return false;
   if (js.isPrivate()) return false;
   if (js.isStatic()) return false;
   switch (eval_node.getNodeType()) {
      case ASTNode.SUPER_METHOD_INVOCATION :
	 return false;
    }

   return true;
}

@Override public boolean isMethodStart()
{
   return eval_node.getNodeType() == ASTNode.METHOD_DECLARATION &&
   after_child == null && run_status == null;
}


@Override public int getLineNumber()
{
   CompilationUnit cu = (CompilationUnit)(eval_node.getRoot());
   return cu.getLineNumber(eval_node.getStartPosition());
}


@Override public String getSourceFile() 
{
   if (eval_node == null) return null;
   JcompSource src = JcompAst.getSource(eval_node);
   if (src != null) return src.getFileName();
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Instance number methods                                                 */
/*                                                                              */
/********************************************************************************/

@Override public int getInstanceNumber()
{
   IfaceMethod find = getReferencedMethod();
   if (find == null) return -1;
   
   IfaceMethod m0 = getMethod();
   IfaceAstReference aref = getAstReference();
   ASTNode callnode = aref.getAstNode();
   
   IfaceProgramPoint pt0 = m0.getStart();
   IfaceAstReference aref0 = pt0.getAstReference();
   ASTNode methodnode = aref0.getAstNode();
   
   InstanceFinder fndr = new InstanceFinder(find,callnode);
   methodnode.accept(fndr);
   
   return fndr.getResult();
}



private class InstanceFinder extends ASTVisitor {
   
   private int cur_counter;
   private int result_counter;
   private IfaceMethod find_method;
   private ASTNode find_node;
   
   InstanceFinder(IfaceMethod im,ASTNode node) {
      find_method = im;
      find_node = node;
      cur_counter = 0;
      result_counter = -1;
    }
   
   int getResult()                              { return result_counter; }
   
   @Override public void preVisit(ASTNode n) {
      IfaceAstReference ar = ast_factory.getAstReference(n,null,null);
      if (n.getNodeType() == find_node.getNodeType()) {
         IfaceMethod m0 = ar.getReferencedMethod();
         if (m0 == find_method) {
            ++cur_counter;
            if (n == find_node) result_counter = cur_counter;
          }
       }
    }
   
}       // end of inner class InstanceFinder 




/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

boolean update()
{
   ASTNode n = ast_factory.mapAstNode(eval_node);
   if (n == null) {
      FaitLog.logE("Unknown AST Node on match: " + eval_node);
      return false;
    }
   eval_node = n;      
   after_child = ast_factory.mapAstNode(after_child);
   
   return true;
}



/********************************************************************************/
/*										*/
/*	Comparison operators					                */
/*										*/
/********************************************************************************/

@Override public int compareTo(ControlAstReference r)
{
   if (eval_node != r.eval_node) {
      if (eval_node == null) return -1;
      if (r.eval_node == null) return 1;
      int d = eval_node.hashCode() - r.eval_node.hashCode();
      if (d < 0) return -1;
      return 1;
    }
   if (after_child != r.after_child) {
      if (after_child == null) return -1;
      if (r.after_child == null) return 1;
      int d = after_child.hashCode() - r.after_child.hashCode();
      if (d < 0) return -1;
      return 1;
    }
   if (run_status == null && r.run_status == null) return 0;
   if (run_status == null && r.run_status != null) return -1;
   if (run_status != null && r.run_status == null) return 1;
   int s = run_status.getReason().ordinal() - r.run_status.getReason().ordinal();
   if (s < 0) return -1;
   if (s > 0) return 1;
   if (run_status.getValue() == r.run_status.getValue()) return 0;
   if (run_status.getValue() == null) return -1;
   if (r.run_status.getValue() == null) return 1;
   s = run_status.getValue().hashCode() - r.run_status.getValue().hashCode();
   if (s < 0) return -1;
   return 1;
}



// @Override public int hashCode() {
   // int hc = 0;
   // if (eval_node != null) hc += eval_node.hashCode();
   // if (after_child != null) hc += after_child.hashCode();
   // if (run_status != null) hc += run_status.hashCode();
   // return hc;
// }
// 
// @Override public boolean equals(Object o) {
   // if (o instanceof ControlAstReference) {
      // ControlAstReference ar = (ControlAstReference) o;
      // if (ar.eval_node != eval_node) return false;
      // if (ar.after_child != after_child) return false;
      // if (ar.run_status == null && run_status != null) return false;
      // if (ar.run_status != null) {
	 // if (run_status == null) return false;
	 // if (ar.run_status.getReason() != run_status.getReason()) return false;
	 // if (ar.run_status.getValue() != run_status.getValue()) return false;
       // }
      // return true;
    // }
   // return false;
// }




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw)
{
   xw.begin("POINT");
   xw.field("KIND","EDIT");
   if (eval_node != null) {
      xw.field("START",eval_node.getStartPosition());
      xw.field("END",eval_node.getStartPosition() + eval_node.getLength());
      xw.field("LINE",getLineNumber());
      String typ = eval_node.getClass().getName();
      int idx = typ.lastIndexOf(".");
      if (idx > 0) typ = typ.substring(idx+1);
      xw.field("NODETYPE",typ);
      xw.field("NODETYPEID",eval_node.getNodeType());
    }
   if (after_child != null) {
      StructuralPropertyDescriptor spd = after_child.getLocationInParent();
      xw.field("AFTER",spd.getId());
      xw.field("AFTERSTART",after_child.getStartPosition());
      String typ = after_child.getClass().getName();
      int idx = typ.lastIndexOf(".");
      if (idx > 0) typ = typ.substring(idx+1);
      xw.field("AFTERTYPE",typ);
      xw.field("AFTERTYPEID",after_child.getNodeType());
    }
   if (run_status != null) {
      xw.field("STATUS",run_status.getReason());
    }
   if (eval_node != null) xw.cdata(eval_node.toString());
   xw.end("POINT");
}


@Override public String toString()
{
   if (eval_node == null) 
      return "UNKNOWN POSITION";
   
   String typ = eval_node.getClass().getName();
   int idx = typ.lastIndexOf(".");
   if (idx > 0) typ = typ.substring(idx+1);
   if (after_child != null) {
      StructuralPropertyDescriptor spd = after_child.getLocationInParent();
      typ += "/" + spd.getId();
      if (spd.isChildListProperty()) {
         List<?> children = (List<?>) eval_node.getStructuralProperty(spd);
         int cidx = children.indexOf(after_child);
         typ += "/" + cidx;
       }
    }

   if (run_status == null) {
      return "@" + typ + ":" + eval_node.getStartPosition();
    }
   else {
      return "@" + typ + ":" + eval_node.getStartPosition() + "^" + run_status.getReason();
    }
}

}	// end of class ControlAstReference




/* end of ControlAstReference.java */

