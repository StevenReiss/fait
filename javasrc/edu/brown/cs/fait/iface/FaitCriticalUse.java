/********************************************************************************/
/*                                                                              */
/*              FaitCriticalUse.java                                            */
/*                                                                              */
/*      Information about the critical use of a method                          */
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



package edu.brown.cs.fait.iface;

import java.util.HashSet;
import java.util.Set;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class FaitCriticalUse
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<IfaceSafetyCheck>   safety_checks;
private Set<IfaceSubtype>       subtype_checks;
private Set<IfaceSafetyCheck>   indirect_safety_checks;
private Set<IfaceSubtype>       indirect_subtype_checks;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public FaitCriticalUse()
{
   safety_checks = new HashSet<>();
   subtype_checks = new HashSet<>();
   indirect_safety_checks = new HashSet<>();
   indirect_subtype_checks = new HashSet<>();
}


/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

public void addSafetyCheck(IfaceSafetyCheck sc)
{
   safety_checks.add(sc);
}

public void addSubtype(IfaceSubtype st)
{       
   subtype_checks.add(st);
}


public boolean noteCaller(FaitCriticalUse fcu)
{
   boolean chng = false;
   for (IfaceSafetyCheck sc : fcu.safety_checks) {
      if (indirect_safety_checks.add(sc)) chng = true;
    }
   for (IfaceSafetyCheck sc : fcu.indirect_safety_checks) {
      if (indirect_safety_checks.add(sc)) chng = true;
    }
   for (IfaceSubtype st : fcu.subtype_checks) {
      if (indirect_subtype_checks.add(st)) chng = true;
    }
   for (IfaceSubtype st : fcu.indirect_subtype_checks) {
      if (indirect_subtype_checks.add(st)) chng = true;
    }
   
   return chng;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public boolean isCriticalUse()
{
   return safety_checks.size() > 0 || subtype_checks.size() > 0;
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   xw.begin("USES");
   for (IfaceSafetyCheck sc : safety_checks) {
      xw.begin("SAFETY");
      xw.field("NAME",sc.getName());
      xw.end("SAFETY");
    }
   for (IfaceSubtype st : subtype_checks) {
      xw.begin("SUBTYPE");
      xw.field("NAME",st.getName());
      xw.end("SUBTYPE");
   }
   for (IfaceSafetyCheck sc : indirect_safety_checks) {
      xw.begin("SAFETY");
      xw.field("CALLER",true);
      xw.field("NAME",sc.getName());
      xw.end("SAFETY");
    }
   for (IfaceSubtype st : indirect_subtype_checks) {
      xw.begin("SUBTYPE");
      xw.field("CALLER",true);
      xw.field("NAME",st.getName());
      xw.end("SUBTYPE");
    }
   xw.end("USES");
}




}       // end of class FaitCriticalUse




/* end of FaitCriticalUse.java */

