/********************************************************************************/
/*                                                                              */
/*              ServerUpdateData.java                                           */
/*                                                                              */
/*      Handle determining what needs to be updated                             */
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

import java.util.ArrayList;
import java.util.List;


import edu.brown.cs.fait.iface.IfaceAstReference;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceUpdateSet;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSource;

class ServerUpdateData implements ServerConstants, IfaceUpdateSet
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<ServerFile>        update_files;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ServerUpdateData(List<ServerFile> files)
{
   update_files = new ArrayList<>(files);
}


/********************************************************************************/
/*                                                                              */
/*      Update checker                                                          */
/*                                                                              */
/********************************************************************************/

@Override public boolean shouldUpdate(IfaceCall call)
{
   IfaceMethod m = call.getMethod();
   IfaceProgramPoint pt = m.getStart();
   if (pt == null) return false;
   IfaceAstReference ar = pt.getAstReference();
   if (ar == null) {
      // TODO: if method is now AST-based, then update
      String cnm = m.getDeclaringClass().getName();
      if (cnm.startsWith("java.") ||
            cnm.startsWith("javax.") ||
            cnm.startsWith("sun.")) return false;
      IfaceMethod m0 = call.getControl().findMethod(m.getDeclaringClass().getName(),
            m.getName(),m.getDescription());
      if (m0 == m) return false;
      return true;               
    }
   
   JcompSource src = JcompAst.getSource(ar.getAstNode());
   if (update_files.contains(src)) return true;
   // might want to do method-level checks
   
   return false;
}



}       // end of class ServerUpdateData




/* end of ServerUpdateData.java */

