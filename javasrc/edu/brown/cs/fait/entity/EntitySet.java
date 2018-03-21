/********************************************************************************/
/*										*/
/*		EntitySet.java							*/
/*										*/
/*	Representation of a set of entities for static checking 		*/
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


class EntitySet implements IfaceEntitySet, EntityConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private EntityFactory		entity_factory;
private BitSet			set_contents;
private Map<Object,EntitySet>   next_map;

private static final Object	MODEL_SET = new Object();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

EntitySet(EntityFactory ef,BitSet s)
{
   entity_factory = ef;
   set_contents = (BitSet) s.clone();
   next_map = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Set methods								*/
/*										*/
/********************************************************************************/

@Override public boolean contains(IfaceEntity s)
{
   return set_contents.get(s.getId());
}


@Override public boolean isEmpty()
{
   return set_contents.isEmpty();
}



@Override public int size()
{
   return set_contents.cardinality();
}



@Override public boolean overlaps(IfaceEntitySet ics)
{
   if (ics == null) return false;

   EntitySet cs = (EntitySet) ics;
   if (set_contents.intersects(cs.set_contents)) return true;

   return false;
}


@Override public boolean contains(IfaceEntitySet es1)
{
   EntitySet es = (EntitySet) es1;
   BitSet bs = (BitSet) es.set_contents.clone();
   bs.andNot(set_contents);
   return bs.isEmpty();
}



/********************************************************************************/
/*										*/
/*	Iterator methods							*/
/*										*/
/********************************************************************************/

@Override public Iterable<IfaceEntity> getEntities()
{
   return new EntitySetIterable(this);
}



private Iterator<IfaceEntity> getEntityIterator()
{
   return new EntitySetIterator(set_contents);
}



private static class EntitySetIterable implements Iterable<IfaceEntity> {

   private EntitySet for_set;

   EntitySetIterable(EntitySet es) {
      for_set = es;
    }

   public Iterator<IfaceEntity> iterator() {
      return for_set.getEntityIterator();
    }

}	// end of inner class EntitySetIterable




private class EntitySetIterator implements Iterator<IfaceEntity> {

   private BitSet bit_set;
   private int	  current_bit;

   EntitySetIterator(BitSet bs) {
      bit_set = bs;
      current_bit = bs.nextSetBit(0);
    }

   @Override public boolean hasNext()			{ return current_bit >= 0; }
   @Override public IfaceEntity next() {
      IfaceEntity fe = entity_factory.getEntity(current_bit);
      current_bit = bit_set.nextSetBit(current_bit+1);
      return fe;
    }

   @Override public void remove() throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
    }

}	// end of inner class EntitySetIterator




/********************************************************************************/
/*										*/
/*	Methods to generate new sets by addition				*/
/*										*/
/********************************************************************************/

@Override public IfaceEntitySet addToSet(IfaceEntitySet es0)
{
   if (es0 == null || es0.size() == 0) return this;
   if (size() == 0) return es0;

   EntitySet es = (EntitySet) es0;
   EntitySet rslt = this;
   List<IfaceEntity> props = null;

   synchronized (this) {
      EntitySet nxt = next_map.get(es);
      if (nxt != null) return nxt;
    }

   BitSet bs = (BitSet) es.set_contents.clone();
   bs.andNot(set_contents);
   if (!bs.isEmpty()) {
      List<IfaceEntity> v = new ArrayList<IfaceEntity>();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
	 IfaceEntity fe = entity_factory.getEntity(i);
	 if (fe != null) v.add(fe);
       }
      bs.or(set_contents);
      rslt = entity_factory.findSet(bs);
      props = v;
    }

   synchronized (this) {
      next_map.put(es,rslt);
    }

   if (props != null) propogateMap(rslt,props);

   return rslt;
}




private void propogateMap(EntitySet rslt,List<IfaceEntity> props)
{
   if (rslt.next_map.size() != 0) return;

   Map<Object,EntitySet> nexts = null;
   synchronized (this) {
      nexts = new HashMap<>(next_map);
    }

   synchronized (rslt) {
      if (rslt.next_map.size() != 0) return;
      for (Map.Entry<Object,EntitySet> ent : nexts.entrySet()) {
         Object o = ent.getKey();
         if (o instanceof IfaceType) {
            IfaceType bdt = (IfaceType) o;
            boolean okfg = false;
            for (IfaceEntity ie : props) {
               IfaceType fdt1 = ie.getDataType();
               if (fdt1 == null ||
                     entity_factory.compatibleTypes(fdt1,bdt)) {
                  okfg = false;
                  break;
                }
             }
            if (okfg) rslt.next_map.put(bdt,ent.getValue());
          }
       }
    }
}





/********************************************************************************/
/*										*/
/*	Methods to generate new sets by restriction				*/
/*										*/
/********************************************************************************/

@Override public IfaceEntitySet restrictByType(IfaceType dt)
{
   return handleTypeRestricts(dt);
}






EntitySet handleTypeRestricts(IfaceType dt)
{
   if (dt == null) return this;
   
   synchronized (this) {
      EntitySet es = next_map.get(dt);
      if (es != null) return es;

      BitSet bs = null;
      boolean havedt = false;
      for (IfaceEntity ie1 : getEntities()) {
	 EntityBase ie = (EntityBase) ie1;
	 boolean fg = true;
	 if (ie.getDataType() != null) {
            IfaceType ty1 = ie.getDataType();
            ty1 = ty1.getRunTimeType();
	    fg = entity_factory.compatibleTypes(ty1,dt);
	  }
	 if (!fg) {
	    if (bs == null) bs = (BitSet) set_contents.clone();
	    bs.clear(ie.getId());
	    // TODO: might want to ignore cast if not in project
	    Collection<IfaceEntity> bl = ie.mutateTo(dt,entity_factory);
	    if (bl != null) {
	       for (IfaceEntity xe : bl) {
		  bs.set(xe.getId());
		  havedt = true;
		}
	     }
	  }
	 else {
	    if (!havedt && ie.getDataType() != null) havedt = true;
	  }
       }

      if (bs != null && !havedt) {
	 bs.clear();
       }

      EntitySet rslt = (bs == null ? this : entity_factory.findSet(bs));
      
      synchronized (this) {
         next_map.put(dt,rslt);
       }

      return rslt;
    }
}







/********************************************************************************/
/*										*/
/*	Methods to generate the set of all model sources of a set		*/
/*										*/
/********************************************************************************/

@Override public IfaceEntitySet getModelSet()
{
   synchronized (next_map) {
      EntitySet es = next_map.get(MODEL_SET);
      if (es == null) {
	 BitSet bs = null;
	 for (IfaceEntity ie : getEntities()) {
	    if (ie.getUserEntity() != null) {
	       if (bs == null) bs = new BitSet();
	       bs.set(ie.getId());
	     }
	  }
	 if (bs == null) es = entity_factory.createEmptySet();
	 else es = entity_factory.findSet(bs);
	 next_map.put(MODEL_SET,es);
       }
      return es;
    }
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuilder buf = new StringBuilder();
   buf.append("{");
   int ctr = 0;
   for (IfaceEntity ie : getEntities()) {
      if (ctr++ != 0) buf.append(",");
      buf.append(ie.toString());
    }
   buf.append("}");
   return buf.toString();
}




}	// end of class EntitySet




/* end of EntitySet.java */
