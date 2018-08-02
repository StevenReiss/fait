/********************************************************************************/
/*										*/
/*		ServerTest.java 						*/
/*										*/
/*	Tests for the server package of fait					*/
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class ServerTest implements ServerConstants, MintConstants 
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static MintControl	mint_control;
private Map<String,Element> done_map;
private static Random random_gen = new Random();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ServerTest()
{
   done_map = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	NIM test								*/
/*										*/
/********************************************************************************/

@Test 
public synchronized void serverTestNim()
{
   runServerTest("nim","nim",0,null);
}



/********************************************************************************/
/*                                                                              */
/*      Nim test with update                                                    */
/*                                                                              */
/********************************************************************************/

@Test
public synchronized void serverTestNimUpdate()
{
   runServerTest("nim","nim",0,"NimComputerPlayer");
}




/********************************************************************************/
/*										*/
/*	UPOD test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestUpod()
{
   runServerTest("upod","upod",0,null);
}



/********************************************************************************/
/*                                                                              */
/*      SEEDE test                                                              */
/*                                                                              */
/********************************************************************************/

@Test
public synchronized void serverTestSeede()
{
   runServerTest("seede","seede",0,null);
}



/********************************************************************************/
/*                                                                              */
/*      JavaSecurity test                                                       */
/*                                                                              */
/********************************************************************************/

@Test
public synchronized void serverTestJavaSecurity()
{
   runServerTest("javasecurity","WebServer",7,null);
}




/********************************************************************************/
/*                                                                              */
/*      Generic testing routine                                                 */
/*                                                                              */
/********************************************************************************/

private void runServerTest(String dir,String pid,int ctr,String updfile)
{
   if (dir == null) dir = pid;
   String mid = "FAIT_TEST_" + pid.toUpperCase();
   
   setupBedrock(dir,mid,pid);
   int rint = random_gen.nextInt(1000000);
   
   try {  
      String [] args = new String[] { "-m", mid, "-DEBUG", "-TRACE",
            "-LOG", "/vol/spr/servertest" + dir + ".log" };
      
      ServerMain.main(args);
      
      mint_control.register("<FAITEXEC TYPE='_VAR_0' />",new FaitHandler());
      
      String sid = "SERVER" + rint;
      CommandArgs cargs = null;
      Element xml= sendReply(sid,"BEGIN",cargs,null);
      Assert.assertTrue(IvyXml.isElement(xml,"RESULT"));
      
      cargs = new CommandArgs("FILES",true);
      Element pxml = sendBubblesXmlReply("OPENPROJECT",pid,cargs,null);
      Assert.assertTrue(IvyXml.isElement(pxml,"RESULT"));
      Element p1 = IvyXml.getChild(IvyXml.getChild(pxml,"PROJECT"),"FILES");
      String files = "";
      File editfile = null;
      for (Element fe : IvyXml.children(p1,"FILE")) {
         if (IvyXml.getAttrBool(fe,"SOURCE")) {
            File f1 = new File(IvyXml.getAttrString(fe,"PATH"));
            File f2 = new File(IvyXml.getText(fe));
            if (f1.exists() && f1.getName().endsWith(".java")) {
               // files += "<FILE NAME='" + f1.getPath() + "'/>";
             }
            if (f2.exists() && f2.getName().endsWith(".java")) {
               files += "<FILE NAME='" + f2.getPath() + "'/>";
             }
            if (updfile != null && f1.getPath().contains(updfile)) editfile = f1;
          }
       }
      xml = sendReply(sid,"ADDFILE",null,files);
      Assert.assertTrue(IvyXml.isElement(xml,"RESULT"));
      
      String rid = "RETURN" + rint;
      cargs = new CommandArgs("ID",rid,"THREADS",1);
      xml = sendReply(sid,"ANALYZE",cargs,null);
      Assert.assertTrue(IvyXml.isElement(xml,"RESULT"));
      Element rslt = waitForAnalysis(rid);
      Assert.assertNotNull(rslt);
      
      int stops = countStops(rslt);
      if (ctr == 0) Assert.assertEquals(stops,0);
      else Assert.assertNotEquals(stops,0);
      if (ctr > 0) Assert.assertTrue(stops <= ctr);
      
      if (updfile != null) {
         cargs = new CommandArgs("FILE",editfile.getPath(),"LENGTH",0,"OFFSET",0);
         xml = sendReply(sid,"TESTEDIT",cargs,null);
         rslt = waitForAnalysis(rid);
         Assert.assertNotNull(rslt);
         Assert.assertEquals(ctr,countStops(rslt));
       }
    }
   finally {
      shutdownBedrock();
    }
}




/********************************************************************************/
/*										*/
/*	Mint handling								*/
/*										*/
/********************************************************************************/

private Element sendReply(String sid,String cmd,CommandArgs args,String xml)
{
   MintDefaultReply rply = new MintDefaultReply();
   send(sid,cmd,args,xml,rply);
   Element rslt = rply.waitForXml();
   return rslt;
}



private void send(String sid,String cmd,CommandArgs args,String xml,MintReply rply)
{
   IvyXmlWriter msg = new IvyXmlWriter();
   msg.begin("FAIT");
   msg.field("DO",cmd);
   msg.field("SID",sid);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 msg.field(ent.getKey(),ent.getValue().toString());
       }
    }
   if (xml != null) msg.xmlText(xml);
   msg.end("FAIT");
   String msgt = msg.toString();
   msg.close();

   if (rply == null) {
      mint_control.send(msgt,rply,MintConstants.MINT_MSG_NO_REPLY);
    }
   else {
      mint_control.send(msgt,rply,MintConstants.MINT_MSG_FIRST_NON_NULL);
    }
}




/********************************************************************************/
/*										*/
/*	Handle responses from FAIT						*/
/*										*/
/********************************************************************************/

private Element waitForAnalysis(String id)
{
   synchronized (done_map) {
      for ( ; ; ) {
	 if (done_map.containsKey(id)) {
	    return done_map.remove(id);
	  }
	 try {
	    done_map.wait(10000);
	  }
	 catch (InterruptedException e) { }
       }
    }
}

private class FaitHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      Element xml = msg.getXml();
      switch (cmd) {
         case "ANALYSIS" :
            String rid = IvyXml.getAttrString(xml,"ID");
            synchronized (done_map) {
               done_map.put(rid,xml);
               done_map.notifyAll();
             }
            msg.replyTo();
            break;
         case "PING" :
            msg.replyTo("<PONG/>");
            break;
         default :
            msg.replyTo();
            break;
       }
    }

}       // end of inner class FaitHandler



/********************************************************************************/
/*										*/
/*	Bubbles Messaging methods						*/
/*										*/
/********************************************************************************/

private static Element sendBubblesXmlReply(String cmd,String proj,Map<String,Object> flds,String cnts)
{
   MintDefaultReply mdr = new MintDefaultReply();
   sendBubblesMessage(cmd,proj,flds,cnts,mdr);
   Element pxml = mdr.waitForXml();
   FaitLog.logD("RECEIVE from BUBBLES: " + IvyXml.convertXmlToString(pxml));
   return pxml;
}



private static void sendBubblesMessage(String cmd)
{
   sendBubblesMessage(cmd,null,null,null,null);
}


private static void sendBubblesMessage(String cmd,String proj,Map<String,Object> flds,String cnts)
{
   sendBubblesMessage(cmd,proj,flds,cnts,null);
}


private static void sendBubblesMessage(String cmd,String proj,Map<String,Object> flds,String cnts,
      MintReply rply)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BUBBLES");
   xw.field("DO",cmd);
   xw.field("BID",SOURCE_ID);
   if (proj != null && proj.length() > 0) xw.field("PROJECT",proj);
   if (flds != null) {
      for (Map.Entry<String,Object> ent : flds.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   xw.field("LANG","eclipse");
   if (cnts != null) xw.xmlText(cnts);
   xw.end("BUBBLES");
   
   String xml = xw.toString();
   xw.close();
   
   FaitLog.logD("SEND to BUBBLES: " + xml);
   
   int fgs = MINT_MSG_NO_REPLY;
   if (rply != null) fgs = MINT_MSG_FIRST_NON_NULL;
   mint_control.send(xml,rply,fgs);
}




/********************************************************************************/
/*                                                                              */
/*      Bedrock setup / shutdown methods                                        */
/*                                                                              */
/********************************************************************************/

private static void setupBedrock(String dir,String mint,String proj)
{
   mint_control = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new TestEclipseHandler());
   
   System.err.println("SETTING UP BEDROCK");
   File ec1 = new File("/u/spr/eclipse-oxygenx/eclipse/eclipse");
   File ec2 = new File("/u/spr/Eclipse/" + dir);
   if (!ec1.exists()) {
      ec1 = new File("/Developer/eclipse42/eclipse");
      ec2 = new File("/Users/spr/Documents/" + dir);
    }
   if (!ec1.exists()) {
      System.err.println("Can't find bubbles version of eclipse to run");
      System.exit(1);
    }
   
   String cmd = ec1.getAbsolutePath();
   cmd += " -application edu.brown.cs.bubbles.bedrock.application";
   cmd += " -data " + ec2.getAbsolutePath();
   cmd += " -bhide";
   cmd += " -nosplash";
   cmd += " -vmargs -Dedu.brown.cs.bubbles.MINT=" + mint;
   cmd += " -Xdebug -Xrunjdwp:transport=dt_socket,address=32328,server=y,suspend=n";
  // cmd += " -Xmx16000m";
   
   System.err.println("RUN: " + cmd);
   
   try {
      for (int i = 0; i < 250; ++i) {
	 if (pingEclipse()) {
	    CommandArgs args = new CommandArgs("LEVEL","DEBUG");
	    sendBubblesMessage("LOGLEVEL",null,args,null);
	    sendBubblesMessage("ENTER");
	    Element pxml = sendBubblesXmlReply("OPENPROJECT",proj,null,null);
	    if (!IvyXml.isElement(pxml,"PROJECT")) pxml = IvyXml.getChild(pxml,"PROJECT");
	    return;
	  }
	 if (i == 0) new IvyExec(cmd);
         else {
            try { Thread.sleep(100); } catch (InterruptedException e) { }
          }
       }
    }
   catch (IOException e) { }
   
   throw new Error("Problem running Eclipse: " + cmd);
}




private static class TestEclipseHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      switch (cmd) {
         case "PING" :
            msg.replyTo("<PONG/>");
            break;
         case "ELISION" :
         case "RESOURCE" :
            break;
         default :
            msg.replyTo();
            break;
       }
    }
   
}       // end of inner class TestEclipseHandler



private static void shutdownBedrock()
{
   System.err.println("Shut down bedrock");
   sendBubblesMessage("EXIT");
   while (pingEclipse()) {
      try {
         Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }
   mint_control.shutDown();
   mint_control = null;
}



private static boolean pingEclipse()
{
   MintDefaultReply mdr = new MintDefaultReply();
   sendBubblesMessage("PING",null,null,null,mdr);
   String r = mdr.waitForString(500);
   return r != null;
}


/********************************************************************************/
/*                                                                              */
/*      Checking methods                                                         */
/*                                                                              */
/********************************************************************************/

private int countStops(Element xml)
{
   int ctr = 0;
   for (Element call : IvyXml.elementsByTag(xml,"CALL")) {
      for (Element dead : IvyXml.children(call,"ERROR")) {
         for (Element pt : IvyXml.children(dead,"POINT")) {
            if (IvyXml.getAttrString(pt,"KIND").equals("EDIT")) {
               ++ctr;
             }
          }
       }
    }
   
   return ctr;
}

}	// end of class ServerTest




/* end of ServerTest.java */

