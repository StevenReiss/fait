/********************************************************************************/
/*                                                                              */
/*              FaitLog.java                                                    */
/*                                                                              */
/*      Logging methods                                                         */
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FaitLog implements FaitConstants
{



/********************************************************************************/
/*                                                                              */
/*      Internal types                                                          */
/*                                                                              */
/********************************************************************************/

public enum LogLevel {
   ERROR, WARNING, INFO, DEBUG
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static LogLevel log_level;
private static boolean	use_stderr;
private static PrintWriter log_writer;
private static boolean  trace_execution;

static {
   use_stderr = false;
   log_level = LogLevel.INFO;
   log_writer = null;
   trace_execution = false;
}


/********************************************************************************/
/*										*/
/*	Logging entries 							*/
/*										*/
/********************************************************************************/

public static void logE(String msg,Throwable t)
{
   log(LogLevel.ERROR,0,msg,t);
}


public static void logE(String msg)
{
   log(LogLevel.ERROR,0,msg,null);
}


public static void logX(String msg) 
{
   Throwable t = new Throwable(msg);
   log(LogLevel.ERROR,0,msg,t);
}


public static void logW(String msg)
{
   log(LogLevel.WARNING,0,msg,null);
}


public static void logI(String msg)
{
   log(LogLevel.INFO,0,msg,null);
}

public static void logI1(String msg)
{
   log(LogLevel.INFO,1,msg,null);
}





public static void logD(String msg,Throwable t)
{
   log(LogLevel.DEBUG,0,msg,t);
}




public static void logD(String msg)
{
   log(LogLevel.DEBUG,0,msg,null);
}


public static void logD1(String msg)
{
   log(LogLevel.DEBUG,1,msg,null);
}



/********************************************************************************/
/*										*/
/*	Control methods 							*/
/*										*/
/********************************************************************************/

public static void setLogLevel(LogLevel lvl)
{
   log_level = lvl;
}


public static void setLogFile(String f)
{
   setLogFile(new File(f));
}



public static void setLogFile(File f)
{
   if (log_writer != null) {
      log_writer.close();
      log_writer = null;
    }
   
   f = f.getAbsoluteFile();
   try {
      log_writer = new PrintWriter(new FileWriter(f));
    }
   catch (IOException e) {
      FaitLog.logE("Can't open log file " + f);
      log_writer = null;
    }
}


public static void useStdErr(boolean fg)
{
   use_stderr = fg;
}


/********************************************************************************/
/*                                                                              */
/*      Execution trace entries                                                 */
/*                                                                              */
/********************************************************************************/

public static boolean isTracing()               { return trace_execution; }

public static void setTracing(boolean fg)       { trace_execution = fg; }




/********************************************************************************/
/*										*/
/*	Actual logging routines 						*/
/*										*/
/********************************************************************************/

private static void log(LogLevel lvl,int indent,String msg,Throwable t)
{
   if (lvl.ordinal() > log_level.ordinal()) return;
   
   String s = lvl.toString().substring(0,1);
   String sth = "*";
   Thread th = Thread.currentThread();
   if (th instanceof IfaceWorkerThread) {
      IfaceWorkerThread wt = (IfaceWorkerThread) th;
      sth = Integer.toString(wt.getWorkerId());
    }
   String pfx = "FAIT:" + sth + ":" + s + ": ";
   
   for (int i = 0; i < indent; ++i) pfx += "   ";
   
   if (log_writer != null) {
      log_writer.println(pfx + msg);
      if (t != null) t.printStackTrace(log_writer);
      log_writer.flush();
    }
   if (use_stderr || log_writer == null) {
      System.err.println(pfx + msg);
      if (t != null) t.printStackTrace();
      System.err.flush();
    }
}








}       // end of class FaitLog




/* end of FaitLog.java */

