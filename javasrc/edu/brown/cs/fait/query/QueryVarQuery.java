/********************************************************************************/
/*										*/
/*		QueryVarQuery.java						*/
/*										*/
/*	Handle variable queries 						*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
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



package edu.brown.cs.fait.query;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import edu.brown.cs.fait.iface.FaitException;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompScope;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryVarQuery implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceControl for_control;
private String	class_name;
private String	method_name;
private String  variable_name;
private int	start_pos;
private int	line_number;
private IvyXmlWriter xml_writer;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

QueryVarQuery(IfaceControl ctrl,String method,int line,int pos,String var,IvyXmlWriter xw)
{
   for_control = ctrl;
   line_number = line;
   start_pos = pos;
   variable_name = var;
   int idx = method.indexOf("(");
   if (idx > 0) {
      method = method.substring(0,idx);
    }
   idx = method.lastIndexOf(".");
   method_name = method.substring(idx+1);
   class_name = method.substring(0,idx);
   int idx1 = class_name.lastIndexOf(".");
   String cnm = class_name;
   if (idx1 > 0) cnm = class_name.substring(idx1+1);
   if (cnm.equals(method_name)) method_name = "<init>";

   xml_writer = xw;
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void process() throws FaitException
{
   int lpos = start_pos;
   ASTNode mbody = null;
   IfaceMethod method = null;
   List<IfaceCall> calls = new ArrayList<>();
   IfaceType typ = for_control.findDataType(class_name);
   for (IfaceMethod im : for_control.findAllMethods(typ,method_name)) {
      if (im.getStart().getAstReference() == null) continue;
      ASTNode mb = im.getStart().getAstReference().getAstNode();
      if (start_pos <= 0) {
         CompilationUnit cu = (CompilationUnit) mb.getRoot();
         // want to get position after spaces at start of line
         lpos = cu.getPosition(line_number,1);
       }
      if (lpos >= mb.getStartPosition() &&
	    lpos <= mb.getStartPosition() + mb.getLength()) {
	 calls.addAll(for_control.getAllCalls(im));
	 mbody = mb;
	 method = im;
	 break;
       }
    }
   if (mbody == null) {
      FaitLog.logE("Can't find method body for var query for " + method_name);
      for (IfaceMethod im : for_control.findAllMethods(typ,method_name)) {
	 FaitLog.logE("\tConsider " + im + " " + start_pos);
	 if (im.getStart().getAstReference() == null) continue;
	 ASTNode mb = im.getStart().getAstReference().getAstNode();
	 FaitLog.logE("\tAST at " + mb.getStartPosition() + " " + mb.getLength());
       }
      return;
    }
   
   IfaceValue refval = null;
   IfaceAstReference astr = null;
   
   if (start_pos > 0) {
      ASTNode child = JcompAst.findNodeAtOffset(mbody,start_pos);
      if (child == null) {
         FaitLog.logE("Can't find child node from " + start_pos + " " + mbody);
         return;
       }
      QueryVarReference qvr = new QueryVarReference(for_control,method,child);
      astr = qvr.getAstReference();
      if (astr == null) {
         FaitLog.logE("Can't find ast reference for " + child);
         return;
       }
      refval = qvr.getRefValue();
    }
   else if (variable_name != null && lpos > 0) {
      ASTNode child = JcompAst.findNodeAtLine(mbody,line_number);
      ASTNode after = null;
      while (child instanceof Expression) {
         after = child;
         child = child.getParent();
       }
      astr = for_control.getAstReference(child,after);
      refval = findReferenceForName(variable_name,method,mbody,astr,child);
    }
   if (refval == null) {
      FaitLog.logE("Can't find reference value");
      return;
    }
   
   xml_writer.begin("VALUESET");
   xml_writer.field("LINE",line_number);
   xml_writer.field("METHOD",method_name);
   xml_writer.field("CLASS",class_name);

   for (IfaceCall ic : calls) {
      for (IfaceCall ic1 : ic.getAlternateCalls()) {
	 IfaceState st0 = for_control.findStateForLocation(ic1,astr);
	 if (st0 == null) {
	    continue;
	  }
	 if (refval != null) {
	    xml_writer.begin("REFVALUE");
	    xml_writer.field("CALL",ic1.getMethod().getFullName() + ic1.getMethod().getDescription());
	    xml_writer.field("CALLID",ic1.hashCode());
	    if (st0.getSafetyStatus() != null) st0.getSafetyStatus().outputXml(xml_writer);
	    xml_writer.begin("REFERENCE");
	    refval.outputXml(xml_writer);
	    xml_writer.end("REFERENCE");
	    st0.getLocation().outputXml(xml_writer);
	    IfaceValue v0 = QueryFactory.dereference(for_control,refval,st0);
	    if (v0 != null) v0.outputXml(xml_writer);
	    xml_writer.end("REFVALUE");
	  }
       }
    }

   for (IfaceSubtype st : for_control.getAllSubtypes()) {
      xml_writer.begin("SUBTYPE");
      xml_writer.field("NAME",st.getName());
      String s = st.getDefaultValues();
      if (s != null) xml_writer.field("DEFAULTS",s);
      xml_writer.end("SUBTYPE");
    }

   xml_writer.end("VALUESET");
}





/********************************************************************************/
/*                                                                              */
/*      Handle mapping variable name to reference                               */
/*                                                                              */
/********************************************************************************/

IfaceValue findReferenceForName(String name,IfaceMethod mthd,ASTNode method,
      IfaceAstReference ppt,ASTNode loc)
{
   IfaceValue rslt = null;
   String name1 = name.replace("?",".");
   int idx = name1.lastIndexOf(".");
   if (idx >= 0) {
      String base = name1.substring(0,idx);
      String field = name1.substring(idx+1);
      IfaceValue basevalue = findReferenceForName(base,mthd,method,ppt,loc); 
      if (basevalue == null) return null;
      IfaceField fld = for_control.findField(basevalue.getDataType(),field);
      rslt = for_control.findRefValue(fld.getType(),basevalue,fld);
    }
   else {
      int slot = -1;
      IfaceType ltyp = null;
      if (name.equals("this") || name.startsWith("this$")) {
         slot = mthd.getLocalOffset(name);
         ltyp = mthd.getDeclaringClass();
       }
      else {
         JcompScope curscp = null;
         for (ASTNode n = loc; n != null; n = n.getParent()) {
            curscp = JcompAst.getJavaScope(n);
            if (curscp != null) break;
          }
         if (curscp == null) return null;
         JcompSymbol sym = curscp.lookupVariable(name);
         if (sym == null) return null;
         slot = mthd.getLocalOffset(sym);
         ltyp = mthd.getLocalType(slot,ppt);
       }
      if (slot < 0 || ltyp == null) return null;
      rslt = for_control.findRefValue(ltyp,slot);
    }
   
   return rslt;
}




}	// end of class QueryVarQuery




/* end of QueryVarQuery.java */

