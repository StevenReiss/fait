/********************************************************************************/
/*										*/
/*		QueryCritical.java						*/
/*										*/
/*	description of class							*/
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.fait.iface.FaitCriticalUse;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceSafetyCheck;
import edu.brown.cs.fait.iface.IfaceSafetyStatus;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryCritical implements QueryConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IvyXmlWriter xml_writer;
private IfaceControl fait_control;
private Map<IfaceMethod,FaitCriticalUse> method_uses;
private Set<IfaceProgramPoint> done_points;
private List<IfaceProgramPoint> work_list;
private IfaceCall for_call;
private Set<IfaceSubtype> ignore_subtypes;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

QueryCritical(IfaceControl ctrl,String ignores,IvyXmlWriter xw)
{
   fait_control = ctrl;
   xml_writer = xw;
   method_uses = null;
   done_points = null;
   work_list = null;
   for_call = null;

   ignore_subtypes = new HashSet<>();
   if (ignores == null) ignores = "CheckNullness ";
   for (IfaceSubtype ist : fait_control.getAllSubtypes()) {
      if (ignores.contains(ist.getName())) {
	 ignore_subtypes.add(ist);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void process()
{
   method_uses = new HashMap<>();
   for (IfaceCall c : fait_control.getAllCalls()) {
      for (IfaceCall c1 : c.getAlternateCalls()) {
	 analyzeCall(c1);
       }
    }

   processCallers();

   xml_writer.begin("CRITICAL");
   for (Map.Entry<IfaceMethod,FaitCriticalUse> ent : method_uses.entrySet()) {
      IfaceMethod im = ent.getKey();
      xml_writer.begin("METHOD");
      xml_writer.field("NAME",im.getFullName());
      xml_writer.field("INPROJECT",fait_control.isInProject(im));
      xml_writer.field("DESCRIPTION",im.getDescription());
      ent.getValue().outputXml(xml_writer);
      xml_writer.end("METHOD");
    }
   xml_writer.end("CRITICAL");

   method_uses = null;
}



/********************************************************************************/
/*										*/
/*	Call analysis methods							*/
/*										*/
/********************************************************************************/

private void analyzeCall(IfaceCall c)
{
   done_points = new HashSet<>();
   work_list = new LinkedList<>();
   for_call = c;

   IfaceState s0 = c.getStartState();
   IfaceSafetyStatus sts0 = s0.getSafetyStatus();
   if (sts0 == null) sts0 = fait_control.getInitialSafetyStatus();
   for (IfaceState s1 : c.getReturnStates()) {
      IfaceSafetyStatus sts1 = s1.getSafetyStatus();
      for (IfaceSafetyCheck chk : fait_control.getAllSafetyChecks()) {
	 if (sts1 == null) sts1 = fait_control.getInitialSafetyStatus();
	if (!sts1.getValue(chk).equals(sts1.getValue(chk))) {
	    addCriticalCall(c,chk);
	  }
       }
    }

   List<IfaceAnnotation> annots = c.getMethod().getReturnAnnotations();
   checkAnnotations(c,annots);

   int nparm = c.getMethod().getNumArgs();
   int lclct = 0;
   Map<IfaceSubtype,IfaceSubtype.Value> defaults = new HashMap<>();
   for (int i = 0; i < nparm; ++i) {
      checkAnnotations(c,c.getMethod().getArgAnnotations(i));
      IfaceValue v0 = s0.getLocal(lclct);
      IfaceType t0 = v0.getDataType();
      if (t0.isCategory2()) ++lclct;
      for (IfaceSubtype ist : fait_control.getAllSubtypes()) {
	 if (ignore_subtypes.contains(ist)) continue;
	 IfaceSubtype.Value stv0 = t0.getValue(ist);
	 IfaceSubtype.Value dflt = ist.getDefaultTypeValue(t0);
	 IfaceSubtype.Value merge = ist.getMergeValue(dflt,stv0);
	 if (defaults.get(ist) != null) {
	    merge = ist.getMergeValue(defaults.get(ist),merge);
	  }
	 defaults.put(ist,merge);
       }
      ++lclct;
    }
   if (c.getResultValue() != null && !c.getResultValue().getDataType().isVoidType()) {
      IfaceValue vr = c.getResultValue();
      IfaceType tr = vr.getDataType();
      for (IfaceSubtype ist : fait_control.getAllSubtypes()) {
	 if (ignore_subtypes.contains(ist)) continue;
	 IfaceSubtype.Value stvr = tr.getValue(ist);
	 IfaceSubtype.Value dflt = ist.getDefaultTypeValue(tr);
	 IfaceSubtype.Value merge = ist.getMergeValue(stvr,dflt);
	 if (merge == dflt) continue;
	 IfaceSubtype.Value maxv = defaults.get(ist);
	 if (maxv != null) {
	    IfaceSubtype.Value nmerge = ist.getMergeValue(merge,maxv);
	    if (nmerge == maxv) continue;
	    addCriticalType(c,ist);
	  }

       }
    }

   for (IfaceState rst : c.getReturnStates()) workOn(rst);

   while (!work_list.isEmpty()) {
      IfaceProgramPoint pt0 = work_list.remove(0);
      if (done_points.add(pt0)) {
	 analyzeProgramPoint(pt0);
       }
    }

   done_points = null;
   work_list = null;
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
   if (st == null) {
      FaitLog.logE("Can't find state for call " + for_call + " at " + pt);
      return;
    }
   for (int i = 0; i < st.getNumPriorStates(); ++i) {
      IfaceState prior = st.getPriorState(i);
      workOn(prior);
    }
}



private void analyzeProgramPoint(IfaceProgramPoint ipp)
{
   addPriorStates(ipp);

   String evt = null;
   IfaceMethod im = ipp.getCalledMethod();

   if (im != null) {
      evt = fait_control.getEventForCall(im,null,null);
    }

   IfaceState stp = fait_control.findStateForLocation(for_call,ipp);
   IfaceState st0 = for_call.getStartState();
   IfaceSafetyStatus startstatus = st0.getSafetyStatus();
   IfaceSafetyStatus pointstatus = stp.getSafetyStatus();
   if (startstatus == null && pointstatus == null) return;
   if (startstatus == null) startstatus = fait_control.getInitialSafetyStatus();
   if (pointstatus == null) pointstatus = fait_control.getInitialSafetyStatus();
   for (IfaceSafetyCheck chk : fait_control.getAllSafetyChecks()) {
      if (!startstatus.getValue(chk).equals(pointstatus.getValue(chk))) {
	 addCriticalCall(for_call,chk);
       }
      if (evt != null && chk.isRelevant(evt)) addCriticalCall(for_call,chk);
    }

   IfaceAnnotation [] annots = fait_control.getAnnotations(ipp);
   if (annots != null) {
      for (IfaceSubtype ist : fait_control.getAllSubtypes()) {
	 for (int i = 0; i < annots.length; ++i) {
	    if (ist.isAnnotationRelevant(annots[i])) {
	       addCriticalType(for_call,ist);
	       break;
	     }
	  }
       }
    }
   if (im != null) {
      checkAnnotations(for_call,im.getReturnAnnotations());
      for (int i = 0; i < im.getNumArgs(); ++i) {
	 checkAnnotations(for_call,im.getArgAnnotations(i));
       }
    }
   IfaceMethod im1 = ipp.getReferencedMethod();
   if (im1 != null && im1 != im) {
      checkAnnotations(for_call,im1.getReturnAnnotations());
      for (int i = 0; i < im1.getNumArgs(); ++i) {
	 checkAnnotations(for_call,im1.getArgAnnotations(i));
       }
    }
   IfaceField ifld = ipp.getReferencedField();
   if (ifld != null) {
      checkAnnotations(for_call,ifld.getAnnotations());
    }

   // now go through stack data for subtype changes
}




/********************************************************************************/
/*										*/
/*	Methods to mark callers of critical routines as critical		*/
/*										*/
/********************************************************************************/

private void processCallers()
{
   List<IfaceMethod> worklist = new LinkedList<>();
   for (IfaceMethod im : method_uses.keySet()) {
      worklist.add(im);
    }

   while (!worklist.isEmpty()) {
      IfaceMethod mthd = worklist.remove(0);
      FaitCriticalUse baseuses = method_uses.get(mthd);
      for (IfaceCall ic : fait_control.getAllCalls(mthd)) {
	 for (IfaceCall ic1 : ic.getAlternateCalls()) {
	    for (IfaceLocation loc : ic1.getCallSites()) {
	       IfaceMethod caller = loc.getMethod();
	       FaitCriticalUse fcu = method_uses.get(caller);
	       if (fcu == null) {
		  fcu = new FaitCriticalUse();
		  method_uses.put(caller,fcu);
		}
	       if (fcu.noteCaller(baseuses)) worklist.add(caller);
	     }
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to record critical information					*/
/*										*/
/********************************************************************************/

private void addCriticalCall(IfaceCall c,IfaceSafetyCheck chk)
{
   IfaceMethod im = c.getMethod();
   FaitCriticalUse crit = method_uses.get(im);
   if (crit == null) {
      crit = new FaitCriticalUse();
      method_uses.put(im,crit);
    }
   crit.addSafetyCheck(chk);
}


private void addCriticalType(IfaceCall c,IfaceSubtype sty)
{
   IfaceMethod im = c.getMethod();
   FaitCriticalUse crit = method_uses.get(im);
   if (crit == null) {
      crit = new FaitCriticalUse();
      method_uses.put(im,crit);
    }
   crit.addSubtype(sty);
}


private void checkAnnotations(IfaceCall c,List<IfaceAnnotation> annots)
{
   if (annots == null) return;

   for (IfaceSubtype ist : fait_control.getAllSubtypes()) {
      for (IfaceAnnotation an : annots) {
	 if (ist.isAnnotationRelevant(an)) {
	    addCriticalType(for_call,ist);
	    break;
	  }
       }
    }
}


}	// end of class QueryCritical




/* end of QueryCritical.java */

