/********************************************************************************/
/*                                                                              */
/*              ProtoStringBuilder.java                                         */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/




package edu.brown.cs.fait.proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.fait.iface.IfaceAuxReference;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.fait.iface.IfaceUpdater;
import edu.brown.cs.fait.iface.IfaceValue;

public class ProtoStringBuilder extends ProtoBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceValue      buffer_value;
private Set<IfaceLocation> buffer_access;

private static IfaceType  string_type;
private static IfaceValue any_string;
private Map<IfaceLocation,Integer> buffer_sets;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ProtoStringBuilder(IfaceControl ic,IfaceType dt)
{
   super(ic,dt);
   
   buffer_value = null;
   buffer_access = new HashSet<>(2);
   buffer_sets = new HashMap<>(4);
   
   if (any_string == null) {
      string_type = fait_control.findDataType("java.lang.String");
      any_string = fait_control.findAnyValue(string_type);
      any_string = any_string.forceNonNull();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void setAnyValue()
{
   IfaceValue v0 = any_string;
   v0 = v0.forceNonNull();
   setBufferValue(v0);
}



@Override public void handleUpdates(IfaceUpdater upd)
{
   for (Iterator<IfaceLocation> it = buffer_access.iterator(); it.hasNext(); ) {
      IfaceLocation loc = it.next();
      if (upd.isLocationRemoved(loc)) {
         it.remove();
       }
    }
}




/********************************************************************************/
/*                                                                              */
/*      Constructor methods                                                     */
/*                                                                              */
/********************************************************************************/

// CHECKSTYLE:OFF

public IfaceValue prototype__constructor(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   buffer_value = fait_control.findConstantStringValue("");
   if (args.size() == 2) {
      IfaceValue v0 = args.get(1);
      IfaceType atyp = v0.getDataType();
      if (atyp.isStringType()) {
         buffer_value = v0;
         buffer_value = buffer_value.forceNonNull();
       }
      else if (!atyp.isPrimitiveType()) {
         appendValue(v0,src);
       }
    }
   return returnAny(fm);
}



/********************************************************************************/
/*                                                                              */
/*      Append methods                                                          */
/*                                                                              */
/********************************************************************************/

public IfaceValue prototype_append(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   if (args.size() == 2) {
      appendValue(args.get(1),src);
      if (!args.get(1).getDataType().isPrimitiveType())
         addSetLocation(src,0);
    }
   else {
      markAsAny(src);
    }
   
   return getReturn(args.get(0));
}



public IfaceValue prototype_appendCodePoint(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return getReturn(args.get(0));
}



/********************************************************************************/
/*                                                                              */
/*      Other methods                                                           */
/*                                                                              */
/********************************************************************************/

// capcaity, charAt, chars, codePointAt, codePointBefore, codePointCount, codePoints,
// ensureCapacity, indexOf, lastIndexOf, length, offsetByCodePoints,
//      can all use default return

public IfaceValue prototype_delete(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return getReturn(args.get(0));
}


public IfaceValue prototype_deleteCharAt(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return getReturn(args.get(0));
}


public IfaceValue prototype_getChars(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   IfaceValue dst = args.get(3);
   IfaceValue v0 = fait_control.findAnyValue(fait_control.findDataType("int"));
   for (IfaceEntity ent : dst.getEntities()) {
      if (ent.getDataType().isArrayType()) {
         ent.addToArrayContents(v0,v0,src);
       }
    }
   
   return returnVoid();
}


public IfaceValue prototype_insert(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return getReturn(args.get(0));
}



public IfaceValue prototype_replace(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return getReturn(args.get(0));
}


public IfaceValue prototype_reverse(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return getReturn(args.get(0));
}


public IfaceValue prototype_setCharAt(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return returnVoid();
}



public IfaceValue prototype_setLength(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return returnVoid();
}



public IfaceValue prototype_subsequence(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   addBufferReference(src);
   return any_string.changeType(buffer_value.getDataType());
}


public IfaceValue prototype_substring(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return any_string.changeType(buffer_value.getDataType());
}


public IfaceValue prototype_toString(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   return buffer_value;
}



public IfaceValue prototype_trimToSize(IfaceMethod fm,List<IfaceValue> args,IfaceLocation src)
{
   markAsAny(src);
   
   return returnVoid();
}


// CHECKSTYLE:ON


/********************************************************************************/
/*                                                                              */
/*      Query methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public List<IfaceAuxReference> getSetLocations(IfaceControl ctl)
{
   if (buffer_sets == null || buffer_sets.isEmpty()) return null;
   
   List<IfaceAuxReference> rslt = new ArrayList<>();
   IfaceType t0 = ctl.findDataType("java.lang.Object");
   for (Map.Entry<IfaceLocation,Integer> ent : buffer_sets.entrySet()) {
      IfaceLocation loc = ent.getKey();
      int offset = ent.getValue();
      IfaceValue rv = ctl.findRefStackValue(t0,offset);
      IfaceAuxReference r = ctl.getAuxReference(loc,rv,IfaceAuxRefType.OPERAND);
      rslt.add(r);
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

private void appendValue(IfaceValue v0,IfaceLocation src)
{
   IfaceValue vnew = buffer_value;
   IfaceType t0 = v0.getDataType();
   if (t0.isStringType()) {
      vnew = buffer_value.performOperation(string_type,v0,FaitOperator.ADD,src);
    }
   else if (t0.isPrimitiveType() || t0.isNumericType()) {
      IfaceValue s0 = fait_control.findConstantStringValue();
      vnew = buffer_value.performOperation(string_type,s0,FaitOperator.ADD,src);
    }
   else {
      IfaceType t1 = string_type.getAnnotatedType(t0);
      IfaceValue v1 = any_string.changeType(t1);
      vnew = buffer_value.performOperation(string_type,v1,FaitOperator.ADD,src);
    }
   setBufferValue(vnew);
}


private void markAsAny(IfaceLocation src)
{
   IfaceValue vnew = any_string.changeType(buffer_value.getDataType());
   setBufferValue(vnew);
}


private IfaceValue getReturn(IfaceValue arg0)
{
   IfaceType t0 = arg0.getDataType();
   IfaceType t1 = t0.getAnnotatedType(buffer_value.getDataType());
   if (t0 == t1) return arg0;
   t1 = t0.getComputedType(arg0,FaitOperator.ASG,buffer_value);
   if (t0 == t1) return arg0;
   
   IfaceValue v0 = arg0.changeType(t1);
   v0 = v0.forceNonNull();
   return v0;
}



private void addBufferReference(IfaceLocation loc) 
{
   if (loc != null) {
      synchronized (this) {
         buffer_access.add(loc);
       }
    }
}


private void addSetLocation(IfaceLocation loc,int stk)
{
   if (loc != null && buffer_sets != null) buffer_sets.put(loc,stk);
}



private void setBufferValue(IfaceValue v0)
{
   v0 = v0.forceNonNull();
   
   if (v0 == buffer_value) return;
   buffer_value = v0;
   
   synchronized (this) {
      for (IfaceLocation loc : buffer_access) {
         fait_control.queueLocation(loc);
       }
    }
}


}       // end of class ProtoStringBuilder




/* end of ProtoStringBuilder.java */

