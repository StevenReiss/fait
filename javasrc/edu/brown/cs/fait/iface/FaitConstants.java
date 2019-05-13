/********************************************************************************/
/*										*/
/*		FaitConstants.java						*/
/*										*/
/*	Flow Analysis Incremental Tool constant definitions			*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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




package edu.brown.cs.fait.iface;


public interface FaitConstants {


/********************************************************************************/
/*                                                                              */
/*      Branch return types                                                     */
/*                                                                              */
/********************************************************************************/

enum TestBranch {
   ANY, ALWAYS, NEVER
}








/********************************************************************************/
/*                                                                              */
/*      Call related types                                                      */
/*                                                                              */
/********************************************************************************/

enum InlineType {
   NONE,                                // don't inline
   DEFAULT,                             // inline based on source set
   THIS,                                // inline based on this argument
   SOURCES,                             // inline based on all sources
   VALUES,                              // inline based on all values
   SPECIAL,                             // based on special type
   NORMAL,                              // using normal rules
}




enum QueueLevel {
   STATIC_INIT,                         // static initializer or called therefrom
   INIT,                                // constructor or called therefrom
   NORMAL                               // normal method
};



enum ErrorLevel {
   NOTE,                                // implied annotations, etc.
   WARNING,                             // possible errors
   ERROR                                // definite errors
}


/********************************************************************************/
/*                                                                              */
/*      Operators                                                               */
/*                                                                              */
/********************************************************************************/

enum FaitOperator {
   MUL, DIV, MOD, ADD, SUB, LSH, RSH, RSHU, LSS, GTR, LEQ, GEQ, EQL, NEQ,
   XOR, AND, OR, POSTINCR, POSTDECR, INCR, DECR, COMP, NEG, NOP, NOT,
   ANDAND, OROR,
   ASG, ASG_ADD, ASG_SUB, ASG_MUL, ASG_DIV, ASG_AND, ASG_OR, ASG_XOR, ASG_MOD,
   ASG_LSH, ASG_RSH, ASG_RSHU, SIG, INSTANCEOF, I2B, I2C, I2S, I2L,
   TOFLOAT, TOINT, TOBYTE, TOCHAR, TOSHORT, TOLONG, TODOUBLE, COMPARE,
   EQL_ZERO, NEQ_ZERO, GEQ_ZERO, GTR_ZERO, LEQ_ZERO, LSS_ZERO, NULL, NONNULL,
   // special operators for subtype consideration
   DEREFERENCE, FIELDACCESS, ELEMENTACCESS, CALL, 
}

enum FaitTypeOperator {
   STARTINIT, DONEINIT
}


enum TryState { BODY, CATCH, FINALLY }



String INITER_NAME = "$$$$clinit$$$$";
String TESTER_NAME = "$$$$test$$$$";
String TEST_FILE_NAME = "TESTDUMMYFILE.java";



/********************************************************************************/
/*                                                                              */
/*      Reporting options                                                       */
/*                                                                              */
/********************************************************************************/

enum ReportOption {
   NONE,
   SOURCE,
   SOURCE_STATS,
   FULL,
   FULL_STATS
}




}	// end of interface FaitConstants




/* end of FaitConstants.java */
