/********************************************************************************/
/*                                                                              */
/*              FaitError.java                                                  */
/*                                                                              */
/*      Simple representation of an error                                       */
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



package edu.brown.cs.fait.iface;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class FaitError implements FaitConstants, IfaceError
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ErrorLevel error_level;
private String error_message;
private IfaceSafetyCheck safety_check;
private IfaceSubtype sub_type;
private Object error_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public FaitError(IfaceSubtype st,ErrorLevel lvl,String msg) 
{
   sub_type = st;
   safety_check = null;
   error_level = lvl;
   error_message = msg;
   error_data = null;
}



public FaitError(IfaceSafetyCheck sc,ErrorLevel lvl,String msg) 
{
   sub_type = null;
   safety_check = sc;
   error_level = lvl;
   error_message = msg;
   error_data = null;
}
  

public FaitError(ErrorLevel lvl,String msg) 
{
   sub_type = null;
   safety_check = null;
   error_level = lvl;
   error_message = msg;
   error_data = null;
}



public FaitError(IfaceError err,int stack)
{
   sub_type = err.getSubtype();
   safety_check = err.getSafetyCheck();
   error_level = err.getErrorLevel();
   error_message = err.getErrorMessage();
   error_data = stack;
}


public FaitError(IfaceError err,IfaceSafetyCheck.Value v)
{
   sub_type = err.getSubtype();
   safety_check = err.getSafetyCheck();
   error_level = err.getErrorLevel();
   error_message = err.getErrorMessage();
   error_data = v;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public ErrorLevel getErrorLevel()             { return error_level; }

@Override public String getErrorMessage()               { return error_message; }

@Override public IfaceSafetyCheck getSafetyCheck()      { return safety_check; }

@Override public IfaceSubtype getSubtype()              { return sub_type; }

@Override public int getStackLocation()              
{
   if (error_data == null || !(error_data instanceof Number)) return -1;
   Number n = (Number) error_data;
   return n.intValue();
}

@Override public IfaceSafetyCheck.Value getSafetyValue()
{
   if (error_data == null || !(error_data instanceof IfaceSafetyCheck.Value)) return null;
   return (IfaceSafetyCheck.Value) error_data;
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void outputXml(IfaceProgramPoint ppt,IvyXmlWriter xw) 
{
   xw.begin("ERROR");
   if (safety_check != null) {
      xw.field("SAFETY",safety_check.getName());
    }
   if (sub_type != null) {
      xw.field("SUBTYPE",sub_type.getName());
    }
   xw.field("LEVEL",error_level);
   xw.field("HASHCODE",hashCode());
   if (error_data != null) xw.field("DATA",error_data);
   xw.textElement("MESSAGE",error_message);
   if (ppt != null) ppt.outputXml(xw);
   xw.end("ERROR");
}




@Override public String toString() 
{
   StringBuffer buf = new StringBuffer();
   buf.append("ERR:");
   buf.append(error_level.toString().charAt(0));
   buf.append(":");
   buf.append(error_message);
   return buf.toString();
}


}       // end of class FaitError




/* end of FaitError.java */

