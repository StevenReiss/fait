/********************************************************************************/
/*                                                                              */
/*              FlowBackElement.java                                            */
/*                                                                              */
/*      Information for back propagation                                        */
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



package edu.brown.cs.fait.flow;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceBackElement;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

class FlowBackElement implements IfaceBackElement, FlowConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceProgramPoint program_point;
private IfaceValue ref_value;
private IfaceType set_type;





/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowBackElement(IfaceProgramPoint pt,IfaceValue ref,IfaceType set) 
{
   if (pt == null) { 
      FaitLog.logD("Attempt to create back element with no program point");
    }
   
   program_point = pt;
   ref_value = ref;
   set_type = set;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceProgramPoint getProgramPoint()    { return program_point; }
@Override public IfaceValue getReference()              { return ref_value; }
@Override public IfaceType getSetType()                 { return set_type; }





}       // end of class FlowBackElement




/* end of FlowBackElement.java */

