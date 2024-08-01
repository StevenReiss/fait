/********************************************************************************/
/*										*/
/*		SafetyFactory.java						*/
/*										*/
/*	Factory for automata-based safety conditions				*/
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



package edu.brown.cs.fait.safety;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceSafetyCheck;
import edu.brown.cs.fait.iface.IfaceSafetyStatus;
import edu.brown.cs.ivy.xml.IvyXml;

public final class SafetyFactory implements SafetyConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<SafetyCheck> all_checks;
private IfaceSafetyStatus initial_status;
private Map<IfaceSafetyStatus,IfaceSafetyStatus> all_status;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public SafetyFactory(IfaceControl ic)
{
   all_checks = new ArrayList<>();
   all_status = new HashMap<>();

   // add static safety checks here

   initial_status = this.getSafetyStatus(null);
}



/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

public IfaceSafetyStatus getInitialStatus()
{
   return initial_status;
}




public void addSpecialFile(Element xml)
{
   if (xml == null) return;

   for (Element s : IvyXml.children(xml,"SAFETY")) {
      SafetyCheckUser scu = new SafetyCheckUser(s);
      all_checks.add(scu);
    }

   initial_status = getSafetyStatus(null);
}



public void clearAllSpecials()
{
   all_checks.clear();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public List<IfaceSafetyCheck> getAllSafetyChecks()
{
   return new ArrayList<>(all_checks);
}



List<SafetyCheck> getAllChecks()
{
   return all_checks;
}

int getNumChecks()
{
   return all_checks.size();
}

SafetyCheck getCheck(int i)
{
   if (i < 0 || i >= all_checks.size()) return null;

   return all_checks.get(i);
}


/********************************************************************************/
/*										*/
/*	Unique safety status methods						*/
/*										*/
/********************************************************************************/

IfaceSafetyStatus getSafetyStatus(int [] sts)
{
   if (sts == null) {
      sts = new int [ all_checks.size() ];
      for (int i = 0; i < sts.length; ++i) {
	 IfaceSafetyCheck.Value v = all_checks.get(i).getInitialState();
	 sts[i] = 1 << v.ordinal();
       }
    }

   SafetyStatus st = new SafetyStatus(this,sts);
   IfaceSafetyStatus ost = all_status.putIfAbsent(st,st);
   if (ost != null) return ost;
   FaitLog.logD("Create new safety status " + st);
   return st;
}



}	// end of class SafetyFactory




/* end of SafetyFactory.java */

