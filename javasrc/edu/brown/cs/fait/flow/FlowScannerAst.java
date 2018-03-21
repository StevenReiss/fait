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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
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
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceImplications;
import edu.brown.cs.fait.iface.IfaceMarker;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceAstStatus.Reason;
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

private FlowQueueInstanceAst		work_queue;
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
   op_map.put(InfixExpression.Operator.CONDITIONAL_AND,FaitOperator.AND);
   op_map.put(InfixExpression.Operator.CONDITIONAL_OR,FaitOperator.OR);
}


private static Object NO_NEXT = new Object();
private static Object NO_NEXT_REPORT = new Object();

private enum TryState { BODY, CATCH, FINALLY }



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowScannerAst(IfaceControl fc,FlowQueue fq,FlowQueueInstanceAst wq)
{
   super(fc,fq);
   work_queue = wq;
   ast_where = null;
   after_node = null;
   cur_state = null;
}



/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

@Override void scanCode()
{
   while (!work_queue.isEmpty()) {
      IfaceProgramPoint ar = work_queue.getNext();
      try {
	 ast_where = ar.getAstReference();
	 processAstNode();
       }
      catch (Throwable t) {
	 FaitLog.logE("Problem ast processing " + work_queue.getCall().getLogName(),t);
       }
    }
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
      if (cur_state != null) work_queue.mergeState(cur_state,nar);
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
   if (after_node == null && md.isConstructor()) {
      JcompType jt = js.getClassType();
      if (jt.needsOuterClass()) {
	 IfaceValue iv = getLocal(1);
	 IfaceValue bv = getLocal(0);
	 IfaceType typ = convertType(jt,FaitAnnotation.NON_NULL);
	 IfaceField fld = fait_control.findField(typ,"this$0");
	 flow_queue.handleFieldSet(fld,getHere(),cur_state,true,iv,bv);
       }
    }
   if (after_node == null) return md.getBody();
   
   if (!js.isConstructorSymbol() && !js.getType().getBaseType().isVoidType()) {
      if (FaitLog.isTracing()) FaitLog.logW("Attempt to do impossible return");
      return NO_NEXT;
    }

   return new FlowAstStatus(Reason.RETURN,(IfaceValue) null);
}


private Object visitThrow(MethodDeclaration md,IfaceAstStatus sts)
{
   switch (sts.getReason()) {
      case RETURN :
	 IfaceValue v0 = sts.getValue();
         IfaceType vtyp = fait_control.findDataType("void");
	 if (v0 == null) v0 = fait_control.findAnyValue(vtyp);
	 flow_queue.handleReturn(work_queue.getCall(),v0);
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
   int val = (v.booleanValue() ? 1 : 0);
   IfaceType btyp = fait_control.findConstantType("boolean",v.booleanValue());
   IfaceValue v0 = fait_control.findRangeValue(btyp,val,val);
   pushValue(v0);
   return null;
}


private Object visit(CharacterLiteral v)
{
   int val = v.charValue();
   IfaceType ctyp = fait_control.findConstantType("char",v.charValue());
   IfaceValue v0 = fait_control.findRangeValue(ctyp,val,val);
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
	 v0 = fait_control.findRangeValue(jt,dv,dv);
         break;
      default :
	 String sv = v.getToken();
	 long lv = getNumericValue(sv);
         jt = fait_control.findConstantType(xjt.getName(),lv);
	 v0 = fait_control.findRangeValue(jt,lv,lv);
	 break;
    }

   pushValue(v0);

   return null;
}


private Object visit(StringLiteral v)
{
   pushValue(fait_control.findConstantStringValue(v.getLiteralValue()));
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
      if (vidx == null) return NO_NEXT_REPORT;
      IfaceValue varr = popActualNonNull();
      if (varr == null) return NO_NEXT_REPORT;
      IfaceType rtyp = convertType(JcompAst.getExprType(v));
      IfaceValue vrslt = fait_control.findRefValue(rtyp,varr,vidx);
      pushValue(vrslt);
    }

   return null;
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
   
   for (int i = 0; i < dims.size(); ++i) {
      sz = popActual();
    }
   
   if (jsize > 1) sz = null;
   if (rslt == null) {
      rslt = flow_queue.handleNewArraySet(getHere(),base,jsize,sz);
    }
   
   pushValue(rslt);

   return null;
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
      
      v2 = assignValue(v1,v2);
      pushValue(v2);
    }
   return null;
}


private Object visit(CastExpression v)
{
   if (after_node == null) return v.getExpression();
   else {
      IfaceValue v0 = popActual();
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceType ctyp = convertType(JcompAst.getJavaType(v.getType()));
      v0 = flow_queue.castValue(ctyp,v0,getHere());
      if (FaitLog.isTracing()) FaitLog.logD1("Cast Value = " + v0);
      if (v0.mustBeNull()) v0 = fait_control.findNullValue(ctyp);
      if (!v0.mustBeNull() && v0.isEmptyEntitySet()) 
         return NO_NEXT_REPORT;
      pushValue(v0);
    }

   return null;
}



private Object visit(ConditionalExpression v)
{
   if (after_node == null) return v.getExpression();
   else if (after_node == v.getExpression()) {
      IfaceValue v0 = popActual();
      if (v0 == null) return NO_NEXT_REPORT;
      TestBranch brslt = getTestResult(v0);
      // flow_queue.handleImplications(work_queue,ast_where,cur_state,brslt);
      if (brslt == TestBranch.NEVER) {
	 return v.getElseExpression();
       }
      else if (brslt == TestBranch.ALWAYS) {
	 return v.getThenExpression();
       }
      else {
	 workOn(v.getElseExpression());
	 return v.getThenExpression();
       }
    }

   return null;
}


private Object visit(FieldAccess v)
{
   if (after_node == null) return v.getExpression();
   else {
      JcompSymbol sym = JcompAst.getReference(v.getName());
      IfaceField fld = getField(sym);
      IfaceValue v0 = popActualNonNull();
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceType rcls = convertType(JcompAst.getExprType(v));
      IfaceValue ref = fait_control.findRefValue(rcls,v0,fld);
      pushValue(ref);
    }

   return null;
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
	 List<?> ext = v.extendedOperands();
	 int idx = ext.indexOf(after_node) + 1;
	 if (idx < ext.size()) return ext.get(idx);
       }
    }
   return null;
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
         ref.forceNonNull();
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
         FaitLog.logE("Unknwon name " + v);
         return NO_NEXT;
       }
    }

   return null;
}



private IfaceValue getThisValue(IfaceType typ)
{
   IfaceValue thisv = getLocal(0);
   if (thisv == null) return null;
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
      IfaceValue v0 = popActualNonNull();
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceType rtyp = convertType(JcompAst.getExprType(v));
      IfaceField fld = getField(sym);
      IfaceValue v1 = fait_control.findRefValue(rtyp,v0,fld);
      pushValue(v1);
    }
   else if (after_node == v.getQualifier() && sym == null) {
      IfaceValue v0 = popActualNonNull();
      if (v0 == null) return NO_NEXT_REPORT;
      IfaceValue v1 = v0.getArrayLength();
      pushValue(v1);
    }

   return null;
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
   IfaceValue v3 = fait_control.findRangeValue(rtyp,i0,i0);
   IfaceValue v4 = performOperation(v3,v2,rtyp,op);
   assignValue(v1,v4);
   pushValue(v2);
   return null;
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
      IfaceValue v3 = fait_control.findRangeValue(rtyp,i0,i0);
      IfaceValue v4 = performOperation(v3,v2,rtyp,op);
      assignValue(v1,v4);
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
   // TODO: handle expression.new ...
   if (after_node == null) {
      IfaceType irty = convertType(rty);
      flow_queue.initialize(irty);
      IfaceEntity ent = getLocalEntity(work_queue.getCall(),ast_where);
      IfaceValue v0 = fait_control.findObjectValue(irty,
	    fait_control.createSingletonSet(ent),
            FaitAnnotation.NON_NULL,FaitAnnotation.INITIALIZED);
      cur_state.pushStack(v0);
      IfaceValue v2 = v0.forceInitialized(FaitAnnotation.UNDER_INITIALIZATION);
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

   JcompSymbol js = JcompAst.getReference(v);
   // check for case with only a default constructor
   if (js != null) {
      if (!processCall(v,narg)) return NO_NEXT_REPORT;
    }
   else {
      if (rty.needsOuterClass()) popValue();
      popValue();
    }

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

   if (!processCall(v,args.size() + delta)) {
      return NO_NEXT_REPORT;
    }

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
      if (!js.isStatic()) return v.getExpression();
    }

   int idx = 0;
   List <?> args = v.arguments();
   if (after_node != null && after_node != v.getExpression()) idx = args.indexOf(after_node) + 1;
   if (idx < args.size()) return args.get(idx);

   int act = args.size();
   if (!js.isStatic()) ++act;
   
   if (!processCall(v,act)) {
      return NO_NEXT_REPORT;
    }

   return null;
}


private Object visit(SuperConstructorInvocation v)
{
   JcompType rty = JcompAst.getExprType(v);
   rty = rty.getSuperType();
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
   if (rty.needsOuterClass()) ++delta;
   if (!processCall(v,args.size()+delta)) return NO_NEXT_REPORT;

   return null;
}


private Object visit(SuperMethodInvocation v)
{
   if (after_node == null) {
      JcompSymbol js = JcompAst.getReference(v.getName());
      if (!js.isStatic()) {
	 IfaceValue v0 = cur_state.getLocal(0);
	 cur_state.pushStack(v0);
       }
    }

   int idx = 0;
   List <?> args = v.arguments();
   if (after_node != null) idx = args.indexOf(after_node) + 1;
   if (idx < args.size()) return args.get(idx);

   if (!processCall(v,args.size())) return NO_NEXT_REPORT;

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
      if (tb != TestBranch.NEVER) {
	 workOn(s);
       }
      if (tb == TestBranch.ALWAYS) {
	 return s;
       }
    }

   return null;
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



private Object visit(ForStatement s)
{
   StructuralPropertyDescriptor spd = null;
   if (after_node != null) spd = after_node.getLocationInParent();
   if (after_node != null && after_node == s.getExpression()) {
      IfaceValue bf = popActual();
      if (bf == null) return NO_NEXT_REPORT;
      TestBranch tb = getTestResult(bf);
      if (tb == TestBranch.NEVER) return null;
      else if (tb == TestBranch.ALWAYS) return s.getBody();
      workOn(s.getBody());
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
      Pair<IfaceState,IfaceState> imps = handleImplications(s);
      if (tb != TestBranch.ALWAYS) {
	 if (s.getElseStatement() != null) {
	    workOn(imps.getElement1(),s.getElseStatement());
          }
         else nextok = true;
       }
      if (tb != TestBranch.NEVER) {
	 workOn(imps.getElement0(),s.getThenStatement());
       }
      if (!nextok) return NO_NEXT;
    }
   
   return null;
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
      if (rval == null) return NO_NEXT_REPORT;
    }

   return new FlowAstStatus(Reason.RETURN,rval);
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
	 IfaceValue v0 = popActualNonNull();
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


private Object visitThrow(SynchronizedStatement s,IfaceAstStatus sts)
{
   cur_state.discardFields();
   return sts;
}


private Object visit(ThrowStatement s)
{
   if (after_node == null) return s.getExpression();
   IfaceValue v = popActualNonNull();
   if (v == null) return NO_NEXT_REPORT;
   flow_queue.handleThrow(work_queue,getHere(),v,cur_state);
   return NO_NEXT;
}


private Object visit(TryStatement s)
{
   if (after_node == null) {
      pushValue(fait_control.findMarkerValue(ast_where,TryState.BODY));
      return s.getBody();
    }
   else if (after_node == s.getFinally()) {
      Object o = popMarker(s);
      if (o instanceof IfaceAstStatus) {
         IfaceAstStatus sts = (IfaceAstStatus) o;
         return sts;
       }
    }
   else {
      Object o = popMarker(s);
      if (o == TryState.BODY || o == TryState.CATCH) {
         Block b = s.getFinally();
         if (b != null) {
            pushValue(fait_control.findMarkerValue(ast_where,TryState.FINALLY));
            return b;
          }
       }
    }
   
   return null;
}


private Object visitThrow(TryStatement s,IfaceAstStatus r)
{
   Object sts = popMarker(s);
   if (r.getReason() == Reason.EXCEPTION && sts == TryState.BODY) {
      IfaceValue exc = r.getValue();
      IfaceType etyp = exc.getDataType();
      for (Object o : s.catchClauses()) {
         CatchClause cc = (CatchClause) o;
         SingleVariableDeclaration svd = cc.getException();
         JcompType ctype = JcompAst.getJavaType(svd.getType());
         IfaceType ctyp = convertType(ctype);
         if (etyp.isCompatibleWith(ctyp)) {
            pushValue(fait_control.findMarkerValue(ast_where,TryState.CATCH));
            pushValue(exc);
            return cc;
          }
       }
    }
   if (sts instanceof TryState && sts != TryState.FINALLY) {
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


private Object visit(WhileStatement s)
{
   if (after_node != s.getExpression()) return s.getExpression();
   else {
      IfaceValue iv = popActual();
      if (iv == null) return NO_NEXT_REPORT;
      TestBranch tb = getTestResult(iv);
      if (tb == TestBranch.NEVER) return null;
      else if (tb == TestBranch.ALWAYS) return s.getBody();
      workOn(s.getBody());
    }
   return null;
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
      IfaceValue cnts = iv.getArrayContents();
      if (cnts != null && cnts.isEmptyEntitySet()) {
         System.err.println("ATTEMPT TO ASSIGN EMPTY ENTITY SET");
         cnts = iv.getArrayContents();
       }
      JcompType jt = JcompAst.getJavaType(s.getParameter());
      IfaceType ijt = convertType(jt);
      if (cnts != null) {
         cnts = flow_queue.castValue(ijt,cnts,getHere());
       }
      if (cnts == null) {
         cnts = fait_control.findMutableValue(ijt);
       }
      int slot = getSlot(JcompAst.getDefinition(s.getParameter().getName()));
      cur_state.setLocal(slot,cnts);
      workOn(s.getBody());
    }

   return null;
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
   IfaceValue dv = fait_control.findRangeValue(ityp,dim,dim);
   IfaceValue av = flow_queue.handleNewArraySet(getHere(),rtyp,1,dv);
   for (int i = dim - 1; i >= 0; --i) {
      IfaceValue iv = popActual();
      if (iv == null) return NO_NEXT_REPORT;
      IfaceValue idxv = fait_control.findRangeValue(ityp,i,i);
      flow_queue.handleArraySet(getHere(),av,iv,idxv);
    }
   pushValue(av);

   return null;
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




/********************************************************************************/
/*										*/
/*	Lambda and reference management 					*/
/*										*/
/********************************************************************************/

private Object visit(LambdaExpression v)
{
   return null;
}


private Object visit(CreationReference v)
{
   return null;
}

private Object visit(ExpressionMethodReference v)
{
   return null;
}


private Object visit(SuperMethodReference v)
{
   return null;
}

private Object visit(TypeMethodReference v)
{
   return null;
}



/********************************************************************************/
/*										*/
/*	Reference management							*/
/*										*/
/********************************************************************************/

private IfaceValue getActualValue(IfaceValue ref,boolean nonnull)
{
   if (ref == null) return null;
   if (!ref.isReference()) return ref;
   // TODO :: handle non-null flag

   IfaceValue base = ref.getRefBase();
   if (base != null) {
      FlowLocation here = getHere();
      IfaceValue idx = ref.getRefIndex();
      if (base.mustBeNull()) {
	 return null;
       }
      if (idx != null) {
	 IfaceValue vrslt = flow_queue.handleArrayAccess(here,base,idx);
         IfaceValue nrslt = getNonNullResult(nonnull,vrslt);
         if (nrslt != null && nrslt != vrslt) flow_queue.handleArraySet(here,base,nrslt,idx);
	 return nrslt;
       }
      else if (ref.getRefField() != null) {
	 boolean oref = false;		// set to true if base == this
	 if (base == getLocal(0)) oref = true;
	 IfaceValue vrslt = flow_queue.handleFieldGet(ref.getRefField(),here,
	       cur_state,oref,base);
         if (FaitLog.isTracing())
            FaitLog.logD1("Field Access " + ref.getRefField() + " = " + vrslt);
         IfaceValue nrslt = getNonNullResult(nonnull,vrslt);
         if (nrslt != null && nrslt != vrslt) 
            flow_queue.handleFieldSet(ref.getRefField(),here,cur_state,oref,nrslt,base);
	 return nrslt;
       }
    }
   else if (ref.getRefSlot() >= 0) {
      IfaceValue vrslt = getLocal(ref.getRefSlot());
      IfaceValue nrslt = getNonNullResult(nonnull,vrslt);
      if (nrslt != null && nrslt != vrslt) cur_state.setLocal(ref.getRefSlot(),nrslt);
      return nrslt;
    }
   else if (ref.getRefField() != null) {
      // static fields
      IfaceValue vrslt = flow_queue.handleFieldGet(ref.getRefField(),getHere(),cur_state,false,null);
      return vrslt;
    }
   else {
      FaitLog.logE("ILLEGAL REF VALUE");
    }

   return ref;
}



private IfaceValue getNonNullResult(boolean nonnull,IfaceValue v)
{
   if (v == null || !nonnull) return v;
   if (v.mustBeNull()) return null;
   if (!v.canBeNull()) return v;
   IfaceValue v1 = v.forceNonNull();
   return v1;
}


private IfaceValue assignValue(IfaceValue ref,IfaceValue v)
{
   IfaceValue v1 = flow_queue.castValue(ref.getDataType(),v,getHere());

   IfaceValue base = ref.getRefBase();
   IfaceField fld = ref.getRefField();
   IfaceValue idx = ref.getRefIndex();
   int slot = ref.getRefSlot();
   if (fld != null) {
      boolean thisref = false;		// need to set
      if (base != null && !fld.isStatic() &&
	    base == cur_state.getLocal(0)) thisref = true;
      flow_queue.handleFieldSet(fld,getHere(),cur_state,thisref,v1,base);
    }
   else if (idx != null) {
      flow_queue.handleArraySet(getHere(),base,v1,idx);
    }
   else if (slot >= 0) {
      setLocal(getHere(),cur_state,slot,v1);
      if (FaitLog.isTracing()) FaitLog.logD("Assign to local " + slot + " = " + v1);
    }
   else {
      FaitLog.logE("Bad assignment");
    }

   return v1;
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


private IfaceValue popActualNonNull()
{
   return getActualValue(popValue(),true);
}


private Object popMarker(ASTNode n)
{
   for ( ; ; ) {
      IfaceValue v = cur_state.popStack();
      if (v instanceof IfaceMarker) {
         IfaceMarker mkr = (IfaceMarker) v;
         IfaceProgramPoint pt = mkr.getProgramPoint();
         IfaceAstReference ar = pt.getAstReference();
         if (ar.getAstNode() != n) continue;
         return mkr.getMarkerValue();
       }
    }
}



private IfaceType convertType(JcompType jt,IfaceAnnotation ... annots)
{
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
   work_queue.mergeState(cur_state,fait_control.getAstReference(n));
}


private void workOn(IfaceState st,ASTNode n)
{
   work_queue.mergeState(st,fait_control.getAstReference(n));
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
   if (fld != null) return fld;
   return fld;
}



private TestBranch getTestResult(IfaceValue v1)
{
   TestBranch tb = v1.branchTest(null,FaitOperator.NEQ_ZERO);
   return tb;
}



private boolean processCall(ASTNode v,int act)
{
   JcompSymbol js = JcompAst.getReference(v);
   JcompType jt = js.getType();
   
   int checkarg = -1;
   if (!js.isStatic()) checkarg = act-1;

   List<IfaceValue> alst = new ArrayList<>();
   for (int i = 0; i < act; ++i) {
      IfaceValue v0 = null;
      if (i == checkarg) v0 = popActualNonNull();
      else v0 = popActual(); 
      if (v0 == null) return false;
      alst.add(v0);
    }
   for (int i = act-1; i >= 0; --i) {
      IfaceValue v0 = alst.get(i);
      pushValue(v0);
    }

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
         IfaceType ityp = fait_control.findDataType("int");
	 IfaceValue szv = fait_control.findRangeValue(ityp,sz,sz);
	 IfaceValue rslt = flow_queue.handleNewArraySet(getHere(),btyp,1,szv);
	 for (int i = sz-1; i >= 0; --i) {
	    IfaceValue iv = popActual();
	    IfaceValue idxv = fait_control.findRangeValue(ityp,i,i);
	    flow_queue.handleArraySet(getHere(),rslt,iv,idxv);
	  }
	 pushValue(rslt);
       }
    }
   if (cur_state == null) return false;
   if (!flow_queue.handleCall(getHere(),cur_state,work_queue)) {
      if (FaitLog.isTracing()) FaitLog.logD("Unknown RETURN value for " + js);
      return false;
    }

   return true;
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
   IfaceType ntyp = rtyp.getComputedType(v,op,v1,v2);
   if (ntyp != rtyp) v = v.restrictByType(ntyp);
   
   return v;
}




/********************************************************************************/
/*										*/
/*	Handle initializations using given value or default     		*/
/*										*/
/********************************************************************************/

private void handleInitialization(JcompSymbol js,ASTNode init)
{
   IfaceValue iv = null;
   if (init != null) iv = popActual();
   else {
      IfaceType it = convertType(js.getType());
      if (it.isNumericType() || it.isBooleanType()) {
         iv = fait_control.findRangeValue(it,0,0);
       }
      else {
         iv = fait_control.findNullValue(it);
       }
    }
   int slot = getSlot(js);
   if (slot >= 0 && iv != null) {
      IfaceValue ref = fait_control.findRefValue(convertType(js.getType()),slot);
      assignValue(ref,iv);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Handle implications                                                     */
/*                                                                              */
/********************************************************************************/

Pair<IfaceState,IfaceState> handleImplications(IfStatement root)
{
   Expression expr = root.getExpression();
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
      IfaceImplications imps = lhsa.getImpliedValues(rhsv,op);
      if (imps == null) return rslt;
      IfaceState sttrue = cur_state;
      IfaceState stfalse = cur_state;
      if (lhsv.isReference()) {
         IfaceValue nlhs = imps.getLhsTrueValue();
         if (nlhs != null && nlhs != lhsa) {
            sttrue = setRefValue(sttrue,lhsv,nlhs);
          }
         nlhs = imps.getLhsFalseValue();
         if (nlhs != null && nlhs != lhsa) {
            stfalse = setRefValue(stfalse,lhsv,nlhs);
          }
       }
      if (rhsv.isReference()) {
         IfaceValue nrhs = imps.getRhsTrueValue();
         if (nrhs != null && nrhs != rhsa) {
            sttrue = setRefValue(sttrue,rhsv,nrhs);
          }
         nrhs = imps.getRhsFalseValue();
         if (nrhs != null && nrhs != rhsa) {
            stfalse = setRefValue(stfalse,rhsv,nrhs);
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
         return fait_control.findRangeValue(ityp,lval,lval);
      case ASTNode.SIMPLE_NAME :
         return getReference(e);
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



private IfaceState setRefValue(IfaceState st0,IfaceValue ref,IfaceValue v)
{
   cur_state = cur_state.cloneState();
   assignValue(ref,v);
   IfaceState rst = cur_state;
   cur_state = st0;
   return rst;
}



private long getNumericValue(String sv)
{
   long lv = 0;
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
   else if (sv.endsWith("L") || sv.endsWith("l")) {
      sv = sv.substring(0,sv.length()-1);
      lv = Long.parseLong(sv);
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
}	// end of class FlowScannerAst




/* end of FlowScannerAst.java */

