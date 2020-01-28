/********************************************************************************/
/*                                                                              */
/*              TestgenReferenceValue.java                                      */
/*                                                                              */
/*      Value that is a reference to a stack/local/field/array                  */
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



package edu.brown.cs.fait.testgen;

import java.util.List;

import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;

class TestgenReferenceValue extends TestgenValue implements TestgenConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int slot_value;  
private int stack_value;   
private TestgenValue base_value;
private IfaceField field_ref;
private TestgenValue index_ref;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TestgenReferenceValue(IfaceType t,int slot,boolean var)
{
   this(t);
   if (var) slot_value = slot;
   else stack_value = slot;
}

TestgenReferenceValue(TestgenValue base,IfaceField fld) 
{
   this(fld.getType());
   base_value = base;
   field_ref = fld;
}

TestgenReferenceValue(TestgenValue base,TestgenValue idx)
 {
   this(base.getDataType().getBaseType());
   base_value = base;
   index_ref = idx;
}

private TestgenReferenceValue(IfaceType t) 
{
   super(t);
   slot_value = -1;
   stack_value = -1;
   base_value = null;
   field_ref = null;
   index_ref = null;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

int getRefStack()
{
   return stack_value;
}


IfaceField getRefField()
{
   return field_ref;
}


int getRefSlot()
{
   return slot_value;
}




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override protected void updateInternal(IfaceControl fc,IfaceState prior,IfaceState cur,List<TestgenValue> rslt)
{
   rslt.add(this);
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/


@Override public String toString() 
{
   StringBuffer buf = new StringBuffer();
   buf.append("<^^^");
   if (stack_value >= 0) {
      buf.append("S");
      buf.append(stack_value);
    }
   else if (slot_value >= 0) {
      buf.append("V");
      buf.append(slot_value);
    }
   else {
      if (base_value != null) {
         buf.append(base_value.toString());
       }
      if (field_ref != null) {
         buf.append(" . ");
         buf.append(field_ref.getFullName());
       }
      else if (index_ref != null) {
         buf.append(" [] ");
         buf.append(index_ref.toString());
       }
    } 
   buf.append(">");
   return buf.toString();
}




}       // end of class TestgenReferenceValue




/* end of TestgenReferenceValue.java */

