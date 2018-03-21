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
/*      Flags for detecting and using null values                               */
/*                                                                              */
/********************************************************************************/

enum XNullFlags {
   NON_NULL,                            // known non-null
   MUST_BE_NULL,                        // known null
   CAN_BE_NULL,                         // possibly null
   NULL,                                // can or must be null
   TEST_NULL,                           // checked for null
   V5, V6, V7,
   SET_NULL,                            // null explicitly set
   V9, V10, 
   NEW_NULL,                            // must, can, explicit (new field)
   V12, V13, V14, V15,
   USE_DIRECT,                          // used without null check
   V17, V18, V19, V20, V21, V22, V23, V24,
   V26, V27, V28, V29, V30, V31;
   
   private static final int MUST_BIT = 1;
   private static final int CAN_BIT = 2;
   private static final int TEST_BIT = 4;
   private static final int EXPLICIT_BIT = 8;
   private static final int AND_BITS = 1;
   
   public boolean mustBeNull() {
      return (ordinal() & MUST_BIT) != 0;
    }
   
   public boolean canBeNull() {
      return (ordinal() & CAN_BIT) != 0;
    }
   
   public boolean testForNull() {
      return (ordinal() & TEST_BIT) != 0;
    }
   
   public boolean nullExplicitlySet() {
      return (ordinal() & EXPLICIT_BIT) != 0;
    }
   
   public XNullFlags merge(XNullFlags f1) {
      int v = (ordinal() | f1.ordinal()) & ~AND_BITS;
      int v1 = (ordinal() & f1.ordinal()) & AND_BITS;
      v |= v1;
      return values()[v];
    }
   
   public XNullFlags forceNonNull() {
      int v = (ordinal() & ~(MUST_BIT | CAN_BIT | EXPLICIT_BIT));
      return values()[v];
    }
   
   public XNullFlags forceTestForNull() {
      int v = (ordinal() | TEST_BIT);
      return values()[v];
    }
   
   public XNullFlags fixup() {
      int v = ordinal();
      if ((v & MUST_BIT) != 0) v |= CAN_BIT;
      return values()[v];
    }
  
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
   ASG, ASG_ADD, ASG_SUB, ASG_MUL, ASG_DIV, ASG_AND, ASG_OR, ASG_XOR, ASG_MOD,
   ASG_LSH, ASG_RSH, ASG_RSHU, SIG, INSTANCEOF, I2B, I2C, I2S, I2L,
   TOFLOAT, TOINT, TOBYTE, TOCHAR, TOSHORT, TOLONG, TODOUBLE, COMPARE,
   EQL_ZERO, NEQ_ZERO, GEQ_ZERO, GTR_ZERO, LEQ_ZERO, LSS_ZERO, NULL, NONNULL,
   // special operators for subtype consideration
   DEREFERENCE, STARTINIT, DONEINIT,
}



String INITER_NAME = "$$$$clinit$$$$";
String TESTER_NAME = "$$$$test$$$$";



}	// end of interface FaitConstants




/* end of FaitConstants.java */
