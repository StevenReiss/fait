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
import java.lang.reflect.*;


public class ProtoFactory implements ProtoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FaitControl	fait_control;
private Map<FaitDataType,Class<?>> class_map;

private static final Class<?> [] cnst_params = new Class<?> [] {
   FaitControl.class, FaitDataType.class
};


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ProtoFactory(FaitControl fc)
{
   fait_control = fc;
   class_map = new HashMap<FaitDataType,Class<?>>();
}



/********************************************************************************/
/*                                                                              */
/*      Methods to create prototypes                                            */
/*                                                                              */
/********************************************************************************/

public IfacePrototype createPrototype(FaitDataType dt)
{
   Class<?> c = null;
   
   synchronized (class_map) {
      if (!class_map.containsKey(dt)) {
         if (!dt.isProjectClass()) {
            if (dt.isDerivedFrom(fait_control.findDataType("Ljava/util/Collection;"))) {
               c = ProtoCollection.class;
             }
            else if (dt.isDerivedFrom(fait_control.findDataType("Ljava/util/Map;"))) {
               c = ProtoMap.class;
             }
          }
         class_map.put(dt,c);
       }
      else c = class_map.get(dt);
    }
   
   if (c == null) return null;
   
   ProtoBase pb = null;
   try {
      Constructor<?> cnst = c.getConstructor(cnst_params);
      pb = (ProtoBase) cnst.newInstance(fait_control,dt);
    }
   catch (NoSuchMethodException e) { }
   catch (Exception e) {
      System.err.println("FAIT: Problem creating class prototype for " + dt + ": " + e);
    }
   
   return pb;
}
      



}	// end of class ProtoFactory




/* end of ProtoFactory.java */

