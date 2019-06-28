/********************************************************************************/
/*                                                                              */
/*              QueryReflection.java                                            */
/*                                                                              */
/*      Handle reflection usage queries                                         */
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceSpecial;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryReflection implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceControl    fait_control;
private IvyXmlWriter    xml_writer;
private Set<IfaceProgramPoint> done_points;
private List<IfaceProgramPoint> work_list;
private IfaceCall       for_call;
private Set<IfaceCall>  found_reflects;
private Set<IfaceCall>  done_reflects;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryReflection(IfaceControl ctrl,IvyXmlWriter xw)
{
   fait_control = ctrl;
   xml_writer = xw;
   done_points = null;
   work_list = null;
   for_call = null;
   found_reflects = null;
   done_reflects = null;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void processCalls()
{
   found_reflects = new HashSet<>();
   done_reflects = new HashSet<>();
   
   Set<String> checks = new HashSet<>();
   checks.add("java.lang.reflect.Constructor.newInstance");
   checks.add("java.lang.reflect.Method.invoke");
   checks.add("java.lang.Class.newInstance");
      
   int level = 0;
   for (IfaceCall c : fait_control.getAllCalls()) {
      for (IfaceCall c1 : c.getAlternateCalls()) {
        processCall(c1,checks,level);
       }
    }
   
   while (!found_reflects.isEmpty()) {
      ++level;
      done_reflects.addAll(found_reflects);
      checks.clear();
      for (IfaceCall c : found_reflects) {
         checks.add(c.getMethod().getFullName());
       }
      found_reflects.clear();
      for (IfaceCall c : fait_control.getAllCalls()) {
         for (IfaceCall c1 : c.getAlternateCalls()) {
            processCall(c1,checks,level);
          }
       }
    }
   
   found_reflects = null;
   done_reflects = null;
}



void processCall(IfaceCall c,Set<String> checks,int level) 
{
   done_points = new HashSet<>();
   work_list = new LinkedList<>();
   for_call = c;

   for (IfaceState rst : c.getReturnStates()) {
      workOn(rst);
    }
   
   while (!work_list.isEmpty()) {
      IfaceProgramPoint pt0 = work_list.remove(0);
      if (done_points.add(pt0)) {
         processProgramPoint(pt0,checks,level);
       }
    }
   
   done_points = null;
   work_list = null;
   for_call = null;
}


private void workOn(IfaceState st)
{
   IfaceLocation loc = st.getLocation();
   if (loc == null) {
      for (int i = 0; i < st.getNumPriorStates(); ++i) {
         IfaceState prior = st.getPriorState(i);
         workOn(prior);
       }
    }
   else {
      if (loc.getCall() != for_call) return;
      IfaceProgramPoint pt = loc.getProgramPoint();
      if (pt == null) return;
      if (!done_points.contains(pt)) work_list.add(pt);
    }
}



private void addPriorStates(IfaceProgramPoint pt)
{
   IfaceState st = fait_control.findStateForLocation(for_call,pt);
   for (int i = 0; i < st.getNumPriorStates(); ++i) {
      IfaceState prior = st.getPriorState(i);
      workOn(prior);
    }
}



private void processProgramPoint(IfaceProgramPoint ipp,Set<String> checks,int level)
{
   addPriorStates(ipp);
   
   IfaceMethod called = ipp.getCalledMethod();
   if (called == null) return;
   String nm = called.getFullName();
   IfaceSpecial sp = fait_control.getCallSpecial(ipp,called);
   if (checks.contains(nm)) {
      xml_writer.begin("REFLECT");
      xml_writer.field("LEVEL",level);
      outputCallData();
      xml_writer.field("RETURN",called.getReturnType().getJavaType().getName());
      xml_writer.field("METHOD",nm);
      ipp.outputXml(xml_writer);
      if (sp != null) sp.outputXml(xml_writer);
      boolean fg = outputCastInformation(ipp);
      if (fg) {
         if (for_call.getMethod().getReturnType().isJavaLangObject()) {
            if (done_reflects.add(for_call)) {
               found_reflects.add(for_call);
             }
          }
       }
      xml_writer.end("REFLECT");
    }
   else if (sp != null && level == 0) {
      IfaceValue iv = sp.getReturnValue(ipp,called);
      if (iv != null && 
            iv.getDataType().getJavaType() != called.getReturnType().getJavaType()) {
         xml_writer.begin("REPLACE");
         outputCallData();
         xml_writer.field("METHOD",nm);
         xml_writer.field("RETURN",called.getReturnType().getJavaType().getName());
         ipp.outputXml(xml_writer);
         sp.outputXml(xml_writer);
         xml_writer.end("REPLACE");
       }
    }
}



private void outputCallData()
{
   xml_writer.field("CALL",for_call.getMethod().getFullName());
   xml_writer.field("FILE",for_call.getMethod().getFile());
   xml_writer.field("DESCRIPTION",for_call.getMethod().getDescription());
   xml_writer.field("CALLID",for_call.hashCode());
   if (fait_control.isInProject(for_call.getMethod())) {
      xml_writer.field("INPROJECT",true);
      xml_writer.field("SOURCE",fait_control.getSourceFile(for_call.getMethod()));
    }
}




/********************************************************************************/
/*                                                                              */
/*      Try to get cast information                                             */
/*                                                                              */
/********************************************************************************/

private boolean outputCastInformation(IfaceProgramPoint pt0)
{
   Set<IfaceProgramPoint> done = new HashSet<>();
   IfaceType casttype = null;
   IfaceProgramPoint pt = pt0;
   int where = 0;               // top of stack
   int wherevar = -1;
   boolean canreturn = false;
   if (pt.getAstReference() == null) {
      IfaceProgramPoint apt = null;
      while (casttype == null && (where >= 0 || wherevar >= 0) && pt != null) {
         IfaceProgramPoint npt = pt.getNext();
         if (apt != null) {
            npt = apt;
            apt = null;
          }
         if (npt == null) break;
         done.add(npt);
         JcodeInstruction ins = npt.getInstruction();
         switch (ins.getOpcode()) {
            case JcodeConstants.CHECKCAST :
               if (where == 0) {
                  casttype = npt.getReferencedType();
                }
               break;
            case JcodeConstants.ASTORE :
            case JcodeConstants.ASTORE_0 :
            case JcodeConstants.ASTORE_1 :
            case JcodeConstants.ASTORE_2 :
            case JcodeConstants.ASTORE_3 :
                if (where == 0) {
                   where = -1;
                   wherevar = ins.getLocalVariable();
                 }
                break;
            case JcodeConstants.ALOAD :
            case JcodeConstants.ALOAD_0 :
            case JcodeConstants.ALOAD_1 :
            case JcodeConstants.ALOAD_2 :
            case JcodeConstants.ALOAD_3 : 
               if (wherevar == ins.getLocalVariable()) {
                  where = 0;
                }
               break;
            case JcodeConstants.GOTO :
               apt = npt.getReferencedTarget();
               if (done.contains(apt)) apt = null;
               break;
            case JcodeConstants.IF_ACMPEQ :
            case JcodeConstants.IF_ACMPNE :
            case JcodeConstants.IF_ICMPEQ :
            case JcodeConstants.IF_ICMPNE :
            case JcodeConstants.IF_ICMPGT :
            case JcodeConstants.IF_ICMPLE :
            case JcodeConstants.IF_ICMPLT :
            case JcodeConstants.IF_ICMPGE:
            case JcodeConstants.IFEQ : 
            case JcodeConstants.IFGE :
            case JcodeConstants.IFGT :
            case JcodeConstants.IFLE : 
            case JcodeConstants.IFLT : 
            case JcodeConstants.IFNE : 
            case JcodeConstants.IFNONNULL :
            case JcodeConstants.IFNULL :
               IfaceProgramPoint xpt = npt.getNext();
               if (done.contains(xpt)) {
                  apt = npt.getReferencedTarget();
                  if (done.contains(apt)) apt = null;
                  npt = null;
                }
               break;
            case JcodeConstants.ARETURN :
               if (where == 0) canreturn = true;
               where = -1;
               wherevar = -1;
               npt = null;
               break;
            default :
               if (where >= 0) where -= ins.getStackPop();
               if (where >= 0) where += ins.getStackPush();
               break;
            // might want to be more sophisticated re stack location
            // or just punt on something complex
          }
         pt = npt;
       }   
    }
   
   if (casttype != null) {
      xml_writer.begin("CAST");
      xml_writer.field("TYPE",casttype.getName());
      xml_writer.field("INTERFACE",casttype.isInterfaceType());
      xml_writer.field("ABSTRACT",casttype.isAbstract());
      xml_writer.field("USABLE",fait_control.canClassBeUsed(casttype));
      List<IfaceType> children = casttype.getChildTypes();
      if (children != null) {
         for (IfaceType ctype : children) {
            xml_writer.begin("CHILD");
            xml_writer.field("TYPE",ctype.getName());
            xml_writer.field("INTERFACE",ctype.isInterfaceType());
            xml_writer.field("ABSTRACT",ctype.isAbstract());
            xml_writer.field("USABLE",fait_control.canClassBeUsed(ctype));
            xml_writer.end("CHILD");
          }
       }
      xml_writer.end("CAST");
    }
   
   return canreturn;
}



}       // end of class QueryReflection




/* end of QueryReflection.java */

