/********************************************************************************/
/*										*/
/*		ServerMonitor.java						*/
/*										*/
/*	Message interface for FAIT server					*/
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



package edu.brown.cs.fait.server;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.rmi.ServerException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.fait.control.ControlMain;
import edu.brown.cs.fait.iface.FaitException;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceDescriptionFile;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.mint.MintConstants.MintSyncMode;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class ServerMonitor implements ServerConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ServerMain		server_control;
private MintControl		mint_control;
private boolean 		is_done;
private Map<String,ServerSession> session_map;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ServerMonitor(ServerMain sm)
{
   server_control = sm;
   is_done = false;
   session_map = new HashMap<String,ServerSession>();

   mint_control = MintControl.create(sm.getMintId(),MintSyncMode.ONLY_REPLIES);
}



/********************************************************************************/
/*										*/
/*	Server methods								*/
/*										*/
/********************************************************************************/

void startServer()
{
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());
   mint_control.register("<BUBBLES DO='_VAR_0' />",new BubblesHandler());
   mint_control.register("<FAIT DO='_VAR_0' SID='_VAR_1' />",new CommandHandler());

   new WaitForExit().start();
}



private synchronized void serverDone()
{
   is_done = true;
   notifyAll();
}



private boolean checkEclipse()
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='PING' />";
   mint_control.send(msg,rply,MintConstants.MINT_MSG_FIRST_NON_NULL);
   String r = rply.waitForString(300000);
   FaitLog.logD("BUBBLES PING " + r);
   if (r == null) return false;
   return true;
}



private class WaitForExit extends Thread {

   WaitForExit() {
      super("WaitForExit");
    }

   @Override public void run() {
      ServerMonitor mon = ServerMonitor.this;
      synchronized (mon) {
         for ( ; ; ) {
            if (checkEclipse()) break;
            try {
               mon.wait(30000l);
             }
            catch (InterruptedException e) { }
          }
   
         while (!is_done) {
            if (!checkEclipse()) is_done = true;
            else {
               try {
                  mon.wait(30000l);
                }
               catch (InterruptedException e) { }
             }
          }
       }
   
      System.exit(0);
    }

}	// end of inner class WaitForExit



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void noteFileChanged(ServerFile sf)
{
   FaitLog.logD("MASTER: begin note changed");
   for (ServerSession ss : session_map.values()) {
      ss.noteFileChanged(sf);
    }
   FaitLog.logD("MASTER: end note changed");
}




/********************************************************************************/
/*										*/
/*	Sending methods 							*/
/*										*/
/********************************************************************************/

void sendMessage(String xml,MintReply rply,int fgs)
{
   mint_control.send(xml,rply,fgs);
}



void sendCommand(String cmd,CommandArgs args,String cnts,MintReply rply)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("FAITEXEC");
   xw.field("TYPE",cmd);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) xw.xmlText(cnts);
   xw.end("FAITEXEC");

   String msg = xw.toString();
   xw.close();

   FaitLog.logI("Send to Bubbles: " + msg);
   if (rply != null) {
      sendMessage(msg,rply,MintConstants.MINT_MSG_FIRST_REPLY);
    }
   else {
      sendMessage(msg,null,MintConstants.MINT_MSG_NO_REPLY);
    }
}




/********************************************************************************/
/*										*/
/*	File-related message handlers						*/
/*										*/
/********************************************************************************/

private void handleErrors(String proj,String file,Element messages)
{
   boolean chng = server_control.handleErrors(proj,file,messages);
   boolean errfree = server_control.isErrorFree();

   for (ServerSession ss : session_map.values()) {
      ServerProject sp = ss.getProject();
      if (sp != null) {
	 if (chng) {
	    sp.resumeAnalysis();
	  }
	 else if (!errfree) {
	    sp.pauseAnalysis();
	  }
       }
    }
}


private void handleEdit(MintMessage msg,String bid,File file,int len,int offset,
      boolean complete,boolean remove,String txt)
{
   if (!bid.equals(SOURCE_ID)) {
      if (msg != null) msg.replyTo();
      return;
    }

   FaitLog.logD("MASTER: Begin edit");

   ServerFile sf = server_control.getFileManager().handleEdit(file,len,offset,complete,txt);

   if (msg != null) msg.replyTo("<OK/>");

   FaitLog.logD("MASTER: EDIT ACCEPTED");

   if (sf != null) noteFileChanged(sf);

   FaitLog.logD("MASTER: End edit");
}



private void handleResourceChange(Element res)
{
   String k = IvyXml.getAttrString(res,"KIND");
   Element re = IvyXml.getChild(res,"RESOURCE");
   String rtyp = IvyXml.getAttrString(res,"TYPE");
   if (rtyp != null && rtyp.equals("FILE")) {
      String fp = IvyXml.getAttrString(re,"LOCATION");
      String proj = IvyXml.getAttrString(re,"PROJECT");
      switch (k) {
	 case "ADDED" :
	 case "ADDED_PHANTOM" :
	    break;
	 case "REMOVED" :
	 case "REMOVED_PHANTOM" :
	    break;
	 default :
	    FaitLog.logI("CHANGE FILE " + fp + " IN " + proj);
	   // TODO: remove old binary from jcode
	    break;
       }
    }
   // detect file saved.  This will come from the edit
}



private void handleResourceFiles(String sid,Element res,IvyXmlWriter xw)
{
   ServerSession ss = session_map.get(sid);
   if (ss == null) return;
   ServerProject proj = ss.getProject();
   xw.begin("FILES");
   for (IfaceDescriptionFile fn : proj.getDescriptionFiles()) {
      xw.begin("FILE");
      xw.field("NAME",fn.getFile().getAbsolutePath());
      xw.field("PRIORITY",fn.getPriority());
      if (fn.getPriority() == IfaceDescriptionFile.PRIORITY_BASE)
         xw.field("BASE",true);
      else if (fn.getPriority() == IfaceDescriptionFile.PRIORITY_BASE_PROJECT)
         xw.field("BASE_PROJECT",true);
      else if (fn.getPriority() == IfaceDescriptionFile.PRIORITY_DEPENDENT_PROJECT)
         xw.field("DEPENDENT_PROJECT",true);
      else if (fn.getPriority() == IfaceDescriptionFile.PRIORITY_LIBRARY)
         xw.field("LIBRARY",true);
      if (fn.getLibrary() != null) xw.field("LIBFILE",fn.getLibrary());
      xw.end("FILE");
    }
   List<File> basefiles = proj.getBaseDescriptionFiles();
   if (basefiles != null) {
      for (File f : basefiles) {
         xw.begin("FILE");
         if (f.getPath().startsWith("*FAIT*")) {
            String rnm = "/" + f.getName();
            URL fn = ControlMain.class.getResource(rnm);
            if (fn != null) {
               xw.field("URL",fn);
               xw.field("NAME",fn.getFile());
             }
          }
         else
            xw.field("NAME",f.getAbsolutePath());
         xw.field("PRIORITY",IfaceDescriptionFile.PRIORITY_BASE);
         xw.field("BASE",true);
         xw.end("FILE");
       }

    }
   xw.end("FILES");
}



/********************************************************************************/
/*										*/
/*	Methods to handle commands						*/
/*										*/
/********************************************************************************/

private void handleBegin(String sid,Element xml,IvyXmlWriter xw) throws ServerException
{
   ServerSession ss = new ServerSession(server_control,sid,xml);
   FaitLog.logD("BEGIN " + sid + " " + ss);
   xw.begin("SESSION");
   xw.field("ID",ss.getSessionId());
   xw.end();
   session_map.put(sid,ss);
}



private void handleRemove(String sid) throws ServerException
{
   ServerSession ss = session_map.remove(sid);
   if (ss == null) FaitLog.logE("Session " + sid + " not found");
}



private void handleAddFile(String sid,Element xml)
{
   ServerSession ss = session_map.get(sid);
   if (ss == null) return;
   ServerProject sp = ss.getProject();

   for (Element e : IvyXml.children(xml,"FILE")) {
      String file = IvyXml.getAttrString(e,"NAME");
      ServerFile sf = server_control.getFileManager().openFile(new File(file));
      if (sf != null) {
         sp.addFile(sf);
       }   
    }

   // sp.getJcompProject().resolve();
}



/********************************************************************************/
/*										*/
/*	Analysis requests							*/
/*										*/
/********************************************************************************/

private void handleAnalyze(String sid,Element xml,IvyXmlWriter xw)
{
   ServerSession ss = session_map.get(sid);
   if (ss == null) return;
   ServerProject sp = ss.getProject();
   int nth = IvyXml.getAttrInt(xml,"THREADS",ss.getNumThread());
   if (nth != 0) ss.setNumThread(nth);
   ReportOption opt = IvyXml.getAttrEnum(xml,"REPORT",ReportOption.FULL_STATS);
   String retid = IvyXml.getAttrString(xml,"ID");
   if (retid == null) {
      retid = sid;
    }
   ss.setReturnId(retid);

   xw.begin("ANALYSIS");
   xw.field("ID",retid);
   xw.end();

   sp.beginAnalysis(nth,retid,opt);
}


/********************************************************************************/
/*                                                                              */
/*      Query commands                                                          */
/*                                                                              */
/********************************************************************************/

private void handleQuery(String sid,Element xml,IvyXmlWriter xw)
{
   ServerSession ss = session_map.get(sid);
   if (ss == null) return;
   ServerProject sp = ss.getProject();
   
   try {
      sp.handleQuery(xml,xw);
    }
   catch (FaitException e) {
      xw.begin("FAITQUERY");
      xw.field("FAIL",true);
      xw.field("ERROR",e.getMessage());
      xw.end("FAITQUERY");
    }
}


private void handleReflection(String sid,Element xml,IvyXmlWriter xw)
{
   ServerSession ss = session_map.get(sid);
   if (ss == null) return;
   ServerProject sp = ss.getProject();
   
   try {
      sp.handleReflection(xml,xw);
    }
   catch (FaitException e) {
      xw.begin("FAITQUERY");
      xw.field("FAIL",true);
      xw.field("ERROR",e.getMessage());
      xw.end("FAITQUERY");
    }
}




private void handlePerformance(String sid,Element xml,IvyXmlWriter xw)
{
   ServerSession ss = session_map.get(sid);
   if (ss == null) return;
   ServerProject sp = ss.getProject();
   
   try {
      sp.handlePerformance(xml,xw);
    }
   catch (FaitException e) {
      xw.begin("FAITQUERY");
      xw.field("FAIL",true);
      xw.field("ERROR",e.getMessage());
      xw.end("FAITQUERY");
    }
}



private void handleFindCritical(String sid,Element xml,IvyXmlWriter xw)
{
   ServerSession ss = session_map.get(sid);
   if (ss == null) return;
   ServerProject sp = ss.getProject();
   
   try {
      sp.handleFindCritical(xml,xw);
    }
   catch (FaitException e) {
      xw.begin("FAITQUERY");
      xw.field("FAIL",true);
      xw.field("ERROR",e.getMessage());
      xw.end("FAITQUERY");
    }
}  



/********************************************************************************/
/*										*/
/*	Handle Messages from Eclipse						*/
/*										*/
/********************************************************************************/

private class EclipseHandler implements MintHandler {

@Override public void receive(MintMessage msg,MintArguments args) {
   String cmd = args.getArgument(0);
   Element e = msg.getXml();

   switch (cmd) {
      case "ELISION" :
	 return;
    }

   FaitLog.logD("Message from eclipse: " + cmd + " " + msg.getText());

   switch (cmd) {
      case "PING" :
      case "PING1" :
      case "PING2" :
      case "PING3" :
	 msg.replyTo("<PONG/>");
	 break;
      case "EDITERROR" :
      case "FILEERROR" :
	 handleErrors(IvyXml.getAttrString(e,"PROJECT"),
	       IvyXml.getAttrString(e,"FILE"),
	       IvyXml.getChild(e,"MESSAGES"));
	 break;
      case "EDIT" :
	 String bid = IvyXml.getAttrString(e,"BID");
	 if (!bid.equals(SOURCE_ID)) {
	    msg.replyTo();
	    break;
	  }
	 String txt = IvyXml.getText(e);
	 boolean complete = IvyXml.getAttrBool(e,"COMPLETE");
	 boolean remove = IvyXml.getAttrBool(e,"REMOVE");
	 if (complete) {
	    byte [] data = IvyXml.getBytesElement(e,"CONTENTS");
	    if (data != null) txt = new String(data);
	    else remove = true;
	  }
	 handleEdit(msg,bid,
	       new File(IvyXml.getAttrString(e,"FILE")),
	       IvyXml.getAttrInt(e,"LENGTH"),
	       IvyXml.getAttrInt(e,"OFFSET"),
	       complete,remove,txt);
	 break;
      case "RUNEVENT" :
	 break;
      case "RESOURCE" :
	 for (Element re : IvyXml.children(e,"DELTA")) {
	    handleResourceChange(re);
	  }
	 break;
      case "EVALUATION" :
      case "CONSOLE" :
	 msg.replyTo();
	 break;
      case "STOP" :
	 FaitLog.logD("Eclipse Message: " + msg.getText());
	 serverDone();
	 break;
    }
}

}	// end of inner class EclipseHandler



/********************************************************************************/
/*										*/
/*	Handle messages from Bubbles						*/
/*										*/
/********************************************************************************/

private class BubblesHandler implements MintHandler {

@Override public void receive(MintMessage msg,MintArguments args) {
   String cmd = args.getArgument(0);
   FaitLog.logD("BUBBLES COMMAND: " + cmd);
   // Element e = msg.getXml();
   switch (cmd) {
      case "EXIT" :
	 serverDone();
	 break;
    }

   msg.replyTo();
}

}	// end of inner class BubblesHandler



/********************************************************************************/
/*										*/
/*	Command handler 							*/
/*										*/
/********************************************************************************/

private String processCommand(String cmd,String sid,Element e) throws ServerException
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");
   switch (cmd) {
      case "PING" :
	 xw.text("PONG");
	 break;
      case "EXIT" :
	 System.exit(0);
	 break;
      case "BEGIN" :
	 handleBegin(sid,e,xw);
	 break;
      case "REMOVE" :
	 handleRemove(sid);
	 break;
      case "ADDFILE" :
	 handleAddFile(sid,e);
	 break;
      case "ANALYZE" :
	 handleAnalyze(sid,e,xw);
	 break;
      case "QUERY" :
         handleQuery(sid,e,xw);
         break;
      case "RESOURCES" :
         handleResourceFiles(sid,e,xw);
         break;
      case "REFLECTION" :
         handleReflection(sid,e,xw);
         break;
      case "PERFORMANCE" :
         handlePerformance(sid,e,xw);
         break;
      case "CRITICAL" :
         handleFindCritical(sid,e,xw);
         break;
      case "TESTEDIT" :
         String txt = IvyXml.getText(e);
         File fil = new File(IvyXml.getAttrString(e,"FILE"));
         handleEdit(null,SOURCE_ID,fil,IvyXml.getAttrInt(e,"LENGTH"),IvyXml.getAttrInt(e,"OFFSET"),
               false,false,txt);
         break;
      default :
	 FaitLog.logE("Unknown command " + cmd);
	 break;
    }
   xw.end("RESULT");
   String rslt = xw.toString();
   xw.close();

   return rslt;
}


private class CommandHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      FaitLog.logI("PROCESS COMMAND: " + msg.getText());
      String cmd = args.getArgument(0);
      String sid = args.getArgument(1);
      Element e = msg.getXml();
      String rslt = null;
      try {
         rslt = processCommand(cmd,sid,e);
         FaitLog.logI("COMMAND RESULT: " + rslt);
       }
      catch (ServerException t) {
         String xmsg = "BEDROCK: error in command " + cmd + ": " + t;
         FaitLog.logE(xmsg,t);
         IvyXmlWriter xw = new IvyXmlWriter();
         xw.cdataElement("ERROR",xmsg);
         rslt = xw.toString();
         xw.close();
       }
      catch (Throwable t) {
         String xmsg = "Problem processing command " + cmd + ": " + t;
         FaitLog.logE(xmsg,t);
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         Throwable xt = t;
         for ( ; xt.getCause() != null; xt = xt.getCause());
         if (xt != null && xt != t) {
            pw.println();
            xt.printStackTrace(pw);
          }
         FaitLog.logE("TRACE: " + sw.toString());
         IvyXmlWriter xw = new IvyXmlWriter();
         xw.begin("ERROR");
         xw.textElement("MESSAGE",xmsg);
         xw.cdataElement("EXCEPTION",t.toString());
         xw.cdataElement("STACK",sw.toString());
         xw.end("ERROR");
         rslt = xw.toString();
         xw.close();
         pw.close();
       }
      msg.replyTo(rslt);
    }

}	// end of inner class CommandHandler



}	// end of class ServerMonitor




/* end of ServerMonitor.java */

