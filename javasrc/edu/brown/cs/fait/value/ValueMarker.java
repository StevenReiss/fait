/********************************************************************************/
/*                                                                              */
/*              ValueMarker.java                                                */
/*                                                                              */
/*      Special marker for the value stack.  Doesn't really hold a value        */
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



package edu.brown.cs.fait.value;

import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceMarker;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

class ValueMarker extends ValueBase implements IfaceMarker
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceProgramPoint       program_point;
private Object                  marker_value;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValueMarker(ValueFactory vf,IfaceType voidtype,IfaceProgramPoint pt,Object v)
{
   super(vf,voidtype,null);
   program_point = pt;
   marker_value = v;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public IfaceValue mergeValue(IfaceValue cv)
{
   return this;
}

@Override public IfaceValue restrictByType(IfaceType dt)
{
   return this;
}



@Override protected IfaceValue newEntityValue(IfaceEntitySet es)
{
   return this;
}


/********************************************************************************/
/*                                                                              */
/*      Marker methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceProgramPoint getProgramPoint()
{
   return program_point;
}


@Override public Object getMarkerValue()
{
   return marker_value;
}



}       // end of class ValueMarker




/* end of ValueMarker.java */

