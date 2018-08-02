/********************************************************************************/
/*                                                                              */
/*              FlowAstStatus.java                                              */
/*                                                                              */
/*      Return status for AST simulation                                        */
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



package edu.brown.cs.fait.flow;import edu.brown.cs.fait.iface.IfaceAstStatus;
import edu.brown.cs.fait.iface.IfaceValue;

class FlowAstStatus implements IfaceAstStatus {


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  status_message;
private Reason  status_reason;
private IfaceValue status_value;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowAstStatus(Reason r,String msg)
{
   status_reason = r;
   status_message = msg;
   status_value = null;
}


FlowAstStatus(Reason r,IfaceValue v)
{
   status_reason = r;
   status_message = null;
   status_value = v;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getMessage()                          
{
   return status_message;
}


@Override public Reason getReason()
{
   return status_reason;
}


@Override public IfaceValue getValue()
{
   return status_value;
}



/********************************************************************************/
/*                                                                              */
/*      Debugging methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append(status_reason.toString());
   buf.append("[");
   if (status_message != null) buf.append(status_message);
   if (status_value != null) {
      if (status_message != null) buf.append(":");
      buf.append(status_value.toString());
    }
   buf.append("]");
   return buf.toString();
}




}       // end of class FlowAstStatus




/* end of FlowAstStatus.java */

