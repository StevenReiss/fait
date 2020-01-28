/********************************************************************************/
/*										*/
/*		FlowScannerAst.java						*/
/*										*/
/*	Flow scanner for AST scanning						*/
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



package edu.brown.cs.fait.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceAstStatus;
import edu.brown.cs.fait.iface.IfaceBackElement;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceImplications;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceStackMarker;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceAstStatus.Reason;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.file.Pair;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;

class FlowScannerAst extends FlowScanner implements FlowConstants, JcodeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceAstReference		ast_where;
private ASTNode 			after_node;
private FlowLocation			here_location;
private IfaceState			cur_state;

private static Map<Object,FaitOperator>      op_map;

static {
   op_map = new HashMap<>();
   op_map.put(InfixExpression.Operator.AND,FaitOperator.AND);
   op_map.put(InfixExpression.Operator.DIVIDE,FaitOperator.DIV);
   op_map.put(InfixExpression.Operator.EQUALS,FaitOperator.EQL);
   op_map.put(InfixExpression.Operator.GREATER,FaitOperator.GTR);
   op_map.put(InfixExpression.Operator.GREATER_EQUALS,FaitOperator.GEQ);
   op_map.put(InfixExpression.Operator.LEFT_SHIFT,FaitOperator.LSH);
   op_map.put(InfixExpression.Operator.LESS,FaitOperator.LSS);
   op_map.put(InfixExpression.Operator.LESS_EQUALS,FaitOperator.LEQ);
   op_map.put(InfixExpression.Operator.MINUS,FaitOperator.SUB);
   op_map.put(InfixExpression.Operator.NOT_EQUALS,FaitOperator.NEQ);
   op_map.put(InfixExpression.Operator.OR,FaitOperator.OR);
   op_map.put(InfixExpression.Operator.PLUS,FaitOperator.ADD);
   op_map.put(InfixExpression.Operator.REMAINDER,FaitOperator.MOD);
   op_map.put(InfixExpression.Operator.RIGHT_SHIFT_SIGNED,FaitOperator.RSH);
   op_map.put(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED,FaitOperator.RSHU);
   op_map.put(InfixExpression.Operator.TIMES,FaitOperator.MUL);
   op_map.put(InfixExpression.Operator.XOR,FaitOperator.XOR);
   op_map.put(PrefixExpression.Operator.COMPLEMENT,FaitOperator.COMP);
   op_map.put(PrefixExpression.Operator.MINUS,FaitOperator.NEG);
   op_map.put(PrefixExpression.Operator.PLUS,FaitOperator.NOP);
   op_map.put(PrefixExpression.Operator.NOT,FaitOperator.NOT);
   op_map.put(Assignment.Operator.ASSIGN,FaitOperator.ASG);
   op_map.put(Assignment.Operator.BIT_AND_ASSIGN,FaitOperator.AND);
   op_map.put(Assignment.Operator.BIT_OR_ASSIGN,FaitOperator.OR);
   op_map.put(Assignment.Operator.BIT_XOR_ASSIGN,FaitOperator.XOR);
   op_map.put(Assignment.Operator.DIVIDE_ASSIGN,FaitOperator.DIV);
   op_map.put(Assignment.Operator.LEFT_SHIFT_ASSIGN,FaitOperator.LSH);
   op_map.put(Assignment.Operator.MINUS_ASSIGN,FaitOperator.SUB);
   op_map.put(Assignment.Operator.PLUS_ASSIGN,FaitOperator.ADD);
   op_map.put(Assignment.Operator.REMAINDER_ASSIGN,FaitOperator.MOD);
   op_map.put(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN,FaitOperator.RSH);
   op_map.put(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN,FaitOperator.RSHU);
   op_map.put(Assignment.Operator.TIMES_ASSIGN,FaitOperator.MUL);
   op_map.put(InfixExpression.Operator.CONDITIONAL_AND,FaitOperator.ANDAND);
   op_map.put(InfixExpression.Operator.CONDITIONAL_OR,FaitOperator.OROR);
}


private static Object NO_NEXT = new Object();
private static Object NO_NEXT_REPORT = new Object();





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowScannerAst(IfaceControl fc,FlowQueue fq,FlowQueueInstanceAst wq)
{
   super(fc,fq,wq);
   ast_where = null;
   after_node = null;
   cur_state = null;
}



/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

@Override int scanCode()
{
   int ctr = 0;

   while (!work_queue.isEmpty()) {
      IfaceProgramPoint ar = work_queue.getNext();
      try {
	 ast_where = ar.getAstReference();
	 processAstNode();
	 ++ctr;
       }
      catch (Throwable t) {
	 FaitLog.logE("Problem ast processing " + work_queue.getCall().getLogName(),t);
       }
    }

   return ctr;
}



int scanBack()
{
   int ctr = 0;

   while (!work_queue.isBackEmpty()) {
      IfaceBackElement be = work_queue.getNextBack();
      try {
	 IfaceProgramPoint pt = be.getProgramPoint();
	 ast_where = pt.getAstReference();
	 processBackNode(be.getReference(),be.getSetType());
	 ++ctr;
       }
      catch (Throwable t) {
	 FaitLog.logE("Problem ast back processing " + work_queue.getCall().getLogName(),t);
       }
    }

   return ctr;
}


/********************************************************************************/
/*										*/
/*	Actual processing							*/
/*										*/
/********************************************************************************/

private void processAstNode()
{
   IfaceAstReference nar = null;
   ASTNode nextnode = null;
   FlowAstStatus nextsts = null;
   if (ast_where.getAstNode() == null) {
      FaitLog.logE("Attempt to execute without AST node");
      return;
    }

   cur_state = work_queue.getState(ast_where);
   if (!flow_queue.checkInitialized(work_queue.getCall(),ast_where)) return;

   cur_state = cur_state.cloneState();
   ASTNode node = ast_where.getAstNode();
   after_node = ast_where.getAfterChild();
   IfaceAstStatus sts = ast_where.getStatus();
   here_location = null;
   Object rslt = null;

   work_queue.getCall().removeErrors(ast_where);

   if (FaitLog.isTracing()) {
      String cls = node.getClass().getName();
      int idx = cls.lastIndexOf(".");
      cls = cls.substring(idx+1);
      String aft = (after_node == null ? "" : "*");
      if (sts != null) aft = "^";
      String cnts = node.toString().replace("\n"," ");
      if (cnts.length() > 64) cnts = cnts.substring(0,62) + "...";
      FaitLog.logD("EVAL " + aft + cls + " " + node.getStartPosition() + " " + cnts);
      if (sts != null) FaitLog.logD1("STATUS: " + sts);
    }

   if (sts == null) {
      switch (node.getNodeType()) {
	 case ASTNode.ANNOTATION_TYPE_DECLARATION :
	 case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
	 case ASTNode.ANONYMOUS_CLASS_DECLARATION :
	 case ASTNode.ARRAY_TYPE :
	 case ASTNode.BLOCK_COMMENT :
	 case ASTNode.COMPILATION_UNIT :
	 case ASTNode.EMPTY_STATEMENT :
	 case ASTNode.ENUM_CONSTANT_DECLARATION :
	 case ASTNode.ENUM_DECLARATION :
	 case ASTNode.IMPORT_DECLARATION :
	 case ASTNode.JAVADOC :
	 case ASTNode.LINE_COMMENT :
	 case ASTNode.MARKER_ANNOTATION :
	 case ASTNode.MEMBER_REF :
	 case ASTNode.MEMBER_VALUE_PAIR :
	 case ASTNode.METHOD_REF :
	 case ASTNode.METHOD_REF_PARAMETER :
	 case ASTNode.MODIFIER :
	 case ASTNode.NAME_QUALIFIED_TYPE :
	 case ASTNode.NORMAL_ANNOTATION :
	 case ASTNode.PACKAGE_DECLARATION :
	 case ASTNode.PARAMETERIZED_TYPE :
	 case ASTNode.PRIMITIVE_TYPE :
	 case ASTNode.QUALIFIED_TYPE :
	 case ASTNode.SIMPLE_TYPE :
	 case ASTNode.SINGLE_MEMBER_ANNOTATION :
	 case ASTNode.TAG_ELEMENT :
	 case ASTNode.TEXT_ELEMENT :
	 case ASTNode.TYPE_DECLARATION :
	 case ASTNode.TYPE_DECLARATION_STATEMENT :
	 case ASTNode.TYPE_PARAMETER :
	 case ASTNode.UNION_TYPE :
	 case ASTNode.WILDCARD_TYPE :
	    break;
	 case ASTNode.ARRAY_ACCESS :
	    rslt = visit((ArrayAccess) node);
	    break;
	 case ASTNode.ARRAY_CREATION :
	    rslt = visit((ArrayCreation) node);
	    break;
	 case ASTNode.ARRAY_INITIALIZER :
	    rslt = visit((ArrayInitializer) node);
	    break;
	 case ASTNode.ASSERT_STATEMENT :
	    rslt = visit((AssertStatement) node);
	    break;
	 case ASTNode.ASSIGNMENT :
	    rslt = visit((Assignment) node);
	    break;
	 case ASTNode.BLOCK :
	    rslt = visit((Block) node);
	    break;
	 case ASTNode.BOOLEAN_LITERAL :
	    rslt = visit((BooleanLiteral) node);
	    break;
	 case ASTNode.BREAK_STATEMENT :
	    rslt = visit((BreakStatement) node);
	    break;
	 case ASTNode.CAST_EXPRESSION :
	    rslt = visit((CastExpression) node);
	    break;
	 case ASTNode.CATCH_CLAUSE :
	    rslt = visit((CatchClause) node);
	    break;
	 case ASTNode.CHARACTER_LITERAL :
	    rslt = visit((CharacterLiteral) node);
	    break;
	 case ASTNode.CLASS_INSTANCE_CREATION :
	    rslt = visit((ClassInstanceCreation) node);
	    break;
	 case ASTNode.CONDITIONAL_EXPRESSION :
	    rslt = visit((ConditionalExpression) node);
	    break;
	 case ASTNode.CONSTRUCTOR_INVOCATION :
	    rslt = visit((ConstructorInvocation) node);
	    break;
	 case ASTNode.CONTINUE_STATEMENT :
	    rslt = visit((ContinueStatement) node);
	    break;
	 case ASTNode.CREATION_REFERENCE :
	    rslt = visit((CreationReference) node);
	    break;
	 case ASTNode.DO_STATEMENT :
	    rslt = visit((DoStatement) node);
	    break;
	 case ASTNode.ENHANCED_FOR_STATEMENT :
	    rslt = visit((EnhancedForStatement) node);
	    break;
	 case ASTNode.EXPRESSION_METHOD_REFERENCE :
	    rslt = visit((ExpressionMethodReference) node);
	    break;
	 case ASTNode.EXPRESSION_STATEMENT :
	    rslt = visit((ExpressionStatement) node);
	    break;
	 case ASTNode.FIELD_ACCESS :
	    rslt = visit((FieldAccess) node);
	    break;
	 case ASTNode.FIELD_DECLARATION :
	    rslt = visit((FieldDeclaration) node);
	    break;
	 case ASTNode.FOR_STATEMENT :
	    rslt = visit((ForStatement) node);
	    break;
	 case ASTNode.IF_STATEMENT :
	    rslt = visit((IfStatement) node);
	    break;
	 case ASTNode.INFIX_EXPRESSION :
	    rslt = visit((InfixExpression) node);
	    break;
	 case ASTNode.INSTANCEOF_EXPRESSION :
	    rslt = visit((InstanceofExpression) node);
	    break;
	 case ASTNode.LABELED_STATEMENT :
	    rslt = visit((LabeledStatement) node);
	    break;
	 case ASTNode.LAMBDA_EXPRESSION :
	    rslt = visit((LambdaExpression) node);
	    break;
	 case ASTNode.METHOD_DECLARATION :
	    rslt = visit((MethodDeclaration) node);
	    break;
	 case ASTNode.METHOD_INVOCATION :
	    rslt = visit((MethodInvocation) node);
	    break;
	 case ASTNode.NULL_LITERAL :
	    rslt = visit((NullLiteral) node);
	    break;
	 case ASTNode.NUMBER_LITERAL :
	    rslt = visit((NumberLiteral) node);
	    break;
	 case ASTNode.PARENTHESIZED_EXPRESSION :
	    rslt = visit((ParenthesizedExpression) node);
	    break;
	 case ASTNode.POSTFIX_EXPRESSION :
	    rslt = visit((PostfixExpression) node);
	    break;
	 case ASTNode.PREFIX_EXPRESSION :
	    rslt = visit((PrefixExpression) node);
	    break;
	 case ASTNode.QUALIFIED_NAME :
	    rslt = visit((QualifiedName) node);
	    break;
	 case ASTNode.RETURN_STATEMENT :
	    rslt = visit((ReturnStatement) node);
	    break;
	 case ASTNode.SIMPLE_NAME :
	    rslt = visit((SimpleName) node);
	    break;
	 case ASTNode.SINGLE_VARIABLE_DECLARATION :
	    rslt = visit((SingleVariableDeclaration) node);
	    break;
	 case ASTNode.STRING_LITERAL :
	    rslt = visit((StringLiteral) node);
	    break;
	 case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
	    rslt = visit((SuperConstructorInvocation) node);
	    break;
	 case ASTNode.SUPER_METHOD_INVOCATION :
	    rslt = visit((SuperMethodInvocation) node);
	    break;
	 case ASTNode.SUPER_FIELD_ACCESS :
	    rslt = visit((SuperFieldAccess) node);
	    break;
	 case ASTNode.SUPER_METHOD_REFERENCE :
	    rslt = visit((SuperMethodReference) node);
	    break;
	 case ASTNode.SWITCH_STATEMENT :
	    rslt = visit((SwitchStatement) node);
	    break;
	 case ASTNode.SWITCH_CASE :
	    rslt = visit((SwitchCase) node);
	    break;
	 case ASTNode.SYNCHRONIZED_STATEMENT :
	    rslt = visit((SynchronizedStatement) node);
	    break;
	 case ASTNode.THIS_EXPRESSION :
	    rslt = visit((ThisExpression) node);
	    break;
	 case ASTNode.THROW_STATEMENT :
	    rslt = visit((ThrowStatement) node);
	    break;
	 case ASTNode.TRY_STATEMENT :
	    rslt = visit((TryStatement) node);
	    break;
	 case ASTNode.TYPE_LITERAL :
	    rslt = visit((TypeLiteral) node);
	    break;
	 case ASTNode.TYPE_METHOD_REFERENCE :
	    rslt = visit((TypeMethodReference) node);
	    break;
	 case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
	    rslt = visit((VariableDeclarationExpression) node);
	    break;
	 case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
	    rslt = visit((VariableDeclarationFragment) node);
	    break;
	 case ASTNode.VARIABLE_DECLARATION_STATEMENT :
	    rslt = visit((VariableDeclarationStatement) node);
	    break;
	 case ASTNode.WHILE_STATEMENT :
	    rslt = visit((WhileStatement) node);
	    break;
	 default :
	    FaitLog.logE("Unknown node type " + node);
	    break;
       }
    }
   else {
      if (cur_state != null) {
	 IfaceAstReference xar = fait_control.getAstReference(node);
	 IfaceState ost = work_queue.getState(xar);
	 if (ost != null) {
	    switch (node.getNodeType()) {
	       case ASTNode.TRY_STATEMENT :
		  break;
	       default :
		  cur_state.resetStack(ost);
		  break;
	     }
	  }
       }

      switch (node.getNodeType()) {
	 case ASTNode.DO_STATEMENT :
	    rslt = visitThrow((DoStatement) node,sts);
	    break;
	 case ASTNode.ENHANCED_FOR_STATEMENT :
	    rslt = visitThrow((EnhancedForStatement) node,sts);
	    break;
	 case ASTNode.FOR_STATEMENT :
	    rslt = visitThrow((ForStatement) node,sts);
	    break;
	 case ASTNode.WHILE_STATEMENT :
	    rslt = visitThrow((WhileStatement) node,sts);
	    break;
	 case ASTNode.SWITCH_STATEMENT :
	    rslt = visitThrow((SwitchStatement) node,sts);
	    break;
	 case ASTNode.METHOD_DECLARATION :
	    rslt = visitThrow((MethodDeclaration) node,sts);
	    break;
	 case ASTNode.SYNCHRONIZED_STATEMENT :
	    rslt = visitThrow((SynchronizedStatement) node,sts);
	    break;
	 case ASTNode.TRY_STATEMENT :
	    rslt = visitThrow((TryStatement) node,sts);
	    break;
	 default :
	    rslt = sts;
	    break;
       }
      if (rslt instanceof FlowAstStatus) {
	 FlowAstStatus nsts = (FlowAstStatus) rslt;
	 rslt = fait_control.getAstReference(node.getParent(),nsts);
       }
    }

   if (rslt != null) {
      if (rslt instanceof FlowAstStatus) {
	 nextsts = (FlowAstStatus) rslt;
       }
      else if (rslt instanceof ASTNode) {
	 nextnode = (ASTNode) rslt;
       }
      else if (rslt instanceof IfaceAstReference) {
	 nar = (IfaceAstReference) rslt;
       }
      else if (rslt == NO_NEXT_REPORT) {
	 work_queue.getCall().addError(ast_where,UNREACHABLE_CODE);
	 return;
       }
      else if (rslt == NO_NEXT) {
	 return;
       }
    }
   if (nar == null) {
      if (nextnode != null) {
	 nar = fait_control.getAstReference(nextnode);
       }
      else if (nextsts != null) {
	 nar = fait_control.getAstReference(node,nextsts);
       }
      else {
	 ASTNode par = node.getParent();
	 if (node instanceof MethodDeclaration) {
	    IfaceAstReference sar = work_queue.getCall().getMethod().getStart().getAstReference();
	    if (node == sar.getAstNode()) return;
	  }
	 nar = fait_control.getAstReference(par,node);
       }
    }
   if (nar != null) {
      if (cur_state != null) {
	 // post-update safety state here
	 IfaceState ost = work_queue.getState(nar);
	 if (ost != null) {
	    cur_state.resetStack(ost);
	    ost.resetStack(cur_state);
	    for (int i = 0; ; ++i) {
	       IfaceValue v0 = ost.getStack(i);
	       IfaceValue v1 = cur_state.getStack(i);
	       if (v0 == null || v1 == null) break;
	       if (v0.isReference() && !v1.isReference()) {
		  // FaitLog.logD("Ref in original");
		}
	       else if (v1.isReference() && !v0.isReference()) {
		  // this happens where there is a call where the an argument
		  // has been replaced by its value.
		  // cur_state.setStack(i,v0);
		  ost.setStack(i,v1);
		  // FaitLog.logD("Ref in new");
		}
	     }
	  }
	 FlowLocation nloc = new FlowLocation(work_queue.getWorkQueue(),work_queue.getCall(),nar);
	 work_queue.mergeState(cur_state,nloc);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Starting a method							*/
/*										*/
/********************************************************************************/

private Object visit(MethodDeclaration md)
{
   JcompSymbol js = JcompAst.getDefinition(md);

   if (after_node == null) {
      JcompType mt = js.getType();
      JcompType mt1 = mt.getBaseType();
      if (mt1 != null && !mt1.isPrimitiveType()) {
	 flow_queue.initialize(convertType(mt1));
       }
      for (JcompType mt2 : mt.getComponents()) {
	 flow_queue.initialize(convertType(mt2));
       }
    }

   if (after_node == null && md.isConstructor()) {
      JcompType jt = js.getClassType();
      if (jt.needsOuterClass()) {
	 IfaceValue iv = getLocal(1);
	 IfaceValue bv = getLocal(0);
	 IfaceType typ = convertType(jt,FaitAnnotation.NON_NULL);
	 IfaceField fld = fait_control.findField(typ,"this$0");
	 flow_queue.handleFieldSet(fld,getHere(),cur_state,true,iv,bv,-1);
       }
    }
   if (after_node == null) return md.getBody();

   if (!js.isConstructorSymbol() && !js.getType().getBaseType().isVoidType()) {
      if (FaitLog.isTracing()) FaitLog.logW("Attempt to do impossible return");
      return NO_NEXT;
    }

   return new FlowAstStatus(Reason.RETURN,(IfaceValue) null);
}



private IfaceValue visitBack(MethodDeclaration md,IfaceValue ref,IfaceType settype)
{
   if (after_node == null) {
      work_queue.getCall().backFlowParameter(ref,settype);
    }

   return null;
}


private Object visitThrow(MethodDeclaration md,IfaceAstStatus sts)
{
   switch (sts.getReason()) {
      case RETURN :
	 IfaceValue v0 = sts.getValue();
	 IfaceType vtyp = fait_control.findDataType("void");
	 if (v0 == null) v0 = fait_control.findAnyValue(vtyp);
	 flow_queue.handleReturn(work_queue.getCall(),v0,cur_state,getHere());
	 break;
      case EXCEPTION :
	 v0 = sts.getValue();
	 flow_queue.handleException(v0,work_queue.getCall());
	 break;
      default :
	 FaitLog.logE("Unexpected status for method: " + sts.getReason());
	 return sts;
    }


   return NO_NEXT;
}



/********************************************************************************/
/*										*/
/*	Constant handling							*/
/*										*/
/********************************************************************************/

private Object visit(BooleanLiteral v)
{
   IfaceValue v0 = fait_control.findConstantValue(v.booleanValue());
   pushValue(v0);
   return null;
}


private Object visit(CharacterLiteral v)
{
   String s = v.getEscapedValue();
   String s1 = IvyFormat.getLiteralValue(s);
   int val;
   if (s1.length() == 0) val = 0;
   else val = s1.charAt(0);
   IfaceType ctyp = fait_control.findConstantType("char",val);
   IfaceValue v0 = fait_control.findConstantValue(ctyp,val);
   pushValue(v0);
   return null;
}


private Object visit(NullLiteral v)
{
   pushValue(fait_control.findNullValue());
   return null;
}


private Object visit(NumberLiteral v)
{
   JcompType xjt = JcompAst.getExprType(v);
   IfaceType jt;
   IfaceValue v0 = null;
   switch (xjt.getName()) {
      case "float" :
      case "double" :
	 String dsv = v.getToken();
	 double dv = getFloatValue(dsv);
	 jt = fait_control.findConstantType(xjt.getName(),dv);
	 v0 = fait_control.findConstantValue(jt,dv);
	 break;
      default :
	 String sv = v.getToken();
	 long lv = getNumericValue(sv);
	 jt = fait_control.findConstantType(xjt.getName(),lv);
	 v0 = fait_control.findConstantValue(jt,lv);
	 break;
    }

   pushValue(v0);

   return null;
}


private Object visit(StringLiteral v)
{
   try {
      String s = v.getEscapedValue();	// this is thread safe, getLiteralValue is not
      String s1 = IvyFormat.getLiteralValue(s);
      pushValue(fait_control.findConstantStringValue(s1));
    }
   catch (Throwable e) {
      FaitLog.logE("Unable to get string literal value for " + v.getEscapedValue(),e);
      pushValue(fait_control.findConstantStringValue(v.getEscapedValue()));
    }
   return null;
}






private Object visit(TypeLiteral v)
{
   JcompType jt = JcompAst.getJavaType(v.getType());
   IfaceType it = convertType(jt);
   IfaceType ctyp = fait_control.findConstantType("java.lang.Class",it);
   IfaceValue v0 = fait_control.findNativeValue(ctyp);
   v0 = v0.forceNonNull();
   pushValue(v0);
   flow_queue.initialize(it);
   return null;
}



/********************************************************************************/
/*										*/
/*	Expression management							*/
/*										*/
/********************************************************************************/

private Object visit(ArrayAccess v)
{
   if (after_node == null) return v.getArray();
   else if (after_node == v.getArray()) return v.getIndex();
   else {
      IfaceValue vidx = popActual();
      if (vidx == null)
	 return NO_NEXT_REPORT;
      IfaceValue varr = popActualNonNull(1);
      if (varr == null)
	 return NO_NEXT_REPORT;
      Integer idx = vidx.getIndexValue();
      if (idx != null && idx < 0) {
	 IfaceType iob = fait_control.findDataType("java.lang.IndexOutOfBoundsException");
	 IfaceValue v0 = fait_control.findAnyValue(iob);
	 flow_queue.handleThrow(work_queue,getHere(),v0,cur_state);
       }
      IfaceType rtyp = convertType(JcompAst.getExprType(v));
      IfaceValue vrslt = fait_control.findRefValue(rtyp,varr,vidx);
      pushValue(vrslt);
    }

   return null;
}


private IfaceValue visitBack(ArrayAccess v,IfaceValue ref)
{
   IfaceValue nextref = null;

   if (after_node == null || after_node == v.getArray()) nextref = ref;
   else if (ref.getRefStack() == 0) {
      // nextref = fait_control.findRefValue(ref.getDataType(),
	    // cur_state.getStack(1),cur_state.getStack(0));
      nextref = null;
    }
   else nextref = adjustRef(ref,2,1);

   return nextref;
}


private Object visit(ArrayCreation v)
{
   List<?> dims = v.dimensions();
   if (after_node == null || after_node != v.getInitializer()) {
      int idx = 0;
      if (after_node != null) idx = dims.indexOf(after_node) + 1;
      if (idx < dims.size()) return dims.get(idx);
    }
   if (v.getInitializer() != null && after_node != v.getInitializer()) {
      return v.getInitializer();
    }

   IfaceType atyp = convertType(JcompAst.getExprType(v));
   int jsize = 0;
   IfaceType base = null;
   for (base = atyp; base.isArrayType(); base = base.getBaseType()) ++jsize;

   IfaceValue sz = null;
   IfaceValue rslt = null;
   if (v.getInitializer() != null) {
      IfaceValue v0 = popActual();
      if (v0.getDataType().isArrayType()) {
	 sz = v0.getArrayLength();
	 rslt = v0;
       }
    }

   List<IfaceValue> szs = new ArrayList<>();
   for (int i = 0; i < dims.size(); ++i) {
      sz = popActual();
      szs.add(sz);
    }

   if (jsize > 1)
      sz = null;
   if (rslt == null) {
      rslt = flow_queue.handleNewArraySet(getHere(),base,jsize,sz);
    }

   pushValue(rslt);

   return null;
}



private IfaceValue visitBack(ArrayCreation v,IfaceValue ref)
{
   if (after_node == null) return ref;
   if (v.getInitializer() != null && after_node == v.getInitializer()) return ref;
   List<?> dims = v.dimensions();
   int idx = dims.indexOf(after_node);
   if (idx < dims.size()-1) return ref;
   int ct = (v.getInitializer() != null ? 1 : 0);
   ct += dims.size();
   IfaceValue nextref = adjustRef(ref,ct,1);

   return nextref;
}


private Object visit(Assignment v)
{
   if (after_node == null) return v.getLeftHandSide();
   else if (after_node == v.getLeftHandSide()) return v.getRightHandSide();
   else  {
      IfaceValue v2 = popActual();
      IfaceValue v1 = popValue();
      FaitOperator fop = op_map.get(v.getOperator());
      if (fop != FaitOperator.ASG) {
	 IfaceValue v1v = getActualValue(v1,true);
	 if (v1v == null) return NO_NEXT_REPORT;
	 IfaceValue v2v = performOperation(v1v,v2,v1v.getDataType(),fop);
	 if (v2v != null) v2 = v2v;
	 else FaitLog.logE("NULL RESULT OF OPERATOR");
       }

      if (v2 == null) FaitLog.logE("Bad assignment attempted");
      v2 = assignValue(v1,v2,0);
      pushValue(v2);
    }
   return null;
}


private IfaceValue visitBack(Assignment v,IfaceValue ref,IfaceType settype)
{
   if (after_node != v.getRightHandSide()) return ref;

   if (ref.getRefStack() == 0) {
      queueBackRefs(getHere(),cur_state,ref,settype);
      return cur_state.getStack(1);
    }

   return adjustRef(ref,2,1);
}


private Object visit(CastExpression v)
{
   if (after_node == null) return v.getExpression();
   else {
      IfaceValue v0 = popActual();
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceType ctyp = convertType(JcompAst.getJavaType(v.getType()));
      IfaceProgramPoint typpt = fait_control.getAstReference(v.getType());
      IfaceAnnotation [] annots = fait_control.getAnnotations(typpt);
      IfaceType rtyp = null;
      if (annots != null)
	 rtyp = ctyp.getAnnotatedType(annots);
      IfaceValue v1 = flow_queue.castValue(ctyp,v0,getHere());
      if (rtyp != null) {
	 v1 = v1.changeType(rtyp);
	 // set v1 type to include annotations given in rtyp
       }
      if (FaitLog.isTracing()) FaitLog.logD1("Cast Value = " + v1);
      if (v1.mustBeNull()) v1 = fait_control.findNullValue(ctyp);
      if (!v1.mustBeNull() && v1.isEmptyEntitySet() && !v1.getDataType().isPrimitiveType()) {
	 return NO_NEXT_REPORT;
       }
      pushValue(v1);
    }

   return null;
}



private Object visit(ConditionalExpression ce)
{
   if (after_node == null) return ce.getExpression();
   else if (after_node == ce.getExpression()) {
      IfaceValue v = popActual();
      if (v == null) return NO_NEXT_REPORT;
      TestBranch tb = getTestResult(v);
      if (FaitLog.isTracing()) FaitLog.logD1("Conditional branch " + tb + " " + v);
      Pair<IfaceState,IfaceState> imps = handleImplications(ce.getExpression());
      if (tb != TestBranch.ALWAYS) {
	 workOn(imps.getElement1(),ce.getElseExpression());
       }
      if (tb != TestBranch.NEVER) {
	 workOn(imps.getElement0(),ce.getThenExpression());
       }
      return NO_NEXT;
    }
   else {
      IfaceValue v = popActual();
      IfaceType ctyp = convertType(JcompAst.getExprType(ce));
      IfaceValue v1 = flow_queue.castValue(ctyp,v,getHere());
      pushValue(v1);
    }

   return null;
}




@SuppressWarnings("unused")
private Object OLDvisit(ConditionalExpression v)
{
   if (after_node == null) return v.getExpression();
   else if (after_node == v.getExpression()) {
      IfaceValue v0 = popActual();
      if (v0 == null) return NO_NEXT_REPORT;
      TestBranch brslt = getTestResult(v0);
      // flow_queue.handleImplications(work_queue,ast_where,cur_state,brslt);
      pushValue(fait_control.findMarkerValue(ast_where,brslt));
      if (brslt == TestBranch.NEVER) {
	 return v.getElseExpression();
       }
      else {
	 return v.getThenExpression();
       }
    }
   else if (after_node == v.getThenExpression()) {
      IfaceValue v0 = popActual();
      Set<Object> mrk = popMarker(v);
      IfaceType t0 = convertType(JcompAst.getExprType(v));
      IfaceValue v1 = flow_queue.castValue(t0,v0,getHere());
      pushValue(v1);
      if (mrk.contains(TestBranch.ANY)) {
	 pushValue(fait_control.findMarkerValue(ast_where,TestBranch.ANY));
	 return v.getElseExpression();
       }
    }
   else if (after_node == v.getElseExpression()) {
      IfaceValue v0 = popActual();
      Set<Object> mrk = popMarker(v);
      IfaceType t0 = convertType(JcompAst.getExprType(v));
      IfaceValue v1 = flow_queue.castValue(t0,v0,getHere());
      if (mrk.contains(TestBranch.ANY)) {
	 IfaceValue v2 = popActual();
	 v1 = v1.mergeValue(v2);
       }
      pushValue(v1);
    }

   return null;
}



private IfaceValue visitBack(ConditionalExpression v,IfaceValue ref)
{
   if (after_node == null) return ref;
   return adjustRef(ref,1,0);
}


private Object visit(FieldAccess v)
{
   if (after_node == null) return v.getExpression();
   else {
      JcompSymbol sym = JcompAst.getReference(v.getName());
      IfaceValue v0 = popActualNonNull(0);
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceType rcls = convertType(JcompAst.getExprType(v));
      IfaceValue ref = null;
      if (sym != null) {
	 IfaceField fld = getField(sym);
	 ref = fait_control.findRefValue(rcls,v0,fld);
       }
      else {
	 // handle .length
	 ref = fait_control.findAnyValue(rcls);
       }
      pushValue(ref);
    }

   return null;
}


private IfaceValue visitBack(FieldAccess v,IfaceValue ref)
{
   if (ref.getRefStack() == 0) {
      JcompSymbol sym = JcompAst.getReference(v.getName());
      IfaceField fld = getField(sym);
      return fait_control.findRefValue(ref.getDataType(),cur_state.getStack(0),fld);
    }
   return adjustRef(ref,0,1);
}


private Object visit(SuperFieldAccess v)
{
   // handleAccess() ...
   String var = "this";
   if (v.getQualifier() != null) {
      JcompType qtyp = JcompAst.getExprType(v.getQualifier());
      var = qtyp.getName() + ".this";
    }
   JcompSymbol fld = JcompAst.getReference(v.getName());
   IfaceField ifld = getField(fld);
   IfaceValue vlhs = cur_state.getLocal(getSlot(var));
   IfaceType rcls = convertType(JcompAst.getExprType(v));
   IfaceValue ref = fait_control.findRefValue(rcls,vlhs,ifld);
   pushValue(ref);

   return null;
}


private Object visit(InfixExpression v)
{
   if (after_node == null) return v.getLeftOperand();
   else if (after_node == v.getLeftOperand()) {
      if (v.getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
	 IfaceValue v1 = popActual();
	 if (v1 == null) return NO_NEXT_REPORT;
	 TestBranch tb = getTestResult(v1);
	 if (tb == TestBranch.NEVER) {
	    pushValue(v1);
	    return null;
	  }
	 else {
	    pushValue(v1);
	    return v.getRightOperand();
	  }
       }
      else if (v.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
	 IfaceValue v1 = popActual();
	 if (v1 == null) return NO_NEXT_REPORT;
	 TestBranch tb = getTestResult(v1);
	 if (tb == TestBranch.ALWAYS) {
	    pushValue(v1);
	    return null;
	  }
	 else {
	    pushValue(v1);
	    return v.getRightOperand();
	  }
       }
      else return v.getRightOperand();
    }
   else {
      IfaceValue v0 = popActual();
      IfaceValue v1 = popActual();
      if (v0 == null || v1 == null) return NO_NEXT_REPORT;
      FaitOperator op = op_map.get(v.getOperator());
      IfaceType rtyp = convertType(JcompAst.getExprType(v));
      IfaceValue v2 = performOperation(v1,v0,rtyp,op);
      if (FaitLog.isTracing()) FaitLog.logD1("Result = " + v2);
      pushValue(v2);
      if (v.hasExtendedOperands()) {
	 if (v.getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
	    TestBranch tb = getTestResult(v2);
	    if (tb == TestBranch.NEVER) return null;
	  }
	 else if (v.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
	    TestBranch tb = getTestResult(v2);
	    if (tb == TestBranch.ALWAYS) return null;
	  }
	 List<?> ext = v.extendedOperands();
	 int idx = ext.indexOf(after_node) + 1;
	 if (idx < ext.size()) return ext.get(idx);
       }
    }
   return null;
}


private IfaceValue visitBack(InfixExpression v,IfaceValue ref,IfaceType settype)
{
   InfixExpression.Operator op = v.getOperator();

   if (after_node == null) return ref;
   else if (op == InfixExpression.Operator.CONDITIONAL_AND ||
	 op == InfixExpression.Operator.CONDITIONAL_OR) {
      if (v.extendedOperands().size() > 0) return  null;
      if (after_node == v.getLeftOperand()) return adjustRef(ref,1,0);
      else return adjustRef(ref,1,1);
    }
   else if (after_node == v.getLeftOperand()) {
      return ref;
    }
   else {
      if (v.extendedOperands().size() > 0) return  null;
      if (ref.getRefStack() == 0) {
	 IfaceValue vl = getActualValue(cur_state,getHere(),cur_state.getStack(1),false);
	 IfaceValue vr = getActualValue(cur_state,getHere(),cur_state.getStack(0),false);
	 List<IfaceType> ntyps = settype.getBackTypes(op_map.get(v.getOperator()),vl,vr);
	 if (ntyps != null) {
	    IfaceType ntyp = ntyps.get(0);
	    if (ntyp != null) {
	       IfaceValue nref = fait_control.findRefStackValue(vl.getDataType(),1);
	       queueBackRefs(getHere(),cur_state,nref,ntyp);
	     }
	    ntyp = ntyps.get(1);
	    if (ntyp != null) {
	       IfaceValue nref = fait_control.findRefStackValue(vr.getDataType(),0);
	       queueBackRefs(getHere(),cur_state,nref,ntyp);
	     }
	  }
	 return null;
       }
      else return adjustRef(ref,2,1);
    }
}



private Object visit(InstanceofExpression v)
{
   if (after_node == null) return v.getLeftOperand();
   else {
      IfaceType rt = convertType(JcompAst.getJavaType(v.getRightOperand()));
      IfaceValue v0 = popActual();
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceValue v2 = fait_control.findAnyValue(rt);
      IfaceType rtyp = fait_control.findDataType("boolean");
      IfaceValue v1 = performOperation(v0,v2,rtyp,FaitOperator.INSTANCEOF);
      pushValue(v1);
    }
   return null;
}


private IfaceValue visitBack(InstanceofExpression v,IfaceValue ref)
{
   if (after_node == null) return ref;
   return adjustRef(ref,1,1);
}



private Object visit(SimpleName v)
{
   JcompSymbol js = JcompAst.getReference(v);
   IfaceType rcls = convertType(JcompAst.getExprType(v));
   if (js != null && js.isFieldSymbol()) {
      IfaceField fld = getField(js);
      IfaceValue ref = null;
      if (js.isStatic()) {
	 ref = fait_control.findRefValue(rcls,null,fld);
       }
      else {
	 IfaceValue thisv = getThisValue(fld.getDeclaringClass());
	 if (thisv == null) return NO_NEXT;
	 ref = fait_control.findRefValue(rcls,thisv,fld);
       }
      pushValue(ref);
    }
   else if (js != null && js.isEnumSymbol()) {
      IfaceValue ref = null;
      IfaceField fld = fait_control.findField(rcls,js.getName());
      if (fld != null) {
	 ref = fait_control.findRefValue(rcls,null,fld);
       }
      if (ref == null) {
	 ref = fait_control.findNativeValue(rcls);
	 ref = ref.forceNonNull();
       }
      pushValue(ref);
    }
   else {
      int slot = getSlot(js);
      if (slot >= 0) {
	 IfaceValue ref = fait_control.findRefValue(rcls,slot);
	 pushValue(ref);
       }
      else {
	 FaitLog.logE("Unknown name " + v + " " + v.getParent().getParent() + " " + getHere());
	 return NO_NEXT;
       }
    }

   if (FaitLog.isTracing()) FaitLog.logD1("Result = " + cur_state.getStack(0));

   return null;
}



private IfaceValue visitBack(SimpleName v,IfaceValue ref)
{
   JcompSymbol js = JcompAst.getReference(v);
   if (ref.getRefStack() == 0) {
      if (js != null && js.isFieldSymbol()) {
	 IfaceField fld = getField(js);
	 if (js.isStatic()) {
	    return fait_control.findRefValue(ref.getDataType(),null,fld);
	  }
	 else {
	    IfaceValue thisv = getThisValue(fld.getDeclaringClass());
	    return fait_control.findRefValue(ref.getDataType(),thisv,fld);
	  }
       }
      else if (js != null && js.isEnumSymbol()) {
	 return null;
       }
      else {
	 int slot = getSlot(js);
	 if (slot >= 0) {
	    return fait_control.findRefValue(ref.getDataType(),slot);
	  }
	 return null;
       }
    }
   else return adjustRef(ref,0,1);
}



private IfaceValue getThisValue(IfaceType typ)
{
   IfaceValue thisv = getLocal(0);
   if (thisv == null)
      thisv = getLocal("this");
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
      IfaceValue rval = fait_control.findRefValue(xtyp,nthis,xfld);
      nthis = getActualValue(rval,true);
      if (xtyp.isDerivedFrom(typ)) return nthis;
    }
   return thisv;
}



private Object visit(QualifiedName v)
{
   JcompSymbol sym = JcompAst.getReference(v.getName());
   if (after_node == null) {
      if (sym != null && sym.isFieldSymbol() && !sym.isStatic()) {
	 return v.getQualifier();
       }
      else if (sym == null && v.getName().getIdentifier().equals("length")) {
	 return v.getQualifier();
       }
      else return v.getName();
    }
   if (after_node == v.getQualifier() && sym != null) {
      IfaceValue v0 = popActualNonNull(0);
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceType rtyp = convertType(JcompAst.getExprType(v));
      IfaceField fld = getField(sym);
      IfaceValue v1 = fait_control.findRefValue(rtyp,v0,fld);
      pushValue(v1);
    }
   else if (after_node == v.getQualifier() && sym == null) {
      IfaceValue v0 = popActualNonNull(0);
      if (v0 == null) return NO_NEXT_REPORT;
      // IfaceValue v1 = v0.getArrayLength();
      IfaceValue v1 = flow_queue.handleArrayLength(getHere(),v0);
      pushValue(v1);
    }

   return null;
}


private IfaceValue visitBack(QualifiedName v,IfaceValue ref)
{
   if (after_node == null) return ref;
   JcompSymbol sym = JcompAst.getReference(v.getName());
   if (after_node == v.getQualifier() && sym != null) {
      if (ref.getRefStack() == 0) {
	 IfaceValue v0 = cur_state.getStack(0);
	 IfaceField fld = getField(sym);
	 if (fld == null) return null;
	 return fait_control.findRefValue(ref.getDataType(),v0,fld);
       }
      return adjustRef(ref,1,1);
    }
   else if (after_node == v.getQualifier() && sym == null) {
      return adjustRef(ref,1,1);
    }
   else return ref;
}



private Object visit(ParenthesizedExpression v)
{
   if (after_node == null) return v.getExpression();

   return null;
}


private Object visit(PostfixExpression v)
{
   if (after_node == null) return v.getOperand();

   int i0 = 1;
   FaitOperator op = FaitOperator.ADD;
   if (v.getOperator() == PostfixExpression.Operator.DECREMENT) {
      i0 = -1;
      op = FaitOperator.SUB;
    }
   IfaceType rtyp = convertType(JcompAst.getExprType(v));
   IfaceValue v1 = popValue();
   IfaceValue v2 = getActualValue(v1,true);
   if (v2 == null) return NO_NEXT_REPORT;
   IfaceValue v3 = fait_control.findConstantValue(rtyp,i0);
   IfaceValue v4 = performOperation(v3,v2,rtyp,op);
   assignValue(v1,v4,0);
   pushValue(v2);
   return null;
}


private IfaceValue visitBack(PostfixExpression v,IfaceValue ref,IfaceType settype)
{
   if (after_node == null) return ref;

   IfaceValue v1 = cur_state.getStack(0);
   if (ref.getRefSlot() == v1.getRefSlot() && v1.getRefSlot() > 0) {
      IfaceValue vl = getActualValue(cur_state,getHere(),v1,false);
      List<IfaceType> ntyps = settype.getBackTypes(op_map.get(v.getOperator()),vl);
      if (ntyps != null && ntyps.get(0) != null) {
	 IfaceType ntyp = ntyps.get(0);
	 queueBackRefs(getHere(),cur_state,v1,ntyp);
       }
      return null;
    }
   if (ref.getRefStack() == 0) {
      IfaceValue vl = getActualValue(cur_state,getHere(),v1,false);
      List<IfaceType> ntyps = settype.getBackTypes(op_map.get(v.getOperator()),vl);
      if (ntyps != null) {
	 IfaceType ntyp = ntyps.get(0);
	 IfaceValue nref = fait_control.findRefStackValue(vl.getDataType(),0);
	 queueBackRefs(getHere(),cur_state,nref,ntyp);
       }
      return null;
    }

   return adjustRef(ref,1,1);
}


private Object visit(PrefixExpression v)
{
   if (after_node == null) return v.getOperand();

   IfaceType rtyp = convertType(JcompAst.getExprType(v));
   FaitOperator op = op_map.get(v.getOperator());
   if (op == null) {
      int i0 = 1;
      op = FaitOperator.ADD;
      if (v.getOperator() == PrefixExpression.Operator.DECREMENT) {
	 i0 = -1;
	 op = FaitOperator.SUB;
       }
      IfaceValue v1 = popValue();
      IfaceValue v2 = getActualValue(v1,true);
      if (v2 == null) return NO_NEXT_REPORT;
      IfaceValue v3 = fait_control.findConstantValue(rtyp,i0);
      IfaceValue v4 = performOperation(v3,v2,rtyp,op);
      assignValue(v1,v4,0);
      pushValue(v4);
    }
   else {
      IfaceValue v1 = popActual();
      if (v1 == null) return NO_NEXT_REPORT;
      IfaceValue v2 = performOperation(v1,v1,rtyp,op);
      pushValue(v2);
    }

   return null;
}


private IfaceValue visitBack(PrefixExpression v,IfaceValue ref,IfaceType settype)
{
   if (after_node == null) return ref;

   IfaceValue v1 = cur_state.getStack(0);
   if (v1 == null) return null;
   if (ref.getRefSlot() == v1.getRefSlot() && v1.getRefSlot() > 0) {
      IfaceValue vl = getActualValue(cur_state,getHere(),v1,false);
      List<IfaceType> ntyps = settype.getBackTypes(op_map.get(v.getOperator()),vl);
      if (ntyps != null && ntyps.get(0) != null) {
	 IfaceType ntyp = ntyps.get(0);
	 queueBackRefs(getHere(),cur_state,v1,ntyp);
       }
      return null;
    }
   if (ref.getRefStack() == 0) {
      IfaceValue vl = getActualValue(cur_state,getHere(),v1,false);
      List<IfaceType> ntyps = settype.getBackTypes(op_map.get(v.getOperator()),vl);
      if (ntyps != null) {
	 IfaceType ntyp = ntyps.get(0);
	 IfaceValue nref = fait_control.findRefStackValue(vl.getDataType(),0);
	 queueBackRefs(getHere(),cur_state,nref,ntyp);
       }
      return null;
    }

   return adjustRef(ref,1,1);
}



private Object visit(ThisExpression v)
{
   JcompType base = null;
   if (v.getQualifier() != null) {
      base = JcompAst.getExprType(v.getQualifier());
    }
   IfaceValue v0 = null;
   if (base != null) {
      IfaceType rtyp = convertType(base);
      v0 = getThisValue(rtyp);
      if (v0 == null) {
	 if (FaitLog.isTracing())
	    FaitLog.logD1("Attempt to use X.this before initialization");
	 return NO_NEXT;
       }
    }
   else {
      v0 = getLocal("this");
    }
   pushValue(v0);

   return null;
}






/********************************************************************************/
/*										*/
/*	Call methods								*/
/*										*/
/********************************************************************************/

private Object visit(ClassInstanceCreation v)
{
   JcompType rty = JcompAst.getJavaType(v.getType());
   if (v.getAnonymousClassDeclaration() != null) {
      rty = JcompAst.getJavaType(v.getAnonymousClassDeclaration());
    }
   // TODO: handle expression.new ...
   if (after_node == null) {
      IfaceType irty = convertType(rty);
      IfaceProgramPoint pt = fait_control.getAstReference(v.getType());
      IfaceAnnotation [] ans = fait_control.getAnnotations(pt);
      if (ans != null) irty = irty.getAnnotatedType(ans);

      flow_queue.initialize(irty);

      IfaceType t1 = irty.getComputedType(FaitTypeOperator.DONEINIT);
      IfaceType t2 = irty.getComputedType(FaitTypeOperator.STARTINIT);

      IfaceEntity ent = null;
      if (fait_control.isSingleAllocation(t1,true))
	 ent = fait_control.findFixedEntity(t1);
      else
	 ent = getLocalEntity(work_queue.getCall(),getHere(),t1);
      IfaceValue v0 = fait_control.findObjectValue(t1,fait_control.createSingletonSet(ent));
      cur_state.pushStack(v0);

      IfaceValue v2 = v0.changeType(t2);
      cur_state.pushStack(v2);

      if (rty.needsOuterClass()) {
	 IfaceType outer = convertType(rty.getOuterType());
	 IfaceValue v1 = getThisValue(outer);
	 cur_state.pushStack(v1);
       }
    }

   int idx = 0;
   List<?> args = v.arguments();
   if (after_node != null) idx = args.indexOf(after_node) + 1;

   if (idx < args.size()) return args.get(idx);

   int narg = args.size()+1;
   for (JcompType xty = rty; xty != null; xty = xty.getOuterType()) {
      if (xty.needsOuterClass()) ++narg;
      else break;
    }

   if (v.getAnonymousClassDeclaration() != null) {
      setExternalSymbols(v.getAnonymousClassDeclaration());
      IfaceValue this0 = getLocal("this");
      if (this0 != null) {
	 IfaceType irty = convertType(rty);
	 IfaceValue thisval = cur_state.getStack(0);
	 IfaceField fld = fait_control.findField(irty,"this$0");
	 if (fld == null) {
	    System.err.println("Cant find this$0 field for " + irty);
	  }
	 else {
	    flow_queue.handleFieldSet(fld,getHere(),null,false,this0,thisval,-1);
	  }
       }
    }

   JcompSymbol js = JcompAst.getReference(v);
   // check for case with only a default constructor
   if (js != null) {
      switch (processCall(v,narg)) {
	 case CONTINUE :
	    break;
	 case NOT_DONE :
	    return NO_NEXT_REPORT;
	 case NO_RETURN:
	    return NO_NEXT;
       }
    }
   else {
      if (rty.needsOuterClass()) popValue();
      popValue();
    }

   return null;
}



private IfaceValue visitBack(ClassInstanceCreation v,IfaceValue ref)
{
   JcompType rty = JcompAst.getJavaType(v.getType());
   if (after_node == null) {
      int ct = (rty.needsOuterClass() ? 3 : 2);
      return adjustRef(ref,ct,0);
    }
   List<?> args = v.arguments();
   int idx = args.indexOf(after_node)+1;
   if (idx < args.size()) return ref;

   return null;
}


private Object visit(ConstructorInvocation v)
{
   JcompSymbol rtn = JcompAst.getReference(v);
   JcompType rty = rtn.getClassType();
   if (after_node == null) {
      IfaceValue v0 = cur_state.getLocal(0);
      cur_state.pushStack(v0);
      if (rty.needsOuterClass()) {
	 IfaceType outer = convertType(rty.getOuterType());
	 IfaceValue v1 = getThisValue(outer);
	 cur_state.pushStack(v1);
       }
    }
   int idx = 0;
   List<?> args = v.arguments();
   if (after_node != null) idx = args.indexOf(after_node) + 1;
   if (idx < args.size()) return args.get(idx);
   int delta = 1;
   if (rty.needsOuterClass()) delta = 2;

   switch (processCall(v,args.size() + delta)) {
      case NOT_DONE :
	 return NO_NEXT_REPORT;
      case NO_RETURN :
	 return NO_NEXT;
      case CONTINUE :
	 break;
    }

   return null;
}


private IfaceValue visitBack(ConstructorInvocation v,IfaceValue ref)
{
   JcompSymbol rtn = JcompAst.getReference(v);
   JcompType rty = rtn.getClassType();
   if (after_node == null) {
      int ct = (rty.needsOuterClass() ? 2 : 1);
      return adjustRef(ref,0,ct);
    }
   List<?> args = v.arguments();
   int idx = args.indexOf(after_node) + 1;
   if (idx < args.size()) return ref;

   return null;
}


private Object visit(MethodInvocation v)
{
   JcompSymbol js = JcompAst.getReference(v.getName());
   if (after_node == null && v.getExpression() == null) {
      if (!js.isStatic()) {
	 IfaceType mcls = convertType(js.getClassType());
	 IfaceValue v0 = getThisValue(mcls);
	 cur_state.pushStack(v0);
       }
    }
   else if (after_node == null && v.getExpression() != null) {
      if (js == null || !js.isStatic()) return v.getExpression();
    }

   int idx = 0;
   List <?> args = v.arguments();
   if (after_node != null && after_node != v.getExpression()) idx = args.indexOf(after_node) + 1;
   if (idx < args.size()) return args.get(idx);

   int act = args.size();
   if (js != null && !js.isStatic()) ++act;

   switch (processCall(v,act)) {
      case NOT_DONE :
	 return NO_NEXT_REPORT;
      case CONTINUE :
	 break;
      case NO_RETURN :
	 return NO_NEXT;
    }

   JcompType jt = JcompAst.getExprType(v);
   if (jt.isErrorType())
      FaitLog.logW("compilation error on call");
   if (!jt.isPrimitiveType() && !jt.isErrorType()) {
      IfaceType it = convertType(jt);
      IfaceValue rslt = cur_state.getStack(0);
      it = it.getAnnotatedType(rslt.getDataType());
      if (!rslt.getDataType().isCompatibleWith(it)) {
	 IfaceValue vrslt = rslt.restrictByType(it);
	 if (vrslt != null && vrslt != rslt) {
	    if (vrslt.mustBeNull() && !rslt.mustBeNull()) {
	       vrslt = rslt.restrictByType(it);
	     }
	    if (FaitLog.isTracing())
	       FaitLog.logD("Change Return value to " + vrslt);
	    cur_state.setStack(0,vrslt);
	  }
       }
    }

   return null;
}



private IfaceValue visitBack(MethodInvocation v,IfaceValue ref,IfaceType settype)
{
   JcompSymbol js = JcompAst.getReference(v.getName());
   int dref = 0;
   if (after_node == null && v.getExpression() == null) {
      if (!js.isStatic()) {
	 dref = 1;
       }
    }
   else if (after_node == null) return ref;

   List<?> args = v.arguments();
   int idx = args.indexOf(after_node)+1;
   if (idx < args.size()) {
      if (dref != 0) return adjustRef(ref,0,dref);
      else return ref;
    }

   int ret = (js.getType().getBaseType().isVoidType() ? 0 : 1);
   if (ref.getRefStack() == 0 && ret == 1) {
      work_queue.getCall().backFlowReturn(getHere(),settype);
      return null;
    }

   if (ref.getRefStack() < 0 && ref.getRefSlot() < 0) return null;
   int sz = args.size();
   if (!js.isStatic()) sz += 1;

   return adjustRef(ref,sz,ret);
}


private Object visit(SuperConstructorInvocation v)
{
   JcompType rty = JcompAst.getExprType(v);
   // rty = rty.getSuperType();
   if (rty == null) return null;	// supertype is Object -- ignore
   if (rty.isJavaLangObject()) return null;

   if (after_node == null) {
      IfaceValue v0 = cur_state.getLocal(0);
      cur_state.pushStack(v0);
      if (rty != null && rty.needsOuterClass()) {
	 IfaceType outer = convertType(rty.getOuterType());
	 IfaceValue v1 = getThisValue(outer);
	 cur_state.pushStack(v1);
       }
    }
   int idx = 0;
   List<?> args = v.arguments();
   if (after_node != null) idx = args.indexOf(after_node) + 1;
   if (idx < args.size()) return args.get(idx);

   int delta = 1;
   if (rty.needsOuterClass()) ++delta;
   switch (processCall(v,args.size()+delta)) {
      case NOT_DONE :
	 // super constructor does nothing -- can ignore
	 if (JcompAst.getReference(v) == null) return null;
	 return NO_NEXT_REPORT;
      case NO_RETURN :
	 return NO_NEXT;
      case CONTINUE :
	 break;
    }

   return null;
}


private Object visit(SuperMethodInvocation v)
{
   JcompSymbol js = JcompAst.getReference(v.getName());
   if (after_node == null) {
      if (!js.isStatic()) {
	 IfaceType mcls = convertType(js.getClassType());
	 IfaceValue v0 = getThisValue(mcls);
	 FaitLog.logD("Push super this value " + v0);
	// IfaceValue v0 = cur_state.getLocal(0);
	 cur_state.pushStack(v0);
       }
    }

   int idx = 0;
   List <?> args = v.arguments();
   if (after_node != null) idx = args.indexOf(after_node) + 1;
   if (idx < args.size()) return args.get(idx);

   int act = args.size();
   if (js != null && !js.isStatic()) ++act;

   switch (processCall(v,act)) {
      case NOT_DONE :
	 return NO_NEXT_REPORT;
      case NO_RETURN :
	 return NO_NEXT;
      case CONTINUE :
	 break;
    }

   JcompType jt = JcompAst.getExprType(v);
   if (jt.isErrorType())
      FaitLog.logW("compilation error on call");
   if (!jt.isPrimitiveType() && !jt.isErrorType()) {
      IfaceType it = convertType(jt);
      IfaceValue rslt = cur_state.getStack(0);
      it = it.getAnnotatedType(rslt.getDataType());
      if (!rslt.getDataType().isCompatibleWith(it)) {
	 IfaceValue vrslt = rslt.restrictByType(it);
	 if (vrslt != null && vrslt != rslt) {
	    if (vrslt.mustBeNull() && !rslt.mustBeNull()) {
	       vrslt = rslt.restrictByType(it);
	     }
	    if (FaitLog.isTracing())
	       FaitLog.logD("Change Return value to " + vrslt);
	    cur_state.setStack(0,vrslt);
	  }
       }
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Statement handling							*/
/*										*/
/********************************************************************************/

private Object visit(AssertStatement s)
{
   if (after_node == null) return s.getExpression();
   popValue();
   return null;
}


private IfaceValue visitBack(AssertStatement s,IfaceValue ref)
{
   if (after_node == null) return ref;
   return adjustRef(ref,1,0);
}



private Object visit(Block s)
{
   int idx = 0;
   List<?> stmts = s.statements();
   if (after_node != null) idx = stmts.indexOf(after_node) + 1;
   if (idx < stmts.size()) return stmts.get(idx);
   return null;
}



private Object visit(BreakStatement s)
{
   if (s.getLabel() != null) {
      return new FlowAstStatus(Reason.BREAK,s.getLabel().getIdentifier());
    }
   return new FlowAstStatus(Reason.BREAK,"");
}



private Object visit(ContinueStatement s)
{
   if (s.getLabel() != null) {
      return new FlowAstStatus(Reason.CONTINUE,s.getLabel().getIdentifier());
    }
   return new FlowAstStatus(Reason.CONTINUE,"");
}



private Object visit(DoStatement s)
{
   if (after_node == null) return s.getBody();
   else if (after_node == s.getBody()) return s.getExpression();
   else {
      IfaceValue v = popActual();
      if (v == null) return NO_NEXT_REPORT;
      TestBranch tb = getTestResult(v);
      if (FaitLog.isTracing()) FaitLog.logD1("While branch " + tb + " " + v);
      Pair<IfaceState,IfaceState> imps = handleImplications(s.getExpression());
      if (tb != TestBranch.NEVER) {
	 workOn(imps.getElement1(),s.getBody());
       }
      if (tb == TestBranch.ALWAYS) {
	 cur_state = imps.getElement0();
	 return s;
       }
      cur_state = imps.getElement1();
    }

   return null;
}



private IfaceValue visitBack(DoStatement s,IfaceValue ref)
{
   if (after_node == null || after_node == s.getBody()) return ref;

   return adjustRef(ref,1,0);
}

private Object visitThrow(DoStatement s,IfaceAstStatus sts)
{
   String lbl = sts.getMessage();
   switch (sts.getReason()) {
      case BREAK :
	 if (checkLabel(s,lbl)) break;
	 else return sts;
      case CONTINUE :
	 if (checkLabel(s,lbl)) return s.getExpression();
	 else return sts;
      default :
	 return sts;
    }

   return null;
}



private Object visit(ExpressionStatement s)
{
   if (after_node == null) return s.getExpression();
   else {
      JcompType typ =JcompAst.getExprType(s.getExpression());
      if (typ != null && !typ.isVoidType()) popValue();
    }
   return null;
}


private IfaceValue visitBack(ExpressionStatement s,IfaceValue ref)
{
   if (after_node == null) return ref;
   return adjustRef(ref,1,0);
}



private Object visit(ForStatement s)
{
   StructuralPropertyDescriptor spd = null;
   if (after_node != null) spd = after_node.getLocationInParent();
   if (after_node != null && after_node == s.getExpression()) {
      IfaceValue bf = popActual();
      if (bf == null) return NO_NEXT_REPORT;
      TestBranch tb = getTestResult(bf);
      if (FaitLog.isTracing()) FaitLog.logD1("For test " + tb + " " + bf);
      Pair<IfaceState,IfaceState> imps = handleImplications(s.getExpression());
      if (tb == TestBranch.NEVER) {
	 cur_state = imps.getElement1();
	 return null;
       }
      else if (tb == TestBranch.ALWAYS) {
	 cur_state = imps.getElement0();
	 return s.getBody();
       }
      workOn(imps.getElement0(),s.getBody());
      cur_state = imps.getElement1();
      return null;
    }

   if (after_node == null || spd == ForStatement.INITIALIZERS_PROPERTY) {
      int idx = 0;
      List<?> inits = s.initializers();
      if (after_node != null) {
	 idx = inits.indexOf(after_node) + 1;
       }
      if (idx < inits.size()) return inits.get(idx);
    }

   if (after_node == s.getBody() || spd == ForStatement.UPDATERS_PROPERTY) {
      List<?> upds = s.updaters();
      int idx = 0;
      if (after_node != s.getBody()) idx = upds.indexOf(after_node) + 1;
      if (idx < upds.size()) return upds.get(idx);
    }

   if (s.getExpression() == null) return s.getBody();
   else return s.getExpression();
}


private IfaceValue visitBack(ForStatement s,IfaceValue ref)
{
   if (after_node != null && after_node == s.getExpression()) {
      return adjustRef(ref,1,0);
    }
   return ref;
}


private Object visitThrow(ForStatement s,IfaceAstStatus sts)
{
   String lbl = sts.getMessage();
   switch (sts.getReason()) {
      case BREAK :
	 if (checkLabel(s,lbl)) break;
	 else return sts;
      case CONTINUE :
	 if (checkLabel(s,lbl)) {
	    if (s.getExpression() != null) return s.getExpression();
	    else return s.getBody();
	  }
	 else return sts;
      default :
	 return sts;
    }

   return null;
}



private Object visit(IfStatement s)
{
   if (after_node == null) return s.getExpression();
   else if (after_node == s.getExpression()) {
      boolean nextok = false;
      IfaceValue v = popActual();
      if (v == null) return NO_NEXT_REPORT;
      TestBranch tb = getTestResult(v);
      if (FaitLog.isTracing()) FaitLog.logD1("If branch " + tb + " " + v);
      Pair<IfaceState,IfaceState> imps = handleImplications(s.getExpression());
      if (tb != TestBranch.NEVER) {
	 workOn(imps.getElement0(),s.getThenStatement());
       }
      if (tb != TestBranch.ALWAYS) {
	 if (s.getElseStatement() != null) {
	    workOn(imps.getElement1(),s.getElseStatement());
	  }
	 else nextok = true;
       }
      if (!nextok) return NO_NEXT;
    }

   return null;
}



private IfaceValue visitBack(IfStatement s,IfaceValue ref)
{
   if (after_node != null && after_node == s.getExpression()) {
      return adjustRef(ref,1,0);
    }
   return ref;
}


private Object visit(LabeledStatement s)
{
   if (after_node == null) return s.getBody();
   return null;
}


private Object visit(ReturnStatement s)
{
   if (after_node == null && s.getExpression() != null) return s.getExpression();

   IfaceValue rval = null;
   if (s.getExpression() != null) {
      rval = popActual();
      IfaceMethod cm = work_queue.getCall().getMethod();
      IfaceType rtyp = FlowQueue.getAnnotatedReturnType(cm);
      rval = flow_queue.castValue(rtyp,rval,getHere());
      if (rval == null) return NO_NEXT_REPORT;
    }

   return new FlowAstStatus(Reason.RETURN,rval);
}


private IfaceValue visitBack(ReturnStatement s,IfaceValue ref)
{
   if (after_node == null && s.getExpression() != null) return ref;

   return adjustRef(ref,1,0);
}



private Object visit(SwitchStatement s)
{
   if (after_node == null) return s.getExpression();
   else if (after_node == s.getExpression()) {
      popValue();
      boolean havedflt = false;
      List<?> stmts = s.statements();
      boolean sw = false;
      for (Object o : stmts) {
	 Statement st = (Statement) o;
	 if (st instanceof SwitchCase) {
	    SwitchCase sc = (SwitchCase) st;
	    if (sc.isDefault()) havedflt = true;
	    sw = true;
	    continue;
	  }
	 else if (sw) {
	    workOn(st);
	    sw = false;
	  }
       }
      if (havedflt) return NO_NEXT;
    }
   else {
      List<?> stmts = s.statements();
      int idx = stmts.indexOf(after_node) + 1;
      if (idx < stmts.size()) return stmts.get(idx);
    }

   return null;
}


private IfaceValue visitBack(SwitchStatement s,IfaceValue ref)
{
   if (after_node != null && after_node == s.getExpression()) {
      return adjustRef(ref,1,0);
    }
   return ref;
}



private Object visitThrow(SwitchStatement s,IfaceAstStatus sts)
{
   String lbl = sts.getMessage();
   switch (sts.getReason()) {
      case BREAK :
	 if (checkLabel(s,lbl)) return null;
	 else return sts;
      default :
	 return sts;
    }
}


private Object visit(SwitchCase s)
{
   return null;
}


private Object visit(SynchronizedStatement s)
{
   if (after_node == null) return s.getExpression();
   else if (after_node == s.getExpression()) {
      if (cur_state != null) {
	 IfaceValue v0 = popActualNonNull(0);
	 if (v0 == null) return NO_NEXT_REPORT;
	 pushValue(v0);
       }
      return s.getBody();
    }
   else {
      popValue();
      cur_state.discardFields();
    }

   return null;
}



private IfaceValue visitBack(SynchronizedStatement s,IfaceValue ref)
{
   if (after_node == null) return ref;
   else if (after_node == s.getExpression()) {
      return adjustRef(ref,0,1);
    }
   else return adjustRef(ref,1,0);
}


private Object visitThrow(SynchronizedStatement s,IfaceAstStatus sts)
{
   cur_state.discardFields();
   return sts;
}


private Object visit(ThrowStatement s)
{
   if (after_node == null) return s.getExpression();
   IfaceValue v = popActualNonNull(0);
   if (v == null) return NO_NEXT_REPORT;
   flow_queue.handleThrow(work_queue,getHere(),v,cur_state);
   return NO_NEXT;
}



private IfaceValue visitBack(ThrowStatement s,IfaceValue ref)
{
   if (after_node == null) return ref;
   return adjustRef(ref,1,0);
}


private Object visit(TryStatement s)
{
   if (after_node == null || after_node.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY) {
      if (after_node == null) {
	 pushValue(fait_control.findMarkerValue(ast_where,TryState.BODY));
       }
      List<?> res = s.resources();
      if (res.isEmpty()) return s.getBody();
      int idx = res.indexOf(after_node) + 1;
      if (idx < res.size()) return res.get(idx);
      return s.getBody();
    }
   else if (after_node == s.getFinally()) {
      Set<Object> oset = popMarker(s);
      for (Object o : oset) {
	 if (o instanceof IfaceAstStatus) {
	    IfaceAstStatus sts = (IfaceAstStatus) o;
	    IfaceAstReference nar = fait_control.getAstReference(s,sts);
	    FlowLocation nloc = new FlowLocation(flow_queue,work_queue.getCall(),nar);
	    work_queue.mergeState(cur_state,nloc);
	  }
       }
      if (oset.contains(TryState.FINALLY)) return null;
      return NO_NEXT;
    }
   else {
      Set<Object> oset = popMarker(s);
      if (oset.contains(TryState.BODY) || oset.contains(TryState.CATCH)) {
	 List<?> res = s.resources();
	 if (!res.isEmpty()) {
	    for (Object o : res) {
	       VariableDeclarationExpression vde = (VariableDeclarationExpression) o;
	       for (Object o1 : vde.fragments()) {
		  VariableDeclarationFragment vdf = (VariableDeclarationFragment) o1;
		  JcompSymbol js = JcompAst.getDefinition(vdf);
		  if (js == null) continue;
		  int slot = getSlot(js);
		  if (slot < 0) continue;
		  IfaceValue v0 = cur_state.getLocal(slot);
		  if (v0 == null || v0.isBad() || v0.mustBeNull()) continue;
		  //TODO: invoke close on v0
		}
	     }
	  }
	 Block b = s.getFinally();
	 if (b != null) {
	    pushValue(fait_control.findMarkerValue(ast_where,TryState.FINALLY));
	    return b;
	  }
       }
    }

   return null;
}


private IfaceValue visitBack(TryStatement s,IfaceValue ref)
{
   if (after_node == null) {
      return adjustRef(ref,0,1);
    }
   else if (after_node == s.getFinally()) {
      return adjustRef(ref,1,0);
    }
   else {
      IfaceValue v = cur_state.getStack(0);
      if (v instanceof IfaceStackMarker) {
	 IfaceStackMarker mkr = (IfaceStackMarker) v;
	 Set<Object> vals = mkr.getMarkerValues();
	 if (vals.contains(TryState.BODY) ||
	       vals.contains(TryState.CATCH)) {
	    Block b = s.getFinally();
	    if (b != null) return adjustRef(ref,0,1);
	  }
       }
    }
   return ref;
}


private Object visitThrow(TryStatement s,IfaceAstStatus r)
{
   Set<Object> stsset = popMarker(s);
   if (r.getReason() == Reason.EXCEPTION && stsset.contains(TryState.BODY)) {
      IfaceValue exc = r.getValue();
      // need to handle multiple catch clauses if exception is general
      for (Object o : s.catchClauses()) {
	 CatchClause cc = (CatchClause) o;
	 SingleVariableDeclaration svd = cc.getException();
	 JcompType ctype = JcompAst.getJavaType(svd.getType());
	 IfaceType ctyp = convertType(ctype);
	 IfaceValue nexc = exc.restrictByType(ctyp);
	 if (nexc != null && nexc.isGoodEntitySet()) {
	    pushValue(fait_control.findMarkerValue(ast_where,TryState.CATCH));
	    pushValue(nexc);
	    return cc;
	  }
       }
    }

   if (!stsset.contains(TryState.FINALLY)) {
      Block b = s.getFinally();
      if (b != null) {
	 pushValue(fait_control.findMarkerValue(ast_where,r));
	 return b;
       }
    }

   return r;
}



private Object visit(CatchClause cc)
{
   if (after_node == null) {
      IfaceValue exc = popActual();
      JcompSymbol js = JcompAst.getDefinition(cc.getException().getName());
      int slot = getSlot(js);
      cur_state.setLocal(slot,exc);
      return cc.getBody();
    }

   return null;
}



private IfaceValue visitBack(CatchClause cc,IfaceValue ref)
{
   if (after_node != null) return ref;

   if (ref.getRefStack() == 0) {
      JcompSymbol js = JcompAst.getDefinition(cc.getException().getName());
      int slot = getSlot(js);
      return fait_control.findRefValue(ref.getDataType(),slot);
    }
   return adjustRef(ref,1,0);
}


private Object visit(WhileStatement s)
{
   if (after_node != s.getExpression()) return s.getExpression();
   else {
      IfaceValue iv = popActual();
      if (iv == null) return NO_NEXT_REPORT;
      TestBranch tb = getTestResult(iv);
      if (FaitLog.isTracing()) FaitLog.logD1("While branch " + tb + " " + iv);
      Pair<IfaceState,IfaceState> imps = handleImplications(s.getExpression());
      if (tb == TestBranch.NEVER) {
	 cur_state = imps.getElement1();
	 return null;
       }
      else if (tb == TestBranch.ALWAYS) {
	 cur_state = imps.getElement0();
	 return s.getBody();
       }
      workOn(imps.getElement0(),s.getBody());
      cur_state = imps.getElement1();
    }
   return null;
}


private IfaceValue visitBack(WhileStatement s,IfaceValue ref)
{
   if (after_node == s.getExpression()) {
      return adjustRef(ref,1,0);
    }
   return ref;
}



private Object visitThrow(WhileStatement s,IfaceAstStatus sts)
{
   String lbl = sts.getMessage();
   switch (sts.getReason()) {
      case BREAK :
	 if (checkLabel(s,lbl)) break;
	 else return sts;
      case CONTINUE :
	 if (checkLabel(s,lbl)) return s.getExpression();
	 else return sts;
      default :
	 return sts;
    }

   return null;
}





/********************************************************************************/
/*										*/
/*	Enhanced for statement code						*/
/*										*/
/********************************************************************************/

private Object visit(EnhancedForStatement s)
{
   if (after_node == null) {
      return s.getExpression();
    }
   else if (after_node == s.getExpression()) {
      IfaceValue iv = popActual();
      if (iv == null) return NO_NEXT_REPORT;
      IfaceValue cnts = null;
      boolean callneeded = false;
      if (iv.getDataType().isArrayType()) {
	 cnts = iv.getArrayContents();
       }
      else {
	 List<IfaceValue> rval = null;
	 for (IfaceEntity ie : iv.getEntities()) {
	    if (ie.getDataType().isArrayType()) {
	       IfaceValue ncnts = ie.getArrayValue(null,fait_control);
	       if (ncnts != null && !ncnts.isEmptyEntitySet()) {
		  if (rval == null) rval = new ArrayList<>();
		  rval.add(ncnts);
		}
	     }
	    else if (ie.getPrototype() != null) {
	       rval = ie.getPrototype().getContents(rval);
	     }
	    else {
	       callneeded = true;
	     }
	  }
       }
      if (callneeded) {
	 cnts = null;
	 IfaceMethod im = fait_control.findMethod(iv.getDataType().getName(),"iterator",null);
	 IfaceValue itrv = null;
	 if (im != null && im.getStart() != null) {
	    pushValue(iv);
	    CallReturn cr = processInternalCall(im);
	    switch (cr) {
	       case NOT_DONE :
		  return NO_NEXT_REPORT;
	       case CONTINUE :
		  break;
	       case NO_RETURN :
		  return NO_NEXT;
	     }
	    itrv = popActual();
	  }
	 if (itrv != null) {
	    IfaceMethod im1 = fait_control.findMethod(itrv.getDataType().getName(),"hasNext",null);
	    if (im1 != null) {
	       pushValue(itrv);
	       CallReturn cr = processInternalCall(im1);
	       switch (cr) {
		  case NOT_DONE :
		     return NO_NEXT_REPORT;
		  case CONTINUE :
		     break;
		  case NO_RETURN :
		     return NO_NEXT;
		}
	       popActual();
	       // if this value is boolean false, then just return;
	     }
	    IfaceMethod im2 = fait_control.findMethod(itrv.getDataType().getName(),"next",null);
	    if (im2 != null) {
	       pushValue(itrv);
	       CallReturn cr = processInternalCall(im2);
	       switch (cr) {
		  case NOT_DONE :
		     return NO_NEXT_REPORT;
		  case CONTINUE :
		     break;
		  case NO_RETURN :
		     return NO_NEXT;
		}
	       cnts = popActual();
	     }
	  }
       }

      if (cnts != null && cnts.isEmptyEntitySet()) {
	 FaitLog.logW("ATTEMPT TO ASSIGN EMPTY ENTITY SET " + iv + " => " + cnts);
	 cnts = iv.getArrayContents();
	 cnts = null;
       }

      if (cnts != null && cnts.mustBeNull()) return null;

      JcompType jt = JcompAst.getJavaType(s.getParameter());
      IfaceType ijt = convertType(jt);
      if (cnts != null) {
	 ijt = ijt.getRunTimeType();
	 IfaceValue cnts1 = flow_queue.castValue(ijt,cnts,getHere());
	 if (cnts1 == null || cnts1.isEmptyEntitySet()) {
	    if (callneeded) return null;
	    FaitLog.logE("CAST TO EMPTY ENTITY SET " + iv + " => " + cnts1);
	    cnts = iv.getArrayContents();
	    cnts1 = flow_queue.castValue(ijt,cnts,getHere());
	    cnts = null;
	  }
	 else cnts = cnts1;
       }
      if (cnts == null) {
	 cnts = fait_control.findMutableValue(ijt);
       }
      int slot = getSlot(JcompAst.getDefinition(s.getParameter().getName()));
      cur_state.setLocal(slot,cnts);
      if (FaitLog.isTracing()) {
	 FaitLog.logD1("Loop value [" + slot + "] = " + cnts);
       }
      workOn(s.getBody());
    }
   else if (after_node == s.getBody()) {
      workOn(s.getExpression());
    }

   return null;
}



private IfaceValue visitBack(EnhancedForStatement s,IfaceValue ref)
{
   if (after_node != null && after_node == s.getExpression()) {
      int slot = getSlot(JcompAst.getDefinition(s.getParameter().getName()));
      if (ref.getRefSlot() == slot) return null;
      return adjustRef(ref,1,0);
    }

   return ref;
}


private Object visitThrow(EnhancedForStatement s,IfaceAstStatus sts)
{
   String lbl = sts.getMessage();
   switch (sts.getReason()) {
      case BREAK :
	 if (checkLabel(s,lbl)) break;
	 else return sts;
      case CONTINUE :
	 if (checkLabel(s,lbl)) return s.getExpression();
	 else return sts;
      default :
	 return sts;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Declaration handling							*/
/*										*/
/********************************************************************************/

private Object visit(ArrayInitializer n)
{
   List<?> exprs = n.expressions();
   int idx = 0;
   if (after_node != null) idx = exprs.indexOf(after_node) + 1;
   if (idx < exprs.size()) return exprs.get(idx);

   int dim = exprs.size();
   IfaceType rtyp = convertType(JcompAst.getExprType(n)).getBaseType();
   IfaceType ityp = fait_control.findDataType("int");
   IfaceValue dv = fait_control.findConstantValue(ityp,dim);
   IfaceValue av = flow_queue.handleNewArraySet(getHere(),rtyp,1,dv);
   int stk = 0;
   for (int i = dim - 1; i >= 0; --i) {
      IfaceValue iv = popActual();
      if (iv == null) return NO_NEXT_REPORT;
      IfaceValue idxv = fait_control.findConstantValue(ityp,i);
      IfaceValue niv = flow_queue.castValue(rtyp,iv,getHere());
      if (niv != null) iv = niv;
      flow_queue.handleArraySet(getHere(),av,iv,idxv,stk);
      ++stk;
    }
   pushValue(av);

   return null;
}


private IfaceValue visitBack(ArrayInitializer n,IfaceValue ref)
{
   List<?> exprs = n.expressions();
   int idx = 0;
   if (after_node != null) idx = exprs.indexOf(after_node) + 1;
   if (idx < exprs.size()) return ref;

   IfaceValue nextref = adjustRef(ref,exprs.size(),1);

   return nextref;
}



private Object visit(FieldDeclaration n)
{
   int idx = 0;
   List<?> frags = n.fragments();
   if (after_node != null) idx = frags.indexOf(after_node) + 1;
   if (idx < frags.size()) return frags.get(idx);
   return null;
}


private Object visit(SingleVariableDeclaration n)
{
   if (after_node == null && n.getInitializer() != null) return n.getInitializer();

   ASTNode par = n.getParent();
   if (par instanceof MethodDeclaration) {
      // handle formal parameters
    }
   else if (par instanceof CatchClause) {
      // handle exception
    }
   else {
      JcompSymbol js = JcompAst.getDefinition(n.getName());
      handleInitialization(js,n.getInitializer());
    }

   return null;
}



private IfaceValue visitBack(SingleVariableDeclaration n,IfaceValue ref)
{
   if (after_node == null && n.getInitializer() != null) return ref;

   if (n.getInitializer() != null) return adjustRef(ref,1,0);

   return ref;
}


private Object visit(VariableDeclarationExpression v)
{
   List<?> frags = v.fragments();
   int idx = 0;
   if (after_node != null) idx = frags.indexOf(after_node) + 1;
   if (idx < frags.size()) return frags.get(idx);
   return null;
}


private Object visit(VariableDeclarationStatement v)
{
   List<?> frags = v.fragments();
   int idx = 0;
   if (after_node != null) idx = frags.indexOf(after_node) + 1;
   if (idx < frags.size()) return frags.get(idx);
   return null;
}


private Object visit(VariableDeclarationFragment v)
{
    if (after_node == null && v.getInitializer() != null) return v.getInitializer();
    JcompSymbol js = JcompAst.getDefinition(v.getName());
    handleInitialization(js,v.getInitializer());
    return null;
}



private IfaceValue visitBack(VariableDeclarationFragment v,IfaceValue ref)
{
   if (after_node != null && after_node == v.getInitializer()) {
      return adjustRef(ref,1,0);
    }
   return ref;
}




/********************************************************************************/
/*										*/
/*	Lambda and reference management 					*/
/*										*/
/********************************************************************************/

private Object visit(LambdaExpression v)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference() &&
	 after_node == null) {
      // handle calling a lambda
    }
   else if (v == work_queue.getCall().getMethod().getStart().getAstReference() &&
	 after_node != null) {
      // handle return from a lambda
    }
   else {
      Map<Object,IfaceValue> bindings = new HashMap<>();
      List<JcompSymbol> params = new ArrayList<>();
      for (Object o : v.parameters()) {
	 if (o instanceof SingleVariableDeclaration) {
	    SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
	    JcompSymbol js = JcompAst.getDefinition(svd.getName());
	    params.add(js);
	  }
	 else {
	    VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
	    JcompSymbol js = JcompAst.getDefinition(vdf);
	    params.add(js);
	  }
       }
      LambdaVisitor lv = new LambdaVisitor();
      v.accept(lv);
      if (lv.getUseThis()) {
	 IfaceValue iv = getLocal("this");
	 bindings.put("this",iv);
       }
      for (JcompSymbol js : lv.getReferences()) {
	 IfaceType rtyp = convertType(js.getType());
	 if (js.isFieldSymbol() && !js.isStatic()) {
	    IfaceField fld = getField(js);
	    IfaceValue ref = fait_control.findRefValue(rtyp,getLocal("this"),fld);
	    bindings.put(js,ref);
	  }
	 else {
	    int slot = getSlot(js);
	    if (slot >= 0) {
	       IfaceValue ref = fait_control.findRefValue(rtyp,slot);
	       bindings.put(js,ref);
	     }
	  }
       }
      IfaceType typ = convertType(JcompAst.getExprType(v));
      IfaceValue rv = null;
      if (typ == null) {
	 rv = fait_control.findAnyObjectValue();
       }
      else {
	 IfaceEntity ie = fait_control.findFunctionRefEntity(getHere(),typ,bindings);
	 IfaceEntitySet set = fait_control.createSingletonSet(ie);
	 rv = fait_control.findObjectValue(typ,set);
       }
      pushValue(rv);
    }

   return null;
}



private IfaceValue visitBack(LambdaExpression v,IfaceValue ref)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference() &&
	 after_node == null) {
      return null;
    }
   else if (v == work_queue.getCall().getMethod().getStart().getAstReference() &&
	 after_node != null) {
      return null;
    }
   return adjustRef(ref,0,1);
}



private static class LambdaVisitor extends ASTVisitor {

   private Set<JcompSymbol> used_syms;
   private Set<JcompSymbol> defd_syms;
   private boolean use_this;

   LambdaVisitor() {
      used_syms = new HashSet<>();
      defd_syms = new HashSet<>();
      use_this = false;
    }

   Set<JcompSymbol> getReferences() {
      used_syms.removeAll(defd_syms);
      return used_syms;
    }

   boolean getUseThis() 			{ return use_this; }

   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null) {
	 if (js.isMethodSymbol()) {
	    if (!js.isStatic()) use_this = true;
	  }
	 else if (js.isFieldSymbol()) {
	    if (!js.isStatic()) use_this = true;
	    else used_syms.add(js);
	  }
	 else if (js.isTypeSymbol()) ;
	 else if (js.isEnumSymbol()) ;
	 else {
	    used_syms.add(js);
	  }
       }
      js = JcompAst.getDefinition(n);
      if (js != null) defd_syms.add(js);
    }

}	// end of inner class LambdaVisitor


private Object visit(CreationReference v)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return evaluateReference(v);
    }
   IfaceValue iv = generateReferenceValue(v);
   pushValue(iv);

   return null;
}


private IfaceValue visitBack(CreationReference v,IfaceValue ref)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return null;
    }
   return adjustRef(ref,0,1);
}

private Object visit(ExpressionMethodReference v)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return evaluateReference(v);
    }

   JcompSymbol js = JcompAst.getReference(v);
   if (after_node == null && !js.isStatic()) {
      ASTNode next = v.getExpression();
      JcompType jty = JcompAst.getJavaType(next);
      JcompType ety = JcompAst.getExprType(next);
      if (jty == null || jty != ety) {
	 return next;
       }
    }

   IfaceValue refval = null;
   if (!js.isStatic() && after_node != null && after_node == v.getExpression()) {
      refval = popActual();
    }
   IfaceValue rv = generateReferenceValue(v,refval);
   pushValue(rv);

   return null;
}


private IfaceValue visitBack(ExpressionMethodReference v,IfaceValue ref)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return null;
    }
   JcompSymbol js = JcompAst.getReference(v);
   if (after_node == null && !js.isStatic()) {
      ASTNode next = v.getExpression();
      JcompType jty = JcompAst.getJavaType(next);
      JcompType ety = JcompAst.getExprType(next);
      if (jty == null || jty != ety) {
	 return ref;
       }
    }
   int ct = 0;
   if (!js.isStatic() && after_node != null && after_node == v.getExpression()) {
      ct = 1;
    }
   return adjustRef(ref,ct,1);
}


private Object visit(SuperMethodReference v)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return evaluateReference(v);
    }

   IfaceValue iv = generateReferenceValue(v);
   pushValue(iv);

   return null;
}


private IfaceValue visitBack(SuperMethodReference v,IfaceValue ref)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return null;
    }

   return adjustRef(ref,0,1);
}

private Object visit(TypeMethodReference v)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return evaluateReference(v);
    }

   IfaceValue iv = generateReferenceValue(v);
   pushValue(iv);

   return null;
}



private IfaceValue visitBack(TypeMethodReference v,IfaceValue ref)
{
   if (v == work_queue.getCall().getMethod().getStart().getAstReference()) {
      return null;
    }

   return adjustRef(ref,0,1);
}



private IfaceValue generateReferenceValue(ASTNode n)
{
   return generateReferenceValue(n,null);
}


private IfaceValue generateReferenceValue(ASTNode n,IfaceValue ref)
{
   IfaceType ntyp = convertType(JcompAst.getExprType(n));
   Map<Object,IfaceValue> bind = null;
   if (ref != null) {
      bind = new HashMap<>();
      bind.put("this",ref);
    }
   IfaceEntity ie = fait_control.findFunctionRefEntity(getHere(),ntyp,bind);
   IfaceEntitySet set = fait_control.createSingletonSet(ie);
   IfaceValue rv = fait_control.findObjectValue(ntyp,set);
   return rv;
}



private Object evaluateReference(ASTNode n)
{
   // JcompSymbol js = JcompAst.getReference(n);
   // JcompType typ = js.getType();

   // need to setup up arguments and bindings here
   // need to instantiate a new call
   // need to get return value
   return null;
}




/********************************************************************************/
/*										*/
/*	Back Propagation methods						*/
/*										*/
/********************************************************************************/

private void processBackNode(IfaceValue ref,IfaceType settype)
{
   cur_state = work_queue.getState(ast_where);
   ASTNode node = ast_where.getAstNode();
   after_node = ast_where.getAfterChild();
   here_location = null;
   IfaceValue nextref = null;

   if (FaitLog.isTracing()) {
      String cls = node.getClass().getName();
      int idx = cls.lastIndexOf(".");
      cls = cls.substring(idx+1);
      String aft = (after_node == null ? "" : "*");
      String cnts = node.toString().replace("\n"," ");
      if (cnts.length() > 64) cnts = cnts.substring(0,62) + "...";
      FaitLog.logD("BACK " + aft + cls + " " + node.getStartPosition() + " " + cnts + " " +
	    settype + " " + ref.getRefSlot() + " " + ref.getRefStack() + " " + ref.getRefField());
    }

   switch (node.getNodeType()) {
      case ASTNode.ANNOTATION_TYPE_DECLARATION :
      case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
      case ASTNode.ANONYMOUS_CLASS_DECLARATION :
      case ASTNode.ARRAY_TYPE :
      case ASTNode.BLOCK_COMMENT :
      case ASTNode.COMPILATION_UNIT :
      case ASTNode.EMPTY_STATEMENT :
      case ASTNode.ENUM_CONSTANT_DECLARATION :
      case ASTNode.ENUM_DECLARATION :
      case ASTNode.IMPORT_DECLARATION :
      case ASTNode.JAVADOC :
      case ASTNode.LINE_COMMENT :
      case ASTNode.MARKER_ANNOTATION :
      case ASTNode.MEMBER_REF :
      case ASTNode.MEMBER_VALUE_PAIR :
      case ASTNode.METHOD_REF :
      case ASTNode.METHOD_REF_PARAMETER :
      case ASTNode.MODIFIER :
      case ASTNode.NAME_QUALIFIED_TYPE :
      case ASTNode.NORMAL_ANNOTATION :
      case ASTNode.PACKAGE_DECLARATION :
      case ASTNode.PARAMETERIZED_TYPE :
      case ASTNode.PRIMITIVE_TYPE :
      case ASTNode.QUALIFIED_TYPE :
      case ASTNode.SIMPLE_TYPE :
      case ASTNode.SINGLE_MEMBER_ANNOTATION :
      case ASTNode.TAG_ELEMENT :
      case ASTNode.TEXT_ELEMENT :
      case ASTNode.TYPE_DECLARATION :
      case ASTNode.TYPE_DECLARATION_STATEMENT :
      case ASTNode.TYPE_PARAMETER :
      case ASTNode.UNION_TYPE :
      case ASTNode.WILDCARD_TYPE :
	 nextref = ref;
	 break;
      case ASTNode.ARRAY_ACCESS :
	 nextref = visitBack((ArrayAccess) node,ref);
	 break;
      case ASTNode.ARRAY_CREATION :
	 nextref = visitBack((ArrayCreation) node,ref);
	 break;
      case ASTNode.ARRAY_INITIALIZER :
	 nextref = visitBack((ArrayInitializer) node,ref);
	 break;
      case ASTNode.ASSERT_STATEMENT :
	 nextref = visitBack((AssertStatement) node,ref);
	 break;
      case ASTNode.ASSIGNMENT :
	 nextref = visitBack((Assignment) node,ref,settype);
	 break;
      case ASTNode.BLOCK :
	 nextref = ref;
	 break;
      case ASTNode.BOOLEAN_LITERAL :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.BREAK_STATEMENT :
	 nextref = ref;
	 break;
      case ASTNode.CAST_EXPRESSION :
	 nextref = ref;
	 break;
      case ASTNode.CATCH_CLAUSE :
	 nextref = visitBack((CatchClause) node,ref);
	 break;
      case ASTNode.CHARACTER_LITERAL :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.CLASS_INSTANCE_CREATION :
	 nextref = visitBack((ClassInstanceCreation) node,ref);
	 break;
      case ASTNode.CONDITIONAL_EXPRESSION :
	 nextref = visitBack((ConditionalExpression) node,ref);
	 break;
      case ASTNode.CONSTRUCTOR_INVOCATION :
	 nextref = visitBack((ConstructorInvocation) node,ref);
	 break;
      case ASTNode.CONTINUE_STATEMENT :
	 nextref = ref;
	 break;
      case ASTNode.CREATION_REFERENCE :
	 nextref = visitBack((CreationReference) node,ref);
	 break;
      case ASTNode.DO_STATEMENT :
	 nextref = visitBack((DoStatement) node,ref);
	 break;
      case ASTNode.ENHANCED_FOR_STATEMENT :
	 nextref = visitBack((EnhancedForStatement) node,ref);
	 break;
      case ASTNode.EXPRESSION_METHOD_REFERENCE :
	 nextref = visitBack((ExpressionMethodReference) node,ref);
	 break;
      case ASTNode.EXPRESSION_STATEMENT :
	 nextref = visitBack((ExpressionStatement) node,ref);
	 break;
      case ASTNode.FIELD_ACCESS :
	 nextref = visitBack((FieldAccess) node,ref);
	 break;
      case ASTNode.FIELD_DECLARATION :
	 nextref = ref;
	 break;
      case ASTNode.FOR_STATEMENT :
	 nextref = visitBack((ForStatement) node,ref);
	 break;
      case ASTNode.IF_STATEMENT :
	 nextref = visitBack((IfStatement) node,ref);
	 break;
      case ASTNode.INFIX_EXPRESSION :
	 nextref = visitBack((InfixExpression) node,ref,settype);
	 break;
      case ASTNode.INSTANCEOF_EXPRESSION :
	 nextref = visitBack((InstanceofExpression) node,ref);
	 break;
      case ASTNode.LABELED_STATEMENT :
	 nextref = ref;
	 break;
      case ASTNode.LAMBDA_EXPRESSION :
	 nextref = visitBack((LambdaExpression) node,ref);
	 break;
      case ASTNode.METHOD_DECLARATION :
	 nextref = visitBack((MethodDeclaration) node,ref,settype);
	 break;
      case ASTNode.METHOD_INVOCATION :
	 nextref = visitBack((MethodInvocation) node,ref,settype);
	 break;
      case ASTNode.NULL_LITERAL :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.NUMBER_LITERAL :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.PARENTHESIZED_EXPRESSION :
	 nextref = ref;
	 break;
      case ASTNode.POSTFIX_EXPRESSION :
	 nextref = visitBack((PostfixExpression) node,ref,settype);
	 break;
      case ASTNode.PREFIX_EXPRESSION :
	 nextref = visitBack((PrefixExpression) node,ref,settype);
	 break;
      case ASTNode.QUALIFIED_NAME :
	 nextref = visitBack((QualifiedName) node,ref);
	 break;
      case ASTNode.RETURN_STATEMENT :
	 nextref = visitBack((ReturnStatement) node,ref);
	 break;
      case ASTNode.SIMPLE_NAME :
	 nextref = visitBack((SimpleName) node,ref);
	 break;
      case ASTNode.SINGLE_VARIABLE_DECLARATION :
	 nextref = visitBack((SingleVariableDeclaration) node,ref);
	 break;
      case ASTNode.STRING_LITERAL :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
	 nextref = null;
	 break;
      case ASTNode.SUPER_METHOD_INVOCATION :
	 nextref = null;
	 break;
      case ASTNode.SUPER_FIELD_ACCESS :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.SUPER_METHOD_REFERENCE :
	 nextref = visitBack((SuperMethodReference) node,ref);
	 break;
      case ASTNode.SWITCH_STATEMENT :
	 nextref = visitBack((SwitchStatement) node,ref);
	 break;
      case ASTNode.SWITCH_CASE :
	 nextref = ref;
	 break;
      case ASTNode.SYNCHRONIZED_STATEMENT :
	 nextref = visitBack((SynchronizedStatement) node,ref);
	 break;
      case ASTNode.THIS_EXPRESSION :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.THROW_STATEMENT :
	 nextref = visitBack((ThrowStatement) node,ref);
	 break;
      case ASTNode.TRY_STATEMENT :
	 nextref = visitBack((TryStatement) node,ref);
	 break;
      case ASTNode.TYPE_LITERAL :
	 nextref = adjustRef(ref,0,1);
	 break;
      case ASTNode.TYPE_METHOD_REFERENCE :
	 nextref = visitBack((TypeMethodReference) node,ref);
	 break;
      case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
	 nextref = ref;
	 break;
      case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
	 nextref = visitBack((VariableDeclarationFragment) node,ref);
	 break;
      case ASTNode.VARIABLE_DECLARATION_STATEMENT :
	 nextref = ref;
	 break;
      case ASTNode.WHILE_STATEMENT :
	 nextref = visitBack((WhileStatement) node,ref);
	 break;
      default :
	 FaitLog.logE("Unknown node type " + node);
	 break;
    }

   if (nextref != null) {
      queueBackRefs(getHere(),cur_state,nextref,settype);
    }
}




/********************************************************************************/
/*										*/
/*	Reference management							*/
/*										*/
/********************************************************************************/

protected IfaceValue getActualValue(IfaceValue ref,boolean nonnull)
{
   return getActualValue(cur_state,getHere(),ref,nonnull);
}






private IfaceValue assignValue(IfaceValue ref,IfaceValue v,int stackref)
{
   IfaceValue lhsv = ref.getRefBase();
   if (lhsv != null) {
      IfaceType t2 = lhsv.checkOperation(FaitOperator.DEREFERENCE,v);
      if (t2 != null && t2 != lhsv.getDataType()) {
	 queueBackRefs(getHere(),cur_state,ref,t2);
       }
    }

   return assignValue(cur_state,getHere(),ref,v,stackref);
}




/********************************************************************************/
/*										*/
/*	Support methods 							*/
/*										*/
/********************************************************************************/

private void pushValue(IfaceValue v)
{
   if (v == null)
      FaitLog.logE("Attempt to push null onto stack");

   cur_state.pushStack(v);
}

private IfaceValue popValue()
{
   return cur_state.popStack();
}


private IfaceValue popActual()
{
   return getActualValue(popValue(),false);
}


private IfaceValue popActualNonNull(int delta)
{
   IfaceValue v0 = getActualValue(cur_state.getStack(0),false);
   checkBackPropagation(getHere(),cur_state,0+delta,
	 v0,FaitOperator.DEREFERENCE,null);

   return getActualValue(popValue(),true);
}


private Set<Object> popMarker(ASTNode n)
{
   while (cur_state.getStack(0) != null) {
      IfaceValue v = cur_state.popStack();
      if (v instanceof IfaceStackMarker) {
	 IfaceStackMarker mkr = (IfaceStackMarker) v;
	 IfaceProgramPoint pt = mkr.getProgramPoint();
	 IfaceAstReference ar = pt.getAstReference();
	 if (ar.getAstNode() != n) continue;
	 return mkr.getMarkerValues();
       }
    }
   return new HashSet<Object>();
}



private IfaceType convertType(JcompType jt,IfaceAnnotation ... annots)
{
   if (jt == null) return null;

   return fait_control.findDataType(jt.getName(),annots);
}

private FlowLocation getHere()
{
   if (here_location == null) {
      here_location =  new FlowLocation(flow_queue,work_queue.getCall(),ast_where);
    }
   return here_location;
}

private void workOn(ASTNode n)
{
   FlowLocation nloc = new FlowLocation(flow_queue,work_queue.getCall(),fait_control.getAstReference(n));

   work_queue.mergeState(cur_state,nloc);
}


private void workOn(IfaceState st,ASTNode n)
{
   FlowLocation nloc = new FlowLocation(flow_queue,work_queue.getCall(),fait_control.getAstReference(n));

   work_queue.mergeState(st,nloc);
}


private boolean checkLabel(Statement s,String lbl)
{
   if (lbl == null || lbl.length() == 0) return true;
   if (s.getParent() instanceof LabeledStatement) {
      LabeledStatement lbs = (LabeledStatement) s.getParent();
      if (lbs.getLabel().getIdentifier().equals(lbl)) return true;
    }
   return false;
}


private int getSlot(Object o)
{
   return work_queue.getCall().getMethod().getLocalOffset(o);
}

private IfaceValue getLocal(int i)
{
   if (i < 0) return null;
   return cur_state.getLocal(i);
}

private IfaceValue getLocal(Object o)
{
   return getLocal(getSlot(o));
}

private IfaceField getField(JcompSymbol sym)
{
   IfaceType fcls = convertType(sym.getClassType());
   IfaceField fld = fait_control.findField(fcls,sym.getFullName());
   return fld;
}



private TestBranch getTestResult(IfaceValue v1)
{
   TestBranch tb = v1.branchTest(null,FaitOperator.NEQ_ZERO);
   return tb;
}



private CallReturn processCall(ASTNode v,int act)
{
   JcompSymbol js = JcompAst.getReference(v);
   if (js == null) return CallReturn.NOT_DONE;

   JcompType jt = js.getType();

   int checkarg = -1;
   if (!js.isStatic()) checkarg = act-1;
   int varct = -1;

   for (int i = 0; i < act; ++i) {
      IfaceValue v0 = cur_state.getStack(i);
      IfaceValue v1 = getActualValue(v0,false);
      if (i == checkarg) {
	 if (v1 != null && v1.canBeNull()) {
	    checkBackPropagation(getHere(),cur_state,i,v1,FaitOperator.DEREFERENCE,null);
	    v1 = getActualValue(v0,true);
	  }
       }
      if (v1 == null) return CallReturn.NOT_DONE;
      if (v1 != v0) cur_state.setStack(i,v1);
    }

   // List<IfaceValue> alst = new ArrayList<>();
   // for (int i = 0; i < act; ++i) {
      // IfaceValue v0 = null;
      // if (i == checkarg) v0 = popActualNonNull(i);
      // else v0 = popActual();
      // if (v0 == null) return false;
      // alst.add(v0);
    // }
   // for (int i = act-1; i >= 0; --i) {
      // IfaceValue v0 = alst.get(i);
      // pushValue(v0);
    // }

   if (jt.isVarArgs()) {
      int aact = act;
      if (!js.isStatic()) --aact;
      int ct = jt.getComponents().size();
      JcompType vtyp = jt.getComponents().get(ct-1);
      IfaceType arrtyp = convertType(vtyp);
      boolean dovar = true;
      if (aact == ct) {
	 IfaceValue varg = popActual();
	 pushValue(varg);
	 if (varg.getDataType().isCompatibleWith(arrtyp)) {
	    dovar = false;
	  }
       }
      if (dovar) {
	 IfaceType btyp = arrtyp.getBaseType();
	 int sz = aact-ct+1;
	 varct = sz;
	 IfaceType ityp = fait_control.findDataType("int");
	 IfaceValue szv = fait_control.findConstantValue(ityp,sz);
	 IfaceValue rslt = flow_queue.handleNewArraySet(getHere(),btyp,1,szv);
	 int stk = 0;
	 for (int i = sz-1; i >= 0; --i) {
	    IfaceValue iv = popActual();
	    if (iv.getDataType().isPrimitiveType() && !btyp.isPrimitiveType()) {
	       IfaceType rt = iv.getDataType().getBaseType();
	       iv = fait_control.findAnyValue(rt);
	     }
	    IfaceValue idxv = fait_control.findConstantValue(ityp,i);
	    flow_queue.handleArraySet(getHere(),rslt,iv,idxv,stk);
	    ++stk;
	  }
	 rslt = rslt.forceNonNull();
	 pushValue(rslt);
       }
    }

   CallReturn rsts = flow_queue.handleCall(getHere(),cur_state,work_queue,varct);
   if (rsts == CallReturn.NOT_DONE) {
      if (FaitLog.isTracing()) FaitLog.logD1("Unknown RETURN value for " + js);
    }

   return rsts;
}




private CallReturn processInternalCall(IfaceMethod im)
{
   if (im == null) return CallReturn.NOT_DONE;
   int act = im.getNumArgs();

   int checkarg = -1;
   if (!im.isStatic()) {
      ++act;
      checkarg = act-1;
    }

   for (int i = 0; i < act; ++i) {
      IfaceValue v0 = cur_state.getStack(i);
      IfaceValue v1 = getActualValue(v0,false);
      if (i == checkarg) {
	 if (v1 != null && v1.canBeNull()) {
	    checkBackPropagation(getHere(),cur_state,i,v1,FaitOperator.DEREFERENCE,null);
	    v1 = getActualValue(v0,true);
	  }
       }
      if (v1 == null) return CallReturn.NOT_DONE;
      if (v1 != v0) cur_state.setStack(i,v1);
    }

   CallReturn rsts = flow_queue.handleCall(getHere(),cur_state,work_queue,-1,im);
   if (rsts == CallReturn.NOT_DONE) {
      if (FaitLog.isTracing()) FaitLog.logD1("Unknown RETURN value for internal call to " + im);
    }

   return rsts;
}



/********************************************************************************/
/*										*/
/*	Operation methods							*/
/*										*/
/********************************************************************************/

private IfaceValue performOperation(IfaceValue v1,IfaceValue v2,IfaceType rtyp,
      FaitOperator op)
{
   v1 = getActualValue(v1,false);
   v2 = getActualValue(v2,false);

   IfaceType t1 = v1.getDataType();
   IfaceType t2 = v2.getDataType();
   if (t1.isNumericType() && t2.isNumericType()) {
      if (t1.isFloatingType() || t2.isFloatingType()) {
	 v1 = v1.toFloating();
	 v2 = v2.toFloating();
       }
    }
   else if (t1.isNumericType() || t2.isNumericType()) {
      if ((t1.isStringType() || t2.isStringType()) && op == FaitOperator.ADD) {
	  if (t1.isStringType()) {
	     v2 = fait_control.findAnyValue(t1);
	   }
	  else {
	     v1 = fait_control.findAnyValue(t2);
	   }
       }
      else if (!t1.isNumericType() && t1.getAssociatedType() != null) {
	 t1 = t1.getAssociatedType().getAnnotatedType(FaitAnnotation.NON_NULL);
	 v1 = fait_control.findAnyValue(t1);
       }
      else if (!t2.isNumericType() && t2.getAssociatedType() != null) {
	 t2 = t2.getAssociatedType().getAnnotatedType(FaitAnnotation.NON_NULL);
	 v2 = fait_control.findAnyValue(t2);
       }
    }

   IfaceValue v = v1.performOperation(rtyp,v2,op,getHere());

   return v;
}




/********************************************************************************/
/*										*/
/*	Handle initializations using given value or default			*/
/*										*/
/********************************************************************************/

private void handleInitialization(JcompSymbol js,ASTNode init)
{
   IfaceValue iv = null;
   if (init != null) iv = popActual();
   else {
      IfaceType it = convertType(js.getType());
      if (it.isNumericType() || it.isBooleanType()) {
	 iv = fait_control.findConstantValue(it,0);
       }
      else {
	 iv = fait_control.findNullValue(it);
       }
    }
   int slot = getSlot(js);
   if (slot >= 0 && iv != null) {
      IfaceValue ref = fait_control.findRefValue(convertType(js.getType()),slot);
      assignValue(ref,iv,-1);
    }
}


/********************************************************************************/
/*										*/
/*	Handle implications							*/
/*										*/
/********************************************************************************/

Pair<IfaceState,IfaceState> handleImplications(Expression expr)
{
   IfaceValue lhsv = null;
   IfaceValue rhsv = null;
   Pair<IfaceState,IfaceState> rslt = Pair.createPair(cur_state,cur_state);

   if (expr instanceof InfixExpression) {
      // TODO: handle AND-AND and OR-OR, handle boolean variables,
      InfixExpression inex = (InfixExpression) expr;
      if (inex.extendedOperands().size() > 0) return rslt;
      lhsv = getValueForOperand(inex.getLeftOperand());
      rhsv = getValueForOperand(inex.getRightOperand());
      if (lhsv == null || rhsv == null) return rslt;
      if (!lhsv.isReference() && !rhsv.isReference()) return rslt;
      IfaceValue lhsa = getActualValue(lhsv,false);
      IfaceValue rhsa = getActualValue(rhsv,false);
      if (lhsa == null || rhsa == null) return rslt;
      FaitOperator op = op_map.get(inex.getOperator());
      IfaceImplications imps = lhsa.getImpliedValues(rhsa,op);
      if (imps == null) return rslt;
      IfaceState sttrue = cur_state;
      IfaceState stfalse = cur_state;
      // TODO ::  need to handle fields here (which are global) in some way
      if (lhsv.isReference() && lhsv.getRefField() == null) {
	 IfaceValue nlhs = imps.getLhsTrueValue();
	 if (nlhs != null && nlhs != lhsa) {
	    sttrue = setRefValue(sttrue,lhsv,nlhs,-1);
	  }
	 nlhs = imps.getLhsFalseValue();
	 if (nlhs != null && nlhs != lhsa) {
	    stfalse = setRefValue(stfalse,lhsv,nlhs,-1);
	  }
       }
      else if (lhsv.isReference() && lhsv.getRefField() != null) {
	 if (lhsv.getRefBase() != null && lhsv.getRefBase() == sttrue.getLocal(0)) {
	    IfaceValue nlhs = imps.getLhsTrueValue();
	    if (nlhs != null && nlhs != lhsa) {
	       sttrue = sttrue.cloneState();
	       sttrue.setFieldValue(lhsv.getRefField(),nlhs);
	     }
	    nlhs = imps.getLhsFalseValue();
	    if (nlhs != null && nlhs != lhsa) {
	       stfalse.setFieldValue(lhsv.getRefField(),nlhs);
	     }
	  }
       }
      if (rhsv.isReference() && rhsv.getRefField() == null) {
	 IfaceValue nrhs = imps.getRhsTrueValue();
	 if (nrhs != null && nrhs != rhsa) {
	    sttrue = setRefValue(sttrue,rhsv,nrhs,-1);
	  }
	 nrhs = imps.getRhsFalseValue();
	 if (nrhs != null && nrhs != rhsa) {
	    stfalse = setRefValue(stfalse,rhsv,nrhs,-1);
	  }
       }

      rslt = Pair.createPair(sttrue,stfalse);
    }

   return rslt;
}



private IfaceValue getValueForOperand(Expression e)
{
   switch (e.getNodeType()) {
      case ASTNode.NULL_LITERAL :
	 return fait_control.findNullValue();
      case ASTNode.NUMBER_LITERAL :
	 NumberLiteral nlit = (NumberLiteral) e;
	 JcompType jtyp = JcompAst.getExprType(e);
	 if (jtyp.isFloatingType()) return null;
	 long lval = getNumericValue(nlit.getToken());
	 IfaceType ityp = convertType(jtyp);
	 return fait_control.findConstantValue(ityp,lval);
      case ASTNode.SIMPLE_NAME :
	 return getReference(e);
      case ASTNode.QUALIFIED_NAME:
	 // TODO: handle simple qualified names
	 break;
    }

   return null;
}





private IfaceValue getReference(ASTNode n)
{
   IfaceValue ref = null;
   switch (n.getNodeType()) {
      case ASTNode.SIMPLE_NAME :
	 visit((SimpleName) n);
	 ref = popValue();
	 break;
    }

   if (ref != null && !ref.isReference()) ref = null;

   return ref;
}



private IfaceState setRefValue(IfaceState st0,IfaceValue ref,IfaceValue v,int stackref)
{
   cur_state = cur_state.cloneState();
   assignValue(ref,v,stackref);
   IfaceState rst = cur_state;
   cur_state = st0;
   return rst;
}



private long getNumericValue(String sv)
{
   long lv = 0;
   if (sv.endsWith("L") || sv.endsWith("l")) {
      sv = sv.substring(0,sv.length()-1);
    }
   if (sv.startsWith("0x") && sv.length() > 2) {
      sv = sv.substring(2);
      lv = Long.parseLong(sv,16);
    }
   else if (sv.startsWith("0X") && sv.length() > 2) {
      sv = sv.substring(2);
      lv = Long.parseLong(sv,16);
    }
   else if (sv.startsWith("0b") && sv.length() > 2) {
      sv = sv.substring(2);
      lv = Long.parseLong(sv,2);
    }
   else if (sv.startsWith("0B") && sv.length() > 2) {
      sv = sv.substring(2);
      lv = Long.parseLong(sv,2);
    }
   else if (sv.startsWith("0") && sv.length() > 1) {
      sv = sv.substring(1);
      lv = Long.parseLong(sv,8);
    }
   else lv = Long.parseLong(sv);

   return lv;
}


private double getFloatValue(String sv)
{
   double dv = 0;
   if (sv.endsWith("F") || sv.endsWith("f")) {
      sv = sv.substring(0,sv.length()-1);
    }
   dv = Double.parseDouble(sv);
   return dv;
}





private void setExternalSymbols(AnonymousClassDeclaration acd) {
   for (Object o : acd.bodyDeclarations()) {
      if (o instanceof MethodDeclaration) {
	 MethodDeclaration md = (MethodDeclaration) o;
	 JcompSymbol js = JcompAst.getDefinition(md);
	 IfaceMethod im = fait_control.findMethod(js);
	 Collection<Object> syms = im.getExternalSymbols();
	 if (syms == null) continue;
	 for (Object so : syms) {
	    int slot = getSlot(so);
	    if (slot < 0) continue;
	    im.setExternalValue(so,cur_state.getLocal(slot));
	  }
       }
    }
}




}	// end of class FlowScannerAst




/* end of FlowScannerAst.java */

