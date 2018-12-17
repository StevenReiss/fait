/********************************************************************************/
/*                                                                              */
/*              QueryBackFlowData.java                                          */
/*                                                                              */
/*      Result information of back flow to a state                              */
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



package edu.brown.cs.fait.query;

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.fait.iface.IfaceBackFlow;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceValue;

class QueryBackFlowData implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<IfaceValue>        aux_references;
private IfaceField              aux_field;
private IfaceValue              aux_array;
private QueryContext            back_context;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryBackFlowData(QueryContext ctx,IfaceBackFlow bf)
{
   back_context = ctx;
   aux_references = bf.getAuxReferences();
   aux_field = bf.getAuxField();
   aux_array = bf.getAuxArray();
}



QueryBackFlowData(QueryContext ctx)
{
   back_context = ctx;
   aux_references = null;
   aux_field = null;
   aux_array = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

QueryContext getContext()                       { return back_context; }

List<IfaceValue> getAuxReferences()             { return aux_references; }

IfaceValue getAuxArray()                        { return aux_array; }

IfaceField getAuxField()                        { return aux_field; }

void addAuxReference(IfaceValue v)  
{ 
   if (aux_references == null) aux_references = new ArrayList<>();
   aux_references.add(v);
}



}       // end of class QueryBackFlowData




/* end of QueryBackFlowData.java */

