/********************************************************************************/
/*										*/
/*		EntityUser.java 						*/
/*										*/
/*	Fait entity definitions user (model) entity representation		*/
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
import edu.brown.cs.ivy.xml.IvyXmlWriter;




class EntityUser extends EntityBase implements IfaceEntity.UserEntity
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	entity_type;
private IfaceLocation base_location;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

EntityUser(String id,IfaceLocation base)
{
   entity_type = id;
   base_location = base;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getEntityType() 		{ return entity_type; }

@Override public boolean isUserEntity()                 { return true; }

@Override public IfaceLocation getLocation()	        { return base_location; }

@Override public UserEntity getUserEntity()		{ return this; }



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected void outputLocalXml(IvyXmlWriter xw)
{
   xw.field("KIND","USER");
   xw.field("ETYPE",entity_type);
}



@Override public String toString()
{
   String rslt = "USER " + getEntityType();
   if (base_location != null) {
      rslt += " @ " + base_location.toString();
    }
   return rslt;
}




}	// end of class EntityUser




/* end of EntityUser.java */

