/********************************************************************************/
/*                                                                              */
/*              ServerSession.java                                              */
/*                                                                              */
/*      Representation of a Bubbles session                                     */
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

import java.util.Random;

import org.w3c.dom.Element;


import edu.brown.cs.ivy.xml.IvyXml;

public class ServerSession implements ServerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ServerMain	sesame_control;
private String		session_id;
private ServerProject	for_project;
private int             num_thread;
private String          return_id;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ServerSession(ServerMain sm,String sid,Element xml)
{
   sesame_control = sm;
   
   String proj = IvyXml.getAttrString(xml,"PROJECT");
   
   for_project = null;
   if (proj == null) {
      Element pxml = sm.getXmlReply("PROJECTS",null,null,null,0);
      if (IvyXml.isElement(pxml,"RESULT")) {
         for (Element pelt : IvyXml.children(pxml,"PROJECT")) {
            String pnm = IvyXml.getAttrString(pelt,"NAME");
            if (for_project == null) for_project = sm.getProject(pnm);
            else for_project.addProject(pnm);
          }
       }
    }
   else for_project = sm.getProject(proj);
   
   if (for_project == null) return;
   
   num_thread = 0;
   return_id = null;
   
   if (sid == null) {
      Random r = new Random();
      sid = "SESAME_" + r.nextInt(10000000);
    }
   session_id = sid;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getSessionId()			{ return session_id; }

public ServerProject getProject()		{ return for_project; }

ServerMain getControl() 			{ return sesame_control; }

int getNumThread()                              { return num_thread; }
void setNumThread(int th)                       { num_thread = th; }

String getReturnId()                            { return return_id; }
void setReturnId(String s)                      { return_id = s; }


void noteFileChanged(ServerFile sf)
{
   for_project.noteFileChanged(sf,false,false);
}








}       // end of class ServerSession




/* end of ServerSession.java */

