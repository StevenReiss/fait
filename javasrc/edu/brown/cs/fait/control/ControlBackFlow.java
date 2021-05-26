/********************************************************************************/
/*                                                                              */
/*              ControlBackFlow.java                                            */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.fait.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

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

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAstStatus;
import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceBackFlow;
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

class ControlBackFlow implements IfaceBackFlow, FaitConstants, JcodeConstants   
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceControl    fait_control;
private IfaceState      prior_state;
private IfaceState      end_state;
private List<IfaceValue> end_refs;
private IfaceProgramPoint execute_point;
private boolean         note_conditions;

private List<IfaceValue> start_refs;
private Collection<IfaceAuxReference> aux_refs;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlBackFlow(IfaceControl fc,IfaceState endstate,IfaceState priorstate,IfaceValue endref)
{
   fait_control = fc;
   prior_state = priorstate;
   end_state = endstate;
   end_refs = new ArrayList<>();
   if (endref != null) end_refs.add(endref);
   execute_point = prior_state.getLocation().getProgramPoint();
   start_refs = null;
   note_conditions = false;
}



ControlBackFlow(IfaceControl fc,IfaceState endstate,IfaceState priorstate,Collection<IfaceAuxReference> refs)
{
   this(fc,endstate,priorstate,null,refs);
}


ControlBackFlow(IfaceControl fc,IfaceState endstate,IfaceState priorstate,Collection<IfaceValue> endrefs,
      Collection<IfaceAuxReference> refs)
{
   fait_control = fc;
   prior_state = priorstate;
   end_state = endstate;
   end_refs = new ArrayList<>();
   if (endrefs != null) end_refs.addAll(endrefs);
   execute_point = prior_state.getLocation().getProgramPoint();
   start_refs = null;
   note_conditions = false;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceValue getStartReference()
{
   if (start_refs == null) return null;
   if (start_refs.isEmpty()) return null;
   return start_refs.get(0);
}

@Override public Collection<IfaceValue> getStartReferences()    { return start_refs; }

@Override public Collection<IfaceAuxReference> getAuxRefs()     { return aux_refs; }




/********************************************************************************/
/*                                                                              */
/*      Computation methods                                                     */
/*                                                                              */
/********************************************************************************/

void computeBackFlow()
{
   computeBackFlow(false);
}


void computeBackFlow(boolean conds)
{
   if (execute_point == null || end_refs == null || end_refs.isEmpty()) return;
   
   note_conditions = conds;
   
   for (IfaceValue eref : end_refs) {
      IfaceValue sref = null;
      if (execute_point.getAstReference() != null) 
         sref = computeAstBackFlow(eref);
      else
         sref = computeByteCodeBackFlow(eref);
      if (sref != null) {
         if (start_refs == null) start_refs = new ArrayList<>();
         start_refs.add(sref);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle byte code back flow                                              */
/*                                                                              */
/********************************************************************************/

IfaceValue computeByteCodeBackFlow(IfaceValue eref)
{
   JcodeInstruction ins = execute_point.getInstruction();
   IfaceValue sref = null;
   
   int stk = eref.getRefStack();
   int var = eref.getRefSlot();
   IfaceField reffld = eref.getRefField();
   IfaceType settype = eref.getDataType();
   
   if (FaitLog.isTracing()) {
      FaitLog.logD("Work BACK on " + ins + " " + eref + " " + var + " " + stk + " " + 
            eref.getRefField());
    }
   
   IfaceValue v0,v1;
   switch (ins.getOpcode()) {
/* OBJECT PROCESSING INSTRUTIONS */
      case DLOAD : case DLOAD_0 : case DLOAD_1 : case DLOAD_2 : case DLOAD_3 :
      case FLOAD : case FLOAD_0 : case FLOAD_1 : case FLOAD_2 : case FLOAD_3 :
      case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
      case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
      case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
	 if (stk == 0) {
	    sref = fait_control.findRefValue(settype,ins.getLocalVariable());
	  }
	 else sref = adjustRef(eref,0,1);
	 break;
      case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
      case DSTORE : case DSTORE_0 : case DSTORE_1 : case DSTORE_2 : case DSTORE_3 :
      case FSTORE : case FSTORE_0 : case FSTORE_1 : case FSTORE_2 : case FSTORE_3 :
      case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
      case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :
	 if (var == ins.getLocalVariable()) {
	    sref = fait_control.findRefStackValue(settype,0);
	  }
	 else sref = adjustRef(eref,1,0);
	 break;
      case DUP :
	 if (stk == 0 || stk == 1) {
	    sref = fait_control.findRefStackValue(settype,0);
	  }
	 else sref = adjustRef(eref,1,2);
	 break;
      case DUP_X1 :
	 if (stk == 2) {
	    sref = fait_control.findRefStackValue(settype,0);
	  }
	 else if (stk == 0 || stk == 1) {
	    sref = eref;
	  }
	 else sref = adjustRef(eref,2,3);
	 break;
      case DUP_X2 :
	 // need to take category 2 values into account
	 if (stk == 3) {
	    sref = fait_control.findRefStackValue(settype,0);
	  }
	 else if (stk == 0 || stk == 1 || stk == 2) {
	    sref = eref;
	  }
	 else sref = adjustRef(eref,3,4);
	 break;
      case DUP2 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) {
	    if (stk == 0 || stk == 1) {
	       sref = fait_control.findRefStackValue(settype,0);
	     }
	    else sref = adjustRef(eref,1,2);
	  }
	 else {
	    if (stk == 0 || stk == 2) {
	       sref = fait_control.findRefStackValue(settype,0);
	     }
	    else if (stk == 1 || stk == 3) {
	       sref = fait_control.findRefStackValue(settype,1);
	     }
	    else {
	       sref = adjustRef(eref,2,4);
	     }
	  }
	 break;
      case DUP2_X1 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) {
	    if (stk == 2) {
	       sref = fait_control.findRefStackValue(settype,0);
	     }
	    else if (stk == 0 || stk == 1) {
	       sref = eref;
	     }
	    else sref = adjustRef(eref,2,3);
	  }
	 else {
	    if (stk == 3) {
	       sref = fait_control.findRefStackValue(settype,0);
	     }
	    else if (stk == 4) {
	       sref = fait_control.findRefStackValue(settype,1);
	     }
	    else if (stk == 0 || stk == 1 || stk == 2) {
	       sref = eref;
	     }
	    else sref = adjustRef(eref,3,5);
	  }
	 break;
      case DUP2_X2 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) {
	    v1 = prior_state.getStack(1);
	    if (v1.isCategory2()) {
	       if (stk == 2) {
		  sref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 0 || stk == 1) {
		  sref = eref;
		}
	       else sref = adjustRef(eref,2,3);
	     }
	    else {
	       if (stk == 3) {
		  sref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 0 || stk == 1 || stk == 2) {
		  sref = eref;
		}
	       else sref = adjustRef(eref,3,4);
	     }
	  }
	 else {
	    v1 = prior_state.getStack(2);
	    if (v1.isCategory2()) {
	       if (stk == 3) {
		  sref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 4) {
		  sref = fait_control.findRefStackValue(settype,1);
		}
	       else if (stk == 0 || stk == 1 || stk == 2) sref = eref;
	       else sref = adjustRef(eref,3,5);
	     }
	    else {
	       if (stk == 4) {
		  sref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 5) {
		  sref = fait_control.findRefStackValue(settype,1);
		}
	       else if (stk == 0 || stk == 1 || stk == 2 || stk == 3) {
		  sref = eref;
		}
	       else sref = adjustRef(eref,4,6);
	     }
	  }
	 break;
      case MONITORENTER :
      case MONITOREXIT :
      case POP :
	 sref = adjustRef(eref,1,0);
	 break;
      case POP2 :
	 v0 = prior_state.getStack(0);
	 if (v0.isCategory2()) sref = adjustRef(eref,0,1);
	 else sref = adjustRef(eref,0,2);
	 break;
      case SWAP :
	 if (stk == 0) {
	    sref = fait_control.findRefStackValue(settype,1);
	  }
	 else if (stk == 1) {
	    sref = fait_control.findRefStackValue(settype,0);
	  }
	 else sref = adjustRef(eref,2,2);
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
            addAuxRef(0);
            addAuxRef(1);
	    sref = null;
	  }
	 else sref = adjustRef(eref,2,1);
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
         sref = adjustRef(eref,0,1);
	 break;
      case D2F : case FNEG : case I2F : case L2F :
      case D2I : case F2I : case L2I : case INEG :
      case D2L : case F2L : case I2L : case LNEG :
      case DNEG : case F2D : case I2D : case L2D :
      case I2B : case I2C : case I2S :
      case INSTANCEOF :
      case CHECKCAST :
	 if (eref.getRefStack() == 0) {
            addAuxRef(0);
	    sref = null;
	  }
	 else sref = adjustRef(eref,1,1);
	 break;
      case IINC :
	 if (var == ins.getLocalVariable()) {
            addAuxVarRef(ins.getLocalVariable());
            sref = null;
          }
	 break;
      case NOP :
	 sref = eref;
	 break;
         
/* BRANCH INSTRUCTIONS */
      case GOTO :
      case GOTO_W :
	 sref = eref;
         break;
      case IF_ACMPEQ : case IF_ACMPNE :
      case IF_ICMPEQ : case IF_ICMPNE :
      case IF_ICMPLT : case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
	 sref = adjustRef(eref,2,0);
         if (note_conditions) {
            addAuxRef(0);
            addAuxRef(1);
          }
	 break;
      case IFEQ : case IFNE : case IFLT : case IFGE : case IFGT : case IFLE :
      case IFNONNULL : case IFNULL :
	 sref = adjustRef(eref,1,0);
         if (note_conditions) {
            addAuxRef(0);
          }
	 break;
      case LOOKUPSWITCH :
      case TABLESWITCH :
	 sref = adjustRef(eref,1,0);
         if (note_conditions) {
            addAuxRef(0);
          }
	 break;
         
/* SUBROUTINE CALLS */
      case JSR : case JSR_W :
	 sref = adjustRef(eref,0,1);
	 break;
      case RET :
	 sref = eref;
	 break;
         
/* CALL INSTRUCTIONS */
      case ARETURN :
      case DRETURN : case FRETURN : case IRETURN : case LRETURN :
      case ATHROW :
	 sref = adjustRef(eref,1,0);
	 break;
      case RETURN :
	 sref = eref;
	 break;
      case INVOKEINTERFACE :
      case INVOKESPECIAL :
      case INVOKESTATIC :
      case INVOKEVIRTUAL :
	 IfaceMethod fm = execute_point.getReferencedMethod();
	 int act = fm.getNumArgs();
	 if (!fm.isStatic()) act += 1;
	 if (fm.getReturnType() == null || fm.getReturnType().isVoidType()) {
	    sref = adjustRef(eref,act,0);
	  }
	 else if (eref.getRefStack() == 0) {
	    sref = null;
	  }
	 else if (reffld != null) {
            addAuxRefs(reffld);
            sref = null;
          }
	 else sref = adjustRef(eref,act,1);
	 break;
      case INVOKEDYNAMIC :
         int narg = 0;
         String [] args = ins.getDynamicReference();
         if (args[1] != null && !args[1].startsWith("()")) {
            Type [] ar = Type.getArgumentTypes(args[1]);
            narg = ar.length;
          }
         if (stk == 0) {
            for (int i = 0; i < narg; ++i) addAuxRef(i);
            sref = null;
          }
         else sref = adjustRef(eref,narg,1);
	 break;
         
/* ARRAY PROCESSING INSTRUCTIONS */
      case AALOAD :
      case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
	 if (stk == 0) {
            addAuxArrayRefs(prior_state.getStack(1));
	    sref = null;
	  }
	 else sref = adjustRef(eref,2,1);
	 break;
      case AASTORE :
      case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
	 sref = adjustRef(eref,3,0);
	 break;
      case ANEWARRAY :
      case NEWARRAY :
	 sref = adjustRef(eref,1,1);
	 break;
      case MULTIANEWARRAY :
	 sref = adjustRef(eref,ins.getIntValue(),1);
	 break;
      case ARRAYLENGTH :
         if (stk == 0) {
            addAuxArrayRefs(prior_state.getStack(0));
          }
	 sref = adjustRef(eref,1,1);
	 break;
         
/* FIELD INSTRUCTIONS */
      case GETFIELD :
	 if (stk == 0) {
            IfaceField fld = execute_point.getReferencedField();
            IfaceValue vobj = prior_state.getStack(0);
            if (prior_state.getFieldValue(fld) != null && vobj == prior_state.getLocal(0)) {
               sref = fait_control.findRefValue(settype,prior_state.getStack(0),fld);
             }
            else {
               addAuxRefs(fld);
               sref = null;
             }
	  }
	 else sref = adjustRef(eref,1,1);
	 break;
      case GETSTATIC :
	 if (stk == 0) {
            addAuxRefs(execute_point.getReferencedField());
            sref = null;
	  }
	 else sref = adjustRef(eref,0,1);
	 break;
      case PUTFIELD :
	 sref = adjustRef(eref,2,0);
	 break;
      case PUTSTATIC :
	 sref = adjustRef(eref,1,0);
	 break;
         
      default :
	 FaitLog.logE("FAIT: Opcode " + ins.getOpcode() + " not found");
	 break;
    }
   
   return sref;
}







/********************************************************************************/
/*                                                                              */
/*      Handle Ast back flow                                                    */
/*                                                                              */
/********************************************************************************/

private IfaceValue computeAstBackFlow(IfaceValue eref)
{
   IfaceAstReference astref = execute_point.getAstReference();
   ASTNode n = astref.getAstNode();
   
   BackVisitor bv = new BackVisitor(eref);
   n.accept(bv);
   
   return bv.getStartRef();
}


private class BackVisitor extends ASTVisitor {
   
   private IfaceValue end_ref;
   private IfaceValue start_back_ref;
   private ASTNode after_node;
   private IfaceAstStatus after_status;
   
   BackVisitor(IfaceValue eref) {
      end_ref = eref;
      start_back_ref = null;
      after_node = execute_point.getAstReference().getAfterChild();
      after_status = execute_point.getAstReference().getStatus();
    }
   
   IfaceValue getStartRef()                     { return start_back_ref; }
   
   @Override public boolean visit(ArrayAccess v) {
      if (after_node == null || after_node == v.getArray()) start_back_ref = end_ref;
      else if (end_ref.getRefStack() == 0) {
         addAuxArrayRefs(prior_state.getStack(1));
         addAuxRef(0);
         addAuxRef(1);
         start_back_ref = null;
       }
      else start_back_ref = adjustRef(end_ref,2,1);
      return false;
    } 
   
   @Override public boolean visit(ArrayCreation v) {
      if (after_node == null) noChange();
      else if (v.getInitializer() != null && after_node == v.getInitializer()) noChange();
      else {
         List<?> dims = v.dimensions();
         int idx = dims.indexOf(after_node);
         if (idx < dims.size()-1) start_back_ref = end_ref;
         else {
            int ct = (v.getInitializer() != null ? 1 : 0);
            ct += dims.size();
            start_back_ref = adjustRef(end_ref,ct,1);
          }
       }
      return false;
    }
   
   @Override public boolean visit(ArrayInitializer n) {
      List<?> exprs = n.expressions();
      int idx = 0;
      if (after_node != null) idx = exprs.indexOf(after_node) + 1;
      if (idx < exprs.size()) noChange();
      else start_back_ref = adjustRef(end_ref,exprs.size(),1);
      
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
            if (v.getOperator() == Assignment.Operator.ASSIGN) {
               IfaceValue pval = prior_state.getStack(0);
               start_back_ref = fait_control.findRefStackValue(pval.getDataType(),0);
             }
            else {
               addAuxRef(0);
               noBack();
             }
          }
         else if (asgval.getRefField() == end_ref.getRefField()) {
            addAuxRef(0);
            noBack();
          }
         else {
            start_back_ref = adjustRef(end_ref,2,1);
          }
         if (v.getOperator() != Assignment.Operator.ASSIGN) {
            if (asgval.getRefSlot() >= 0) {
               addAuxVarRef(asgval.getRefSlot());
             }
            else if (asgval.getRefField() != null) {
               addAuxRef(asgval,IfaceAuxRefType.OPERAND);
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
         start_back_ref = fait_control.findRefValue(end_ref.getDataType(),slot);
       }
      else {
         popOne();
       }   
      return false;
    }
   
   @Override public boolean visit(ClassInstanceCreation v) {
      List<?> args = v.arguments();
      JcompType rty = JcompAst.getJavaType(v.getType());
      int ct = (rty.needsOuterClass() ? 2 : 1);
      
      if (after_node == null) {
         start_back_ref = adjustRef(end_ref,0,ct+1);
       }
      else if (after_node == v.getExpression()) {
         noChange();
       }
      else {
         int idx = args.indexOf(after_node)+1;
         if (idx < args.size()) noChange();
         else start_back_ref = adjustRef(end_ref,args.size()+ct,0);
       }
      return false;
    }
   
   @Override public boolean visit(ConditionalExpression v) {
      if (after_node == null) noChange();
      else if (after_node == v.getExpression()) {
         popOne();
         if (note_conditions) addAuxRef(0);
       }
      else {
         noChange();
       }
      return false;
    }
   
   @Override public boolean visit(ConstructorInvocation v) {
      JcompSymbol rtn = JcompAst.getReference(v);
      JcompType rty = rtn.getClassType();
      int ct = (rty.needsOuterClass() ? 2 : 1);
      if (after_node == null) {
         start_back_ref = adjustRef(end_ref,0,ct);
       }
      else {
         List<?> args = v.arguments();
         int idx = args.indexOf(after_node) + 1;
         if (idx < args.size()) noChange();
         else {
            if (end_ref.getRefStack() < 0 && end_ref.getRefSlot() < 0) start_back_ref = null;
            else {
               int sz = args.size() + ct;
               start_back_ref = adjustRef(end_ref,sz,0);
             }
          }
       }
      return false;
    }
   
   @Override public boolean visit(CreationReference v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_back_ref = null;
       }
      else pushOne();
      return false;
    }
   
   @Override public boolean visit(DoStatement s) {
      if (after_node == null || after_node == s.getBody()) noChange();
      else {
         popOne();
         if (note_conditions) addAuxRef(0);
       }     
      return false;
    }
   
   @Override public boolean visit(EnhancedForStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         int slot = getSlot(JcompAst.getDefinition(s.getParameter().getName()));
         if (end_ref.getRefSlot() == slot) {
            addAuxRef(0);
            start_back_ref = null;
          }
         else popOne();
       }
      else noChange();
      return false;
    }
   
   @Override public boolean visit(ExpressionMethodReference v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_back_ref = null;
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
         start_back_ref = adjustRef(end_ref,ct,1);
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
            start_back_ref = fait_control.findRefValue(end_ref.getDataType(),prior_state.getStack(0),fld);
          }
         else {
            start_back_ref = null;
            addAuxRefs(fld);
          }
       }
      else start_back_ref = adjustRef(end_ref,1,1);
      return false;
    }
   
   @Override public boolean visit(ForStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         if (note_conditions) addAuxRef(0);
         popOne();
       }
      else noChange();
      return false;
    }
   
   @Override public boolean visit(IfStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         if (note_conditions) addAuxRef(0);
         popOne();
       }
      else noChange();
      return false;
    }
   
   @Override public boolean visit(InfixExpression v) {
      if (after_node == null) noChange();
      else if (after_node == v.getLeftOperand()) return noChange();
      else if (end_ref.getRefStack() == 0) {
         start_back_ref = null;
         addAuxRef(0);
         addAuxRef(1);
       }
      else start_back_ref = adjustRef(end_ref,2,1);
      return false;
    }
   
   @Override public boolean visit(InstanceofExpression v) {
      if (after_node == null) noChange();
      else if (end_ref.getRefStack() == 0) {
         start_back_ref = null;
         addAuxRef(0);
       }
      else {
         start_back_ref = adjustRef(end_ref,1,1);
       }
      return false;
    }
   
   @Override public boolean visit(LambdaExpression v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_back_ref = null;
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
      int idx = 0;
      if (after_node != null) idx = args.indexOf(after_node)+1;
      if (idx < args.size()) {
         if (dref != 0) start_back_ref = adjustRef(end_ref,0,dref);
         else noChange();
       }
      else {
         int ret = (js.getType().getBaseType().isVoidType() ? 0 : 1);
         if (js.isConstructorSymbol()) ret = 0;
         if (end_ref.getRefStack() == 0 && ret == 1) {
            start_back_ref = null;
          }
         else if (end_ref.getRefStack() < 0 && end_ref.getRefSlot() < 0) start_back_ref = null;
         else {
            int sz = args.size();
            if (!js.isStatic()) sz += 1;
            start_back_ref = adjustRef(end_ref,sz,ret);
          }
       }
      return false;
    }
   
   @Override public boolean visit(PostfixExpression v) {
      if (after_node == null) return noChange();
      IfaceValue v1 = prior_state.getStack(0);
      if (end_ref.getRefSlot() == v1.getRefSlot() && v1.getRefSlot() > 0) {
         start_back_ref = null;
         addAuxVarRef(v1.getRefSlot());
       }
      else if (end_ref.getRefStack() == 0) {
         start_back_ref = null;
         addAuxRef(0);
       }
      else start_back_ref = adjustRef(end_ref,1,1);
      return false;
    }
   
   @Override public boolean visit(PrefixExpression v) {
      if (after_node == null) return noChange();
      IfaceValue v1 = prior_state.getStack(0);
      if (v1 == null) return noBack();
      if (end_ref.getRefSlot() == v1.getRefSlot() && v1.getRefSlot() > 0) {
         addAuxVarRef(v1.getRefSlot());
         start_back_ref = null;
       }
      else if (end_ref.getRefStack() == 0) {
         addAuxRef(0);
         start_back_ref = null;
       }
      else start_back_ref = adjustRef(end_ref,1,1);
      return false;
    }
   
   @Override public boolean visit(QualifiedName v) {
      if (after_node == null) return noChange();
      JcompSymbol sym = JcompAst.getReference(v.getName());
      if (after_node == v.getQualifier() && sym != null) {
         if (end_ref.getRefStack() == 0) {
            IfaceValue v0 = prior_state.getStack(0);
            IfaceField fld = getField(sym);
            if (fld == null) start_back_ref = null;
            else start_back_ref = fait_control.findRefValue(end_ref.getDataType(),v0,fld);
          }
         else start_back_ref =  adjustRef(end_ref,1,1);
       }
      else if (after_node == v.getQualifier() && sym == null) {
         start_back_ref = adjustRef(end_ref,1,1);
       }
      else start_back_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(ReturnStatement s) {
      if (after_status !=  null) noChange();
      else if (after_node == null && s.getExpression() != null) start_back_ref = end_ref;
      else if (s.getExpression() != null && end_ref.getRefStack() == 0) {
        noChange();
       }
      else popOne();
      return false;
    }
   
   @Override public boolean visit(SimpleName v) {
      JcompSymbol js = JcompAst.getReference(v);
      int slot = getSlot(js);
      if (end_ref.getRefStack() == 0) {
         if (js != null && js.isFieldSymbol()) {
            IfaceField fld = getField(js);
            if (!js.isStatic() && prior_state.getFieldValue(fld) != null) {
               IfaceValue thisv = getThisValue(fld.getDeclaringClass());
               start_back_ref = fait_control.findRefValue(end_ref.getDataType(),thisv,fld);
             }
            else {
               start_back_ref = null;
               addAuxRefs(fld);
             }
          }
         else if (js != null && js.isEnumSymbol()) {
            start_back_ref = null;
          }
         else {
            addAuxVarRef(slot);
            start_back_ref = null;
          }
       }
      else start_back_ref = adjustRef(end_ref,0,1);
      return false;
    }
   
   @Override public boolean visit(SingleVariableDeclaration n) {
      if (after_node == null && n.getInitializer() != null) start_back_ref = end_ref;
      else if (n.getInitializer() != null) start_back_ref = adjustRef(end_ref,1,0);
      else start_back_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(SuperMethodReference v) {
      if (v == execute_point.getMethod().getStart().getAstReference()) {
         start_back_ref = null;
       }
      else start_back_ref = adjustRef(end_ref,0,1);
      return false;
    }
   
   @Override public boolean visit(SwitchStatement s) {
      if (after_node != null && after_node == s.getExpression()) {
         start_back_ref =  adjustRef(end_ref,1,0);
         if (note_conditions) addAuxRef(0);
       }
      else start_back_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(SynchronizedStatement s) {
      if (after_node == null) start_back_ref = end_ref;
      else if (after_node == s.getExpression()) {
         start_back_ref = adjustRef(end_ref,0,1);
       }
      else start_back_ref = adjustRef(end_ref,1,0);
      return false;
    }
   
   @Override public boolean visit(ThrowStatement s) {
      if (after_node == null) start_back_ref = end_ref;
      else start_back_ref = adjustRef(end_ref,1,0);
      return false;
    }
   
   @Override public boolean visit(TryStatement s) {
      if (after_node == null) {
         start_back_ref =  adjustRef(end_ref,0,1);
       }
      else if (after_node == s.getFinally()) {
         start_back_ref = adjustRef(end_ref,1,0);
       }
      else {
         start_back_ref = end_ref;   
         IfaceValue v = prior_state.getStack(0);
         if (v instanceof IfaceStackMarker) {
            IfaceStackMarker mkr = (IfaceStackMarker) v;
            Set<Object> vals = mkr.getMarkerValues();
            if (vals.contains(TryState.BODY) ||
                  vals.contains(TryState.CATCH)) {
               Block b = s.getFinally();
               if (b != null) start_back_ref = adjustRef(end_ref,0,1);
             }
          }
       }
      return false;
    }
   
  @Override public boolean visit(VariableDeclarationFragment v) {
      if (after_node != null && after_node == v.getInitializer()) {
         JcompSymbol sym = JcompAst.getDefinition(v);
         start_back_ref = adjustRef(end_ref,1,0);
         if (sym != null) {
            int defslot = end_state.getLocation().getMethod().getLocalOffset(sym);
            if (defslot == end_ref.getRefSlot()) {
               addAuxRef(0);
               start_back_ref = null;
             }
          }
       }
      else start_back_ref = end_ref;
      return false;
    }
   
   @Override public boolean visit(WhileStatement s) {
      if (after_node == s.getExpression()) {
         start_back_ref = adjustRef(end_ref,1,0);
         if (note_conditions) addAuxRef(0);
       }
      else start_back_ref = end_ref;
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
      start_back_ref = null;
      return false;
    }
   private boolean noChange() {
      start_back_ref = end_ref;
      return false;
    }
   private boolean pushOne() {
      start_back_ref = adjustRef(end_ref,0,1);
      return false;
    }
   private boolean popOne() {
      start_back_ref = adjustRef(end_ref,1,0);
      return false;
    }
   
}       // end of inner class BackVisitor



/********************************************************************************/
/*                                                                              */
/*      Common helper methods                                                   */
/*                                                                              */
/********************************************************************************/

protected IfaceValue adjustRef(IfaceValue ref,int pop,int push)
{
   if (ref == null) return null;
   int idx = ref.getRefStack();
   if (idx >= 0) {
      if (idx < push) return null;
      if (pop == push) return ref;
      return fait_control.findRefStackValue(ref.getDataType(),idx-push+pop);
    }
   return ref;
}




private void addAuxRef(int stk)
{
   IfaceValue val = prior_state.getStack(stk);
   if (val == null) {
      return;
    }
   IfaceValue ref = fait_control.findRefStackValue(val.getDataType(),stk);
   
   addAuxRef(ref,IfaceAuxRefType.OPERAND);
}


private void addAuxRef(IfaceValue ref,IfaceAuxRefType typ)
{
   if (ref == null || !ref.isReference()) return;
   addAuxRef(fait_control.getAuxReference(prior_state.getLocation(),ref,typ));
}


private void addAuxRef(IfaceAuxReference ref)
{
   if (aux_refs == null) aux_refs = new HashSet<>();
   aux_refs.add(ref);
}


private void addAuxVarRef(int slot)
{
   if (slot < 0) return;
   IfaceValue val = prior_state.getLocal(slot);
   IfaceValue ref = fait_control.findRefValue(val.getDataType(),slot);
   addAuxRef(ref,IfaceAuxRefType.LOCAL_REF);
}


private void addAuxRefs(IfaceField fld)
{
   if (fld == null) return;
   Collection<IfaceAuxReference> refs = fait_control.getAuxRefs(fld);
   if (refs == null) return;
   for (IfaceAuxReference r : refs) addAuxRef(r);
}


private void addAuxArrayRefs(IfaceValue arr)
{
   if (arr == null) return;
   Collection<IfaceAuxReference> refs = fait_control.getAuxArrayRefs(arr);
   if (refs == null) return;
   for (IfaceAuxReference r : refs) addAuxRef(r);
}




}       // end of class ControlBackFlow




/* end of ControlBackFlow.java */

