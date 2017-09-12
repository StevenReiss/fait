/********************************************************************************/
/*                                                                              */
/*              IfaceIvyProject.java                                            */
/*                                                                              */
/*      description of class                                                    */
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.ivy.project.IvyProject;

public class IfaceIvyProject implements FaitProject
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IvyProject      base_project;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public IfaceIvyProject(IvyProject ip)
{
   base_project = ip;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public boolean isProjectClass(String cls)
{
   return base_project.isUserClass(cls);
}



@Override public String getClasspath()
{
   StringBuffer buf = new StringBuffer();
   for (String s : base_project.getClassPath()) {
      if (buf.length() > 0) buf.append(File.pathSeparator);
      buf.append(s);
    }
   return buf.toString();
}



@Override public Collection<String> getBaseClasses()
{
   Collection<String> rslt = new ArrayList<>();
   for (String s : base_project.getStartClasses()) {
      rslt.add(s);
    }
   return rslt;
}



@Override public Collection<String> getStartClasses()
{
   return null;
}



@Override public FaitMethodData createMethodData(FaitCall arg0)
{
   return null;
}



@Override public List<File> getDescriptionFile()
{
   return null;
}



}       // end of class IfaceIvyProject




/* end of IfaceIvyProject.java */

