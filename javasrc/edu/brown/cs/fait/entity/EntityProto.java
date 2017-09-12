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

import edu.brown.cs.fait.iface.*;
import edu.brown.cs.ivy.jcode.JcodeDataType;
import edu.brown.cs.ivy.jcode.JcodeField;

import java.util.*;

class EntityProto extends EntityBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfacePrototype          proto_handler;

private JcodeDataType            data_type;
private FaitLocation            source_location;
private boolean                 is_mutable;
private boolean                 is_native;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EntityProto(JcodeDataType typ,IfacePrototype from,FaitLocation src)
{
   data_type = typ;
   source_location = src;
   proto_handler = from;
   is_mutable = false;
   is_native = false;
}


EntityProto(JcodeDataType typ,IfacePrototype from,boolean mutable)
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

@Override public JcodeDataType getDataType()             { return data_type; }
@Override public IfacePrototype getPrototype()          { return proto_handler; }
@Override public boolean isNative()                     { return is_native; }




/********************************************************************************/
/*                                                                              */
/*      Field methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void setFieldContents(IfaceValue v,JcodeField fld)
{
   proto_handler.setField(v,fld);
}

@Override public boolean addToFieldContents(IfaceValue v,JcodeField fld)
{
   return proto_handler.addToField(v,fld);
}

@Override public IfaceValue getFieldValue(JcodeField fld)
{
   return proto_handler.getField(fld);
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


@Override public boolean addToArrayContents(IfaceValue v,IfaceValue idx,FaitLocation src)
{
   return proto_handler.setArrayContents(idx,v);
}


@Override public IfaceValue getArrayValue(IfaceValue idx,FaitControl fc)
{
   return proto_handler.getArrayContents(idx);
}



/********************************************************************************/
/*                                                                              */
/*      Mutation methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public Collection<IfaceEntity> mutateTo(JcodeDataType typ,FaitLocation src,EntityFactory ef)
{
   JcodeDataType rslt = null;
   boolean mut = false;
   
   if (is_mutable && typ.isDerivedFrom(getDataType())) rslt = typ;
   if (getDataType().isInterface() || getDataType().isAbstract()) {
      if (typ.isInterface()) {
         JcodeDataType ctyp = getDataType().findChildForInterface(typ);
         if (ctyp != null) rslt = ctyp;
       }
      else if (typ.isDerivedFrom(getDataType())) rslt = typ;
    }
   else if (is_mutable && getDataType().isJavaLangObject()) {
      if (typ.isInterface() || typ.isAbstract()) {
         rslt = typ;
         mut = true;
       }
    }
   
   if (rslt != null) {
      if (is_mutable) {
         List<IfaceEntity> rtn = new ArrayList<IfaceEntity>();
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

