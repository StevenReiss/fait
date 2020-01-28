/********************************************************************************/
/*                                                                              */
/*              TestenLocation.java                                             */
/*                                                                              */
/*      Program location that should be executed in test case                   */
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

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.ivy.xml.IvyXml;

@SuppressWarnings("unused")
class TestgenLocation implements TestgenConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String node_id;
private boolean is_start;
private boolean is_end;
private String method_name;
private String method_signature;
private int call_id;
private String source_file;
private int     start_offset;
private int     end_offset;
private int     line_number;
private int     ins_offset;
private String  after_id;
private int     after_start;
private IfaceMethod for_method;
private IfaceCall for_call;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TestgenLocation(IfaceControl fc,Element xml) 
{
   node_id = IvyXml.getAttrString(xml,"ID");
   is_start = IvyXml.getAttrBool(xml,"START");
   is_end = IvyXml.getAttrBool(xml,"END");
   method_name = IvyXml.getAttrString(xml,"METHOD");
   method_signature = IvyXml.getAttrString(xml,"SIGNATURE");
   int idx = method_name.lastIndexOf(".");
   for_method = fc.findMethod(method_name.substring(0,idx),
         method_name.substring(idx+1),method_signature);
   
   call_id = IvyXml.getAttrInt(xml,"CALLID");
   source_file = IvyXml.getAttrString(xml,"FILE");
   Element loc = IvyXml.getChild(xml,"POINT");
   start_offset = IvyXml.getAttrInt(loc,"START");
   end_offset = IvyXml.getAttrInt(loc,"END");
   after_id = IvyXml.getAttrString(loc,"AFTER");
   after_start = IvyXml.getAttrInt(loc,"AFTERSTART");
   ins_offset = IvyXml.getAttrInt(loc,"LOC");
   
   for_call = null;
   for (IfaceCall c : fc.getAllCalls(for_method)) {
      for (IfaceCall c1 : c.getAlternateCalls()) {
	 if (call_id == 0 || call_id == c1.hashCode()) {
	    for_call = c1;
	    break;
	  }
	 else if (for_call == null) for_call = c1;
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceCall getCall()                     { return for_call; }

boolean match(IfaceProgramPoint pt) 
{
   IfaceAstReference aref = pt.getAstReference();
   if (aref == null) {
      if (ins_offset == pt.getInstruction().getIndex()) return true;
      return false;
    }
   int soff = aref.getAstNode().getStartPosition();
   int eoff = soff + aref.getAstNode().getLength();
   if (soff == start_offset && eoff == end_offset) {
      if (aref.getAfterChild() == null) {
         if (after_id == null) return true;
       }
      else {
         String aid = aref.getAfterChild().getLocationInParent().getId();
         if (aid.equals(after_id)) {
            int aoff = aref.getAfterChild().getStartPosition();
            if (aoff == after_start) return true;
          }
       }
    }
   return false;
}


}       // end of class TestenLocation




/* end of TestenLocation.java */

