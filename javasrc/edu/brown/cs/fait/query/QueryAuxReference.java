/********************************************************************************/
/*                                                                              */
/*              QueryAuxReference.java                                          */
/*                                                                              */
/*      Auxiliary reference to look at for back flow                            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.fait.query;

import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceValue;


class QueryAuxReference implements IfaceAuxReference 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceLocation   base_location;
private IfaceValue      ref_value;
private IfaceAuxRefType aux_type;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryAuxReference(IfaceLocation loc,IfaceValue ref,IfaceAuxRefType typ)
{
   base_location = loc;
   ref_value = ref;
   aux_type = typ;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceLocation getLocation()            { return base_location; }

@Override public IfaceValue getReference()              { return ref_value; }

@Override public IfaceAuxRefType getRefType()           { return aux_type; }



/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object o) 
{
   if (o instanceof IfaceAuxReference) {
      IfaceAuxReference r = (IfaceAuxReference) o;
      if (r.getLocation().equals(base_location)) {
         IfaceValue oref = r.getReference();
         if (ref_value.getRefStack() == oref.getRefStack() &&
               ref_value.getRefField() == oref.getRefField() &&
               ref_value.getRefSlot() == oref.getRefSlot()) {
            IfaceValue v0 = ref_value.getRefBase();
            IfaceValue v1 = oref.getRefBase();
            if (v0 == null && v1 == null) return true;
            if (v0 != null && v0.equals(v1)) return true;
          }
       }
           
    }
   
   return false;
}


@Override public int hashCode()
{
   int hc = base_location.hashCode();
   hc ^= ref_value.getRefStack();
   hc ^= ref_value.getRefSlot()*100;
   IfaceField fld = ref_value.getRefField();
   if (fld != null) hc ^= ref_value.getRefField().hashCode();
   IfaceValue bas = ref_value.getRefBase();
   if (bas != null) hc ^= bas.hashCode();
   return hc;
}



}       // end of class QueryAuxReference




/* end of QueryAuxReference.java */

