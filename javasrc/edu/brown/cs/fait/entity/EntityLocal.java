/********************************************************************************/
/*                                                                              */
/*              EntityLocal.java                                                */
/*                                                                              */
/*      Entity for a new operator                                               */
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



package edu.brown.cs.fait.entity;

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeField;




class EntityLocal extends EntityObject
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private FaitLocation    base_location;
private boolean         is_unique;
private EntityObject    non_unique;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EntityLocal(FaitLocation where,JcodeDataType cls,boolean uniq)
{
   super(cls);
   base_location = where;
   is_unique = uniq;
   non_unique = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods
/*                                                                              */
/********************************************************************************/

@Override public FaitLocation getLocation()     { return base_location; }



/********************************************************************************/
/*                                                                              */
/*      Unique source methods                                                   */
/*                                                                              */
/********************************************************************************/

boolean isUnique()                      { return is_unique; }

@Override public synchronized IfaceEntity makeNonunique()
{
   if (!is_unique) return this;
   
   if (non_unique == null) {
      non_unique = new EntityLocal(base_location,getDataType(),false);
      copyFields(non_unique);
    }
   
   return non_unique;
}



FaitEntity getNonunique()               { return non_unique; }




@Override public void setFieldContents(IfaceValue fv,JcodeField fld)
{
   if (non_unique != null) non_unique.setFieldContents(fv,fld);
   super.setFieldContents(fv,fld);
}


@Override public boolean addToFieldContents(IfaceValue fv,JcodeField fld)
{
   boolean fg = false;
   
   if (non_unique != null) {
      fg |= non_unique.addToFieldContents(fv,fld);
    }
   
   fg |= super.addToFieldContents(fv,fld);
   
   return fg;
}


@Override public FaitValue getFieldValue(JcodeField fld)
{
   if (non_unique != null) return non_unique.getFieldValue(fld);
   return super.getFieldValue(fld);
}




/********************************************************************************/
/*                                                                              */
/*      Output and debugging methods                                            */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   String uq = (is_unique ? "New*" : "New");
   String loc = "NONE";
   if (base_location != null) loc = base_location.toString();
   
   return "Local " + uq + " " + getDataType().getName() + " @ " + loc + " " + hashCode();
}


}       // end of class EntityLocal




/* end of EntityLocal.java */

