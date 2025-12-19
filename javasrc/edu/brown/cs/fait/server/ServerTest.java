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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
   runServerTest("fait","fait",4,null);
}



/********************************************************************************/
/*										*/
/*	Bubbles test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestBubbles()
{
   runServerTest("bubblesx","bubbles",0,null,false);
}



/********************************************************************************/
/*										*/
/*	Catre test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestCatre()
{
   runServerTest("catre","catre",0,null,false);
}



/********************************************************************************/
/*										*/
/*	NanoXML test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestNanoXml()
{
   List<QueryTest> qts = new ArrayList<>();

   String sq =
      "<STACK>\n" +
      "<EXCEPTION>org.junit.ComparisonFailure: expected:&lt;ns:[]Bar&gt; but was:&lt;ns:[:]Bar&gt;</EXCEPTION>\n" +
      "<FRAME METHOD='org.junit.Assert.assertEquals' FILE='Assert.java' LINE='115' />\n" +
      "<FRAME METHOD='org.junit.Assert.assertEquals' FILE='Assert.java' LINE='144' />\n" +
      "<FRAME METHOD='net.n3.nanoxml.ParserTest1.testParsing16' FILE='ParserTest1.java' LINE='457' />\n" +
      "<FRAME METHOD='org.junit.runners.model.FrameworkMethod$1.runReflectiveCall' " +
	 "FILE='FrameworkMethod.java' LINE='50' />\n" +
      "<FRAME METHOD='org.junit.internal.runners.model.ReflectiveCallable.run' " +
	 "FILE='ReflectiveCallable.java' LINE='12' />\n" +
      "<FRAME METHOD='org.junit.runners.model.FrameworkMethod.invokeExplosively' " +
	 "FILE='FrameworkMethod.java' LINE='47' />\n" +
      "<FRAME METHOD='org.junit.internal.runners.statements.InvokeMethod.evaluate' " +
	 "FILE='InvokeMethod.java' LINE='17' />\n" +
      "<FRAME METHOD='org.junit.runners.ParentRunner.runLeaf' FILE='ParentRunner.java' LINE='325' />\n" +
      "<FRAME METHOD='org.junit.runners.BlockJUnit4ClassRunner.runChild' " +
	 "FILE='BlockJUnit4ClassRunner.java' LINE='78' />\n" +
      "<FRAME METHOD='org.junit.runners.BlockJUnit4ClassRunner.runChild' " +
	 "FILE='BlockJUnit4ClassRunner.java' LINE='57' />\n" +
      "<FRAME METHOD='org.junit.runners.ParentRunner$3.run' FILE='ParentRunner.java' LINE='290' />\n" +
      "<FRAME METHOD='org.junit.runners.ParentRunner$1.schedule' FILE='ParentRunner.java' LINE='71' />\n" +
      "<FRAME METHOD='org.junit.runners.ParentRunner.runChildren' FILE='ParentRunner.java' LINE='288' />\n" +
      "<FRAME METHOD='org.junit.runners.ParentRunner.access$000' FILE='ParentRunner.java' LINE='58' />\n" +
      "<FRAME METHOD='org.junit.runners.ParentRunner$2.evaluate' FILE='ParentRunner.java' LINE='268' />\n" +
      "<FRAME METHOD='org.junit.runners.ParentRunner.run' FILE='ParentRunner.java' LINE='363' />\n" +
      "<FRAME METHOD='org.junit.runner.JUnitCore.run' FILE='JUnitCore.java' LINE='137' />\n" +
      "<FRAME METHOD='org.junit.runner.JUnitCore.run' FILE='JUnitCore.java' LINE='115' />\n" +
      "<FRAME METHOD='edu.brown.cs.bubbles.batt.BattJUnit.process' FILE='BattJUnit.java' LINE='414' />\n" +
      "<FRAME METHOD='edu.brown.cs.bubbles.batt.BattJUnit.main' FILE='BattJUnit.java' LINE='82' />\n" +
      "</STACK>";
   StackStartTest sst = new StackStartTest(sq);
   qts.add(sst);

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("EXPR");
   xw.field("AFTER","arguments");
   xw.field("AFTEREND",15382);
   xw.field("AFTERSTART",15341);
   xw.field("AFTERTYPE","MethodInvocation");
   xw.field("AFTERTYPEID",32);
   xw.field("END",15359);
   xw.field("LINE",457);
   xw.field("NODETYPE","MethodInvocation");
   xw.field("NODETYPEID",32);
   xw.field("START",15318);
   xw.textElement("TEXT","xml.getChildAtIndex(0).getQualifiedName()");
   xw.end("EXPR");
   String cnts = xw.toString();
   xw.close();

   FlowQueryTest fq1 = new FlowQueryTest(cnts,
//	 "CONDDEPTH",50,"DEPTH",100,
	 "FILE","/pro/nanoxml/test/net/n3/nanoxml/ParserTest1.java",
	 "LINE",457,"METHOD","net.n3.nanoxml.ParserTest1.testParsing16",
	 "QTYPE","EXPRESSION");
   qts.add(fq1);
   FlowQueryTest fq2 = new FlowQueryTest(null,
	 "FILE","/pro/nanoxml/test/net/n3/nanoxml/ParserTest1.java",
	 "LINE",373,"METHOD","net.n3.nanoxml.StdXMLParser.processElement()",
	 "START",11304,
	 "TOKEN","name",
	 "QTYPE","TOKEN");
   qts.add(fq2);

   runServerTest("nanoxml","nanoxml",0,null,qts);
}



@Test
public synchronized void serverTestMathV1()
{
   runServerTest("debug_mathv1","mathv1",0,null,false);
}



/********************************************************************************/
/*										*/
/*	Tetris test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestTetris()
{
   List<QueryTest> qts = new ArrayList<>();

   String cnts =
      "<EXPR AFTER='arguments' AFTEREND='2919' AFTERSTART='2917' AFTERTYPE='SimpleName' AFTERTYPEID='42' END='2872' " +
	 "LINE='61' NODETYPE='MethodInvocation' NODETYPEID='32' START='2870'>\n" +
      "<TEXT>b4</TEXT>\n" +
      "</EXPR>\n" +
      "<STACK>\n" +
      "<FRAME CLASS='org.junit.Assert' FSIGN='(java.lang.String)' METHOD='fail' " +
	 "SIGNATURE='(Ljava/lang/String;)V' />\n" +
      "<FRAME CLASS='org.junit.Assert' FSIGN='(java.lang.String,java.lang.Object,java.lang.Object)' " +
	 "METHOD='failNotEquals' " + "SIGNATURE='(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V' />\n" +
      "<FRAME CLASS='org.junit.Assert' FSIGN='(java.lang.String,java.lang.Object,java.lang.Object)' " +
	 "METHOD='assertEquals' SIGNATURE='(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V' />\n" +
      "<FRAME CLASS='net.percederberg.tetris.TetrisTest' FSIGN='()' METHOD='testSquareRotate' SIGNATURE='()V' />\n" +
      "<FRAME CLASS='java.lang.invoke.LambdaForm$DMH.0x000000080108c000' FSIGN='(java.lang.Object,java.lang.Object)' " +
	 "METHOD='invokeVirtual' SIGNATURE='(Ljava/lang/Object;Ljava/lang/Object;)V' />\n" +
      "<FRAME CLASS='java.lang.invoke.LambdaForm$MH.0x000000080108cc00' FSIGN='(java.lang.Object,java.lang.Object)' " +
	 "METHOD='invoke' SIGNATURE='(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='java.lang.invoke.Invokers$Holder' FSIGN='(java.lang.Object,java.lang.Object,java.lang.Object)' " +
	 "METHOD='invokeExact_MT' " + 
         "SIGNATURE='(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='jdk.internal.reflect.DirectMethodHandleAccessor' FSIGN='(java.lang.Object,java.lang.Object[])' " +
	 "METHOD='invokeImpl' SIGNATURE='(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='jdk.internal.reflect.DirectMethodHandleAccessor' FSIGN='(java.lang.Object,java.lang.Object[])' " +
	 "METHOD='invoke' SIGNATURE='(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='java.lang.reflect.Method' FSIGN='(java.lang.Object,java.lang.Object[])' " +
	 "METHOD='invoke' SIGNATURE='(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='org.junit.runners.model.FrameworkMethod$1' FSIGN='()' " +
	 "METHOD='runReflectiveCall' SIGNATURE='()Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='org.junit.internal.runners.model.ReflectiveCallable' FSIGN='()' " +
	 "METHOD='run' SIGNATURE='()Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='org.junit.runners.model.FrameworkMethod' FSIGN='(java.lang.Object,java.lang.Object[])' " +
	 "METHOD='invokeExplosively' SIGNATURE='(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;' />\n" +
      "<FRAME CLASS='org.junit.internal.runners.statements.InvokeMethod' " +
	 "FSIGN='()' " +
	 "METHOD='evaluate' SIGNATURE='()V' />\n" +
      "<FRAME CLASS='org.junit.runners.ParentRunner&lt;T&gt;' " +
	 "FSIGN='(org.junit.runners.model.Statement,org.junit.runner.Description," + 
         "org.junit.runner.notification.RunNotifier)' " +
	 "METHOD='runLeaf' SIGNATURE='(Lorg/junit/runners/model/Statement;Lorg/junit/runner/Description;" + 
         "Lorg/junit/runner/notification/RunNotifier;)V' />\n" +
      "<FRAME CLASS='org.junit.runners.BlockJUnit4ClassRunner' " +
	 "FSIGN='(org.junit.runners.model.FrameworkMethod,org.junit.runner.notification.RunNotifier)' " +
	 "METHOD='runChild' SIGNATURE='(Lorg/junit/runners/model/FrameworkMethod;" + 
         "Lorg/junit/runner/notification/RunNotifier;)V' />\n" +
      "<FRAME CLASS='org.junit.runners.BlockJUnit4ClassRunner' " +
	 "FSIGN='(java.lang.Object,org.junit.runner.notification.RunNotifier)' " +
	 "METHOD='runChild' SIGNATURE='(Ljava/lang/Object;Lorg/junit/runner/notification/RunNotifier;)V' />\n" +
      "<FRAME CLASS='org.junit.runners.ParentRunner$3' " +
	 "FSIGN='()' " +
	 "METHOD='run' SIGNATURE='()V' />\n" +
      "<FRAME CLASS='org.junit.runners.ParentRunner$1' " +
	 "FSIGN='(java.lang.Runnable)' " +
	 "METHOD='schedule' SIGNATURE='(Ljava/lang/Runnable;)V' />\n" +
      "<FRAME CLASS='org.junit.runners.ParentRunner&lt;T&gt;' " +
	 "FSIGN='(org.junit.runner.notification.RunNotifier)' " +
	 "METHOD='runChildren' SIGNATURE='(Lorg/junit/runner/notification/RunNotifier;)V' />\n" +
      "<FRAME CLASS='org.junit.runners.ParentRunner&lt;T&gt;' " +
	 "FSIGN='(org.junit.runners.ParentRunner,org.junit.runner.notification.RunNotifier)' " +
	 "METHOD='access$000' SIGNATURE='(Lorg/junit/runners/ParentRunner;" + 
         "Lorg/junit/runner/notification/RunNotifier;)V' />\n" +
      "<FRAME CLASS='org.junit.runners.ParentRunner$2' " +
	 "FSIGN='()' " +
	 "METHOD='evaluate' SIGNATURE='()V' />\n" +
      "<FRAME CLASS='org.junit.runners.ParentRunner&lt;T&gt;' " +
	 "FSIGN='(org.junit.runner.notification.RunNotifier)' " +
	 "METHOD='run' SIGNATURE='(Lorg/junit/runner/notification/RunNotifier;)V' />\n" +
      "<FRAME CLASS='org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference' " +
	 "FSIGN='(org.eclipse.jdt.internal.junit.runner.TestExecution)' " +
	 "METHOD='run' SIGNATURE='(Lorg/eclipse/jdt/internal/junit/runner/TestExecution;)V' />\n" +
      "<FRAME CLASS='org.eclipse.jdt.internal.junit.runner.TestExecution' " +
	 "FSIGN='(org.eclipse.jdt.internal.junit.runner.ITestReference[])' " +
	 "METHOD='run' SIGNATURE='([Lorg/eclipse/jdt/internal/junit/runner/ITestReference;)V' />\n" +
      "<FRAME CLASS='org.eclipse.jdt.internal.junit.runner.RemoteTestRunner' " +
	 "FSIGN='(java.lang.String[],java.lang.String,org.eclipse.jdt.internal.junit.runner.TestExecution)' " +
	 "METHOD='runTests' SIGNATURE='([Ljava/lang/String;Ljava/lang/String;" + 
         "Lorg/eclipse/jdt/internal/junit/runner/TestExecution;)V' />\n" +
      "<FRAME CLASS='org.eclipse.jdt.internal.junit.runner.RemoteTestRunner' " +
	 "FSIGN='(org.eclipse.jdt.internal.junit.runner.TestExecution)' " +
	 "METHOD='runTests' SIGNATURE='(Lorg/eclipse/jdt/internal/junit/runner/TestExecution;)V' />\n" +
      "<FRAME CLASS='org.eclipse.jdt.internal.junit.runner.RemoteTestRunner' " +
	 "FSIGN='()' " +
	 "METHOD='run' SIGNATURE='()V' />\n" +
      "<FRAME CLASS='org.eclipse.jdt.internal.junit.runner.RemoteTestRunner' " +
	 "FSIGN='(java.lang.String[])' " +
	 "METHOD='main' SIGNATURE='([Ljava/lang/String;)V' />\n" +
      "</STACK>\n";

   FlowQueryTest fq1 = new FlowQueryTest(cnts,
					    "CONDDEPTH",4,"DEPTH",10,
					    "FILE","/pro/tetris/src/net/percederberg/tetris/TetrisTest.java",
					    "LINE",61,
					    "METHOD","net.percederberg.tetris.TetrisTest.testSquareRotate",
					    "QTYPE","EXPRESSION");
   qts.add(fq1);

   runServerTest("tetris","tetris",0,null,qts);
}



/********************************************************************************/
/*										*/
/*	JavaSecurity test							*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestJavaSecurity()
{
   List<QueryTest> vqs = new ArrayList<>();

   VarQueryTest vq1 = new VarQueryTest(79,2020,"q","edu.brown.cs.securitylab.SecurityDatabase.query(String,Object[])",
	 "/research/people/spr/javasecurity/javasrc/edu/brown/cs/securitylab/SecurityDatabase.java");
   vqs.add(vq1);
   VarQueryTest vq2 = new VarQueryTest(146,5516,"use_database",
	 "edu.brown.cs.securitylab.SecurityAccount.handleLoginRequest(SecurityRequest)",
	 "/research/people/spr/javasecurity/javasrc/edu/brown/cs/securitylab/SecurityAccount.java");
   vqs.add(vq2);

   EntityQueryTest edt = new EntityQueryTest(vq2,"98","QTYPE","TO");
   vqs.add(edt);

   runServerTest("javasecurity","WebServer",50,null,vqs);
}




/********************************************************************************/
/*										*/
/*	Tutorial test								*/
/*										*/
/********************************************************************************/

@Test
public synchronized void serverTestTutorial()
{
   List<QueryTest> vqs = new ArrayList<>();

   VarQueryTest vq1 = new VarQueryTest(137,3923,"g",
	 "edu.brown.cs.bubbles.tutorial.romp.Board.drawCircleInches(int,int,int,Graphics)",
	 "/Users/spr/Eclipse/tutorial/romp/src/edu/brown/cs/bubbles/tutorial/romp/Board.java");
   vqs.add(vq1);

   EntityQueryTest edt = new EntityQueryTest(vq1,"Debug","QTYPE","TO");
   vqs.add(edt);

   runServerTest("tutorial","romp",50,null,vqs);
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
      runServerTest(dir,pid,ctr,updfile,null,false);
    }
   catch (Throwable t) {
      FaitLog.logE("Test failed",t);
      throw t;
    }
}

private void runServerTest(String dir,String pid,int ctr,String updfile,
      List<QueryTest> vqs)
{
   try {
      runServerTest(dir,pid,ctr,updfile,vqs,false);
    }
   catch (Throwable t) {
      FaitLog.logE("Test failed",t);
      throw t;
    }
}



private void runServerTest(String dir,String pid,int ctr,String updfile,boolean timed)
{
   runServerTest(dir,pid,ctr,updfile,null,timed);
}


private void runServerTest(String dir,String pid,int ctr,String updfile,
      List<QueryTest> qs,boolean timed)
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
      nthread = (default_threads == 1 ? 4 : default_threads);
      try {
	 if (nthstr != null) nthread = Integer.parseInt(nthstr);
       }
      catch (NumberFormatException e) { }
    }

   File log = new File("/vol/spr");
   if (!log.exists()) {
      log = new File("/Users/spr/fait");
    }
   String loghead = log.getAbsolutePath() + "/";

   try {
      String [] args = new String[] { "-m", mid, "-DEBUG", "-TRACE",
	    "-LOG", loghead + "servertest" + dir + ".log" };
      if (timed) args = new String[] { "-m", mid,
	    "-LOG", loghead + "timedtest_" + nthread + "_" + dir + ".log" };
      if (timed && updfile != null && updfile.equals("*ALL*")) {
	 args = new String[] { "-m", mid,
	       "-LOG", loghead +  "updatetest_" + nthread + "_" + dir + ".log" };
       }
      if (!timed && updfile != null && updfile.equals("*ALL*")) {
	 args = new String[] { "-m", mid, "-DEBUG", "-TRACE",
	       "-LOG", loghead + "updatetest" + dir + ".log" };
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
	       // if (f2.getName().contains("SecurityWebServer")) continue;
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
      if (ctr == 0) Assert.assertEquals(0,stops);
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

      if (qs != null) {
	 for (QueryTest vq : qs) {
	    vq.process(sid,rid);
	  }
       }
    }
   catch (Throwable t) {
      System.err.println("PROBLEM RUNNING TEST");
      t.printStackTrace();
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
	    done_map.wait(1000);
	  }
	 catch (InterruptedException e) { }
       }
    }
}

private final class FaitHandler implements MintHandler {

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
      ec1 = new File("/vol/Developer/java-2023-06/Eclipse.app/Contents/MacOS/eclipse");
      ec2 = new File("/Users/spr/Eclipse/" + dir);
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
	 if (i == 0) {
            ServerMonitor.pongEclipse(); 
            new IvyExec(cmd);
          }
	 else {
	    try {
	       Thread.sleep(100);
             }
            catch (InterruptedException e) { }
	  }
       }
    }
   catch (IOException e) { }
   
   throw new Error("Problem running Eclipse: " + cmd);
}




private static final class TestEclipseHandler implements MintHandler {

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
	       "QTYPE","ERROR",
	       "LINE",lno,
	       "METHOD",mthd,
	       "START",spos,"LOCATION",loc);
	 Element rslt = sendReply(sid,"QUERY",cargs,null);
	 Element q = IvyXml.getChild(rslt,"RESULTSET");
	 Assert.assertNotNull(q);
       }
    }
}


/********************************************************************************/
/*										*/
/*	Generic query tests							*/
/*										*/
/********************************************************************************/

private abstract class QueryTest {

   abstract void process(String sid,String rid);

}	// end of inner class QueryTest



/********************************************************************************/
/*										*/
/*	Variable query tests							*/
/*										*/
/********************************************************************************/

private class VarQueryTest extends QueryTest {

   private int line_number;
   private int start_offset;
   private String token_name;
   private String method_name;
   private String file_name;
   private Element query_result;

   VarQueryTest(int line,int start,String tok,String meth,String file) {
      line_number = line;
      start_offset = start;
      token_name = tok;
      method_name = meth;
      file_name = file;
      query_result = null;
    }

   Element getQueryResult()			{ return query_result; }
   String getFileName() 			{ return file_name; }
   String getTokenName()			{ return token_name; }

   @Override void process(String sid,String rid) {
      CommandArgs cargs = new CommandArgs("FILE",file_name,"LINE",line_number,

	    "START",start_offset,"TOKEN",token_name,"METHOD",method_name);
      Element xml = sendReply(sid,"VARQUERY",cargs,null);
      System.err.println("RESULT OF VARQUERY: " + IvyXml.convertXmlToString(xml));
      Assert.assertNotEquals(xml,null);
      query_result = IvyXml.getChild(xml,"VALUESET");
    }

}	// end of inner class VarQueryTest




/********************************************************************************/
/*										*/
/*	Entity query tests							*/
/*										*/
/********************************************************************************/

private class EntityQueryTest extends QueryTest {

   private CommandArgs command_args;
   private VarQueryTest var_query;
   private String match_text;

   EntityQueryTest(VarQueryTest qtest,String txt,Object... args) {
      command_args = new CommandArgs();
      for (int i = 0; i < args.length; i += 2) {
	 command_args.put((String) args[i],args[i+1]);
       }
      var_query = qtest;
      match_text = txt;
    }

   @Override void process(String sid,String rid) {
      Element qxml = var_query.getQueryResult();
      Element useref = null;
      Element useval = null;
      Element useent = null;
      top: for (Element refxml : IvyXml.children(qxml,"REFVALUE")) {
	 for (Element valxml : IvyXml.children(refxml,"VALUE")) {
	    Element seet = IvyXml.getChild(valxml,"ENTITYSET");
	    for (Element entxml : IvyXml.children(seet,"ENTITY")) {
	       if (checkMatch(entxml,match_text)) {
		  useref = refxml;
		  useval = valxml;
		  useent = entxml;
		  break top;
		}
	     }
	  }
       }
      Assert.assertNotEquals(useref,null);
      Assert.assertNotEquals(useval,null);
      Assert.assertNotEquals(useent,null);

      String cnm = IvyXml.getAttrString(useref,"CALL");
      cnm += "@" + IvyXml.getAttrString(useref,"CALLID");
      command_args.put("METHOD",cnm);
      command_args.put("FILE",var_query.getFileName());
      command_args.put("VARIABLE",var_query.getTokenName());

      Element loc = IvyXml.getChild(useref,"LOCATION");
      Element locp = IvyXml.getChild(loc,"POINT");
      command_args.put("LINE",IvyXml.getAttrInt(locp,"LINE"));
      command_args.put("START",IvyXml.getAttrInt(locp,"START"));
      command_args.put("LOCATION",IvyXml.getAttrInt(locp,"NODETYPEID"));
      int afterstart = IvyXml.getAttrInt(locp,"AFTERSTART");
      if (afterstart >= 0) {
	 command_args.put("AFTER",afterstart);
	 command_args.put("AFTERLOCATION",IvyXml.getAttrInt(locp,"AFTERTYPEID"));
       }

      Element typv = IvyXml.getChild(useval,"TYPE");
      command_args.put("TYPE",IvyXml.getAttrString(typv,"BASE"));

      command_args.put("ENTITY",IvyXml.getAttrInt(useent,"ID"));

      if (command_args.get("QTYPE") == null) {
	 if (command_args.get("SUBTYPE") != null) command_args.put("QTYPE","EXPLAIN");
	 else command_args.put("QTYPE","TO");
       }

      IvyXmlWriter refxw = new IvyXmlWriter();
      Element basv = IvyXml.getChild(useref,"REFERENCE");
      Element basv1 = IvyXml.getChild(basv,"VALUE");

      refxw.begin("REFERENCE");
      refxw.field("BASEID",IvyXml.getAttrInt(basv1,"BASE"));
      if (IvyXml.getAttrString(basv1,"FIELD") != null) {
	 refxw.field("FIELD",IvyXml.getAttrString(basv1,"FIELD"));
       }
      if (IvyXml.getAttrInt(basv1,"SLOT") >= 0) {
	 refxw.field("SLOT",IvyXml.getAttrInt(basv1,"SLOT"));
       }
      if (IvyXml.getAttrInt(basv1,"STACK") >= 0) {
	 refxw.field("STACK",IvyXml.getAttrInt(basv1,"STACK"));
       }
      refxw.end("REFERENCE");

      String refs = refxw.toString();
      refxw.close();

      Element xml = sendReply(sid,"QUERY",command_args,refs);
      System.err.println("RESULT OF ENTITYQUERY: " + IvyXml.convertXmlToString(xml));
      Assert.assertNotEquals(xml,null);
    }

   private boolean checkMatch(Element entxml,String txt) {
      String desc = IvyXml.getTextElement(entxml,"DESCRIPTION");
      if (desc.contains(txt)) return true;
      return false;
    }

}	// end of inner class EntityQueryTest



/********************************************************************************/
/*										*/
/*	Flow query test 							*/
/*										*/
/********************************************************************************/

private class FlowQueryTest extends QueryTest {

   private CommandArgs command_args;
   private String command_cnts;

   FlowQueryTest(String cnts,Object... args) {
      command_args = new CommandArgs();
      for (int i = 0; i < args.length; i += 2) {
	 command_args.put((String) args[i],args[i+1]);
       }
      command_cnts = cnts;
    }

   @Override void process(String sid,String rid) {
      Element xml = sendReply(sid,"FLOWQUERY",command_args,command_cnts);
      System.err.println("RESULT OF FLOWQUERY: " + IvyXml.convertXmlToString(xml));
      Assert.assertNotEquals(xml,null);
    }

}	// end of inner class FlowQueryTest



/********************************************************************************/
/*										*/
/*	Stack start test							*/
/*										*/
/********************************************************************************/

private class StackStartTest extends QueryTest {

   private CommandArgs command_args;
   private String command_cnts;

   StackStartTest(String cnts,Object... args) {
      command_args = new CommandArgs();
      for (int i = 0; i < args.length; i += 2) {
	 command_args.put((String) args[i],args[i+1]);
       }
      command_cnts = cnts;
    }

   @Override void process(String sid,String rid) {
      Element xml = sendReply(sid,"STACKSTART",command_args,command_cnts);
      System.err.println("RESULT OF STACKSTART: " + IvyXml.convertXmlToString(xml));
      Assert.assertNotEquals(xml,null);
    }

}	// end of inner class StackStartTest



/********************************************************************************/
/*										*/
/*	Main program to run a particular test without junit			*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   ServerTest st = new ServerTest();
   default_threads = 8;
   st.serverTestTimedJboard();
}




}	// end of class ServerTest




/* end of ServerTest.java */

