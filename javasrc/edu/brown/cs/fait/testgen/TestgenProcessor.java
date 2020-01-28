/********************************************************************************/
/*                                                                              */
/*              TestgenProcessor.java                                           */
/*                                                                              */
/*      Derive prior test generation state from current one                     */
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceState;

class TestgenProcessor implements TestgenConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceControl    fait_control;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

TestgenProcessor(IfaceControl ctrl)
{
   fait_control = ctrl;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

TestgenState computePriorState(TestgenState testst)
{
   IfaceProgramPoint pt = testst.getProgramPoint();
   IfaceState pstate = fait_control.findStateForLocation(testst.getCall(),pt);
   
   IfaceState prior = null;
   for (int i = 0; i < pstate.getNumPriorStates(); ++i) {
      IfaceState st0 = pstate.getPriorState(i);
      while (st0.getNumPriorStates() == 1 && st0.getLocation() == null) {
         st0 = st0.getPriorState(0);
       } 
      if (pstate.getNumPriorStates() == 1) {
         prior = st0;
         break;
       }
      else {
         TestgenLocation ploc = testst.getPriorLocation();
         if (ploc == null) {
            prior = st0;
            break;
          }
         else if (ploc.match(st0.getLocation().getProgramPoint())) {
            prior = st0;
            break;
          }
       }
    }
   
   Set<TestgenConstraint> next = new HashSet<>();
   for (TestgenConstraint cnst : testst.getConstraints()) {
      Collection<TestgenConstraint> upd = cnst.update(fait_control,prior,pstate);
      if (upd != null) next.addAll(upd);
    }
   
   // now update the constraints based on the prior state, its actions, etc.
   // will need to know the next state for checking conditionals
   // will need to update any value references
   // will need to create expression constraints as needed
   
   return null;
}


}       // end of class TestgenProcessor




/* end of TestgenProcessor.java */

