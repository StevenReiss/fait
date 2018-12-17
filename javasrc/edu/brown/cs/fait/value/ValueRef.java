/********************************************************************************/
/*                                                                              */
/*              ValueRef.java                                                   */
/*                                                                              */
/*      Reference value for AST evaluation                                      */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
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



package edu.brown.cs.fait.value;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;



class ValueRef extends ValueBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int             variable_slot;
private IfaceValue      base_value;
private IfaceField      field_name;
private IfaceValue      index_value;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValueRef(ValueFactory vf,IfaceType dt,int var,IfaceValue base,IfaceField fld,IfaceValue idx)
{
   super(vf,dt,null);
   
   variable_slot = var;
   base_value = base;
   field_name = fld;
   index_value = idx;
   
   if (base_value == null && field_name != null && !field_name.isStatic()) {
      System.err.println("ILLEGAL REFERENCE VALUE");
    }
   if (base == null && var == NO_REF && fld == null && idx == null) {
      FaitLog.logE("ILLEGAL REFERENCE VALUE");
    }
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public boolean isReference()                  { return true; }

@Override public int getRefSlot()                       
{ 
   if (variable_slot < 0) return NO_REF;
   
   return variable_slot; 
}

@Override public int getRefStack()
{
   if (variable_slot >= NO_REF) return NO_REF;
   return -variable_slot - 2;
}

@Override public IfaceValue getRefBase()                { return base_value; }
@Override public IfaceField getRefField()               { return field_name; }
@Override public IfaceValue getRefIndex()               { return index_value; }




@Override public IfaceValue mergeValue(IfaceValue v)    
{
   if (v == null || v == this) return this;
   
   if (v instanceof ValueRef) {
      ValueRef vr = (ValueRef) v;
      IfaceValue nb = vr.base_value;
      if (base_value != null) {
         nb = base_value.mergeValue(vr.base_value);
       }
      IfaceValue ni = vr.index_value;
      if (index_value != null) {
         ni = index_value.mergeValue(vr.index_value);
       }
      if (nb != base_value || ni != index_value) {
         IfaceType typ = findCommonParent(getDataType(),v.getDataType());
         return new ValueRef(value_factory,typ,variable_slot,nb,field_name,ni);
       }
    }
   
   return this;
}


@Override public IfaceValue restrictByType(IfaceType dt)
{
   IfaceType nt = getDataType().restrictBy(dt);
   if (nt == getDataType()) return this;
   return new ValueRef(value_factory,nt,variable_slot,base_value,field_name,index_value);
}



@Override public IfaceValue changeType(IfaceType dt)
{
   if (dt == getDataType()) return this;
   return new ValueRef(value_factory,dt,variable_slot,base_value,field_name,index_value);
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("[ ^^^");
   if (variable_slot >= 0) {
      buf.append(" v");
      buf.append(variable_slot);
    }
   else if (variable_slot < -1) {
      buf.append(" s");
      buf.append(getRefStack());
    }
   else if (field_name != null) {
      buf.append(" f:");
      buf.append(field_name);
    }
   buf.append(" ");
   buf.append(getDataType());
   buf.append("]");
   return buf.toString();
}





}       // end of class ValueRef




/* end of ValueRef.java */

