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

import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAstStatus;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
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
      System.err.println("EVAL NODE SHOULDN'T BE NULL");
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
   if (js != null) {
       return ast_factory.getMethod(js);
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



/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

void update()
{
   eval_node = ast_factory.mapAstNode(eval_node);
   after_child = ast_factory.mapAstNode(after_child);
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
   xw.field("START",eval_node.getStartPosition());
   xw.field("END",eval_node.getStartPosition() + eval_node.getLength());
   String typ = eval_node.getClass().getName();
   int idx = typ.lastIndexOf(".");
   if (idx > 0) typ = typ.substring(idx+1);
   xw.field("NODETYPE",typ);
   if (after_child != null) {
      StructuralPropertyDescriptor spd = after_child.getLocationInParent();
      xw.field("AFTER",spd.getId());
    }
   if (run_status != null) {
      xw.field("STATUS",run_status.getReason());
    }
   xw.cdata(eval_node.toString());
   xw.end("POINT");
}


@Override public String toString()
{
   String typ = eval_node.getClass().getName();
   int idx = typ.lastIndexOf(".");
   if (idx > 0) typ = typ.substring(idx+1);

   return "@" + typ + ":" + eval_node.getStartPosition();
}

}	// end of class ControlAstReference




/* end of ControlAstReference.java */

