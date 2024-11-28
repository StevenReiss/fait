/********************************************************************************/
/*                                                                              */
/*              EntityProto.java                                                */
/*                                                                              */
/*      Manage entity prototypes                                                */
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

import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfacePrototype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceUpdater;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class EntityProto extends EntityBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfacePrototype          proto_handler;

private IfaceType               data_type;
private IfaceLocation           source_location;
private boolean                 is_mutable;
private boolean                 is_native;
   



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EntityProto(IfaceType typ,IfacePrototype from,IfaceLocation src)
{
   data_type = typ;
   source_location = src;
   proto_handler = from;
   is_mutable = false;
   is_native = false;
}


EntityProto(IfaceType typ,IfacePrototype from,boolean mutable)
{
   data_type = typ;
   source_location = null;
   proto_handler = from;
   is_mutable = mutable;
   is_native = true;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public IfaceType getDataType()                { return data_type; }
@Override public IfacePrototype getPrototype()          { return proto_handler; }
@Override public boolean isNative()                     { return is_native; }
@Override public boolean isFixed()                      { return is_native | is_mutable; }




/********************************************************************************/
/*                                                                              */
/*      Field methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void setFieldContents(IfaceValue v,String fldkey)
{
   proto_handler.setField(v,fldkey);
}

@Override public boolean addToFieldContents(IfaceValue v,String key)
{
   return proto_handler.addToField(v,key);
}

@Override public IfaceValue getFieldValue(String fldkey){
   return proto_handler.getField(fldkey);
}



/********************************************************************************/
/*                                                                              */
/*      Array methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void setArrayContents(IfaceValue v)
{
   proto_handler.setArrayContents(null,v);
}


@Override public boolean addToArrayContents(IfaceValue v,IfaceValue idx,IfaceLocation src)
{
   return proto_handler.setArrayContents(idx,v);
}



@Override public boolean replaceArrayContents(IfaceValue v,IfaceLocation src)
{
   return proto_handler.setArrayContents(null,v);
}


@Override public IfaceValue getArrayValue(IfaceValue idx,IfaceControl ctl)
{
   return proto_handler.getArrayContents(idx);
}

@Override public List<IfaceValue> getContents(List<IfaceValue> rslt) 
{
   return proto_handler.getContents(rslt);
}



/********************************************************************************/
/*                                                                              */
/*      Mutation methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public Collection<IfaceEntity> mutateTo(IfaceType typ,EntityFactory ef)
{
   IfaceType rslt = null;
   boolean mut = false;
   
   if (is_mutable && typ.isDerivedFrom(getDataType())) rslt = typ;
   if (getDataType().isInterfaceType() || getDataType().isAbstract()) {
      if (typ.isInterfaceType()) {
         IfaceType ctyp = getDataType().findChildForInterface(typ);
         if (ctyp != null) rslt = ctyp;
       }
      else if (typ.isDerivedFrom(getDataType())) rslt = typ;
    }
   else if (is_mutable && getDataType().isJavaLangObject()) {
      if (typ.isInterfaceType() || typ.isAbstract()) {
         rslt = typ;
         mut = true;
       }
    }
   
   if (rslt != null) {
      if (is_mutable) {
         List<IfaceEntity> rtn = new ArrayList<>();
         rtn.add(new EntityProto(rslt,proto_handler,mut));
         return rtn;
       }
    }
   
   return null;
}




/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void handleUpdates(IfaceUpdater upd)
{
   proto_handler.handleUpdates(upd);
}

         

/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected void outputLocalXml(IvyXmlWriter xw) 
{
   xw.field("KIND","PROTO");
}









@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   
   buf.append("Proto New ");
   if (getDataType() != null) buf.append(getDataType().getName());
   else buf.append("<<no type>>");
   if (source_location != null) {
      buf.append(" @ ");
      buf.append(source_location);
    }
   
   return buf.toString();
}



}       // end of class EntityProto




/* end of EntityProto.java */

