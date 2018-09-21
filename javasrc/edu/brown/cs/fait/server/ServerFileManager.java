/********************************************************************************/
/*                                                                              */
/*              ServerFileManager.java                                          */
/*                                                                              */
/*      File Manager for open files                                             */
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.ivy.xml.IvyXml;

class ServerFileManager implements ServerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<File,ServerFile>	known_files;
private ServerMain		sesame_control;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ServerFileManager(ServerMain sm)
{
   sesame_control = sm;
   known_files = new HashMap<File,ServerFile>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

ServerFile openFile(File f)
{
   synchronized (known_files) {
      ServerFile sf = known_files.get(f);
      if (sf != null) return sf;
    }
   
   Map<String,Object> args = new HashMap<String,Object>();
   args.put("FILE",f.getPath());
   args.put("CONTENTS",Boolean.TRUE);
   
   Element filerslt = sesame_control.getXmlReply("STARTFILE",null,args,null,0);
   if (!IvyXml.isElement(filerslt,"RESULT")) {
      FaitLog.logE("Can't open file " + f);
      return null;
    }
   String linesep = IvyXml.getAttrString(filerslt,"LINESEP");
   byte [] data = IvyXml.getBytesElement(filerslt,"CONTENTS");
   if (data == null) return null;
   
   String cnts = new String(data);
   
   FaitLog.logD("File " + f + " started");
   
   synchronized (known_files) {
      ServerFile sf = known_files.get(f);
      if (sf == null) {
	 sf = new ServerFile(f,cnts,linesep);
	 known_files.put(f,sf);
       }
      return sf;
    }
}


void closeFile(File f)
{
   synchronized (known_files) {
      known_files.remove(f);
    }
}



/********************************************************************************/
/*										*/
/*	File Action methods							*/
/*										*/
/********************************************************************************/

ServerFile handleEdit(File f,int len,int offset,boolean complete,String txt)
{
   ServerFile sf = known_files.get(f);
   if (sf == null) return null;
   if (complete && txt == null) {
      closeFile(f);
      return null;
    }
    
   sf.editFile(len,offset,txt,complete);
   
   return sf;
}








}       // end of class ServerFileManager




/* end of ServerFileManager.java */

