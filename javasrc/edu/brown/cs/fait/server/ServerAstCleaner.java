/********************************************************************************/
/*										*/
/*		ServerAstCleaner.java						*/
/*										*/
/*	Clean up ASTs to make them easier to evaluate				*/
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

// import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.ivy.jcomp.JcompAst;


class ServerAstCleaner extends ASTVisitor implements ServerConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CompilationUnit ast_root;
private AST		the_ast;

private static final HashSet<String> TEST_ANNOTATIONS;
private static final String TEST_THIS = "test_this";

private static boolean do_tests = false;

static {
   TEST_ANNOTATIONS = new HashSet<>();
   TEST_ANNOTATIONS.add("org.junit.Test");
   TEST_ANNOTATIONS.add("Test");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ServerAstCleaner(ServerProject sp,CompilationUnit cu)
{
   ast_root = cu;
   the_ast = cu.getAST();
}



/********************************************************************************/
/*										*/
/*	Non-semantic based cleanup						*/
/*										*/
/********************************************************************************/

void precleanAst()
{
   for (Object typn : ast_root.types()) {
      if (typn instanceof TypeDeclaration || typn instanceof EnumDeclaration) {
	 fixType((AbstractTypeDeclaration) typn,false);
       }
    }
}


// CHECKSTYLE:OFF
@SuppressWarnings("unchecked")
private void fixType(AbstractTypeDeclaration td,boolean inner)
// CHECKSTYLE:ON
{
   boolean havecnst = false;
   List<Initializer> initers = new ArrayList<>();
   boolean haveinits = false;
   boolean havecinits = false;
   boolean iface = false;
   boolean istesting = false;
   Type supertype = null;

   if (td instanceof TypeDeclaration) {
      TypeDeclaration ttd = (TypeDeclaration) td;
      iface = ttd.isInterface();
      supertype = ttd.getSuperclassType();
    }
   else if (td instanceof EnumDeclaration) {
      haveinits = true;
    }

   for (Object o : td.bodyDeclarations()) {
      BodyDeclaration bd = (BodyDeclaration) o;
      if (bd instanceof MethodDeclaration) {
	 MethodDeclaration md = (MethodDeclaration) bd;
	 if (md.isConstructor()) havecnst = true;
	 for (Object o1 : md.modifiers()) {
	    if (o1 instanceof Annotation) {
	       Annotation annot = (Annotation) o1;
	       String id = annot.getTypeName().getFullyQualifiedName();
	       if (TEST_ANNOTATIONS.contains(id))
		  istesting = true;
	     }
	  }
       }
      else if (bd instanceof Initializer) {
	 initers.add((Initializer) bd);
       }
      else if (bd instanceof FieldDeclaration) {
	 FieldDeclaration fd = (FieldDeclaration) bd;
	 for (Object o1 : fd.fragments()) {
	    VariableDeclarationFragment vdf = (VariableDeclarationFragment) o1;
	    if (vdf.getInitializer() != null) {
	       if (Modifier.isStatic(fd.getModifiers()) || iface) haveinits = true;
	       else havecinits = true;
	     }
	  }
       }
    }

   if (!havecnst) {
      if (iface) havecnst = true;
      else if (Modifier.isStatic(td.getModifiers()) || !inner) {
	 if (!havecinits) havecnst = true;
	 if (td instanceof TypeDeclaration && ((TypeDeclaration) td).getSuperclassType() != null)
	    havecnst = false;
	 if (td instanceof EnumDeclaration) havecnst = false;
       }
    }

   if (haveinits || initers.size() > 0) {
      MethodDeclaration md = the_ast.newMethodDeclaration();
      md.setBody(the_ast.newBlock());
      md.setName(the_ast.newSimpleName(INITER_NAME));
      md.setReturnType2(the_ast.newPrimitiveType(PrimitiveType.VOID));
      md.modifiers().addAll(the_ast.newModifiers(Modifier.STATIC | Modifier.PUBLIC));

      int enumctr = 0;
      int numenum = 0;
      for (Object o : td.bodyDeclarations()) {
	 if (o instanceof FieldDeclaration) {
	    FieldDeclaration fd = (FieldDeclaration) o;
	    if (Modifier.isStatic(fd.getModifiers()) || iface) {
	       for (Object o1 : fd.fragments()) {
		  VariableDeclarationFragment vdf = (VariableDeclarationFragment) o1;
		  if (vdf.getInitializer() != null) {
		     Assignment asgn = the_ast.newAssignment();
		     asgn.setLeftHandSide(the_ast.newSimpleName(vdf.getName().getIdentifier()));
		     asgn.setRightHandSide((Expression) ASTNode.copySubtree(the_ast,vdf.getInitializer()));
		     ExpressionStatement es = the_ast.newExpressionStatement(asgn);
		     md.getBody().statements().add(es);
		   }
		}
	     }
	  }
       }
      if (td instanceof EnumDeclaration) {
	 for (Object o5 : ((EnumDeclaration) td).enumConstants()) {
	    EnumConstantDeclaration ecd = (EnumConstantDeclaration) o5;
	    ++numenum;
	    Assignment asgn = the_ast.newAssignment();
	    asgn.setLeftHandSide(the_ast.newSimpleName(ecd.getName().getIdentifier()));
	    ClassInstanceCreation cic = the_ast.newClassInstanceCreation();
	    Name n1 = JcompAst.getQualifiedName(the_ast,td.getName().getIdentifier());
	    Type t1 = the_ast.newSimpleType(n1);
	    cic.setType(t1);
	    if (ecd.arguments().size() > 0) {
	       for (Object o1 : ecd.arguments()) {
		  Expression e1 = (Expression) o1;
		  Expression e2 = (Expression) ASTNode.copySubtree(the_ast,e1);
		  cic.arguments().add(e2);
		}
	     }
	    else {
	       StringLiteral e4 = the_ast.newStringLiteral();
	       e4.setLiteralValue(ecd.getName().getIdentifier());
	       Expression e3 = JcompAst.newNumberLiteral(the_ast,enumctr++);
	       cic.arguments().add(e4);
	       cic.arguments().add(e3);
	     }
	    asgn.setRightHandSide(cic);
	    ExpressionStatement es = the_ast.newExpressionStatement(asgn);
	    md.getBody().statements().add(es);
	  }
       }
      if (numenum > 0) {
	 VariableDeclarationFragment vdf = the_ast.newVariableDeclarationFragment();
	 vdf.setName(the_ast.newSimpleName("$VALUES"));
	 FieldDeclaration vfd = the_ast.newFieldDeclaration(vdf);
	 vfd.modifiers().addAll(the_ast.newModifiers(Modifier.STATIC | Modifier.PRIVATE));
	 Type t1 = the_ast.newSimpleType(the_ast.newSimpleName(td.getName().getIdentifier()));
	 Type t2 = the_ast.newArrayType(t1);
	 vfd.setType(t2);
	 td.bodyDeclarations().add(vfd);

	 ArrayCreation e1 = the_ast.newArrayCreation();
	 Type t3 = the_ast.newSimpleType(the_ast.newSimpleName(td.getName().getIdentifier()));
	 ArrayType t4 = the_ast.newArrayType(t3);
	 e1.setType(t4);
	 ArrayInitializer ai = the_ast.newArrayInitializer();
	 for (Object o4 : ((EnumDeclaration) td).enumConstants()) {
	    EnumConstantDeclaration ecd = (EnumConstantDeclaration) o4;
	    SimpleName sn = the_ast.newSimpleName(ecd.getName().getIdentifier());
	    ai.expressions().add(sn);
	  }
	 e1.setInitializer(ai);
	 Assignment asgn = the_ast.newAssignment();
	 asgn.setLeftHandSide(the_ast.newSimpleName("$VALUES"));
	 asgn.setRightHandSide(e1);
	 ExpressionStatement es = the_ast.newExpressionStatement(asgn);
	 md.getBody().statements().add(es);
       }
      for (Initializer initer : initers) {
	 md.getBody().statements().add((Statement) ASTNode.copySubtree(the_ast,initer.getBody()));
       }

      FaitLog.logD("CREATED/MODIFIED initializer: " + md);
      td.bodyDeclarations().add(md);
    }

   if (!havecnst) {
      MethodDeclaration md = the_ast.newMethodDeclaration();
      md.setConstructor(true);
      Block blk = the_ast.newBlock();
      md.setBody(blk);
      md.setName(the_ast.newSimpleName(td.getName().getIdentifier()));
      if (td instanceof EnumDeclaration) {
	 md.modifiers().addAll(the_ast.newModifiers(Modifier.PRIVATE));
	 SingleVariableDeclaration p1 = the_ast.newSingleVariableDeclaration();
	 p1.setName(JcompAst.getSimpleName(the_ast,"name"));
	 Type t1 = the_ast.newSimpleType(JcompAst.getQualifiedName(the_ast,"java.lang.String"));
	 p1.setType(t1);
	 md.parameters().add(p1);
	 SingleVariableDeclaration p2 = the_ast.newSingleVariableDeclaration();
	 p2.setName(JcompAst.getSimpleName(the_ast,"ord"));
	 Type t2 = the_ast.newPrimitiveType(PrimitiveType.INT);
	 p2.setType(t2);
	 md.parameters().add(p2);
	 SuperConstructorInvocation sci = the_ast.newSuperConstructorInvocation();
	 sci.arguments().add(JcompAst.getSimpleName(the_ast,"name"));
	 sci.arguments().add(JcompAst.getSimpleName(the_ast,"ord"));
	 blk.statements().add(sci);
       }
      else {
	 md.modifiers().addAll(the_ast.newModifiers(Modifier.PUBLIC));
       }
      td.bodyDeclarations().add(md);
      FaitLog.logD("CREATED constructor: " + md);
    }

   if (supertype != null || havecinits) {
      for (Object o : td.bodyDeclarations()) {
	 if (o instanceof MethodDeclaration) {
	    MethodDeclaration md = (MethodDeclaration) o;
	    if (!md.isConstructor()) continue;
	    Block blk = md.getBody();
	    List<Object> stmts = blk.statements();
	    boolean fnd = false;
	    int idx = 0;
            boolean cnstinv = false;
	    for (Object stmtn : stmts) {
               if (stmtn instanceof ConstructorInvocation) cnstinv = true;
	       if (stmtn instanceof SuperConstructorInvocation) fnd = true;
	     }
            if (cnstinv) continue;      // not needed if it calls this(...)
	    if (fnd) idx = 1;
	    if (!fnd && supertype != null) {
	       SuperConstructorInvocation sci = the_ast.newSuperConstructorInvocation();
	       stmts.add(0,sci);
	       idx = 1;
	     }
	    if (havecinits) {
	       for (Object o1 : td.bodyDeclarations()) {
		  if (o1 instanceof FieldDeclaration) {
		     FieldDeclaration fd = (FieldDeclaration) o1;
		     if (!Modifier.isStatic(fd.getModifiers())) {
			for (Object o2 : fd.fragments()) {
			   VariableDeclarationFragment vdf = (VariableDeclarationFragment) o2;
			   if (vdf.getInitializer() != null) {
			      Assignment asgn = the_ast.newAssignment();
                              FieldAccess fa = the_ast.newFieldAccess();
                              fa.setExpression(the_ast.newThisExpression());
                              fa.setName(the_ast.newSimpleName(vdf.getName().getIdentifier()));
                              asgn.setLeftHandSide(fa);
			      asgn.setRightHandSide((Expression) ASTNode.copySubtree(
                                    the_ast,vdf.getInitializer()));
			      ExpressionStatement es = the_ast.newExpressionStatement(asgn);
			      md.getBody().statements().add(idx++,es);
			    }
			 }
		      }
		   }
		}
	     }
	  }
       }
    }

   if (td instanceof EnumDeclaration) {
      addEnumMethods((EnumDeclaration) td);
    }

   if (istesting) {
      addTestMethods(td);
    }

   for (Object o : td.bodyDeclarations()) {
      if (o instanceof TypeDeclaration || o instanceof EnumDeclaration) {
	 fixType((AbstractTypeDeclaration) o,true);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Generate code for enumeration initialization and implicit methods	*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
private void addEnumMethods(EnumDeclaration ed)
{
   boolean havevalues = false;
   boolean havevalueof = false;

   for (Object o : ed.bodyDeclarations()) {
      if (o instanceof MethodDeclaration) {
	 MethodDeclaration md = (MethodDeclaration) o;
	 if (md.getName().getIdentifier().equals("values")) {
	    if (md.parameters().size() == 0) havevalues = true;
	  }
	 else if (md.getName().getIdentifier().equals("valueOf")) {
	    if (md.parameters().size() == 1) havevalueof = true;
	  }
       }
    }

   if (!havevalues) {
      MethodDeclaration md = the_ast.newMethodDeclaration();
      Block bk = the_ast.newBlock();
      md.setBody(bk);
      md.modifiers().addAll(the_ast.newModifiers(Modifier.STATIC | Modifier.PUBLIC));
      md.setName(the_ast.newSimpleName("values"));
      Type t3 = the_ast.newSimpleType(the_ast.newSimpleName(ed.getName().getIdentifier()));
      ArrayType t4 = the_ast.newArrayType(t3);
      md.setReturnType2(t4);
      ReturnStatement rs = the_ast.newReturnStatement();
      rs.setExpression(the_ast.newSimpleName("$VALUES"));
      bk.statements().add(rs);
      ed.bodyDeclarations().add(md);
    }

   if (!havevalueof) {
      MethodDeclaration md = the_ast.newMethodDeclaration();
      Block bk = the_ast.newBlock();
      md.setBody(bk);
      md.modifiers().addAll(the_ast.newModifiers(Modifier.STATIC | Modifier.PUBLIC));
      md.setName(the_ast.newSimpleName("valueOf"));
      Type t3 = the_ast.newSimpleType(the_ast.newSimpleName(ed.getName().getIdentifier()));
      md.setReturnType2(t3);
      SingleVariableDeclaration svd = the_ast.newSingleVariableDeclaration();
      svd.setName(the_ast.newSimpleName("s"));
      svd.setType(the_ast.newSimpleType(JcompAst.getQualifiedName(the_ast,"java.lang.String")));
      md.parameters().add(svd);
      MethodInvocation mi = the_ast.newMethodInvocation();
      mi.setName(the_ast.newSimpleName("valueOf"));
      TypeLiteral tl = the_ast.newTypeLiteral();
      tl.setType(the_ast.newSimpleType(the_ast.newSimpleName(ed.getName().getIdentifier())));
      mi.arguments().add(tl);
      mi.arguments().add(the_ast.newSimpleName("s"));
      ReturnStatement rs = the_ast.newReturnStatement();
      rs.setExpression(mi);
      bk.statements().add(rs);
      ed.bodyDeclarations().add(md);
    }

   FaitLog.logD("UPDATED ENUM: " + ed);
}



/********************************************************************************/
/*										*/
/*	Generate code for test cases						*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
private void addTestMethods(AbstractTypeDeclaration atd)
{
   if (!do_tests) return;
   
   // THIS IS NOT USED
   
   MethodDeclaration md = the_ast.newMethodDeclaration();
   md.setBody(the_ast.newBlock());
   md.setName(the_ast.newSimpleName(TESTER_NAME));
   md.setReturnType2(the_ast.newPrimitiveType(PrimitiveType.VOID));
   md.modifiers().addAll(the_ast.newModifiers(Modifier.STATIC|Modifier.PUBLIC));
   addTestCalls("BeforeClass",atd,md.getBody());

   VariableDeclarationFragment vdf = the_ast.newVariableDeclarationFragment();
   vdf.setName(the_ast.newSimpleName(TEST_THIS));
   ClassInstanceCreation cic = the_ast.newClassInstanceCreation();
   
   Name n1 = JcompAst.getQualifiedName(the_ast,atd.getName().getIdentifier());
   Type t1 = the_ast.newSimpleType(n1);
   cic.setType(t1);
   vdf.setInitializer(cic);
   VariableDeclarationStatement vds = the_ast.newVariableDeclarationStatement(vdf);
   Name n2 = JcompAst.getQualifiedName(the_ast,atd.getName().getIdentifier());
   Type t2 = the_ast.newSimpleType(n2);
   vds.setType(t2);
   md.getBody().statements().add(vds);

   addTestCalls("Before",atd,md.getBody());
   addTestCalls("Test",atd,md.getBody());
   addTestCalls("After",atd,md.getBody());
   addTestCalls("AfterClass",atd,md.getBody());

   atd.bodyDeclarations().add(md);

   FaitLog.logD("Added Test: " + md);
}



@SuppressWarnings("unchecked")
private void addTestCalls(String annot,AbstractTypeDeclaration atd,Block b)
{
   for (Object o : atd.bodyDeclarations()) {
      if (o instanceof MethodDeclaration) {
	 MethodDeclaration md = (MethodDeclaration) o;
	 boolean fnd = false;
	 boolean isstatic = false;
	 for (Object o1 : md.modifiers()) {
	    if (o1 instanceof Annotation) {
	       Annotation an = (Annotation) o1;
	       String id = an.getTypeName().getFullyQualifiedName();
	       if (id.equals(annot) || id.equals("org.junit." + annot)) fnd = true;
	     }
	    else if (o1 instanceof Modifier) {
	       Modifier mod = (Modifier) o1;
	       if (mod.isStatic()) isstatic = true;
	     }
	  }
	 if (fnd) {
	    MethodInvocation mi = the_ast.newMethodInvocation();
	    mi.setName(JcompAst.getSimpleName(the_ast,md.getName().getIdentifier()));
	    if (!isstatic) {
	       Name n2 = the_ast.newSimpleName(TEST_THIS);
	       mi.setExpression(n2);
	     }
	    ExpressionStatement es = the_ast.newExpressionStatement(mi);
	    b.statements().add(es);
	  }
       }
    }
}


/********************************************************************************/
/*										*/
/*	Clean up a resolved AST.  Return true is something changed		*/
/*										*/
/********************************************************************************/

boolean cleanAst()
{
   return false;
}



/********************************************************************************/
/*										*/
/*	Check if a class is a starting class					*/
/*										*/
/********************************************************************************/



}	// end of class ServerAstCleaner




/* end of ServerAstCleaner.java */

