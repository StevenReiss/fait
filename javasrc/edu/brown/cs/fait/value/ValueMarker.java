/********************************************************************************/
/*										*/
/*		ValueMarker.java						*/
/*										*/
/*	Special marker for the value stack.  Doesn't really hold a value        */
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



package edu.brown.cs.fait.value;

import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceStackMarker;
import java.util.Set;
import java.util.HashSet;

class ValueMarker extends ValueBase implements IfaceStackMarker
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceProgramPoint	program_point;
private Set<Object>		marker_value;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValueMarker(ValueFactory vf,IfaceType voidtype,IfaceProgramPoint pt,Object v)
{
   super(vf,voidtype,null);
   program_point = pt;
   if (v == null) marker_value = null;
   else {
      marker_value = new HashSet<>(2);
      marker_value.add(v);
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override public IfaceValue mergeValue(IfaceValue cv)
{
   if (cv instanceof ValueMarker) {
      ValueMarker vm = (ValueMarker) cv;
      Set<Object> nval = null;
      if (marker_value != null) {
	 nval = new HashSet<>(marker_value);
       }
      if (vm.marker_value != null) {
	 if (nval == null) nval = new HashSet<>();
	 nval.addAll(vm.marker_value);
       }
      if (nval == null) return this;
      if (nval.equals(marker_value)) return this;
      if (nval.equals(vm.marker_value)) return vm;
      ValueMarker nvm = new ValueMarker(value_factory,getDataType(),program_point,null);
      nvm.marker_value = nval;
      return nvm;
    }

   return this;
}

@Override public IfaceValue restrictByType(IfaceType dt)
{
   return this;
}


@Override public IfaceValue changeType(IfaceType dt)
{
   return this;
}







/********************************************************************************/
/*										*/
/*	Marker methods								*/
/*										*/
/********************************************************************************/

@Override public IfaceProgramPoint getProgramPoint()
{
   return program_point;
}


@Override public Set<Object> getMarkerValues()
{
   return marker_value;
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override protected void outputLocalXml(IvyXmlWriter xw)
{
   xw.field("KIND","MARKER");
}



@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("MARKER[");
   buf.append(getProgramPoint().toString());
   if (marker_value != null) {
      buf.append("=");
      buf.append(marker_value.toString());
    }
   buf.append("]");
   return buf.toString();
}

}	// end of class ValueMarker




/* end of ValueMarker.java */

