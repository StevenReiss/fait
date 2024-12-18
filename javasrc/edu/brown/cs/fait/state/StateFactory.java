/********************************************************************************/
/*										*/
/*		StateFactory.java						*/
/*										*/
/*	Factory class for creating and managing states				*/
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



package edu.brown.cs.fait.state;

import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceSafetyStatus;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceUpdater;
import edu.brown.cs.fait.iface.IfaceValue;




public class StateFactory implements StateConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private StateField	field_values;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public StateFactory(IfaceControl fc)
{
   field_values = new StateField(fc);
}



/********************************************************************************/
/*										*/
/*	State creation methods							*/
/*										*/
/********************************************************************************/

public IfaceState createState(int numlocal,IfaceSafetyStatus sts)
{
   return new StateBase(numlocal,sts);
}



/********************************************************************************/
/*										*/
/*	Field management methods						*/
/*										*/
/********************************************************************************/

public IfaceValue getFieldValue(IfaceState state,IfaceField fld,IfaceValue base,boolean thisref,
				   IfaceLocation src)
{
   return field_values.getFieldValue((StateBase) state,fld,base,thisref,src);
}



public boolean setFieldValue(IfaceState st,IfaceField fld,
				IfaceValue v,IfaceValue base,boolean thisref,
				IfaceLocation src)
{
   return field_values.setFieldValue((StateBase) st,fld,thisref,v,base,src);
}



public void handleUpdates(IfaceUpdater upd)
{
   field_values.handleUpdates(upd);
}


}	// end of class StateFactory




/* end of StateFactory.java */
