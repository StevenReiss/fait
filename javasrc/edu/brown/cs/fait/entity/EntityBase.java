/********************************************************************************/
/*										*/
/*		EntityBase.java 						*/
/*										*/
/*	Basic entity holder abstract class					*/
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

import java.util.*;


class EntityBase implements IfaceEntity, EntityConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int		entity_id;
private boolean 	used_in_lock;


private static int		entity_counter = 0;
private static List<EntityBase> all_entities = new Vector<EntityBase>();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected EntityBase()
{
   synchronized (EntityBase.class) {
      entity_id = entity_counter++;
      all_entities.add(this);
    }
   used_in_lock = false;
}



/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

@Override public int getId()				{ return entity_id; }

@Override public FaitDataType getDataType()		{ return null; }
@Override public FaitLocation getLocation()	{ return null; }
@Override public boolean isUsedInLock() 		{ return used_in_lock; }
@Override public UserEntity getUserEntity()		{ return null; }
@Override public IfacePrototype getPrototype()          { return null; }

@Override public boolean isNative()		{ return false; }
@Override public boolean isUserEntity()         { return false; }




/********************************************************************************/
/*										*/
/*	Static access methods							*/
/*										*/
/********************************************************************************/

static EntityBase getEntity(int id)
{
   return all_entities.get(id);
}




/********************************************************************************/
/*										*/
/*	IFaceEntity methods							*/
/*										*/
/********************************************************************************/

@Override public IfaceEntity makeNonunique()				{ return null; }

public Collection<IfaceEntity> mutateTo(FaitDataType dt,FaitLocation fl,EntityFactory factory)
{
   return null;
}

@Override public void setFieldContents(IfaceValue fv,FaitField fld)	 { }
@Override public boolean addToFieldContents(IfaceValue fv,FaitField fld)
{
   return false;
}
@Override public FaitValue getFieldValue(FaitField fld) 	
{
   return null;
}


@Override public void setArrayContents(IfaceValue fv)			{ }
@Override public FaitValue getArrayValue(IfaceValue idx) 		{ return null; }
@Override public boolean addToArrayContents(IfaceValue fv,IfaceValue idx,FaitLocation loc)
{
   return false;
}





/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void handleUpdates(IfaceUpdater upd)			{ }






}	// end of class EntityBase




/* end of EntityBase.java */

