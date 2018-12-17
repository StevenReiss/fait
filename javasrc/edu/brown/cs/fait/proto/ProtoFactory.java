/********************************************************************************/
/*										*/
/*		ProtoFactory.java						*/
/*										*/
/*	Factory class for creating and managing prototypes for system classes	*/
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



package edu.brown.cs.fait.proto;

import edu.brown.cs.fait.iface.*;

import java.util.*;


public class ProtoFactory implements ProtoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private enum ProtoWhich {
   NONE, COLLECTION, MAP
}



private IfaceControl	fait_control;
private Map<IfaceType,ProtoWhich> class_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ProtoFactory(IfaceControl fc)
{
   fait_control = fc;
   class_map = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Methods to create prototypes                                            */
/*                                                                              */
/********************************************************************************/

public IfacePrototype createPrototype(IfaceType dt)
{
   ProtoWhich which = null;
   
   synchronized (class_map) {
      which = class_map.get(dt);
      if (which == null) {
         if (dt.isDerivedFrom(fait_control.findDataType("java.util.Collection"))) {
            which = ProtoWhich.COLLECTION;
          }
         else if (dt.isDerivedFrom(fait_control.findDataType("java.util.Map"))) {
            which = ProtoWhich.MAP;
          }
         else which = ProtoWhich.NONE;
         class_map.put(dt,which);
       }
    }
   
   
   switch (which) {
      default :
         return null;
      case COLLECTION :
         return new ProtoCollection(fait_control,dt);
      case MAP :
         return new ProtoMap(fait_control,dt);
    }
}
      


/********************************************************************************/
/*                                                                              */
/*      Check if method is handled by prototype code                            */
/*                                                                              */
/********************************************************************************/

static boolean isMethodRelevant(IfaceMethod fm,IfaceType basetype)
{
   IfaceType dt = fm.getDeclaringClass();
   String dnm = dt.getName();
   if (dnm.startsWith("java.util.") || dnm.equals("java.lang.Iterable") ||
         dnm.startsWith("sun.security.")) {
      return true;
    }
   for (IfaceMethod im : fm.getParentMethods()) {
      IfaceType pt = im.getDeclaringClass();
      if (pt.getName().startsWith("java.util.")) {
         return true;
       }
    }
   
   if (FaitLog.isTracing()) {
      FaitLog.logD1("Non-prototype method on prototype: " + fm);
    }
   
   return false;
}


}	// end of class ProtoFactory




/* end of ProtoFactory.java */

