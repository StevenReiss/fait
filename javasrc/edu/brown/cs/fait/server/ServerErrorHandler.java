/********************************************************************************/
/*                                                                              */
/*              ServerErrorHandler.java                                         */
/*                                                                              */
/*      Track errors in the code                                                */
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



package edu.brown.cs.fait.server;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.ivy.xml.IvyXml;

class ServerErrorHandler implements ServerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,ProblemState>     current_errors;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ServerErrorHandler(ServerMain sm)
{
   current_errors = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean isErrorFree()
{
   return current_errors.isEmpty();
}


int errorCount()
{
   int ct = 0;
   for (ProblemState ps : current_errors.values()) {
      ct += ps.getNumErrors();
    }
   return ct;
}



/********************************************************************************/
/*                                                                              */
/*      Handle error messages from the back end                                 */
/*                                                                              */
/********************************************************************************/

boolean handleErrors(String proj,String forfile,Element ep)
{
   boolean haderrs = !current_errors.isEmpty();
   
   ProblemState ps = new ProblemState(ep);
   if (ps.getNumErrors() == 0) current_errors.remove(forfile);
   else current_errors.put(forfile,ps);
   
   FaitLog.logI("Error state " + errorCount() + " " + isErrorFree() + " " + 
         current_errors.size());
   
   if (haderrs && current_errors.isEmpty()) return true;
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Problem State for a file                                                */
/*                                                                              */
/********************************************************************************/

private static class ProblemState {
   
   private int num_errors;
   
   ProblemState(Element ep) {
      num_errors = 0;
      for (Element e : IvyXml.children(ep,"PROBLEM")) {
         if (IvyXml.getAttrBool(e,"ERROR")) ++num_errors;
       }
    }
   
   int getNumErrors()                           { return num_errors; }
   
}       // end of inner class ProblemState



}       // end of class ServerErrorHandler




/* end of ServerErrorHandler.java */

