/********************************************************************************/
/*                                                                              */
/*              IfaceLog.java                                                   */
/*                                                                              */
/*      Class to handle logging throughout fait                                 */
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


import java.io.*;

public class IfaceLog implements FaitConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PrintWriter             log_writer;
private LogLevel                log_level;

private static IfaceLog         the_logger;

static {
   the_logger = new IfaceLog();
}


/********************************************************************************/
/*                                                                              */
/*      Static methods                                                          */
/*                                                                              */
/********************************************************************************/

public static void logD(String msg)
{
   the_logger.log(LogLevel.DEBUG,0,msg);
}


public static void logD1(String msg)
{
   the_logger.log(LogLevel.DEBUG,1,msg);
}


public static void logI(String msg)
{
   the_logger.log(LogLevel.INFO,0,msg);
}


public static void logW(String msg)
{
   the_logger.log(LogLevel.WARNING,0,msg);
}


public static void logE(String msg)
{
   the_logger.log(LogLevel.ERROR,0,msg);
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private IfaceLog()
{
   log_level = LogLevel.WARNING;
   log_writer = null;
   
   try {
      log_writer = new PrintWriter(new FileWriter("fait.log"));
    }
   catch (IOException e) { }
   if (log_writer == null) {
      log_writer = new PrintWriter(new OutputStreamWriter(System.err));
    }
}




/********************************************************************************/
/*                                                                              */
/*      Logging methods                                                         */
/*                                                                              */
/********************************************************************************/

private void log(LogLevel lvl,int indent,String msg)
{
   if (lvl.ordinal() > log_level.ordinal()) return;
   
   log_writer.print(lvl.toString().charAt(0));
   log_writer.print(": ");
   for (int i = 0; i < indent; ++i) {
      log_writer.print("   ");
    }
   log_writer.println(msg);
}
   


}       // end of class IfaceLog




/* end of IfaceLog.java */

