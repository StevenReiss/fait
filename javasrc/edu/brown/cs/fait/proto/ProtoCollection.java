/********************************************************************************/
/*										*/
/*		ProtoColleciton.java						*/
/*										*/
/*	Prototypes for colleciotns						*/
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

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceUpdater;
import edu.brown.cs.fait.iface.IfaceValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class ProtoCollection extends ProtoBase
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IfaceValue	element_value;
private IfaceEntity	array_entity;
private IfaceEntity	iter_entity;
private IfaceEntity	listiter_entity;
private IfaceEntity	enum_entity;
private Map<IfaceProgramPoint,IfaceEntity> sublist_entity;
private IfaceValue	comparator_value;

private Set<IfaceLocation> element_change;
private Set<IfaceLocation> first_element;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ProtoCollection(IfaceControl fc,IfaceType dt)
{
   super(fc,dt);

   element_value = null;
   array_entity = null;
   iter_entity = null;
   listiter_entity = null;
   enum_entity = null;
   comparator_value = null;
   sublist_entity = null;

   first_element = new HashSet<>(4);
   element_change = new HashSet<>(4);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void setAnyValue()
{
   IfaceType t0 = fait_control.findDataType("java.lang.Object");
   IfaceValue v0 = fait_control.findMutableValue(t0);
   element_value = v0.mergeValue(element_value);
}


/********************************************************************************/
/*										*/
/*	Collection constructor methods						*/
/*										*/
/********************************************************************************/

// CHECKSTYLE:OFF

public IfaceValue prototype__constructor(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   if (args.size() == 2) {
      IfaceType atyp = fm.getArgType(0);
      if (atyp.isIntType()) ;
      else if (atyp.getName().equals("java.util.Comparator")) {
	 comparator_value = args.get(1);
       }
      else if (atyp.isArrayType()) {
	 addArrayElements(args.get(1),src);
       }
      else {
	 prototype_addAll(fm,args,src);
       }
    }

   return returnAny(fm);
}



/********************************************************************************/
/*										*/
/*	Methods to add to a collection						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_add(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   if (args.size() < 2) returnAny(fm);
   
   IfaceValue nv = args.get(1);

   if (args.size() == 3) {
      if (nv.getDataType().isIntType()) {
	 nv = args.get(2);		// add(int,Object), set(int,Object)
       }
    }

   IfaceValue ov = element_value;
   mergeElementValue(nv,true);
   
   if (fm == null) return null;

   if (!fm.getReturnType().isVoidType()) {
      addElementChange(src);
      if (nv == null) return null;
      if (fm.getReturnType().isJavaLangObject()) return nv;
      else if (ov == null) return returnTrue();
    }

   return returnAny(fm);
}



public IfaceValue prototype_addAll(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue nv;

   if (args.size() == 2) {			 // addAll(colleciton)
      nv = args.get(1);
    }
   else {
      nv = args.get(2); 			// addAll(int,collection)
    }

   boolean canchng = false;

   for (IfaceEntity ie : nv.getEntities()) {
      IfacePrototype cp = ie.getPrototype();
      if (cp != null && cp instanceof ProtoCollection) {
	 ProtoCollection pc = (ProtoCollection) cp;
	 pc.addElementChange(src);
	 if (pc.element_value != null) {
	    mergeElementValue(pc.element_value,true);
	    canchng = true;
	  }
       }
    }

   if (!canchng) return returnFalse();

   return returnAny(fm);
}



private void addArrayElements(IfaceValue val,IfaceLocation src)
{
   for (IfaceEntity xe : val.getEntities()) {
      if (xe.getDataType().isArrayType()) {
	 // need to add reference here
       }
    }
   IfaceValue cv = val.getArrayContents();
   if (cv != null) {
      mergeElementValue(cv,true);
    }
}



public IfaceValue prototype_push(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue nv = args.get(1);

   prototype_add(fm,args,src);

   return nv;
}



public IfaceValue prototype_addElement(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_addFirst(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_addLast(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_insertElementAt(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_indexOf(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   if (element_value == null) return returnInt(-1);
   return returnAny(fm);
}


public IfaceValue prototype_setElementAt(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_offer(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_put(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_set(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_add(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	 Miscellaneous collection methods					*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_clone(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceType dt = getDataType();
   IfaceValue av = fait_control.findAnyValue(dt);
   if (av != null) return av;

   IfaceEntity subs = fait_control.findPrototypeEntity(dt,this,src,false);
   IfaceEntitySet cset = fait_control.createSingletonSet(subs);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,FaitAnnotation.NON_NULL);

   return cv;
}


public IfaceValue prototype_comparator(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   if (comparator_value != null) return comparator_value;

   return returnNull(fm);
}



public IfaceValue prototype_contains(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue v = args.get(1);

   addElementChange(src);

   if (element_value == null) return returnFalse();
   else if (v.mustBeNull() && !element_value.canBeNull()) return returnFalse();
   // else check data type compatability

   return returnAny(fm);
}




public IfaceValue prototype_containsAll(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   addElementChange(src);

   return returnAny(fm);
}


public IfaceValue prototype_size(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   synchronized (this) {
      first_element.add(src);
    }

   if (element_value == null) return returnInt(0);

   return fait_control.findAnyValue(fait_control.findDataType("int"));
}



public IfaceValue prototype_isEmpty(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   synchronized (this) {
      first_element.add(src);
    }

   if (element_value == null) return returnTrue();

   return returnAny(fm);
}




public synchronized IfaceValue prototype_setSize(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue cv = args.get(1);
   IfaceValue zero = fait_control.findConstantValue(fait_control.findDataType("int"),0);
   if (cv == zero) {
      // removeall
      return returnAny(fm);
    }

   IfaceValue nullv = fait_control.findNullValue();
   mergeElementValue(nullv,true);

   return prototype_remove(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	Array methods								*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_toArray(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue cv = null;

   if (!fm.getReturnType().isVoidType()) addElementChange(src);
   // addElementChange(src);

   if (args.size() == 2) {
      cv = args.get(1);
      for (IfaceEntity ie : cv.getEntities()) {
	 if (ie.replaceArrayContents(element_value,src)) {
	    if (FaitLog.isTracing())
	       FaitLog.logD1("Prototype: note entity changed " + ie + " (" + ie.hashCode() + ")");
	  }
       }
    }
   else {
      synchronized (this) {
	 if (array_entity == null) {
	    IfaceType dt = fait_control.findDataType("java.lang.Object");
	    array_entity = fait_control.findArrayEntity(dt,prototype_size(fm,null,src));
	  }
	 array_entity.replaceArrayContents(element_value,src);
	 IfaceEntitySet cset = fait_control.createSingletonSet(array_entity);
	 cv = fait_control.findObjectValue(array_entity.getDataType(),cset,FaitAnnotation.NON_NULL);
       }
    }

   return cv;
}




public IfaceValue prototype_copyInto(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   prototype_toArray(fm,args,src);

   return returnVoid();
}




/********************************************************************************/
/*										*/
/*	Methods to access elements						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_get(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   if (!fm.getReturnType().isVoidType()) addElementChange(src);

   if (element_value == null) {
      element_value = fait_control.findNullValue();
    }

   return element_value;
}


public IfaceValue prototype_elementAt(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_first(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_firstElement(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_lastElement(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}



public IfaceValue prototype_getFirst(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_getLast(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_peek(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_poll(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue v0 = prototype_get(fm,args,src);
   IfaceValue v1 = v0.allowNull();
   return v1;
}


public IfaceValue prototype_pop(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}



public IfaceValue prototype_take(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_get(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	Element removal methods 						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_remove(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   if (!fm.getReturnType().isVoidType()) addElementChange(src);

   if (args.size() > 2) {
      IfaceValue v = args.get(1);
      if (v.getDataType().isIntType()) return prototype_get(fm,args,src);
      if (element_value == null) return returnFalse();
      else if (v.mustBeNull() && !element_value.canBeNull()) return returnFalse();
      // check data type compatability
    }

   if (fm.getReturnType().isJavaLangObject()) {
      IfaceValue cv = element_value;
      if (cv == null) return returnNull(fm);
      cv = cv.allowNull();
      return cv;
    }

   return returnAny(fm);
}




public IfaceValue prototype_removeElement(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeElementAt(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeFirst(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeLast(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeRange(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_remove(fm,args,src);
}







/********************************************************************************/
/*										*/
/*	Elements returning subcollections					*/
/*										*/
/********************************************************************************/


public IfaceValue prototype_subList(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
  // IfaceType dt = fait_control.findDataType("java.util.List",FaitAnnotation.NON_NULL);
   IfaceType dt = args.get(0).getDataType();

   IfaceEntity ie = null;
   if (sublist_entity == null) sublist_entity = new HashMap<>();
   ie = sublist_entity.get(src.getProgramPoint());
   if (ie == null) {
      ie = fait_control.findPrototypeEntity(dt,this,src,false);
      sublist_entity.put(src.getProgramPoint(),ie);
    }
   IfaceEntitySet eset = fait_control.createSingletonSet(ie);
   IfaceValue v = fait_control.findObjectValue(dt,eset,FaitAnnotation.NON_NULL);

   return v;
}



public IfaceValue prototype_headSet(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_subList(fm,args,src);
}



public IfaceValue prototype_subSet(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_subList(fm,args,src);
}



public IfaceValue prototype_tailSet(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return prototype_subList(fm,args,src);
}

public IfaceValue prototype_sort(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return returnAny(fm);
}

public synchronized IfaceValue prototype_elements(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceType dt = fait_control.findDataType("java.util.Enumeration",FaitAnnotation.NON_NULL);

   if (enum_entity == null) {
      ProtoBase cp = new CollectionEnum(fait_control);
      enum_entity = fait_control.findPrototypeEntity(dt,cp,null,false);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(enum_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,FaitAnnotation.NON_NULL);

   return cv;
}



public IfaceValue prototype_equals(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return returnAny(fm);
}




/********************************************************************************/
/*										*/
/*	Iterator methods							*/
/*										*/
/********************************************************************************/

synchronized public IfaceValue prototype_iterator(IfaceMethod fm,
      List<IfaceValue> args,IfaceLocation src)
{
   IfaceType dt = fait_control.findDataType("java.util.Iterator",FaitAnnotation.NON_NULL);

   if (iter_entity == null) {
      ProtoBase cp = new CollectionIter(fait_control);
      iter_entity = fait_control.findPrototypeEntity(dt,cp,null,false);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(iter_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,FaitAnnotation.NON_NULL);

   return cv;
}


synchronized public IfaceValue prototype_listIterator(IfaceMethod fm,
      List<IfaceValue> args,IfaceLocation src)
{
   IfaceType dt = fait_control.findDataType("java.util.ListIterator",FaitAnnotation.NON_NULL);

   if (listiter_entity == null) {
      ProtoBase cp = new CollectionListIter(fait_control);
      listiter_entity = fait_control.findPrototypeEntity(dt,cp,null,false);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(listiter_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,FaitAnnotation.NON_NULL);

   return cv;
}


// CHECKSTYLE:ON

/********************************************************************************/
/*										*/
/*	Methods to handle state and value changes				*/
/*										*/
/********************************************************************************/

synchronized void mergeElementValue(IfaceValue v,boolean upd,IfaceLocation loc)
{
   mergeElementValue(v,upd);
   addElementChange(loc);
}


synchronized void mergeElementValue(IfaceValue v,boolean update)
{
   if (v != null && v.getDataType().isVoidType()) {
      FaitLog.logE("SET collection element void");
      return;
    }

   if (element_value == null) setElementValue(v,update);
   else if (v != null) setElementValue(element_value.mergeValue(v),update);
}


void setElementValue(IfaceValue v,boolean upd)
{
   if (v == element_value || v == null) return;
   
   if (FaitLog.isTracing()) {
      FaitLog.logD1("Update collection element value = " + v);
    }

   synchronized (this) {
      if (element_value == null) {
	 for (IfaceLocation loc : first_element) {
	    fait_control.queueLocation(loc);
	  }
	 first_element.clear();
       }

      element_value = v;
    }

   synchronized (element_change) {
      for (IfaceLocation loc : element_change) {
	 fait_control.queueLocation(loc);
       }
    }
}


void addElementChange(IfaceLocation src)
{
   if (src != null) {
      synchronized (this) {
	 element_change.add(src);
       }
    }
}


synchronized void addFirstElement(IfaceLocation src)
{
   if (src != null) {
      first_element.add(src);
    }
}


@Override public IfaceValue getArrayContents(IfaceValue v)
{
   return element_value;
}

@Override public List<IfaceValue> getContents(List<IfaceValue> rslt)
{
   if (element_value == null || element_value.mustBeNull()) return rslt;
   if (rslt == null) rslt = new ArrayList<>();
   rslt.add(element_value);
   return rslt;
}

IfaceValue getElementValue()			{ return element_value; }



/********************************************************************************/
/*										*/
/*	Methods for incremental updates 					*/
/*										*/
/********************************************************************************/

@Override public void handleUpdates(IfaceUpdater upd)
{
   if (element_value != null) {
      IfaceValue iv = upd.getNewValue(element_value);
      if (iv != null) element_value = iv;
    }
   if (comparator_value != null) {
      IfaceValue iv = upd.getNewValue(comparator_value);
      if (iv != null) comparator_value = iv;
    }

   if (array_entity != null) {
      array_entity.handleUpdates(upd);
    }

   for (Iterator<IfaceLocation> it = element_change.iterator(); it.hasNext(); ) {
      IfaceLocation loc = it.next();
      if (upd.isLocationRemoved(loc)) it.remove();
    }

   for (Iterator<IfaceLocation> it = first_element.iterator(); it.hasNext(); ) {
      IfaceLocation loc = it.next();
      if (upd.isLocationRemoved(loc)) it.remove();
    }
}



/********************************************************************************/
/*										*/
/*	Protoype iterator for collections					*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unused")
private class CollectionIter extends ProtoBase {

   CollectionIter(IfaceControl fc) {
      super(fc,fc.findDataType("java.util.Iterator",FaitAnnotation.NON_NULL));
    }

   // CHECKSTYLE:OFF
   
   public IfaceValue prototype_hasNext(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_next(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      addElementChange(src);
      if (element_value != null && element_value.isEmptyEntitySet())
	 return fait_control.findAnyValue(element_value.getDataType());
      return element_value;
    }
   
   // CHECKSTYLE:ON

}	// end of inner class CollectionIter





@SuppressWarnings("unused")
private class CollectionListIter extends ProtoBase {

   CollectionListIter(IfaceControl fc) {
      super(fc,fc.findDataType("java.util.ListIterator",FaitAnnotation.NON_NULL));
    }
   
   // CHECKSTYLE:OFF

   public IfaceValue prototype_add(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      return ProtoCollection.this.prototype_add(fm,args,src);
    }

   public IfaceValue prototype_hasNext(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_hasPrevious(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      return prototype_hasNext(fm,args,src);
    }

   public IfaceValue prototype_next(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      addElementChange(src);
      if (element_value != null && element_value.isEmptyEntitySet())
	 return fait_control.findAnyValue(element_value.getDataType());
      return element_value;
    }

   public IfaceValue prototype_previous(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      return prototype_next(fm,args,src);
    }

   public IfaceValue prototype_set(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      return ProtoCollection.this.prototype_set(fm,args,src);
    }
   
   // CHECKSTYLE:ON

}	// end of inner class CollectionListIter




@SuppressWarnings("unused")
private class CollectionEnum extends ProtoBase {

   CollectionEnum(IfaceControl fc) {
      super(fc,fc.findDataType("java.util.Enumeration",FaitAnnotation.NON_NULL));
    }
   
   // CHECKSTYLE:OFF

   public IfaceValue prototype_hasMoreElements(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_nextElement(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src) {
      addElementChange(src);
      return element_value;
    }
   
   // CHECKSTYLE:ON

}	// end of inner class CollectionEnum

}	// end of class ProtoCollection




/* end of ProtoCollection.java */
