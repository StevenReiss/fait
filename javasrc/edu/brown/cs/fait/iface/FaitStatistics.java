/********************************************************************************/
/*                                                                              */
/*              FaitStatistics.java                                             */
/*                                                                              */
/*      Statistics inforamtion                                                  */
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



package edu.brown.cs.fait.iface;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class FaitStatistics
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int     num_scans;
private int     num_forward;
private int     num_backward;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public FaitStatistics()
{
   this(0,0,0);
}


public FaitStatistics(int scan,int fwd,int bkwd)
{
   num_scans = scan;
   num_forward = fwd;
   num_backward = bkwd;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public int getNumScans()                { return num_scans; }
public int getNumForward()              { return num_forward; }
public int getNumBackward()             { return num_backward; }



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

public void add(FaitStatistics fs)
{
   num_scans += fs.num_scans;
   num_forward += fs.num_forward;
   num_backward += fs.num_backward;
}



public void add(FaitStatistics fs,double fract)
{
   num_scans += Math.round(fs.num_scans*fract);
   num_forward += Math.round(fs.num_forward*fract);
   num_backward += Math.round(fs.num_backward*fract);
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(String id,IvyXmlWriter xw)
{
   xw.begin(id);
   xw.field("SCANS",num_scans);
   xw.field("FORWARD",num_forward);
   xw.field("BACKWARD",num_backward);
   xw.end(id);
}


public boolean accept(double cutoff,double scancutoff)
{
   if (num_forward >= cutoff || num_scans >= scancutoff) return true;
   
   return false;
}



}       // end of class FaitStatistics




/* end of FaitStatistics.java */

