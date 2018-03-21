/********************************************************************************/
/*                                                                              */
/*              FaitAnnotation.java                                             */
/*                                                                              */
/*      Simple annotation for internal use in FAIT                              */
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



package edu.brown.cs.fait.iface;



public class FaitAnnotation implements IfaceAnnotation
{



/********************************************************************************/
/*                                                                              */
/*      Known annotations                                                       */
/*                                                                              */
/********************************************************************************/

public static FaitAnnotation MUST_BE_NULL = new FaitAnnotation("MustBeNull");
public static FaitAnnotation NULLABLE = new FaitAnnotation("Nullable");
public static FaitAnnotation NON_NULL = new FaitAnnotation("NonNull");

public static FaitAnnotation INITIALIZED = new FaitAnnotation("Initialized");
public static FaitAnnotation UNDER_INITIALIZATION = new FaitAnnotation("UnderInitialization");



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String annotation_name;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public FaitAnnotation(String name)
{
   annotation_name = name;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getAnnotationName()
{
   return annotation_name;
}


@Override public Object getAnnotationValue(String key)
{
   return null;
}



}       // end of class FaitAnnotation




/* end of FaitAnnotation.java */

