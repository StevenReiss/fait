/********************************************************************************/
/*                                                                              */
/*              TestgenState.java                                               */
/*                                                                              */
/*      State holder for test case generation                                   */
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



package edu.brown.cs.fait.testgen;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceProgramPoint;

class TestgenState implements TestgenConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceProgramPoint       program_point;
private IfaceCall               for_call;
private Set<TestgenConstraint>  constraint_set;
private List<TestgenLocation>   trace_path;
private int                     path_index;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TestgenState(IfaceCall c,IfaceProgramPoint pt,List<TestgenLocation> path,TestgenConstraint initcnst)
{
   for_call = c;
   program_point = pt;
   constraint_set = new HashSet<>();
   if (initcnst != null) constraint_set.add(initcnst);
   trace_path = path;
   if (path != null && path.size() > 0) path_index = 0;
   else path_index = -1;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceProgramPoint getProgramPoint()                     { return program_point; }
IfaceCall getCall()                                     { return for_call; }
Collection<TestgenConstraint> getConstraints()          { return constraint_set; }

TestgenLocation getCurrentLocation()
{
   if (path_index < 0) return null;
   return trace_path.get(path_index);
}


TestgenLocation getPriorLocation()
{
   if (path_index < 0 || path_index+1 >= trace_path.size()) return null;
   return trace_path.get(path_index+1);
}



}       // end of class TestgenState




/* end of TestgenState.java */

