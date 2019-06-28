/********************************************************************************/
/*                                                                              */
/*              ControlDescriptionFile.java                                     */
/*                                                                              */
/*      Implementation of a prioritized description file                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
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



package edu.brown.cs.fait.control;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import edu.brown.cs.fait.iface.IfaceDescriptionFile;


public class ControlDescriptionFile implements IfaceDescriptionFile,
        Comparable<ControlDescriptionFile>
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int priority_value;
private File file_name;
private int instance_count;
private File library_file;

private static AtomicInteger instance_counter = new AtomicInteger();


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ControlDescriptionFile(File f,int p) 
{
   priority_value = p;
   file_name = f;
   library_file = null;
   instance_count = instance_counter.incrementAndGet();
}

public ControlDescriptionFile(File f,File lib) 
{
   priority_value = PRIORITY_LIBRARY;
   file_name = f;
   library_file = lib;
   instance_count = instance_counter.incrementAndGet();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public File getFile()                      { return file_name; }
@Override public int getPriority()                   { return priority_value; }
@Override public File getLibrary()                   { return library_file; }




/********************************************************************************/
/*                                                                              */
/*      Comparator methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public int compareTo(ControlDescriptionFile f1)
{
   if (priority_value < f1.priority_value) return 1;
   if (priority_value > f1.priority_value) return -1;
   if (instance_count < f1.instance_count) return -1;
   if (instance_count > f1.instance_count) return 1;
   return 0;
}


}       // end of class ControlDescriptionFile




/* end of ControlDescriptionFile.java */

