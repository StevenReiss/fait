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

import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import java.util.Collection;




class EntityLocal extends EntityObject
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceLocation    base_location;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EntityLocal(IfaceLocation where,IfaceType cls,IfacePrototype ptyp)
{
   super(cls,ptyp);
   base_location = where;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods
/*                                                                              */
/********************************************************************************/

@Override public IfaceLocation getLocation()     { return base_location; }



/********************************************************************************/
/*                                                                              */
/*      Unique source methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public void setFieldContents(IfaceValue fv,String key)
{
   super.setFieldContents(fv,key);
}


@Override public boolean addToFieldContents(IfaceValue fv,String key)
{
   boolean fg = false;
   
   fg |= super.addToFieldContents(fv,key);
   
   return fg;
}


@Override public IfaceValue getFieldValue(String key)
{
   return super.getFieldValue(key);
}


/********************************************************************************/
/*                                                                              */
/*      Mutation methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public Collection<IfaceEntity> mutateTo(IfaceType typ,EntityFactory ef)
{
   Collection<IfaceEntity> rslt = super.mutateTo(typ,ef);
   
   if (getPrototype() != null && typ.getBaseType() == getDataType().getBaseType()) {
//    rslt = new ArrayList<>();
//    rslt.add(ef.createLocalEntity(base_location,typ,getPrototype()));
    }

  return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Output and debugging methods                                            */
/*                                                                              */
/********************************************************************************/

@Override protected void outputLocalXml(IvyXmlWriter xw)
{
   xw.field("KIND","LOCAL");
}



@Override public String toString()
{
   String loc = "NONE";
   if (base_location != null) loc = base_location.toString();
   String typ = "";
   if (getPrototype() != null) typ = "PROTO ";
   
   return "Local " + typ + getDataType().getName() + " @ " + loc + " " + hashCode();
}



}       // end of class EntityLocal




/* end of EntityLocal.java */

