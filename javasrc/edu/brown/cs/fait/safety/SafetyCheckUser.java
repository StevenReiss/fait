/********************************************************************************/
/*                                                                              */
/*              SafetyCheckUser.java                                            */
/*                                                                              */
/*      User defined safety check                                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
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



package edu.brown.cs.fait.safety;

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitError;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.ivy.xml.IvyXml;

class SafetyCheckUser extends SafetyCheck
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SafetyCheckUser(Element xml)
{
   super(IvyXml.getAttrString(xml,"NAME"));
   loadCheck(xml);
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void loadCheck(Element xml)
{
   String first = null;
   boolean haveinit = false;;
   for (Element selt : IvyXml.children(xml,"STATE")) {
      String name = IvyXml.getAttrString(selt,"NAME");
      boolean init = IvyXml.getAttrBool(selt,"INITIAL");
      defineState(name,init);
      if (first == null) first = name;
      haveinit |= init;
    }
   if (!haveinit && first != null) defineState(first,true);
   
   for (Element selt : IvyXml.children(xml,"STATE")) {
      String name = IvyXml.getAttrString(selt,"NAME");
      Value v = defineState(name,false);
      for (Element telt : IvyXml.children(selt,"ON")) {
         String evtnm = IvyXml.getAttrString(telt,"EVENT");
         String tonm = IvyXml.getAttrString(telt,"GOTO");
         IfaceError ierr = null;
         String err = IvyXml.getTextElement(telt,"ERROR");
         String warn = IvyXml.getTextElement(telt,"WARNING");
         String note = IvyXml.getTextElement(telt,"NOTE");
         if (err != null) ierr = new FaitError(this,ErrorLevel.ERROR,err);
         else if (warn != null) ierr = new FaitError(this,ErrorLevel.WARNING,warn);
         else if (note != null) ierr = new FaitError(this,ErrorLevel.NOTE,note);
         Value tv = defineState(tonm,false);
         defineTransition(v,evtnm,tv,ierr);
       }
      Element delt = IvyXml.getChild(selt,"ELSE");
      if (delt == null) defineDefault(v,v);
      else {
         String tonm = IvyXml.getAttrString(delt,"GOTO");
         Value tv = null;
         if (tonm.equals("*")) tv = v;
         else tv = defineState(tonm,false);
         defineDefault(v,tv);
       }
    }
}


}       // end of class SafetyCheckUser




/* end of SafetyCheckUser.java */

