/********************************************************************************/
/*                                                                              */
/*              TestgenFactory.java                                             */
/*                                                                              */
/*      Factory class for test case generation from flow analysis               */
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

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitException;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class TestgenFactory implements TestgenConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceControl    fait_control;
private List<TestgenLocation> target_locations;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public TestgenFactory(IfaceControl ctrl)
{
   fait_control = ctrl;
}


/********************************************************************************/
/*                                                                              */
/*      Test case generation request                                            */
/*                                                                              */
/********************************************************************************/

public void generateTestCase(Element xml,IvyXmlWriter xw) throws FaitException
{
   Element qxml = IvyXml.getChild(xml,"QUERY");
   Element exml = IvyXml.getChild(qxml,"ERROR");
   String mthd = IvyXml.getAttrString(qxml,"METHOD");
   String msgn = IvyXml.getAttrString(qxml,"SIGNATURE");
   int idx = mthd.lastIndexOf(".");
   String mcls = mthd.substring(0,idx);
   String mnam = mthd.substring(idx+1);
   IfaceMethod fm = fait_control.findMethod(mcls,mnam,msgn);
   
   IfaceCall call = null;
   int cid = IvyXml.getAttrInt(qxml,"CALL");
   for (IfaceCall c : fait_control.getAllCalls(fm)) {
      for (IfaceCall c1 : c.getAlternateCalls()) {
	 if (cid == 0 || cid == c1.hashCode()) {
	    call = c1;
	    break;
	  }
	 else if (call == null) call = c1;
       }
    }
   IfaceError error = null;
   IfaceProgramPoint startpt = null;
   int errid = IvyXml.getAttrInt(exml,"HASHCODE");
   outer :
      for (IfaceProgramPoint pt : call.getErrorLocations()) {
       for (IfaceError er : call.getErrors(pt)) {
          if (er.hashCode() == errid) {
             error = er;
             startpt = pt;
             break outer;
           }
        }
    }
   
   target_locations = new ArrayList<>();
   Element path = IvyXml.getChild(xml,"PATH");
   for (Element nxml : IvyXml.children(path,"NODE")) {
      TestgenLocation loc = new TestgenLocation(fait_control,nxml);
      target_locations.add(loc);
    }
   
   FaitLog.logI("Generate test case " + IvyXml.convertXmlToString(xml) + " " +
        error + " " + startpt);
   
   TestgenConstraint initcnst = 
      TestgenConstraint.createConstraint(fait_control,call,startpt,error);
   
   TestgenState initstate = new TestgenState(call,startpt,target_locations,initcnst);
   
   throw new FaitException("Not Implemented Yet " + initstate);
}



}       // end of class TestgenFactory




/* end of TestgenFactory.java */

