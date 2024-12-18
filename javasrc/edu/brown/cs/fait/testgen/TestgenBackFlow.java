/********************************************************************************/
/*                                                                              */
/*              TestgenBackFlow.java                                            */
/*                                                                              */
/*      Handle update test generation components on back flow                   */
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



package edu.brown.cs.fait.testgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.objectweb.asm.Type;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceStackMarker;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;

class TestgenBackFlow implements TestgenConstants, JcodeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceControl    fait_control;
private IfaceState      prior_state;
private IfaceState      end_state;
private IfaceProgramPoint execute_point;





/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TestgenBackFlow(IfaceControl fc,IfaceState endstate,IfaceState priorstate)
{
   fait_control = fc;
   prior_state = priorstate;
   end_state = endstate;
   execute_point = prior_state.getLocation().getProgramPoint();
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

List<TestgenValue> adjustReference(TestgenReferenceValue rv)
{
   List<TestgenValue> rslt = new ArrayList<>();
   
   if (execute_point == null || rv == null) return rslt;
   
   if (execute_point.getAstReference() != null)
      adjustAstReference(rv,rslt);
   else 
      adjustCodeReference(rv,rslt);
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Handle byte code reference adjustment                                   */
/*                                                                              */
/********************************************************************************/

// CHECKSTYLE:OFF
private void adjustCodeReference(TestgenReferenceValue tr,List<TestgenValue> rslt)
// CHECKSTYLE:ON
{
   JcodeInstruction ins = execute_point.getInstruction();
   int stk = tr.getRefStack();
   int var = tr.getRefSlot();
   IfaceField reffld = tr.getRefField();
   IfaceType settype = tr.getDataType();
   TestgenValue start = null;
   
   IfaceValue v0;
   IfaceValue v1;
   switch (ins.getOpcode()) {
/* OBJECT PROCESSING INSTRUTIONS */
      case DLOAD : case DLOAD_0 : case DLOAD_1 : case DLOAD_2 : case DLOAD_3 :
      case FLOAD : case FLOAD_0 : case FLOAD_1 : case FLOAD_2 : case FLOAD_3 :
      case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
      case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
      case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
	 if (stk == 0) {
            start = TestgenValue.createSlot(settype,ins.getLocalVariable());
	  }
	 else start = pushOne(tr);
	 break;
      case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
      case DSTORE : case DSTORE_0 : case DSTORE_1 : case DSTORE_2 : case DSTORE_3 :
      case FSTORE : case FSTORE_0 : case FSTORE_1 : case FSTORE_2 : case FSTORE_3 :
      case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
      case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :
	 if (var == ins.getLocalVariable()) {
            start = TestgenValue.createStack(settype,0);
	  }
	 else popOne(tr);
	 break;
      case DUP :
	 if (stk == 0 || stk == 1) {
            start = TestgenValue.createStack(settype,0);
	  }
	 else start = adjustRef(tr,1,2);
	 break;
      case DUP_X1 :
	 if (stk == 2) {
	    start = TestgenValue.createStack(settype,0);
	  }
	 else if (stk == 0 || stk == 1) {
	    start = tr;
	  }
	 else start = adjustRef(tr,2,3);
	 break;
      case DUP_X2 :
	 // need to take category 2 values into account
	 if (stk == 3) {
	    start = TestgenValue.createStack(settype,0);
	  }
	 else if (stk == 0 || stk == 1 || stk == 2) {
	    start = tr;
	  }
	 else start = adjustRef(tr,3,4);
	 break;
      case DUP2 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) {
	    if (stk == 0 || stk == 1) {
               start = TestgenValue.createStack(settype,0);
	     }
	    else start = adjustRef(tr,1,2);
	  }
	 else {
	    if (stk == 0 || stk == 2) {
	       start = TestgenValue.createStack(settype,0);
	     }
	    else if (stk == 1 || stk == 3) {
	       start = TestgenValue.createStack(settype,1);
	     }
	    else {
	       start = adjustRef(tr,2,4);
	     }
	  }
	 break;
      case DUP2_X1 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) {
	    if (stk == 2) {
	       start = TestgenValue.createStack(settype,0);
	     }
	    else if (stk == 0 || stk == 1) {
	       start = tr;
	     }
	    else start = adjustRef(tr,2,3);
	  }
	 else {
	    if (stk == 3) {
	       start = TestgenValue.createStack(settype,0);
	     }
	    else if (stk == 4) {
	       start = TestgenValue.createStack(settype,1);
	     }
	    else if (stk == 0 || stk == 1 || stk == 2) {
	       start = tr;
	     }
	    else start = adjustRef(tr,3,5);
	  }
	 break;
      case DUP2_X2 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) {
	    v1 = prior_state.getStack(1);
	    if (v1.isCategory2()) {
	       if (stk == 2) {
		  start = TestgenValue.createStack(settype,0);
		}
	       else if (stk == 0 || stk == 1) {
		  start = tr;
		}
	       else start = adjustRef(tr,2,3);
	     }
	    else {
	       if (stk == 3) {
		  start = TestgenValue.createStack(settype,0);
		}
	       else if (stk == 0 || stk == 1 || stk == 2) {
		  start = tr;
		}
	       else start = adjustRef(tr,3,4);
	     }
	  }
	 else {
	    v1 = prior_state.getStack(2);
	    if (v1.isCategory2()) {
	       if (stk == 3) {
		  start = TestgenValue.createStack(settype,0);
		}
	       else if (stk == 4) {
		  start = TestgenValue.createStack(settype,1);
		}
	       else if (stk == 0 || stk == 1 || stk == 2) start = tr;
	       else start = adjustRef(tr,3,5);
	     }
	    else {
	       if (stk == 4) {
		  start = TestgenValue.createStack(settype,0);
		}
	       else if (stk == 5) {
		  start = TestgenValue.createStack(settype,1);
		}
	       else if (stk == 0 || stk == 1 || stk == 2 || stk == 3) {
		  start = tr;
		}
	       else start = adjustRef(tr,4,6);
	     }
	  }
	 break;
      case MONITORENTER :
      case MONITOREXIT :
      case POP :
	 start = popOne(tr);
	 break;
      case POP2 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) start = adjustRef(tr,0,1);
	 else start = adjustRef(tr,0,2);
	 break;
      case SWAP :
	 if (stk == 0) {
	    start = TestgenValue.createStack(settype,1);
	  }
	 else if (stk == 1) {
	    start = TestgenValue.createStack(settype,0);
	  }
	 else start = adjustRef(tr,2,2);
	 break;
         
/* ARITHMETIC INSTRUCTIONS */
      case DADD : case DDIV : case DMUL : case DREM : case DSUB :
      case FADD : case FDIV : case FMUL : case FREM : case FSUB :
      case IADD : case IDIV : case IMUL : case IREM : case ISUB :
      case IAND : case IOR : case IXOR :
      case ISHL : case ISHR : case IUSHR :
      case LADD : case LDIV : case LMUL : case LREM : case LSUB :
      case LAND : case LOR : case LXOR :
      case LSHL : case LSHR : case LUSHR :
      case DCMPG : case DCMPL :
      case FCMPG : case FCMPL :
      case LCMP :
	 if (stk == 0) {
            addAuxRef(0,rslt);
            addAuxRef(1,rslt);
	    start = null;
	  }
	 else start = adjustRef(tr,2,1);
	 break;
      case BIPUSH :
      case SIPUSH :
      case NEW :
      case ACONST_NULL :
      case DCONST_0 :
      case DCONST_1 :
      case FCONST_0 :
      case FCONST_1 :
      case FCONST_2 :
      case LCONST_0 :
      case LCONST_1 :
      case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
      case ICONST_5 : case ICONST_M1 :
      case LDC :
      case LDC_W :
      case LDC2_W :
	 start = pushOne(tr);
	 break;
      case D2F : case FNEG : case I2F : case L2F :
      case D2I : case F2I : case L2I : case INEG :
      case D2L : case F2L : case I2L : case LNEG :
      case DNEG : case F2D : case I2D : case L2D :
      case I2B : case I2C : case I2S :
      case INSTANCEOF :
      case CHECKCAST :
	 if (tr.getRefStack() == 0) {
            addAuxRef(0,rslt);
	    start = null;
	  }
	 else start = adjustRef(tr,1,1);
	 break;
      case IINC :
	 if (var == ins.getLocalVariable()) {
            addAuxVarRef(ins.getLocalVariable(),rslt);
            start = null;
          }
	 break;
      case NOP :
	 start = tr;
	 break;
         
/* BRANCH INSTRUCTIONS */
      case GOTO :
      case GOTO_W :
	 start = tr;
         break;
      case IF_ACMPEQ : case IF_ACMPNE :
      case IF_ICMPEQ : case IF_ICMPNE :
      case IF_ICMPLT : case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
	 start = adjustRef(tr,2,0);
	 break;
      case IFEQ : case IFNE : case IFLT : case IFGE : case IFGT : case IFLE :
      case IFNONNULL : case IFNULL :
	 start = adjustRef(tr,1,0);
	 break;
      case LOOKUPSWITCH :
      case TABLESWITCH :
	 start = popOne(tr);
	 break;
         
/* SUBROUTINE CALLS */
      case JSR : case JSR_W :
	 start = pushOne(tr);
	 break;
      case RET :
	 start = tr;
	 break;
         
/* CALL INSTRUCTIONS */
      case ARETURN :
      case DRETURN : case FRETURN : case IRETURN : case LRETURN :
      case ATHROW :
	 start = popOne(tr);
	 break;
      case RETURN :
	 start = tr;
	 break;
      case INVOKEINTERFACE :
      case INVOKESPECIAL :
      case INVOKESTATIC :
      case INVOKEVIRTUAL :
	 IfaceMethod fm = execute_point.getReferencedMethod();
	 int act = fm.getNumArgs();
	 if (!fm.isStatic()) act += 1;
	 if (fm.getReturnType() == null || fm.getReturnType().isVoidType()) {
	    start = adjustRef(tr,act,0);
	  }
	 else if (tr.getRefStack() == 0) {
	    start = null;
	  }
	 else if (reffld != null) {
            addAuxRefs(reffld,rslt);
            start = null;
          }
	 else start = adjustRef(tr,act,1);
	 break;
      case INVOKEDYNAMIC :
         int narg = 0;
         String [] args = ins.getDynamicReference();
         if (args[1] != null && !args[1].startsWith("()")) {
            Type [] ar = Type.getArgumentTypes(args[1]);
            narg = ar.length;
          }
         if (stk == 0) {
            for (int i = 0; i < narg; ++i) addAuxRef(i,rslt);
            start = null;
          }
         else start = adjustRef(tr,narg,1);
	 break;
         
/* ARRAY PROCESSING INSTRUCTIONS */
      case AALOAD :
      case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
	 if (stk == 0) {
            addAuxArrayRefs(prior_state.getStack(1),rslt);
	    start = null;
	  }
	 else start = adjustRef(tr,2,1);
	 break;
      case AASTORE :
      case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
	 start = adjustRef(tr,3,0);
	 break;
      case ANEWARRAY :
      case NEWARRAY :
	 start = adjustRef(tr,1,1);
	 break;
      case MULTIANEWARRAY :
	 start = adjustRef(tr,ins.getIntValue(),1);
	 break;
      case ARRAYLENGTH :
         if (stk == 0) {
            addAuxArrayRefs(prior_state.getStack(0),rslt);
          }
	 start = adjustRef(tr,1,1);
	 break;
         
/* FIELD INSTRUCTIONS */
      case GETFIELD :
	 if (stk == 0) {
            IfaceField fld = execute_point.getReferencedField();
            IfaceValue vobj = prior_state.getStack(0);
            if (prior_state.getFieldValue(fld) != null && vobj == prior_state.getLocal(0)) {
               TestgenValue base = TestgenValue.create(prior_state.getStack(0));
               start = TestgenValue.createFieldRef(base,fld);
             }
            else {
               addAuxRefs(fld,rslt);
               start = null;
             }
	  }
	 else start = adjustRef(tr,1,1);
	 break;
      case GETSTATIC :
	 if (stk == 0) {
            addAuxRefs(execute_point.getReferencedField(),rslt);
            start = null;
	  }
	 else start = adjustRef(tr,0,1);
	 break;
      case PUTFIELD :
	 start = adjustRef(tr,2,0);
	 break;
      case PUTSTATIC :
	 start = adjustRef(tr,1,0);
	 break;
         
      default :
	 FaitLog.logE("FAIT: Opcode " + ins.getOpcode() + " not found");
	 break;
    }
   if (start != null) {
      rslt.add(start);
    }
}



/********************************************************************************/
/*                                                                              */
/*      AST methods                                                             */
/*                                                                              */
/********************************************************************************/

private void adjustAstReference(TestgenReferenceValue rv,List<TestgenValue> rslt)
{
   IfaceAstReference astref = execute_point.getAstReference();
   ASTNode n = astref.getAstNode();
   
   BackVisitor bv = new BackVisitor(astref,rv);
   n.accept(bv);
   rslt.addAll(bv.getAuxRefs());
   TestgenValue start = bv.getStartReference();
   if (start != null) rslt.add(start);
}


private class BackVisitor extends ASTVisitor {
   
   private ASTNode after_node;
   private TestgenReferenceValue end_ref;
   private TestgenValue start_ref;
   private List<TestgenValue> aux_refs;
   
   BackVisitor(IfaceAstReference ref,TestgenReferenceValue rv) {
      after_node = ref.getAfterChild();
      end_ref = rv;
      start_ref = null;
      aux_refs = new ArrayList<>();
    }
   
   TestgenValue getStartReference() {
      return start_ref;
    }
   
   List<TestgenValue> getAuxRefs() {
      return aux_refs; 
    }
   
   @Override public boolean visit(ArrayAccess v) {
      if (after_node == null || after_node == v.getArray()) start_ref = end_ref;
      else if (end_ref.getRefStack() == 0) {
         addAuxArrayRefs(prior_state.getStack(1),aux_refs);
         start_ref = null;
       }
      else start_ref = adjustRef(end_ref,2,1);
      return false;
    } 
   
   @Override public boolean visit(ArrayCreation v) {
      if (after_node == null) noChange();
      else if (v.getInitializer() != null && after_node == v.getInitializer()) noChange();
      else {
         List<?> dims = v.dimensions();
         int idx = dims.indexOf(after_node);
         if (idx < dims.size()-1) start_ref = end_ref;
         else {
            int ct = (v.getInitializer() != null ? 1 : 0);
            ct += dims.size();
            start_ref = adjustRef(end_ref,ct,1);
          }
       }
      return false;
    }
   
   @Override public boolean visit(ArrayInitializer n) {
      List<?> exprs = n.expressions();
      int idx = 0;
      if (after_node != null) idx = exprs.indexOf(after_node) + 1;
      if (idx < exprs.size()) noChange();
      else start_ref = adjustRef(end_ref,exprs.size(),1);
      
      return false;
    }
   
   @Override public boolean visit(AssertStatement s) {
      if (after_node == null) noChange();
      else popOne();
      return false;
    }
   
   @Override public boolean visit(Assignment v) {
      if (after_node != v.getRightHandSide()) noChange();
      else {
         IfaceValue asgval = prior_state.getStack(1);
         if (asgval.getRefSlot() == end_ref.getRefSlot()) {
            addAuxRef(0,aux_refs);
            noBack();
          }
         else if (asgval.getRefField() == end_ref.getRefField()) {
            addAuxRef(0,aux_refs);
            noBack();
          }
         else {
            start_ref = adjustRef(end_ref,2,1);
          }
         if (v.getOperator() != Assignment.Operator.ASSIGN) {
            if (asgval.getRefSlot() >= 0) {
               addAuxVarRef(asgval.getRefSlot(),aux_refs);
             }
            else if (asgval.getRefField() != null) {
               addAuxRef(asgval,aux_refs);
             }
          }
       }
      return false;
    }
   
   @Override public boolean visit(CatchClause cc) {
      if (after_node != null) noChange();
      else if (end_ref.getRefStack() == 0) {
         JcompSymbol js = JcompAst.getDefinition(cc.getException().getName());
         int slot = getSlot(js);
         start_ref = TestgenValue.createSlot(end_ref.getDataType(),slot);
       }
      else {
         popOne();
       }   
      return false;
    }
   
   @Override public boolean visit(ClassInstanceCreation v) {
      List<?> args = v.arguments();
      JcompType rty = JcompAst.getJavaType(v.getType());
      if (after_node == null && args.size() == 0) {
         pushOne();
       }
      else if (after_node == null) {
         int ct = (rty.needsOuterClass() ? 3 : 2);
         start_ref = adjustRef(end_ref,ct,0);
       }
      else {
         int idx = args.indexOf(after_node)+1;
         if (idx < args.size()) noChange();
         else start_ref = adjustRef(end_ref,args.size()+1,0);
       }
      return false;
    }
   
   @Override public boolean visit(ConditionalExpression v) {
      if (after_node == null) noChange();
      else if (after_node == v.getExpression()) {
         popOne();
       }
      else {
         noChange();
       }
      return false;
    }
   
   @Override public boolean visit(ConstructorInvocation v) {
      JcompSymbol rtn = JcompAst.getReference(v);
      JcompType rty = rtn.getClassType();
      if (after_node == null) {
         int ct = (rty.needsOuterClass() ? 2 : 1);
         start_ref = adjustRef(end_ref,0,ct);
       }
      else {
         List<?> args = v.arguments();
         int idx = args.indexOf(after_node) + 1;
         if (idx < args.size()) noChange();
         else start_ref = null;
       }
      return false;
    }
   
   @Override public boolean visit(CreationReference v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_ref = null;
       }
      else pushOne();
      return false;
    }
   
   @Override public boolean visit(DoStatement s) {
      if (after_node == null || after_node == s.getBody()) noChange();
      else popOne();
      return false;
    }
   
   @Override public boolean visit(EnhancedForStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         int slot = getSlot(JcompAst.getDefinition(s.getParameter().getName()));
         if (end_ref.getRefSlot() == slot) start_ref = null;
         else popOne();
       }
      else noChange();
      return false;
    }
   
   @Override public boolean visit(ExpressionMethodReference v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_ref = null;
       }
      else {
         JcompSymbol js = JcompAst.getReference(v);
         if (after_node == null && !js.isStatic()) {
            ASTNode next = v.getExpression();
            JcompType jty = JcompAst.getJavaType(next);
            JcompType ety = JcompAst.getExprType(next);
            if (jty == null || jty != ety) {
               return noChange();
             }
          }
         int ct = 0;
         if (!js.isStatic() && after_node != null && after_node == v.getExpression()) {
            ct = 1;
          }
         start_ref = adjustRef(end_ref,ct,1);
       }
      return false;
    }
   
   @Override public boolean visit(ExpressionStatement s) {
      if (after_node == null) noChange();
      else popOne();
      return false;
    }
   
   @Override public boolean visit(FieldAccess v) {
      if (after_node == null) noChange();
      else if (end_ref.getRefStack() == 0) {
         JcompSymbol sym = JcompAst.getReference(v.getName());
         IfaceField fld = getField(sym);
         if (prior_state.getStack(0) == prior_state.getLocal(0) && 
               !execute_point.getMethod().isStatic() && prior_state.getFieldValue(fld) != null) {
            TestgenValue base = TestgenValue.create(prior_state.getStack(0));
            start_ref = TestgenValue.createFieldRef(base,fld);
          }
         else {
            start_ref = null;
            addAuxRefs(fld,aux_refs);
          }
       }
      else start_ref = adjustRef(end_ref,1,1);
      return false;
    }
   
   @Override public boolean visit(ForStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         popOne();
       }
      else noChange();
      return false;
    }
   
   @Override public boolean visit(IfStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         popOne();
       }
      else noChange();
      return false;
    }
   
   @Override public boolean visit(InfixExpression v) {
      if (after_node == null) noChange();
      else if (after_node == v.getLeftOperand()) return noChange();
      else if (end_ref.getRefStack() == 0) {
         start_ref = null;
         addAuxRef(0,aux_refs);
         addAuxRef(1,aux_refs);
       }
      else start_ref = adjustRef(end_ref,2,1);
      return false;
    }
   
   @Override public boolean visit(InstanceofExpression v) {
      if (after_node == null) noChange();
      else if (end_ref.getRefStack() == 0) {
         start_ref = null;
         addAuxRef(0,aux_refs);
       }
      else {
         start_ref = adjustRef(end_ref,1,1);
       }
      return false;
    }
   
   @Override public boolean visit(LambdaExpression v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_ref = null;
       }
      else pushOne();
      return false;
    }
   
   @Override public boolean visit(MethodInvocation v) {
      JcompSymbol js = JcompAst.getReference(v.getName());
      int dref = 0;
      if (after_node == null && v.getExpression() == null) {
         if (!js.isStatic()) {
            // this implicitly pushed onto stack
            dref = 1;
          }
       }
      else if (after_node == null) {
         return noChange();
       }
      
      List<?> args = v.arguments();
      int idx = args.indexOf(after_node)+1;
      if (idx < args.size()) {
         if (dref != 0) start_ref = adjustRef(end_ref,0,dref);
         else noChange();
       }
      else {
         int ret = (js.getType().getBaseType().isVoidType() ? 0 : 1);
         if (js.isConstructorSymbol()) ret = 0;
         if (end_ref.getRefStack() == 0 && ret == 1) {
            start_ref = null;
          }
         else if (end_ref.getRefStack() < 0 && end_ref.getRefSlot() < 0) start_ref = null;
         else {
            int sz = args.size();
            if (!js.isStatic()) sz += 1;
            start_ref = adjustRef(end_ref,sz,ret);
          }
       }
      return false;
    }
   
   @Override public boolean visit(PostfixExpression v) {
      if (after_node == null) return noChange();
      IfaceValue v1 = prior_state.getStack(0);
      if (end_ref.getRefSlot() == v1.getRefSlot() && v1.getRefSlot() > 0) {
         start_ref = null;
         addAuxVarRef(v1.getRefSlot(),aux_refs);
       }
      else if (end_ref.getRefStack() == 0) {
         start_ref = null;
         addAuxRef(0,aux_refs);
       }
      else start_ref = adjustRef(end_ref,1,1);
      return false;
    }
   
   @Override public boolean visit(PrefixExpression v) {
      if (after_node == null) return noChange();
      IfaceValue v1 = prior_state.getStack(0);
      if (v1 == null) return noBack();
      if (end_ref.getRefSlot() == v1.getRefSlot() && v1.getRefSlot() > 0) {
         addAuxVarRef(v1.getRefSlot(),aux_refs);
         start_ref = null;
       }
      else if (end_ref.getRefStack() == 0) {
         addAuxRef(0,aux_refs);
         start_ref = null;
       }
      else start_ref = adjustRef(end_ref,1,1);
      return false;
    }
   
   @Override public boolean visit(QualifiedName v) {
      if (after_node == null) return noChange();
      JcompSymbol sym = JcompAst.getReference(v.getName());
      if (after_node == v.getQualifier() && sym != null) {
         if (end_ref.getRefStack() == 0) {
            IfaceValue v0 = prior_state.getStack(0);
            IfaceField fld = getField(sym);
            if (fld == null) start_ref = null;
            else { 
               TestgenValue base = TestgenValue.create(v0);
               start_ref = TestgenValue.createFieldRef(base,fld);
             }
          }
         else start_ref =  adjustRef(end_ref,1,1);
       }
      else if (after_node == v.getQualifier() && sym == null) {
         start_ref = adjustRef(end_ref,1,1);
       }
      else start_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(ReturnStatement s) {
      if (after_node == null && s.getExpression() != null) start_ref = end_ref;
      else if (s.getExpression() != null && end_ref.getRefStack() == 0) {
         noChange();
       }
      else popOne();
      return false;
    }
   
   @Override public boolean visit(SimpleName v) {
      JcompSymbol js = JcompAst.getReference(v);
      if (end_ref.getRefStack() == 0) {
         if (js != null && js.isFieldSymbol()) {
            IfaceField fld = getField(js);
            if (!js.isStatic() && prior_state.getFieldValue(fld) != null) {
               IfaceValue thisv = getThisValue(fld.getDeclaringClass());
               TestgenValue base = TestgenValue.create(thisv);
               start_ref = TestgenValue.createFieldRef(base,fld);
             }
            else {
               start_ref = null;
               addAuxRefs(fld,aux_refs);
             }
          }
         else if (js != null && js.isEnumSymbol()) {
            start_ref = null;
          }
         else {
            int slot = getSlot(js);
            addAuxVarRef(slot,aux_refs);
            start_ref = null;
          }
       }
      else start_ref = adjustRef(end_ref,0,1);
      return false;
    }
   
   @Override public boolean visit(SingleVariableDeclaration n) {
      if (after_node == null && n.getInitializer() != null) start_ref = end_ref;
      else if (n.getInitializer() != null) start_ref = adjustRef(end_ref,1,0);
      else start_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(SuperMethodReference v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_ref = null;
       }
      else start_ref = adjustRef(end_ref,0,1);
      return false;
    }
   
   @Override public boolean visit(SwitchStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         start_ref =  adjustRef(end_ref,1,0);
       }
      else start_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(SynchronizedStatement s) {
      if (after_node == null) start_ref = end_ref;
      else if (after_node == s.getExpression()) {
         start_ref = adjustRef(end_ref,0,1);
       }
      else start_ref = adjustRef(end_ref,1,0);
      return false;
    }
   
   @Override public boolean visit(ThrowStatement s) {
      if (after_node == null) start_ref = end_ref;
      else start_ref = adjustRef(end_ref,1,0);
      return false;
    }
   
   @Override public boolean visit(TryStatement s) {
      if (after_node == null) {
         start_ref =  adjustRef(end_ref,0,1);
       }
      else if (after_node == s.getFinally()) {
         start_ref = adjustRef(end_ref,1,0);
       }
      else {
         start_ref = end_ref;   
         IfaceValue v = prior_state.getStack(0);
         if (v instanceof IfaceStackMarker) {
            IfaceStackMarker mkr = (IfaceStackMarker) v;
            Set<Object> vals = mkr.getMarkerValues();
            if (vals.contains(TryState.BODY) ||
                  vals.contains(TryState.CATCH)) {
               Block b = s.getFinally();
               if (b != null) start_ref = adjustRef(end_ref,0,1);
             }
          }
       }
      return false;
    }
   
   @Override public boolean visit(VariableDeclarationFragment v) {
      if (after_node != null && after_node == v.getInitializer()) {
         JcompSymbol sym = JcompAst.getDefinition(v);
         start_ref = adjustRef(end_ref,1,0);
         if (sym != null) {
            int defslot = end_state.getLocation().getMethod().getLocalOffset(sym);
            if (defslot == end_ref.getRefSlot()) {
               addAuxRef(0,aux_refs);
               start_ref = null;
             }
          }
       }
      else start_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(WhileStatement s) {
      if (after_node == s.getExpression()) {
         start_ref = adjustRef(end_ref,1,0);
       }
      else start_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(AnnotationTypeDeclaration n)          { return noChange(); }
   @Override public boolean visit(AnnotationTypeMemberDeclaration n)    { return noChange(); } 
   @Override public boolean visit(AnonymousClassDeclaration n)          { return noChange(); } 
   @Override public boolean visit(ArrayType n)                          { return noChange(); } 
   @Override public boolean visit(Block n)                              { return noChange(); }  
   @Override public boolean visit(BlockComment n)                       { return noChange(); } 
   @Override public boolean visit(BooleanLiteral n)                     { return pushOne(); } 
   @Override public boolean visit(BreakStatement n)                     { return noChange(); } 
   @Override public boolean visit(CastExpression n)                     { return noChange(); }  
   @Override public boolean visit(CharacterLiteral n)                   { return pushOne(); } 
   @Override public boolean visit(CompilationUnit n)                    { return noChange(); } 
   @Override public boolean visit(ContinueStatement n)                  { return noChange(); } 
   @Override public boolean visit(Dimension n)                          { return noChange(); } 
   @Override public boolean visit(EmptyStatement n)                     { return noChange(); } 
   @Override public boolean visit(EnumConstantDeclaration n)            { return noChange(); } 
   @Override public boolean visit(EnumDeclaration n)                    { return noChange(); } 
   @Override public boolean visit(FieldDeclaration n)                   { return noChange(); } 
   @Override public boolean visit(ImportDeclaration n)                  { return noChange(); } 
   @Override public boolean visit(Initializer n)                        { return noChange(); } 
   @Override public boolean visit(IntersectionType n)                   { return noChange(); } 
   @Override public boolean visit(Javadoc n)                            { return noChange(); } 
   @Override public boolean visit(LabeledStatement n)                   { return noChange(); } 
   @Override public boolean visit(LineComment n)                        { return noChange(); } 
   @Override public boolean visit(MarkerAnnotation n)                   { return noChange(); } 
   @Override public boolean visit(MemberRef n)                          { return noChange(); } 
   @Override public boolean visit(MemberValuePair n)                    { return noChange(); } 
   @Override public boolean visit(MethodDeclaration n)                  { return noChange(); } 
   @Override public boolean visit(MethodRef n)                          { return noChange(); } 
   @Override public boolean visit(MethodRefParameter n)                 { return noChange(); } 
   @Override public boolean visit(Modifier n)                           { return noChange(); } 
   @Override public boolean visit(NameQualifiedType n)                  { return noChange(); } 
   @Override public boolean visit(NormalAnnotation n)                   { return noChange(); } 
   @Override public boolean visit(NullLiteral n)                        { return pushOne(); }
   @Override public boolean visit(NumberLiteral n)                      { return pushOne(); }
   @Override public boolean visit(PackageDeclaration n)                 { return noChange(); }
   @Override public boolean visit(ParameterizedType n)                  { return noChange(); } 
   @Override public boolean visit(ParenthesizedExpression n)            { return noChange(); } 
   @Override public boolean visit(PrimitiveType n)                      { return noChange(); }  
   @Override public boolean visit(QualifiedType n)                      { return noChange(); }
   @Override public boolean visit(SimpleType n)                         { return noChange(); }
   @Override public boolean visit(SingleMemberAnnotation n)             { return noChange(); }
   @Override public boolean visit(StringLiteral n)                      { return pushOne(); }
   
   @Override public boolean visit(TextBlock n)                          { return pushOne(); }
   @Override public boolean visit(SuperConstructorInvocation n)         { return noBack(); }
   @Override public boolean visit(SuperFieldAccess n)                   { return pushOne(); }
   @Override public boolean visit(SuperMethodInvocation n)              { return noBack(); }
   @Override public boolean visit(SwitchCase n)                         { return noChange(); }
   @Override public boolean visit(TagElement n)                         { return noChange(); }
   @Override public boolean visit(TextElement n)                        { return noChange(); }
   @Override public boolean visit(ThisExpression n)                     { return pushOne(); }
   @Override public boolean visit(TypeDeclaration n)                    { return noChange(); }
   @Override public boolean visit(TypeDeclarationStatement n)           { return noChange(); }
   @Override public boolean visit(TypeLiteral n)                        { return pushOne(); }
   @Override public boolean visit(TypeMethodReference n)                { return noChange(); }
   @Override public boolean visit(TypeParameter n)                      { return noChange(); }
   @Override public boolean visit(UnionType n)                          { return noChange(); }
   @Override public boolean visit(VariableDeclarationExpression n)      { return noChange(); }
   @Override public boolean visit(VariableDeclarationStatement n)       { return noChange(); }
   @Override public boolean visit(WildcardType n)                       { return noChange(); }
   
   
   private int getSlot(Object js) {
      return execute_point.getMethod().getLocalOffset(js);
    }
   
   private IfaceField getField(JcompSymbol sym) {
      IfaceType fcls = fait_control.findDataType(sym.getClassType().getName());
      IfaceField fld = fait_control.findField(fcls,sym.getFullName());
      if (fld != null) return fld;
      return fld;
    }
   
   private IfaceValue getThisValue(IfaceType typ) {
      IfaceValue thisv = prior_state.getLocal(0);
      if (thisv == null)
         thisv = prior_state.getLocal(getSlot("this"));
      if (thisv == null)
         return null;
      IfaceType thistyp = thisv.getDataType();
      if (thistyp.equals(typ)) return thisv;
      if (thistyp.isDerivedFrom(typ)) return thisv;
      for (IfaceValue nthis = thisv; nthis != null; ) {
         IfaceType ntyp = nthis.getDataType();
         IfaceField xfld = fait_control.findField(ntyp,"this$0");
         if (xfld == null) break;
         IfaceType xtyp = xfld.getType();
         // IfaceValue rval = fait_control.findRefValue(xtyp,nthis,xfld);
         // NEED TO FIX THIS
         // nthis = getActualValue(rval,true);
         if (xtyp.isDerivedFrom(typ)) return nthis;
       }
      return thisv;
    }
 
   private boolean noBack() {
      start_ref = null;
      return false;
    }
   
   private boolean noChange() {
      start_ref = end_ref;
      return false;
    }
   
   
   private boolean pushOne() {
      start_ref = adjustRef(end_ref,0,1);
      return false;
    }
   
   
   private boolean popOne() {
      start_ref = adjustRef(end_ref,1,0);
      return false;
    }
   
   
   
}       // end of inner class BackVisitor

/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

TestgenValue pushOne(TestgenReferenceValue rv)
{
   return adjustRef(rv,0,1);
}

TestgenValue popOne(TestgenReferenceValue rv)
{
   return adjustRef(rv,1,0);
}


TestgenValue adjustRef(TestgenReferenceValue rv,int pop,int push)
{
   if (rv == null) return null;
   int idx = rv.getRefStack();
   if (idx >= 0) {
      if (idx < push) return null;
      if (pop == push) return rv;
      return TestgenValue.createStack(rv.getDataType(),idx-push+pop);
    }
   return rv;
}



/********************************************************************************/
/*                                                                              */
/*      Methods to handle side references                                       */
/*                                                                              */
/********************************************************************************/

void addAuxRef(int stk,List<TestgenValue> rslt)
{ }



void addAuxVarRef(int stk,List<TestgenValue> rslt)
{ }

void addAuxRefs(IfaceField fld,List<TestgenValue> rslt)
{ }

void addAuxArrayRefs(IfaceValue arr,List<TestgenValue> rslt)
{ }

void addAuxRef(IfaceAuxReference ref,List<TestgenValue> rslt)
{ }

void addAuxRef(IfaceValue val,List<TestgenValue> rslt)
{ }



}       // end of class TestgenBackFlow




/* end of TestgenBackFlow.java */

