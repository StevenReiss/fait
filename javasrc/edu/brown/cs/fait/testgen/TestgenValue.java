/********************************************************************************/
/*                                                                              */
/*              TestgenValue.java                                               */
/*                                                                              */
/*      Holder of a value for a constraint                                      */
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

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

abstract class TestgenValue implements TestgenConstants
{



/********************************************************************************/
/*                                                                              */
/*      Creation method                                                         */
/*                                                                              */
/********************************************************************************/

static TestgenValue create(IfaceControl fc,boolean v)
{
   long bv = (v ? 1 : 0);
   return new PrimitiveValue(fc.findConstantType("boolean",v),bv);
}

static TestgenValue create(IfaceControl fc,int v)
{
   return new PrimitiveValue(fc.findConstantType("int",v),v);
}


static TestgenValue create(IfaceControl fc,long v)
{
   return new PrimitiveValue(fc.findConstantType("long",v),v);
}


static TestgenValue create(IfaceControl fc,String v)
{
   return new PrimitiveValue(fc.findConstantType("java.lang.String",v),v);
}



static TestgenValue create(IfaceType rtyp,FaitOperator op,TestgenValue lhs,TestgenValue rhs)
{
   return new ExpressionValue(rtyp,op,lhs,rhs);
}


static TestgenValue create(IfaceType rtyp,FaitOperator op,TestgenValue lhs)
{
   return new ExpressionValue(rtyp,op,lhs,null);
}


static TestgenValue createNull(IfaceControl fc)
{
   IfaceValue v0 = fc.findNullValue();
   return new PrimitiveValue(v0.getDataType(),null);
}



static TestgenValue createStack(IfaceType t0,int offset)
{
   return new TestgenReferenceValue(t0,offset,false);
}


static TestgenValue createFieldRef(TestgenValue base,IfaceField fld)
{
   return new TestgenReferenceValue(base,fld);
}

static TestgenValue createSlot(IfaceType t0,int slot)
{
   return new TestgenReferenceValue(t0,slot,false);
}


static TestgenValue create(IfaceValue v0)
{
   if (v0 == null) return null;
   if (v0.isReference()) {
      int stk = v0.getRefStack();
      if (stk >= 0) return new TestgenReferenceValue(v0.getDataType(),stk,false);
      int slot = v0.getRefSlot();
      if (slot >= 0) return new TestgenReferenceValue(v0.getDataType(),slot,true);
      if (v0.getRefField() != null) {
         return new TestgenReferenceValue(create(v0.getRefBase()),v0.getRefField());
       }
      else if (v0.getRefIndex() != null) {
         return new TestgenReferenceValue(create(v0.getRefBase()),
               create(v0.getRefIndex()));
       }
    }
   if (v0.getIndexValue() != null) {
      return new PrimitiveValue(v0.getDataType(),v0.getIndexValue());
    }
   else {
      return new VariableValue(v0);
    }
}


static TestgenValue createVariable(IfaceType t0)
{
   return new VariableValue(t0);
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceType       data_type;

private static int      variable_counter = 0;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected TestgenValue(IfaceType typ)
{ 
   data_type = typ;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

IfaceType getDataType()                         { return data_type; }



/********************************************************************************/
/*                                                                              */
/*      Update value                                                            */
/*                                                                              */
/********************************************************************************/

List<TestgenValue> update(IfaceControl fc,IfaceState prior,IfaceState cur) 
{
   List<TestgenValue> rslt = new ArrayList<>();
   
   updateInternal(fc,prior,cur,rslt);
   
   return rslt;
}


protected abstract void updateInternal(IfaceControl fc,IfaceState prior,IfaceState cur,
      List<TestgenValue> rslt);




/********************************************************************************/
/*                                                                              */
/*      Primitive value                                                         */
/*                                                                              */
/********************************************************************************/

private static class PrimitiveValue extends TestgenValue {
   
   private Object value_constant;
   
   PrimitiveValue(IfaceType t,Object v) {
      super(t);
      value_constant = v;
    }
   
   @Override public String toString() {
      return "<" + value_constant + ">";
    }
   
   @Override protected void updateInternal(IfaceControl fc,IfaceState prior,IfaceState cur,
         List<TestgenValue> rslt) { 
      rslt.add(this);
    }
   
}       // end of inner class PrimitiveValue







/********************************************************************************/
/*                                                                              */
/*      Expression value                                                        */
/*                                                                              */
/********************************************************************************/

private static class ExpressionValue extends TestgenValue {

   private TestgenValue lhs_value;
   private TestgenValue rhs_value;
   private FaitOperator expr_op;
   
   ExpressionValue(IfaceType rtyp,FaitOperator op,TestgenValue lhs,TestgenValue rhs) {
      super(rtyp);
      lhs_value = lhs;
      rhs_value = rhs;
      expr_op = op;
    }
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("<");
      buf.append(expr_op);
      buf.append(": ");
      buf.append(lhs_value.toString());
      buf.append(",");
      buf.append(rhs_value.toString());
      return buf.toString();
    }
   
   @Override protected void updateInternal(IfaceControl fc,IfaceState prior,
         IfaceState cur,List<TestgenValue> rslt) {
      List<TestgenValue> nlhs = lhs_value.update(fc,prior,cur);
      List<TestgenValue> nrhs = rhs_value.update(fc,prior,cur);
      if (nlhs.size() == 1 && nlhs.get(0) == lhs_value &&
            nrhs.size() == 1 && nrhs.get(0) == rhs_value) {
         rslt.add(this);
       }
      else {
         for (TestgenValue lv : nlhs) {
            for (TestgenValue rv : nrhs) {
               ExpressionValue ev = new ExpressionValue(getDataType(),expr_op,lv,rv);
               rslt.add(ev);
             }
          }
       }
    }
   
}       // end of inner class ExpressionValue



/********************************************************************************/
/*                                                                              */
/*      Constraint Variable Value                                               */
/*                                                                              */
/********************************************************************************/

private static class VariableValue extends TestgenValue {
   
   private String variable_name;
   private IfaceValue base_range;
   
   VariableValue(IfaceType typ) {
      super(typ);
      variable_name = "V_" + (++variable_counter);
      base_range = null;
    }
   
   VariableValue(IfaceValue v0) {
      super(v0.getDataType());
      variable_name = "V_" + (++variable_counter);
      if (v0.getMinValue() == null && v0.getMaxValue() == null) base_range = null;
      else base_range = v0;
    }
   
   @Override protected void updateInternal(IfaceControl fc,IfaceState prior,IfaceState cur,
         List<TestgenValue> rslt) { 
      rslt.add(this);
    }
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("<");
      buf.append(variable_name);
      if (base_range != null) {
         buf.append(" ");
         buf.append(base_range.getMinValue());
         buf.append("..");
         buf.append(base_range.getMaxValue());
       }
      buf.append(">");
      return buf.toString();
    }
   
}       // end of inner class VariableValue



}       // end of class TestgenValue




/* end of TestgenValue.java */

