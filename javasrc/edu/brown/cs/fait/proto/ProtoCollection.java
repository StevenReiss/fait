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
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeMethod;

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

public ProtoCollection(FaitControl fc,JcodeDataType dt)
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

public IfaceValue prototype__constructor(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (args.size() == 2) {
      JcodeDataType atyp = fm.getArgType(0);
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

public synchronized  IfaceValue prototype_add(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
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



public synchronized IfaceValue prototype_addAll(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
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



public IfaceValue prototype_push(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue nv = args.get(1);

   prototype_add(fm,args,src);

   return nv;
}



public IfaceValue prototype_addElement(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_addFirst(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_addLast(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_insertElementAt(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_indexOf(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (element_value == null) return returnInt(-1);
   return returnAny(fm);
}


public IfaceValue prototype_setElementAt(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}


public IfaceValue prototype_offer(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_put(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}



public IfaceValue prototype_set(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_add(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	 Miscellaneous collection methods					*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_clone(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue av = fait_control.findAnyValue(getDataType());
   if (av != null) return av;
   
   IfaceEntity subs = fait_control.findPrototypeEntity(getDataType(),this,src);
   IfaceEntitySet cset = fait_control.createSingletonSet(subs);
   IfaceValue cv = fait_control.findObjectValue(getDataType(),cset,NullFlags.NON_NULL);

   return cv;
}


public IfaceValue prototype_comparator(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (comparator_value != null) return comparator_value;

   return returnNull(fm);
}



public synchronized IfaceValue prototype_contains(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   IfaceValue v = args.get(1);

   addElementChange(src);

   if (element_value == null) return returnFalse();
   else if (v.mustBeNull() && !element_value.canBeNull()) return returnFalse();
   // else check data type compatability

   return returnAny(fm);
}




public synchronized IfaceValue prototype_containsAll(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   addElementChange(src);
   
   return returnAny(fm);
}


public IfaceValue prototype_size(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   first_element.add(src);

   if (element_value == null) return returnInt(0);

   return returnAny(fm);
}



public IfaceValue prototype_isEmpty(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   first_element.add(src);

   if (element_value == null) return returnTrue();

   return returnAny(fm);
}




public synchronized IfaceValue prototype_setSize(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
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

public synchronized IfaceValue prototype_toArray(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
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
	 JcodeDataType dt = fait_control.findDataType("Ljava/lang/Object;");
	 array_entity = fait_control.findArrayEntity(dt,prototype_size(fm,null,src));
       }
      array_entity.addToArrayContents(element_value,null,src);
      IfaceEntitySet cset = fait_control.createSingletonSet(array_entity);
      cv = fait_control.findObjectValue(array_entity.getDataType(),cset,NullFlags.NON_NULL);
    }

   return cv;
}




public IfaceValue prototype_copyInto(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   prototype_toArray(fm,args,src);

   return returnVoid();
}




/********************************************************************************/
/*										*/
/*	Methods to access elements						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_get(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   if (!fm.getReturnType().isVoid()) addElementChange(src);

   return element_value;
}


public IfaceValue prototype_elementAt(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_first(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_firstElement(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_lastElement(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}



public IfaceValue prototype_getFirst(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_getLast(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_peek(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_poll(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}


public IfaceValue prototype_pop(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}



public IfaceValue prototype_take(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_get(fm,args,src);
}




/********************************************************************************/
/*										*/
/*	Element removal methods 						*/
/*										*/
/********************************************************************************/

public IfaceValue prototype_remove(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
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




public IfaceValue prototype_removeElement(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeElementAt(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeFirst(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeLast(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}



public IfaceValue prototype_removeRange(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_remove(fm,args,src);
}







/********************************************************************************/
/*										*/
/*	Elements returning subcollections					*/
/*										*/
/********************************************************************************/


public IfaceValue prototype_subList(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   JcodeDataType dt = fait_control.findDataType("Ljava/util/List;");
   IfaceEntity ie = fait_control.findPrototypeEntity(dt,this,src);
   IfaceEntitySet eset = fait_control.createSingletonSet(ie);
   IfaceValue v = fait_control.findObjectValue(dt,eset,NullFlags.NON_NULL);

   return v;
}



public IfaceValue prototype_headSet(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_subList(fm,args,src);
}



public IfaceValue prototype_subSet(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_subList(fm,args,src);
}



public IfaceValue prototype_tailSet(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return prototype_subList(fm,args,src);
}



public synchronized IfaceValue prototype_elements(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   JcodeDataType dt = fait_control.findDataType("Ljava/util/Enumeration;");

   if (enum_entity == null) {
      ProtoBase cp = new CollectionEnum(fait_control);
      enum_entity = fait_control.findPrototypeEntity(dt,cp,null);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(enum_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,NullFlags.NON_NULL);

   return cv;
}



public IfaceValue prototype_equals(JcodeMethod fm,List<IfaceValue> args,FaitLocation src)
{
   return returnAny(fm);
}




/********************************************************************************/
/*										*/
/*	Iterator methods							*/
/*										*/
/********************************************************************************/

synchronized public IfaceValue prototype_iterator(JcodeMethod fm,
      List<IfaceValue> args,FaitLocation src)
{
   JcodeDataType dt = fait_control.findDataType("Ljava/util/Iterator;");

   if (iter_entity == null) {
      ProtoBase cp = new CollectionIter(fait_control);
      iter_entity = fait_control.findPrototypeEntity(dt,cp,null);
    }

   IfaceEntitySet cset = fait_control.createSingletonSet(iter_entity);
   IfaceValue cv = fait_control.findObjectValue(dt,cset,NullFlags.NON_NULL);

   return cv;
}


synchronized public IfaceValue prototype_listIterator(JcodeMethod fm,
      List<IfaceValue> args,FaitLocation src)
{
   JcodeDataType dt = fait_control.findDataType("Ljava/util/ListIterator;");

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

   public IfaceValue prototype_hasNext(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_next(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
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

   public IfaceValue prototype_add(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      return ProtoCollection.this.prototype_add(fm,args,src);
    }

   public IfaceValue prototype_hasNext(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_hasPrevious(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      return prototype_hasNext(fm,args,src);
    }

   public IfaceValue prototype_next(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 addElementChange(src);
	 return element_value;
       }
    }

   public IfaceValue prototype_previous(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      return prototype_next(fm,args,src);
    }

   public IfaceValue prototype_set(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      return ProtoCollection.this.prototype_set(fm,args,src);
    }

}	// end of inner class CollectionListIter




@SuppressWarnings("unused")
private class CollectionEnum extends ProtoBase {

   CollectionEnum(FaitControl fc) {
      super(fc,fc.findDataType("Ljava/util/Enumeration;"));
    }

   public IfaceValue prototype_hasMoreElements(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 first_element.add(src);
	 if (element_value == null) return returnFalse();
	 return returnAny(fm);
       }
    }

   public IfaceValue prototype_nextElement(JcodeMethod fm,List<IfaceValue> args,FaitLocation src) {
      synchronized (ProtoCollection.this) {
	 addElementChange(src);
	 return element_value;
       }
    }

}	// end of inner class CollectionEnum

}	// end of class ProtoCollection




/* end of ProtoCollection.java */
