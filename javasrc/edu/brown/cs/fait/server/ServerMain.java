/********************************************************************************/
/*                                                                              */
/*              ServerMain.java                                                 */
/*                                                                              */
/*      Main program for FAIT flow analysis server                              */
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
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public final class ServerMain implements ServerConstants, MintConstants 
{



/********************************************************************************/
/*                                                                              */
/*      Main Program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   ServerMain sm = new ServerMain(args);
   sm.process();
}




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String                  message_id;
private ServerFileManager       file_manager;
private ServerMonitor           message_monitor;
private ServerErrorHandler      error_handler;
private Map<String,ServerProject> project_map;

private static JcompControl     jcomp_base;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private ServerMain(String [] args)
{
   FaitLog.setup();
   
   message_id = null;
   project_map = new HashMap<>();
   jcomp_base = new JcompControl();
   
   scanArgs(args);
   
   file_manager = new ServerFileManager(this);
   message_monitor = new ServerMonitor(this);
   error_handler = new ServerErrorHandler(this);
}



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {           // -m <MINTID>
	    message_id = args[++i];
	  }
         else if (args[i].startsWith("-T")) {                           // -Trace
            FaitLog.setTracing(true);
          }
         else if (args[i].startsWith("-D")) {                           // -Debug
            FaitLog.setLogLevel(FaitLog.LogLevel.DEBUG);
          }             
         else if (args[i].startsWith("-O")) {                           // -Output
            FaitLog.useStdErr(true);
          }
         else if (args[i].startsWith("-L") && i+1 < args.length) {      // -L logfile
            FaitLog.setLogFile(new File(args[++i]));
          }
	 else badArgs();
       }
      else badArgs();
    }
   
   if (message_id == null) {
      message_id = System.getProperty("edu.brown.cs.bubbles.MINT");
      if (message_id == null) message_id = System.getProperty("edu.brown.cs.bubbles.mint");
      if (message_id == null) message_id = BOARD_MINT_NAME;
    }
}



private void badArgs()
{
   System.err.println("Server: ServerMain -m <message_id>");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

static JcompControl getJcompBase()		{ return jcomp_base; }

ServerFileManager getFileManager()		{ return file_manager; }


ServerMonitor getMonitor()			{ return message_monitor; }

String getMintId()				{ return message_id; }




/********************************************************************************/
/*										*/
/*	Project managment							*/
/*										*/
/********************************************************************************/

ServerProject getProject(String name) {
   if (name == null) return null;
   
   synchronized (project_map) {
      ServerProject sp = project_map.get(name);
      if (sp == null) {
	 sp = new ServerProject(this,name);
	 project_map.put(name,sp);
       }
      return sp;
    }
}



/********************************************************************************/
/*										*/
/*	Messaging methods							*/
/*										*/
/********************************************************************************/

void response(String cmd,CommandArgs args,String cnts,MintReply rply)
{
   message_monitor.sendCommand(cmd,args,cnts,rply);
}




String getStringReply(String cmd,String proj,Map<String,Object> flds,String cnts,long delay)
{
   MintDefaultReply rply = new MintDefaultReply();
   sendMessage(cmd,proj,flds,cnts,rply,MINT_MSG_FIRST_NON_NULL);
   String rslt = rply.waitForString(delay);
   
   FaitLog.logD("Reply: " + rslt);
   
   return rslt;
}


Element getXmlReply(String cmd,String proj,Map<String,Object> flds,String cnts,long delay)
{   
   MintDefaultReply rply = new MintDefaultReply();
   sendMessage(cmd,proj,flds,cnts,rply,MINT_MSG_FIRST_NON_NULL);
   Element rslt = rply.waitForXml(delay);
   
   FaitLog.logD("Reply: " + IvyXml.convertXmlToString(rslt));
   
   return rslt;
}





private void sendMessage(String cmd,String proj,Map<String,Object> flds,String cnts,
      MintReply rply,int fgs)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BUBBLES");
   xw.field("DO",cmd);
   xw.field("BID",SOURCE_ID);
   if (proj != null && proj.length() > 0) xw.field("PROJECT",proj);
   xw.field("LANG","Eclipse");
   if (flds != null) {
      for (Map.Entry<String,Object> ent : flds.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("BUBBLES");
   String msg = xw.toString();
   xw.close();
   
   FaitLog.logD("SEND: " + msg);
   
   message_monitor.sendMessage(msg,rply,fgs);
}


/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   message_monitor.startServer();
}


/********************************************************************************/
/*                                                                              */
/*      Error methods                                                           */
/*                                                                              */
/********************************************************************************/

boolean handleErrors(String proj,String file,Element message)
{
   return error_handler.handleErrors(proj,file,message);
}


boolean isErrorFree()
{
   return error_handler.isErrorFree();
}






}       // end of class ServerMain




/* end of ServerMain.java */

