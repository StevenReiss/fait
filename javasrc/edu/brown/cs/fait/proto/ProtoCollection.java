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

import edu.brown.cs.fait.iface.*;

import java.util.*;



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
private IfaceValue	comparator_value;

private Set<FaitLocation> element_change;
private Set<FaitLocation> first_element;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ProtoCollection(FaitControl fc,FaitDataType dt)
{
   super(fc,dt);

   element_value = null;
   array_entity = null;
   iter_entity = null;
   listiter_entity = null;
   enum_entity = null;
   comparator_value = null;

   first_element = new HashSet<FaitLocation>(4);
   element_change = new HashSet<FaitLocation>(4);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Collection constructor methods						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype__constructor(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (args.size() == 2) {
      FaitDataType atyp = fm.getArgType(0);
      if (atyp.getName().equals("int")) ;
      else if (atyp.getName().equals("java.util.Comparator")) {
	 comparator_value = args.get(1);
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

public synchronized  IfaceValue prototype_add(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue nv = args.get(1);

   if (args.size() == 3) {
      if (nv.getDataType().isInt()) {
	 nv = args.get(2);		// add(int,Object), set(int,Object)
       }
    }

   IfaceValue ov = element_value;
   mergeElementValue(nv);

   if (!fm.getReturnType().isVoid()) {
      addElementChange(src);
      if (nv == null) return null;
      if (fm.getReturnType().isJavaLangObject()) return nv;
      else if (ov == null) return returnTrue();
    }

   return returnAny(fm);
}



public synchronized IfaceValue prototype_addAll(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
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
	    mergeElementValue(pc.element_value);
	    canchng = true;
	  }
       }
    }

   if (!canchng) return returnFalse();

   return returnAny(fm);
}



public IfaceValue prototype_push(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue nv = args.get(1);

   prototype_add(fm,args,src);

   return nv;
}



public IfaceValue prototype_addElement(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_addFirst(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_addLast(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_insertElementAt(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_offer(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_put(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_set(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	 Miscellaneous collection methods					*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_clone(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceEntity subs = fait_control.findPrototypeEntity(getDataType(),this,src);
   IfaceEntitySet cset = fait_control.createSingletonSet(subs);
   IfaceValue cv = fait_control.findObjectValue(getDataType(),cset,NullFlags.NON_NULL);

   return cv;
}


public IfaceValue prototype_comparator(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (comparator_value != null) return comparator_value;

   return returnNull(fm);
}



public synchronized IfaceValue prototype_contains(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue v = args.get(1);

   addElementChange(src);

   if (element_value == null) return returnFalse();
   else if (v.mustBeNull() && !element_value.canBeNull()) return returnFalse();
   // else check data type compatability

   return returnAny(fm);
}


public IfaceValue prototype_size(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   first_element.add(src);

   if (element_value == null) return returnInt(0);

   return returnAny(fm);
}



public IfaceValue prototype_isEmpty(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   first_element.add(src);

   if (element_value == null) return returnTrue();

   return returnAny(fm);
}




public synchronized IfaceValue prototype_setSize(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue cv = args.get(1);
   IfaceValue zero = fait_control.findRangeValue(fait_control.findDataType("I"),0,0);
   if (cv == zero) {
      // removeall
      return returnAny(fm);
    }

   IfaceValue nullv = fait_control.findNullValue();
   mergeElementValue(nullv);

   return prototype_remove(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	Array methods								*/
/*										*/
/********************************************************************************/

public synchronized IfaceValue prototype_toArray(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue cv = null;

   if (!fm.getReturnType().isVoid()) addElementChange(src);

   if (args.size() == 2) {
      cv = args.get(1);
      for (IfaceEntity ie : cv.getEntities()) {
	 ie.addToArrayContents(element_value,null,src);
       }
    }
   else {
      if (array_entity == null) {
	 FaitDataType dt = fait_control.findDataType("Ljava/lang/Object;");
	 array_entity = fait_control.findArrayEntity(dt,prototype_size(fm,null,src));
	 array_entity.addToArrayContents(element_value,null,src);
	 IfaceEntitySet cset = fait_control.createSingletonSet(array_entity);
	 cv = fait_control.findObjectValue(array_entity.getDataType(),cset,NullFlags.NON_NULL);
       }
    }

   return cv;
}




public IfaceValue prototype_copyInto(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   prototype_toArray(fm,args,src);

   return returnVoid();
}




/********************************************************************************/
/*										*/
/*	Methods to access elements						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_get(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (!fm.getReturnType().isVoid()) addElementChange(src);

   return element_value;
}


public IfaceValue prototype_elementAt(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_first(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_firstElement(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}



public IfaceValue prototype_getFirst(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_getLast(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_peek(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_poll(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_pop(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}



public IfaceValue prototype_take(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	Element removal methods 						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_remove(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (!fm.getReturnType().isVoid()) addElementChange(src);

   if (args.size() > 2) {
      IfaceValue v = args.get(1);
      if (v.getDataType().isInt()) return prototype_get(fm,args,src);
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




public IfaceValue prototype_removeElement(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeElementAt(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeFirst(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeLast(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeRange(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}







/********************************************************************************/
/*										*/
/*	Elements returning subcollections					*/
/*										*/
/********************************************************************************/


public IfaceValue prototype_subList(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   FaitDataType dt = fait_control.findDataType("Ljava/util/List;");
   IfaceEntity ie = fait_control.findPrototypeEntity(dt,this,src);
   IfaceEntitySet eset = fait_control.createSingletonSet(ie);
   IfaceValue v = fait_control.findObjectValue(dt,eset,NullFlags.NON_NULL);

   return v;
}



public IfaceValue prototype_headSet(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_subList(fm,args,src);
}



public IfaceValue prototype_subSet(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_subList(fm,args,src);
}



public IfaceValue prototype_tailSet(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_subList(fm,args,src);
}



public synchronized IfaceValue prototype_elements(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   FaitDataType dt = fait_control.findDataType("Ljava/util/Enumeration;");

   if (enum_entity == null) {
      ProtoBase cp = new CollectionEnum(fait_control);
      enum_entity = fait_control.findPrototypeEntity(dt,cp,null);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(enum_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,NullFlags.NON_NULL);

   return cv;
}



public IfaceValue prototype_equals(FaitMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return returnAny(fm);
}




/********************************************************************************/
/*										*/
/*	Iterator methods							*/
/*										*/
/********************************************************************************/

synchronized public IfaceValue prototype_iterator(FaitMethod fm,
      List<IfaceValue> args,FaitLocation src)
{
   FaitDataType dt = fait_control.findDataType("Ljava/util/Iterator;");

   if (iter_entity == null) {
      ProtoBase cp = new CollectionIter(fait_control);
      iter_entity = fait_control.findPrototypeEntity(dt,cp,null);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(iter_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,NullFlags.NON_NULL);

   return cv;
}


synchronized public IfaceValue prototype_listIterator(FaitMethod fm,
      List<IfaceValue> args,FaitLocation src)
{
   FaitDataType dt = fait_control.findDataType("Ljava/util/ListIterator;");

   if (listiter_entity == null) {
      ProtoBase cp = new CollectionListIter(fait_control);
      listiter_entity = fait_control.findPrototypeEntity(dt,cp,null);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(listiter_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,NullFlags.NON_NULL);

   return cv;
}


/********************************************************************************/
/*										*/
/*	Methods to handle state and value changes				*/
/*										*/
/********************************************************************************/

synchronized void mergeElementValue(IfaceValue v)
{
   if (element_value == null) setElementValue(v);
   else if (v != null) setElementValue(element_value.mergeValue(v));
}


synchronized void setElementValue(IfaceValue v)
{
   if (v == element_value || v == null) return;

   if (element_value == null) {
      for (FaitLocation loc : first_element) {
	 fait_control.queueLocation(loc);
       }
      first_element.clear();
    }

   element_value = v;

   for (FaitLocation loc : element_change) {
      fait_control.queueLocation(loc);
    }
}


synchronized void addElementChange(FaitLocation src)
{
   if (src != null) {
      element_change.add(src);
    }
}


synchronized void addFirstElement(FaitLocation src)
{
   if (src != null) {
      first_element.add(src);
    }
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
}



/********************************************************************************/
/*										*/
/*	Protoype iterator for collections					*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unused")
private class CollectionIter extends ProtoBase {

   CollectionIter(FaitControl fc) {
      super(fc,fc.findDataType("Ljava/util/Iterator;"));
    }

   public IfaceValue prototype_hasNext(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_next(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 addElementChange(src);
	 return element_value;
       }
    }

}	// end of inner class CollectionIter





@SuppressWarnings("unused")
private class CollectionListIter extends ProtoBase {

   CollectionListIter(FaitControl fc) {
      super(fc,fc.findDataType("Ljava/util/ListIterator;"));
    }

   public IfaceValue prototype_add(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      return ProtoCollection.this.prototype_add(fm,args,src);
    }

   public IfaceValue prototype_hasNext(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_hasPrevious(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      return prototype_hasNext(fm,args,src);
    }

   public IfaceValue prototype_next(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 addElementChange(src);
	 return element_value;
       }
    }

   public IfaceValue prototype_previous(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      return prototype_next(fm,args,src);
    }

   public IfaceValue prototype_set(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      return ProtoCollection.this.prototype_set(fm,args,src);
    }

}	// end of inner class CollectionListIter




@SuppressWarnings("unused")
private class CollectionEnum extends ProtoBase {

   CollectionEnum(FaitControl fc) {
      super(fc,fc.findDataType("Ljava/util/Enumeration;"));
    }

   public IfaceValue prototype_hasMoreElements(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_nextElement(FaitMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 addElementChange(src);
	 return element_value;
       }
    }

}	// end of inner class CollectionEnum

}	// end of class ProtoCollection




/* end of ProtoCollection.java */
