/********************************************************************************/
/*                                                                              */
/*              CheckInitialization.java                                        */
/*                                                                              */
/*      Initialization subtypes                                                 */
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



package edu.brown.cs.fait.type;

import edu.brown.cs.fait.iface.IfaceBaseType;
import edu.brown.cs.fait.iface.IfaceSubtype;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceValue;

import static edu.brown.cs.fait.type.CheckInitialization.InitializationState.*;


public class CheckInitialization extends TypeSubtype
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

public enum InitializationState implements IfaceSubtype.Value
{
   INITIALIZED,
   UNKNOWN,
   UNDER_INITIALIZATION;
   
   @Override public IfaceSubtype getSubtype()   { return our_type; }
}

private static CheckInitialization our_type = new CheckInitialization();




/********************************************************************************/
/*                                                                              */
/*      Static access                                                           */
/*                                                                              */
/********************************************************************************/

public static synchronized CheckInitialization getType()
{
   if (our_type == null) {
      our_type = new CheckInitialization();
    }
   return our_type;
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CheckInitialization()
{
   defineMerge(INITIALIZED,UNKNOWN,UNKNOWN);
   defineMerge(INITIALIZED,UNDER_INITIALIZATION,UNKNOWN);
   defineMerge(UNKNOWN,UNDER_INITIALIZATION,UNKNOWN);
   
   defineRestrict(INITIALIZED,UNKNOWN,INITIALIZED);
   defineRestrict(INITIALIZED,UNDER_INITIALIZATION,INITIALIZED);
   defineRestrict(UNKNOWN,INITIALIZED,INITIALIZED);
   defineRestrict(UNKNOWN,UNDER_INITIALIZATION,UNDER_INITIALIZATION);
   defineRestrict(UNDER_INITIALIZATION,INITIALIZED,INITIALIZED);
   defineRestrict(UNDER_INITIALIZATION,UNKNOWN,UNDER_INITIALIZATION);
   
   defineAttribute("Initialized",INITIALIZED);
   defineAttribute("UnderInitialization",UNDER_INITIALIZATION);
   defineAttribute("UnknownInitialization",UNKNOWN);
}




/********************************************************************************/
/*                                                                              */
/*      Default value methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public InitializationState getDefaultValue(IfaceBaseType typ)
{
   return UNKNOWN;
}


@Override public InitializationState getDefaultConstantValue(IfaceBaseType typ,Object cnst)
{
   return INITIALIZED;
}



@Override public InitializationState getDefaultUninitializedValue(IfaceType typ)
{
   return UNKNOWN;
}


@Override public IfaceSubtype.Value getComputedValue(IfaceValue rslt,
      FaitOperator op,IfaceValue v0,IfaceValue v1)
{
   switch (op) {
      case FIELDACCESS :
      case ELEMENTACCESS :
      case DEREFERENCE :
         return null;
      default :
         return INITIALIZED;
    }
}


@Override public IfaceSubtype.Value getComputedValue(FaitTypeOperator op,IfaceSubtype.Value oval)
{
   switch (op) {
      case DONEINIT :
         return INITIALIZED;
      case STARTINIT :
         return UNDER_INITIALIZATION;
      default :
         break;
    }
   
   return super.getComputedValue(op,oval);
}

}       // end of class CheckInitialization




/* end of CheckInitialization.java */

