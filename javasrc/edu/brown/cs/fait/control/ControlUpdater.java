/********************************************************************************/
/*										*/
/*		ControlUpdater.java						*/
/*										*/
/*	Incremental update manager						*/
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



package edu.brown.cs.fait.control;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.fait.iface.FaitConstants;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceUpdateSet;
import edu.brown.cs.fait.iface.IfaceUpdater;
import edu.brown.cs.fait.iface.IfaceValue;

class ControlUpdater implements FaitConstants, IfaceUpdater
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceUpdateSet		update_set;
private ControlMain		fait_control;
private Set<IfaceCall>		new_updates;
private Set<IfaceCall>		done_updates;
private Set<IfaceEntity>	remove_entities;
private Map<IfaceEntitySet,IfaceEntitySet> entityset_map;
private Map<IfaceValue,IfaceValue> value_map;
private Set<QueueItem>		queued_calls;
private Set<IfaceType>		updated_types;
private Set<IfaceBaseType>	updated_basetypes;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ControlUpdater(ControlMain cm,IfaceUpdateSet upd)
{
   fait_control = cm;
   update_set = upd;
   new_updates = null;
   done_updates = null;
   remove_entities = new HashSet<>();
   entityset_map = new HashMap<>();
   value_map = new HashMap<>();
   queued_calls = new HashSet<>();
   updated_types = new HashSet<>();
   updated_basetypes = new HashSet<>();
}



/********************************************************************************/
/*										*/
/*	Update (call) Processing methods					*/
/*										*/
/********************************************************************************/

void processUpdate()
{
   // get initial set of updated calls
   Set<IfaceCall> upds = new HashSet<>();
   for (IfaceCall ic : fait_control.getAllCalls()) {
      if (update_set.shouldUpdate(ic)) {
	 upds.add(ic);
       }
    }

   fait_control.removeCalls(upds);

   // loop over those calls to remove them and find more to update
   done_updates = new HashSet<>();
   for ( ; ; ) {
      new_updates = null;
      done_updates.addAll(upds);
      for (IfaceCall ic : upds) {
	 ic.removeForUpdate(this);
       }
      if (new_updates == null) break;
      upds = new_updates;
    }

   Collection<IfaceType> utyps = update_set.getUpdatedTypes(fait_control);
   if (utyps != null) {
      for (IfaceType typ : utyps) {
	 if (typ != null) {
	    updated_types.add(typ);
	    if (typ.getJavaType() != null) updated_basetypes.add(typ.getJavaType());
	  }
       }
    }

   // next we have to update entity sets and remove the entities
   fait_control.updateEntitySets(this);

   // then use the entity sets to update values related to entities
   fait_control.updateValues(this);

   // then update states
   fait_control.updateStates(this);
   fait_control.updateFlow(this);

   // add queued calls to work queue
   for (QueueItem qi : queued_calls) {
      qi.addCall(fait_control,this);
    }

   done_updates = null;
}




/********************************************************************************/
/*										*/
/*	Update access methods							*/
/*										*/
/********************************************************************************/

@Override public boolean shouldUpdate(IfaceCall ic)
{
   if (new_updates != null && new_updates.contains(ic)) return true;
   if (done_updates != null && done_updates.contains(ic)) return true;;
   return update_set.shouldUpdate(ic);
}



@Override public void removeCall(IfaceCall ic)
{
   if (done_updates.contains(ic)) return;

   if (new_updates == null) new_updates = new HashSet<>();
   new_updates.add(ic);
   
   FaitLog.logD("CALL","Add to call updates " + ic.hashCode() + " " + ic);
}



@Override public boolean isLocationRemoved(IfaceLocation loc)
{
   return done_updates.contains(loc.getCall());
}


@Override public boolean isCallRemoved(IfaceCall ic)
{
   if (done_updates.contains(ic)) return true;

   return false;
}


@Override public boolean isTypeRemoved(IfaceType it)
{
   if (updated_types.contains(it)) return true;

   return false;
}


@Override public boolean isTypeRemoved(IfaceBaseType it)
{
   if (updated_basetypes.contains(it)) return true;

   return false;
}


@Override public Collection<IfaceType> getTypesRemoved()
{
   return updated_types;
}



/********************************************************************************/
/*										*/
/*	Requeue items								*/
/*										*/
/********************************************************************************/

@Override public void addToWorkQueue(IfaceCall ic,IfaceProgramPoint pt)
{
   queued_calls.add(new QueueItem(ic,pt));
}


private static class QueueItem {

   private IfaceCall for_call;
   private IfaceProgramPoint from_where;

   QueueItem(IfaceCall ic,IfaceProgramPoint pt) {
      for_call = ic;
      from_where = pt;
    }

   void addCall(ControlMain cm,ControlUpdater upd) {
      if (!upd.isCallRemoved(for_call)) {
	 cm.queueLocation(for_call,from_where);
       }
    }

   @Override public int hashCode() {
      return for_call.hashCode() + from_where.hashCode();
    }

   @Override public boolean equals(Object o) {
      if (o instanceof QueueItem) {
	 QueueItem qi = (QueueItem) o;
	 return for_call == qi.for_call && from_where == qi.from_where;
       }
      return false;
    }

}	// end of inner class QueueItem




/********************************************************************************/
/*										*/
/*	Entity Set setup methods						*/
/*										*/
/********************************************************************************/

@Override public void addEntityToRemove(IfaceEntity ie)
{
   remove_entities.add(ie);
}

@Override public Collection<IfaceEntity> getEntitiesToRemove()
{
   return remove_entities;
}


@Override public void addToEntitySetMap(IfaceEntitySet oset,IfaceEntitySet nset)
{
   if (oset != null && nset != null && oset != nset) entityset_map.put(oset,nset);
}



/********************************************************************************/
/*										*/
/*	Value setup methods							*//*										  */
/********************************************************************************/

public void addToValueMap(IfaceValue oval,IfaceValue nval)
{
   if (oval != null && nval != null && oval != nval) {
      value_map.put(oval,nval);
      if (nval.getDataType().isNumericType()) {
	 FaitLog.logE("CONTROL","Setting value map numeric " + oval + " -> " + nval);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

public IfaceValue getNewValue(IfaceValue v)
{
   return value_map.get(v);
}


public IfaceEntitySet getNewEntitySet(IfaceEntitySet s)
{
   return entityset_map.get(s);
}


}	// end of class ControlUpdater




/* end of ControlUpdater.java */

