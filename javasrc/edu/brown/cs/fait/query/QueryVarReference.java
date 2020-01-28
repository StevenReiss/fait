/********************************************************************************/
/*                                                                              */
/*              QueryVarReference.java                                          */
/*                                                                              */
/*      Computer reference and program point for a variable query               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
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



package edu.brown.cs.fait.query;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;


class QueryVarReference extends ASTVisitor implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceControl fait_control;
private IfaceMethod for_method;
private IfaceValue ref_value;
private IfaceType ref_type;
private ASTNode base_node;
private ASTNode after_node;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryVarReference(IfaceControl ctrl,IfaceMethod m,ASTNode n)
{
   fait_control = ctrl;
   for_method = m;
   
   ref_value = null;
   ref_type = null;
   base_node = null;
   after_node = null;
   
   n.accept(this);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

IfaceAstReference getAstReference()
{
   if (base_node == null) return null;
   
   return fait_control.getAstReference(base_node,after_node);
}



IfaceValue getRefValue()
{
   return ref_value;
}



/********************************************************************************/
/*                                                                              */
/*      Simple name processing                                                  */
/*                                                                              */
/********************************************************************************/

@Override public boolean visit(SimpleName n) 
{
   JcompSymbol js = JcompAst.getReference(n);
   if (js == null) js = JcompAst.getDefinition(n);
   if (js == null) return false;
   getTypeData(js.getType());
   if (js.isFieldSymbol()) {
      handleFieldReference(n,js);
    }
   else if (js.isMethodSymbol()) {
      handleMethodReference(n,js);
    }
   else if (js.isEnumSymbol()) return false;
   else if (js.isTypeSymbol()) return false;
   else if (js.isConstructorSymbol()) return false;
   else {
      handleVariableReference(n,js);
    }
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Handle fields                                                           */
/*                                                                              */
/********************************************************************************/

private void handleFieldReference(ASTNode n,JcompSymbol js) 
{
   ASTNode refnode = n;
   
   switch (n.getParent().getNodeType()) {
      case ASTNode.QUALIFIED_NAME :
         QualifiedName qn = (QualifiedName) n.getParent();
         if (qn.getName() != n) break;
         if (!js.isStatic()) refnode = qn;
         break;
      case ASTNode.FIELD_ACCESS :
         FieldAccess fac = (FieldAccess) n.getParent();
         if (fac.getName() != n) break;
         refnode = fac;
         break;
      case ASTNode.SUPER_FIELD_ACCESS :
         refnode = n.getParent();
         break;
      case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) n.getParent();
         if (vdf.getName() != n) break;
         if (vdf.getInitializer() == null) return;
         after_node = vdf.getInitializer();
         base_node = vdf;
         ref_value = fait_control.findRefStackValue(ref_type,0);
         return;
      default :
         break;
    }
   
   // refnode is the node where n is used
   // the next state is where n should be in the stack
   after_node = refnode;
   base_node = refnode.getParent();
   ref_value = fait_control.findRefStackValue(ref_type,0);
}




/********************************************************************************/
/*                                                                              */
/*      Handle method symbols                                                   */
/*                                                                              */
/********************************************************************************/

private void handleMethodReference(ASTNode n,JcompSymbol js)
{
   if (js.getType().getBaseType().isVoidType()) return;
   
   switch (n.getParent().getNodeType()) {
      case ASTNode.METHOD_INVOCATION :
         MethodInvocation mi = (MethodInvocation) n.getParent();
         if (mi.getName() != n) return;
         after_node = mi;
         base_node = mi.getParent();
         ref_value = fait_control.findRefStackValue(ref_type,0);
         break;
      case ASTNode.METHOD_DECLARATION :
         // should flag all returns?
         // is there a way of referencing the dynamic returned value?
         break;
      default :
         break;
    }
   
   return;
}



/********************************************************************************/
/*                                                                              */
/*      Handle Local variables                                                 */
/*                                                                              */
/********************************************************************************/

private void handleVariableReference(ASTNode n,JcompSymbol js)
{
   ASTNode refnode = n;
   
   switch (n.getParent().getNodeType()) {
      case ASTNode.SINGLE_VARIABLE_DECLARATION :
      case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
         VariableDeclaration vd = (VariableDeclaration) n.getParent();
         if (vd.getName() == n) {
            if (vd.getInitializer() != null) {
               after_node = vd.getInitializer();
               base_node = vd;
               ref_value = fait_control.findRefStackValue(ref_type,0);
               return;
             }
            else if (vd.getParent() instanceof MethodDeclaration) {
               int slot = for_method.getLocalOffset(js);
               after_node = null;
               base_node = vd.getParent();
               ref_value = fait_control.findRefValue(ref_type,slot);
               return;
             }
            else if (vd.getParent() instanceof CatchClause) {
               after_node = null;
               base_node = vd.getParent();
               ref_value = fait_control.findRefStackValue(ref_type,0);
               return;
             }
            else return;
          }
         break;
    }
   
   after_node = refnode;
   base_node = refnode.getParent();
   ref_value = fait_control.findRefStackValue(ref_type,0);
}




/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private void getTypeData(JcompType jt)
{
   ref_type = fait_control.findDataType(jt.getName());
}


}       // end of class QueryVarReference




/* end of QueryVarReference.java */

