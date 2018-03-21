/********************************************************************************/
/*										*/
/*		EntityTest.java 						*/
/*										*/
/*	Test methods for entities						*/
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



package edu.brown.cs.fait.entity;

import edu.brown.cs.fait.iface.*;


import org.junit.*;




public class EntityTest
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private EntityFactory	entity_factory;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public EntityTest()
{
   entity_factory = new EntityFactory(null);
}


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Test for creating sources						*/
/*										*/
/********************************************************************************/

@Test public void createSources()
{
   for (int i = 0; i < 10000; ++i) {
      String id = "Source_" + i;
      entity_factory.createUserEntity(id,null);
    }

   Assert.assertEquals(entity_factory.getEntity(5555).getId(),5555);
}



/********************************************************************************/
/*										*/
/*	Test for set operations 						*/
/*										*/
/********************************************************************************/

@Test public void setTest()
{
   IfaceEntitySet es0 = entity_factory.createEmptySet();
   IfaceEntitySet es1 = entity_factory.createEmptySet();
   
   for (int i = 0; i < 10000; ++i) {
      String id = "Source_" + i;
      entity_factory.createUserEntity(id,null);
    }
   
   for (int i = 0; i < 100; i += 6) {
      IfaceEntity ie = entity_factory.getEntity(i);
      IfaceEntitySet ns = entity_factory.createSingletonSet(ie);
      es0 = es0.addToSet(ns);
    }

   for (int i = 0; i < 100; i += 9) {
      IfaceEntity ie = entity_factory.getEntity(i);
      IfaceEntitySet ns = entity_factory.createSingletonSet(ie);
      es1 = es1.addToSet(ns);
    }

   Assert.assertEquals(es0.size(),17);
   Assert.assertEquals(es1.size(),12);

   IfaceEntitySet es2 = es0.addToSet(es1);
   Assert.assertEquals(es2.size(),23);
}




}	// end of class EntityTest




/* end of EntityTest.java */

