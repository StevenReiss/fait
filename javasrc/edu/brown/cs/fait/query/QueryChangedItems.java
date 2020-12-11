/********************************************************************************/
/*                                                                              */
/*              QueryChangedItems.java                                          */
/*                                                                              */
/*      Find set of changed items from a location for its method                */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.fait.query;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class QueryChangedItems implements QueryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IfaceControl    for_control;
private IfaceProgramPoint starting_point;
private IvyXmlWriter    xml_writer;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

QueryChangedItems(IfaceControl ctrl,IfaceProgramPoint pt,IvyXmlWriter xw)
{
   for_control = ctrl;
   starting_point = pt;
   xml_writer = xw;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process() 
{
   ASTNode n = starting_point.getAstReference().getAstNode();
   if (n == null) return;
   if (for_control ==  null || xml_writer == null) return;
}




}       // end of class QueryChangedItems




/* end of QueryChangedItems.java */

