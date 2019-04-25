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
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

private static int MAX_UPDATE = 50;

private static int default_threads = 1;



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
/*										*/
/*	Nim test with update							*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestNimUpdate()
{
   runServerTest("nim","nim",0,"NimComputerPlayer");
}



@Test
public synchronized void serverTestNimUpdateTimed()
{
   default_threads = 4;
   runServerTest("nim","nim",0,"*ALL*",true);
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



@Test
public synchronized void serverTestUpodTimed()
{
   runServerTest("upod","upod",0,"*ALL*",true);

}



/********************************************************************************/
/*										*/
/*	SEEDE test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestSeede()
{
   runServerTest("seede","seede",0,null);
}



/********************************************************************************/
/*										*/
/*	S6 test 								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestS6()
{
   runServerTest("s6","s6",0,null);
}



/********************************************************************************/
/*										*/
/*	Fait test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestFait()
{
   runServerTest("fait","fait",0,null);
}







/********************************************************************************/
/*										*/
/*	Bubbles tes								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestBubbles()
{
   runServerTest("bubblesx","bubbles",0,null,true);
}



/********************************************************************************/
/*										*/
/*	JavaSecurity test							*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestJavaSecurity()
{
   runServerTest("javasecurity","WebServer",50,null);
}


@Test
public synchronized void serverTestJavaSecurityUpdateTimed()
{
   default_threads = 4;
   runServerTest("javasecurity","WebServer",50,"*ALL*",true);
}



@Test
public synchronized void serverTestJavaSecurityUpdate()
{
   runServerTest("javasecurity","WebServer",50,"*ALL*",false);
}


@Test
public synchronized void serverTestTimedJavaSecurity()
{
   runServerTest("javasecurity","WebServer",11,null,true);
}



/********************************************************************************/
/*										*/
/*	WebGoat(spr) test							*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestWebGoat()
{
   runServerTest("webgoatspr","webgoat",2,null);
}


/********************************************************************************/
/*										*/
/*	SecuriBench test							*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestJboard()
{
   runServerTest("jboard","jboard",1000,null);
}


@Test
public synchronized void serverTestBlueBlog()
{
   runServerTest("blueblog","blueblog",100,null);
}


@Test
public synchronized void serverTestPebble()
{
   runServerTest("pebble","pebble",1000,null);
}



@Test
public synchronized void serverTestPersonalBlog()
{
   runServerTest("personalblog","personalblog",1000,null);
}


@Test
public synchronized void serverTestRoller()
{
   runServerTest("roller","roller",1000,null);
}



@Test
public synchronized void serverTestSnipSnap()
{
   runServerTest("snipsnap","snipsnap",1000,null);
}



@Test
public synchronized void serverTestWebgoatBench()
{
   runServerTest("webgoatbench","webgoatbench",1000,null);
}




/********************************************************************************/
/*										*/
/*	SecuriBench timed tests 						*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestTimedJboard()
{
   runServerTest("jboard","jboard",1000,null,true);
}


@Test
public synchronized void serverTestTimedBlueBlog()
{
   runServerTest("blueblog","blueblog",100,null,true);
}


@Test
public synchronized void serverTestTimedPebble()
{
   runServerTest("pebble","pebble",1000,null,true);
}



@Test
public synchronized void serverTestTimedPersonalBlog()
{
   runServerTest("personalblog","personalblog",1000,null,true);
}


@Test
public synchronized void serverTestTimedRoller()
{
   runServerTest("roller","roller",1000,null,true);
}



@Test
public synchronized void serverTestTimedSnipSnap()
{
   runServerTest("snipsnap","snipsnap",1000,null,true);
}



@Test
public synchronized void serverTestTimedWebgoatBench()
{
   runServerTest("webgoatbench","webgoatbench",1000,null,true);
}



/********************************************************************************/
/*										*/
/*	SecuriBench timed update tests						*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestTimedUpdateJboard()
{
   runServerTest("jboard","jboard",1000,"*ALL*",true);
}


@Test
public synchronized void serverTestTimedUpdateBlueBlog()
{
   runServerTest("blueblog","blueblog",100,"*ALL*",true);
}


@Test
public synchronized void serverTestTimedUpdatePebble()
{
   runServerTest("pebble","pebble",1000,"*ALL*",true);
}



@Test
public synchronized void serverTestTimedUpdatePersonalBlog()
{
   runServerTest("personalblog","personalblog",1000,"*ALL*",true);
}


@Test
public synchronized void serverTestTimedUpdateRoller()
{
   runServerTest("roller","roller",1000,"*ALL*",true);
}



@Test
public synchronized void serverTestTimedUpdateSnipSnap()
{
   runServerTest("snipsnap","snipsnap",1000,"*ALL*",true);
}


@Test
public synchronized void serverTestTimedUpdateWebgoatBench()
{
   runServerTest("webgoatbench","webgoatbench",1000,"*ALL*",true);
}


/********************************************************************************/
/*										*/
/*	Generic testing routine 						*/
/*										*/
/********************************************************************************/

private void runServerTest(String dir,String pid,int ctr,String updfile)
{
   try {
      runServerTest(dir,pid,ctr,updfile,false);
    }
   catch (Throwable t) {
      FaitLog.logE("Test failed",t);
      throw t;
    }
}



private void runServerTest(String dir,String pid,int ctr,String updfile,boolean timed)
{
   Set<File> testfiles = new HashSet<>();

   int rint = random_gen.nextInt(1000000);

   if (dir == null) dir = pid;
   String mid = "FAIT_TEST_" + pid.toUpperCase() + "_" + rint;

   setupBedrock(dir,mid,pid);

   int nthread = default_threads;
   if (timed) {
      String nthstr = System.getenv("FAIT_THREADS");
      FaitLog.logD("FAIT_THREAD = " + nthstr);
      nthread = 4;
      try {
	 nthread = Integer.parseInt(nthstr);
       }
      catch (NumberFormatException e) { }
    }

   try {
      String [] args = new String[] { "-m", mid, "-DEBUG", "-TRACE",
	    "-LOG", "/vol/spr/servertest" + dir + ".log" };
      if (timed) args = new String[] { "-m", mid,
	    "-LOG", "/vol/spr/timedtest_" + nthread + "_" + dir + ".log" };
      if (timed && updfile != null && updfile.equals("*ALL*")) {
	 args = new String[] { "-m", mid,
	       "-LOG", "/vol/spr/updatetest_" + nthread + "_" + dir + ".log" };
       }
      if (!timed && updfile != null && updfile.equals("*ALL*")) {
	 args = new String[] { "-m", mid, "-DEBUG", "-TRACE",
	       "-LOG", "/vol/spr/updatetest" + dir + ".log" };
       }

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
	    if (f2.exists() && f2.getName().endsWith(".java")) {
	       try {
		  f2 = f2.getCanonicalFile();
		}
	       catch (IOException e) { }
	       if (testfiles.add(f2)) {
		  files += "<FILE NAME='" + f2.getPath() + "'/>";
		}
	     }
	    if (updfile != null && f1.getPath().contains(updfile)) editfile = f1;
	  }
       }
      xml = sendReply(sid,"ADDFILE",null,files);
      Assert.assertTrue(IvyXml.isElement(xml,"RESULT"));

      String rid = "RETURN" + rint;
      String typ = "FULL_STATS";
      if (timed) typ = "FULL";
      cargs = new CommandArgs("ID",rid,"THREADS",nthread,"REPORT",typ);
      xml = sendReply(sid,"ANALYZE",cargs,null);
      Assert.assertTrue(IvyXml.isElement(xml,"RESULT"));
      Element rslt = waitForAnalysis(rid);
      Assert.assertNotNull(rslt);

      int stops = countStops(rslt);
      FaitLog.logD("STOPS: " + stops + " " + ctr);
      if (ctr == 0) Assert.assertEquals(stops,0);
      else Assert.assertNotEquals(stops,0);
      if (ctr > 0) Assert.assertTrue(stops <= ctr);

      errorQueries(sid,rslt);

      if (updfile != null) {
	 if (updfile.equals("*ALL*")) {
	    double ct = testfiles.size();
	    for (File f : testfiles) {
	       if (f.getPath().contains("InvokeServlets")) continue;
	       if (f.getPath().contains("FAIT_TEST_CLASS_GENERATED")) continue;
	       if (ct > MAX_UPDATE) {
		  double lim = MAX_UPDATE / ct;
		  if (random_gen.nextDouble() > lim) continue;
		}
	       cargs = new CommandArgs("FILE",f.getPath(),"LENGTH",0,"OFFSET",0);
	       xml = sendReply(sid,"TESTEDIT",cargs,null);
	       rslt = waitForAnalysis(rid);
	       Assert.assertNotNull(rslt);
	       int nstops = countStops(rslt);
	       // Assert.assertEquals(stops,countStops(rslt));
	       FaitLog.logI("UPDATESTOPS: " + nstops + " " + stops);
	     }
	  }
	 else {
	    cargs = new CommandArgs("FILE",editfile.getPath(),"LENGTH",0,"OFFSET",0);
	    xml = sendReply(sid,"TESTEDIT",cargs,null);
	    rslt = waitForAnalysis(rid);
	    Assert.assertNotNull(rslt);
	    Assert.assertEquals(stops,countStops(rslt));
	  }
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
	 if (ent.getValue() == null) continue;
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
	    if (IvyXml.getAttrBool(xml,"STARTED")) break;
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

}	// end of inner class FaitHandler



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
/*										*/
/*	Bedrock setup / shutdown methods					*/
/*										*/
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
  // cmd += " -Xdebug -Xrunjdwp:transport=dt_socket,address=32328,server=y,suspend=n";
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

}	// end of inner class TestEclipseHandler



private static void shutdownBedrock()
{
   System.err.println("SHUT DOWN BEDROCK");
   sendBubblesMessage("EXIT");
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
/*										*/
/*	Checking methods							 */
/*										*/
/********************************************************************************/

private int countStops(Element xml)
{
   int ctr = 0;
   Set<String> found = new HashSet<>();

   for (Element call : IvyXml.elementsByTag(xml,"CALL")) {
      for (Element dead : IvyXml.children(call,"ERROR")) {
	 String emsg = IvyXml.getTextElement(dead,"MESSAGE");
	 for (Element pt : IvyXml.children(dead,"POINT")) {
	    String pttxt = IvyXml.convertXmlToString(pt);
	    String key = pttxt + "@" + emsg;
	    if (found.add(key)) {
	       if (IvyXml.getAttrString(dead,"SUBTYPE") != null) ++ctr;
	       else if (IvyXml.getAttrString(dead,"SAFETY") != null) ++ctr;
	       else if (IvyXml.getAttrString(pt,"KIND").equals("EDIT")) ++ctr;
	     }
	  }
       }
    }

   return ctr;
}



private void errorQueries(String sid,Element xml)
{
   for (Element call : IvyXml.elementsByTag(xml,"CALL")) {
      for (Element err : IvyXml.children(call,"ERROR")) {
	 if (!IvyXml.getAttrString(err,"LEVEL").equals("ERROR")) continue;
	 int lno = 0;
	 int spos = -1;
	 int loc = -1;
	 for (Element pt : IvyXml.children(err,"POINT")) {
	    if (IvyXml.getAttrString(pt,"KIND").equals("EDIT")) {
	       lno = IvyXml.getAttrInt(pt,"LINE");
	       spos = IvyXml.getAttrInt(pt,"START");
	       if (lno > 0) break;
	     }
	    else {
	       lno = IvyXml.getAttrInt(pt,"LINE");
	       loc = IvyXml.getAttrInt(pt,"LOC");
	     }
	
	  }
	 if (lno <= 0) continue;
	 String mthd = IvyXml.getAttrString(call,"CLASS");
	 mthd += "." + IvyXml.getAttrString(call,"METHOD");
	 mthd += IvyXml.getAttrString(call,"SIGNATURE");
	 mthd += "@" + IvyXml.getAttrString(call,"HASHCODE");
	 CommandArgs cargs = new CommandArgs("ERROR",IvyXml.getAttrString(err,"HASHCODE"),
	       "FILE",IvyXml.getAttrString(call,"FILE"),
	       "LINE",lno,
	       "METHOD",mthd,
	       "START",spos,"LOCATION",loc);
	 Element rslt = sendReply(sid,"QUERY",cargs,null);
	 Element q = IvyXml.getChild(rslt,"RESULTSET");
	 Assert.assertNotNull(q);
       }
    }
}

}	// end of class ServerTest




/* end of ServerTest.java */

