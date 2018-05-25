/********************************************************************************/
/*                                                                              */
/*              ValueString.java                                                */
/*                                                                              */
/*      String objects                                                          */
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

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

class ValueString extends ValueObject
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          known_value;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValueString(ValueFactory vf,IfaceType typ,IfaceEntitySet es,IfaceAnnotation [] fgs)
{
   super(vf,typ,es,fgs);
   known_value = null;
}


ValueString(ValueFactory vf,IfaceType typ,String s,IfaceEntitySet es)
{
   super(vf,typ,es,FaitAnnotation.NON_NULL);
   known_value = s;
}


/********************************************************************************/
/*                                                                              */
/*      Operations                                                              */
/*                                                                              */
/********************************************************************************/

@Override protected IfaceValue localPerformOperation(IfaceType typ,IfaceValue rhs,
      FaitOperator op,IfaceLocation src) 
{
   switch (op) {
      case ADD :
         if (rhs instanceof ValueString) {
            ValueString rval = (ValueString) rhs;
            if (known_value != null && rval.known_value != null) {
               return value_factory.constantString(known_value + rval.known_value);
             }
          }
         return value_factory.constantString();
    }
   
   return super.localPerformOperation(typ,rhs,op,src);
}


}       // end of class ValueString




/* end of ValueString.java */

