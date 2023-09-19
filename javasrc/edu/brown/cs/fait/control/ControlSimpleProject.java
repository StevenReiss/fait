/********************************************************************************/
/*                                                                              */
/*              ControlSimpleProject.java                                       */
/*                                                                              */
/*      Simple project for testing and other uses                               */
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



package edu.brown.cs.fait.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.IfaceDescriptionFile;
import edu.brown.cs.fait.iface.IfaceProject;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.jcomp.JcompTyper;

public class ControlSimpleProject implements IfaceProject, FaitConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcodeFactory    jcode_factory;
private JcompProject    jcomp_project;
private JcompTyper      jcomp_typer;
private String          project_prefix;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ControlSimpleProject(String cp,String pfx)
{
   jcode_factory = new JcodeFactory();
   if (cp != null) {
      jcode_factory.addToClassPath(cp);
    }
   JcompControl ctrl = new JcompControl(jcode_factory);
   List<JcompSource> srcs = new ArrayList<>();
   jcomp_project = ctrl.getProject(jcode_factory,srcs);
   jcomp_project.resolve();
   jcomp_typer = jcomp_project.getResolveTyper();
   
   project_prefix = pfx;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public JcompTyper getTyper()                  { return jcomp_typer; }
@Override public JcodeFactory getJcodeFactory()         { return jcode_factory; }
@Override public JcompProject getJcompProject()         { return jcomp_project; }


@Override public Collection<IfaceDescriptionFile> getDescriptionFiles() { return null; }


@Override public boolean isProjectClass(String cls) {
   if (cls.startsWith(project_prefix)) return true;
   return false;
}

@Override public boolean isEditableClass(String cls)    { return false; }

@Override public String getSourceFileForClass(String cls)
{
   return null;
}

}       // end of class ControlSimpleProject




/* end of ControlSimpleProject.java */

