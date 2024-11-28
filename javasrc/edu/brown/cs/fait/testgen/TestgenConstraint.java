/********************************************************************************/
/*                                                                              */
/*              TestgenConstraint.java                                          */
/*                                                                              */
/*      Representation of a constraint for test case generation                 */
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

import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceError;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.brown.cs.fait.iface.IfaceCall;

abstract class TestgenConstraint implements TestgenConstants
{



/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

static TestgenConstraint createConstraint(IfaceControl fc,IfaceCall c,IfaceProgramPoint pt,IfaceError err)
{
   return null;
}

static TestgenConstraint createConstraint(TestgenValue expr)
{
   return new ExpressionConstraint(expr);
}




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected TestgenConstraint()
{
}



/********************************************************************************/
/*                                                                              */
/*      Updating methods                                                        */
/*                                                                              */
/********************************************************************************/

Collection<TestgenConstraint> update(IfaceControl fc,IfaceState prior,IfaceState cur)
{
   List<TestgenConstraint> rslt = new ArrayList<>();
   
   localUpdate(fc,prior,cur,rslt);
   
   return rslt;
}


protected abstract void localUpdate(IfaceControl fc,IfaceState prior,IfaceState cur,
      List<TestgenConstraint> rslt);




/********************************************************************************/
/*                                                                              */
/*      Expression constraint                                                   */
/*                                                                              */
/********************************************************************************/

private static class ExpressionConstraint extends TestgenConstraint {

   private TestgenValue expression_value;
   
   ExpressionConstraint(TestgenValue expr) {
      expression_value = expr;
    }
   
   @Override protected void localUpdate(IfaceControl fc,IfaceState prior,IfaceState cur,
         List<TestgenConstraint> rslt) {
      List<TestgenValue> nvals = expression_value.update(fc,prior,cur);
      if (nvals.size() == 1 && nvals.get(0) == expression_value) {
         rslt.add(this);
       }
      else {
         for (TestgenValue expv : nvals) {
            ExpressionConstraint exc = new ExpressionConstraint(expv);
            rslt.add(exc);
          }
       }
    }
   
}       // end of inner class ExpressionConstraint



}       // end of class TestgenConstraint




/* end of TestgenConstraint.java */

