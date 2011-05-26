/********************************************************************************/
/*										*/
/*		FlowScanner.java						*/
/*										*/
/*	Scanner to symbolically execute a method				*/
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



package edu.brown.cs.fait.flow;

import edu.brown.cs.fait.iface.*;

import java.util.*;

class FlowScanner implements FlowConstants, FaitOpcodes
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private FlowQueue		flow_queue;
private FaitControl		fait_control;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowScanner(FaitControl fc,FlowQueue fq)
{
   fait_control = fc;
   flow_queue = fq;
}



/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

void scanCode(FlowQueueInstance wq)
{
   while (!wq.isEmpty()) {
      FaitInstruction fi = wq.getNext();
      try {
	 processInstruction(fi,wq);
       }
      catch (Throwable t) {
	 System.err.println("FAIT: Problem processing " + wq.getCall().getMethod().getName());
	 t.printStackTrace();
       }
    }
}



/********************************************************************************/
/*										*/
/*	Scan a single instruction						*/
/*										*/
/********************************************************************************/

private void processInstruction(FaitInstruction ins,FlowQueueInstance wq)
{
   IfaceState st1 = wq.getState(ins);
   IfaceCall call = wq.getCall();

   // FaitLog.log("Work on " + ins);

   if (!flow_queue.checkInitialized(st1,call,ins)) return;

   st1 = st1.cloneState();

   IfaceValue v0,v1,v2;
   int i0;
   boolean oref;
   IfaceEntity ent;
   TestBranch brslt;
   FaitDataType dtyp;

   if (ins.getIndex() == 0 && call.getMethod().isSynchronized()) {
      if (!call.getMethod().isStatic()) {
	 v0 = st1.getLocal(0);
	 call.setAssociation(AssociationType.SYNC,ins,v0);
       }
    }

   FaitInstruction nins = call.getMethod().getInstruction(ins.getIndex() + 1);
   FlowLocation here = new FlowLocation(call,ins);

   switch (ins.getOpcode()) {
/* OBJECT PROCESSING INSTRUTIONS */
      case NEW :
	 flow_queue.initialize(ins.getTypeReference());
	 ent = getLocalEntity(call,ins);
	 v0 = fait_control.findObjectValue(ins.getTypeReference(),
	       fait_control.createSingletonSet(ent),
	       NullFlags.NON_NULL);
	 call.setAssociation(AssociationType.NEW,ins,v0);
	 st1.pushStack(v0);
	 break;
      case ACONST_NULL :
	 st1.pushStack(fait_control.findNullValue());
	 break;
      case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
	 i0 = ins.getLocalVariable();
	 st1.pushStack(st1.getLocal(i0));
	 break;
      case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
	 v0 = st1.popStack();
	 st1.setLocal(ins.getLocalVariable(),v0);
	 // FaitLog.log("\tSet local " + ins.getLocalVariable() + " = " + v0);
	 break;
      case CHECKCAST :
	 v0 = st1.popStack();
	 boolean pfg = ins.getTypeReference().isProjectClass();
	 if (pfg && v0.getDataType().isProjectClass()) pfg = false;
	 if (pfg && v0.getDataType().isJavaLangObject()) pfg = false;
	 if (pfg && v0.getDataType().isInterface()) pfg = false;
	 v0 = v0.restrictByType(ins.getTypeReference(),pfg,here);
	 if (v0.mustBeNull()) v0 = fait_control.findNullValue(ins.getTypeReference());
	 if (!v0.mustBeNull() && v0.isEmptyEntitySet()) nins = null;
	 // FaitLog.log("\tCast result = " + v0);
	 st1.pushStack(v0);
	 break;
      case DUP :
	 st1.handleDup(false,0);
	 break;
      case DUP_X1 :
	 st1.handleDup(false,1);
	 break;
      case DUP_X2 :
	 st1.handleDup(false,2);
	 break;
      case DUP2 :
	 st1.handleDup(true,0);
	 break;
      case DUP2_X1 :
	 st1.handleDup(true,1);
	 break;
      case DUP2_X2 :
	 st1.handleDup(true,2);
	 break;
      case MONITORENTER :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v0 = st1.popStack();
	 call.setAssociation(AssociationType.SYNC,ins,v0);
	 break;
      case MONITOREXIT :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v0 = st1.popStack();
	 call.setAssociation(AssociationType.SYNC,ins,v0);
	 st1.discardFields();
	 break;
      case POP :
	 st1.popStack();
	 break;
      case POP2 :
	 v0 = st1.popStack();
	 if (!v0.isCategory2()) st1.popStack();
	 break;
      case SWAP :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 st1.pushStack(v0);
	 st1.pushStack(v1);
	 break;

/* ARITHMETIC INSTRUCTIONS */
      case INSTANCEOF :
	 v0 = st1.popStack();
	 v2 = fait_control.findAnyValue(ins.getTypeReference());
	 v1 = v0.performOperation(fait_control.findDataType("I"),v2,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case DADD : case DDIV : case DMUL : case DREM : case DSUB :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = v1.performOperation(fait_control.findDataType("D"),v0,ins.getOpcode(),here);
	 st1.pushStack(v2);
	 break;
      case FADD : case FDIV : case FMUL : case FREM : case FSUB :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = v1.performOperation(fait_control.findDataType("F"),v0,ins.getOpcode(),here);
	 st1.pushStack(v2);
	 break;
      case IADD : case IDIV : case IMUL : case IREM : case ISUB :
      case IAND : case IOR : case IXOR :
      case ISHL : case ISHR : case IUSHR :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = v1.performOperation(fait_control.findDataType("I"),v0,ins.getOpcode(),here);
	 st1.pushStack(v2);
	 break;
      case DCMPG : case DCMPL :
      case FCMPG : case FCMPL :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = fait_control.findRangeValue(fait_control.findDataType("I"),-1,1);
	 st1.pushStack(v2);
	 break;
      case LADD : case LDIV : case LMUL : case LREM : case LSUB :
      case LAND : case LOR : case LXOR :
      case LSHL : case LSHR : case LUSHR :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = v1.performOperation(fait_control.findDataType("L"),v0,ins.getOpcode(),here);
	 st1.pushStack(v2);
	 break;
      case BIPUSH :
      case SIPUSH :
	 i0 = ins.getIntValue();
	 v0 = fait_control.findRangeValue(fait_control.findDataType("I"),i0,i0);
	 st1.pushStack(v0);
	 break;
      case DCONST_0 : case DCONST_1 :
	 v0 = fait_control.findAnyValue(fait_control.findDataType("D"));
	 st1.pushStack(v0);
	 break;
      case DLOAD : case DLOAD_0 : case DLOAD_1 : case DLOAD_2 : case DLOAD_3 :
      case FLOAD : case FLOAD_0 : case FLOAD_1 : case FLOAD_2 : case FLOAD_3 :
      case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
      case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
	 i0 = ins.getLocalVariable();
	 st1.pushStack(st1.getLocal(i0));
	 break;
      case FCONST_0 : case FCONST_1 : case FCONST_2 :
	 v0 = fait_control.findAnyValue(fait_control.findDataType("F"));
	 st1.pushStack(v0);
	 break;
      case LCONST_0 : case LCONST_1 :
	 v0 = fait_control.findAnyValue(fait_control.findDataType("L"));
	 st1.pushStack(v0);
	 break;
      case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
      case ICONST_5 : case ICONST_M1 :
	 i0 = ins.getIntValue();
	 v0 = fait_control.findRangeValue(fait_control.findDataType("I"),i0,i0);
	 st1.pushStack(v0);
	 break;
      case LDC :
      case LDC_W :
      case LDC2_W :
	 Object ov = ins.getObjectValue();
	 if (ov instanceof String) {
	    v0 = fait_control.findConstantStringValue((String) ov);
	  }
	 else if (ov instanceof Float) {
	    v0 = fait_control.findAnyValue(fait_control.findDataType("F"));
	  }
	 else if (ov instanceof Double) {
	    v0 = fait_control.findAnyValue(fait_control.findDataType("D"));
	  }
	 else if (ov instanceof Long) {
	    long l0 = (Long) ov;
	    v0 = fait_control.findRangeValue(fait_control.findDataType("L"),l0,l0);
	  }
	 else if (ov instanceof Integer) {
	    i0 = (Integer) ov;
	    v0 = fait_control.findRangeValue(fait_control.findDataType("I"),i0,i0);
	  }
	 else {
	    v0 = fait_control.findNativeValue(fait_control.findDataType("Ljava/lang/Class;"));
	    v0 = v0.forceNonNull();
	  }
	 st1.pushStack(v0);
	 break;
      case D2F : case FNEG : case I2F : case L2F :
	 v0 = st1.popStack();
	 v1 = v0.performOperation(fait_control.findDataType("F"),v0,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case D2I : case F2I : case L2I : case INEG :
	 v0 = st1.popStack();
	 v1 = v0.performOperation(fait_control.findDataType("I"),v0,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case IINC :
	 v0 = st1.getLocal(ins.getLocalVariable());
	 i0 = ins.getIntValue();
	 v1 = fait_control.findRangeValue(fait_control.findDataType("I"),i0,i0);
	 v2 = v0.performOperation(fait_control.findDataType("I"),v1,ins.getOpcode(),here);
	 st1.setLocal(ins.getLocalVariable(),v2);
	 break;
      case D2L : case F2L : case I2L : case LNEG :
	 v0 = st1.popStack();
	 v1 = v0.performOperation(fait_control.findDataType("L"),v0,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case LCMP :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = v1.performOperation(fait_control.findDataType("I"),v0,ins.getOpcode(),here);
	 st1.pushStack(v2);
	 break;
      case DNEG : case F2D : case I2D : case L2D :
	 v0 = st1.popStack();
	 v1 = v0.performOperation(fait_control.findDataType("D"),v0,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case I2B :
	 v0 = st1.popStack();
	 v1 = v0.performOperation(fait_control.findDataType("B"),v0,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case I2C :
	 v0 = st1.popStack();
	 v1 = v0.performOperation(fait_control.findDataType("C"),v0,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case I2S :
	 v0 = st1.popStack();
	 v1 = v0.performOperation(fait_control.findDataType("S"),v0,ins.getOpcode(),here);
	 st1.pushStack(v1);
	 break;
      case NOP :
	 break;
      case DSTORE : case DSTORE_0 : case DSTORE_1 : case DSTORE_2 : case DSTORE_3 :
      case FSTORE : case FSTORE_0 : case FSTORE_1 : case FSTORE_2 : case FSTORE_3 :
      case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
      case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :
	 i0 = ins.getLocalVariable();
	 v0 = st1.popStack();
	 st1.setLocal(i0,v0);
	 if (v0.isCategory2()) st1.setLocal(i0+1,null);
	 break;

/* BRANCH INSTRUCTIONS */
      case GOTO :
      case GOTO_W :
	 nins = ins.getTargetInstruction();
	 break;
      case IF_ACMPEQ : case IF_ACMPNE :
      case IF_ICMPEQ : case IF_ICMPNE :
      case IF_ICMPLT : case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 // FaitLog.log("\tCompare " + v1 + " :: " + v0);
	 brslt = v1.branchTest(v0,ins.getOpcode());
	 if (brslt != TestBranch.NEVER) wq.mergeState(st1,ins.getTargetInstruction());
	 if (brslt == TestBranch.ALWAYS) nins = null;
	 break;
      case IFEQ : case IFNE : case IFLT : case IFGE : case IFGT : case IFLE :
      case IFNONNULL : case IFNULL :
	 v0 = st1.popStack();
	 // FaitLog.log("\tTest Value = " + v0);
	 brslt = v0.branchTest(v0,ins.getOpcode());
	 st1 = flow_queue.handleImplications(wq,ins,st1,brslt);
	 if (brslt != TestBranch.NEVER) wq.mergeState(st1,ins.getTargetInstruction());
	 if (brslt == TestBranch.ALWAYS) nins = null;
	 break;
      case LOOKUPSWITCH :
      case TABLESWITCH :
	 v0 = st1.popStack();
	 for (FaitInstruction xin : ins.getTargetInstructions()) {
	    wq.mergeState(st1,xin);
	  }
	 nins = null;
	 break;

/* SUBROUTINE CALLS */

      case JSR : case JSR_W :
	 st1.pushStack(fait_control.findBadValue());
	 st1.pushReturn(nins);
	 nins = ins.getTargetInstruction();
	 break;
      case RET :
	 nins = st1.popReturn();
	 break;

/* CALL INSTRUCTIONS */

      case ARETURN :
      case DRETURN : case FRETURN : case IRETURN : case LRETURN :
	 v0 = st1.popStack();
         flow_queue.handleReturn(call,st1,v0);
	 nins = null;
	 break;
      case RETURN :
         v0 = fait_control.findAnyValue(fait_control.findDataType("V"));
         flow_queue.handleReturn(call,st1,v0);
         nins = null;
	 break;
      case INVOKEINTERFACE :
      case INVOKESPECIAL :
      case INVOKESTATIC :
      case INVOKEVIRTUAL :
         handleAccess(here,st1);
         if (!flow_queue.handleCall(here,st1,wq)) {
            nins = null;
          }
         if (ins.getMethodReference().getName().equals("exit")) {
            if (ins.getMethodReference().getDeclaringClass().getName().equals("java.lang.System")) {
               nins = null;
             }
          }
         break;
      case ATHROW :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v0 = st1.popStack();
	 call.setAssociation(AssociationType.THROW,ins,v0);
	 nins = null;
         flow_queue.handleThrow(here,v0,st1);
	 break;
         
/* ARRAY PROCESSING INSTRUCTIONS */

      case AALOAD :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v2 = st1.popStack();		// index
	 v0 = st1.popStack();		// array
	 call.setAssociation(AssociationType.THISREF,ins,v0);
	 v1 = flow_queue.handleArrayAccess(here,v0,v2);
	 // FaitLog.log("\tArray " + v0 + " index " + v2 + " == " + v1);
	 st1.pushStack(v1);
	 break;
      case AASTORE :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = st1.popStack();
	 call.setAssociation(AssociationType.THISREF,ins,v2);
	 if (v2.mustBeNull()) {
	    // log
	    nins = null;
	    break;
	  }
	 flow_queue.handleArraySet(here,v2,v0,v1);
	 break;
      case ANEWARRAY :
	 v0 = st1.popStack();
	 flow_queue.initialize(ins.getTypeReference());
	 v1 = flow_queue.handleNewArraySet(here,ins.getTypeReference(),1,v0);
	 st1.pushStack(v1);
	 break;
      case MULTIANEWARRAY :
	 i0 = ins.getIntValue();
	 dtyp = ins.getTypeReference();
	 flow_queue.initialize(dtyp);
	 dtyp = dtyp.getArrayType();
	 for (int i = 0; i < ins.getIntValue(); ++i) {
	    v0 = st1.popStack();
	  }
	 v1 = flow_queue.handleNewArraySet(here,ins.getTypeReference(),ins.getIntValue(),null);
	 st1.pushStack(v1);
	 break;
      case NEWARRAY :
	 v0 = st1.popStack();
	 v1 = flow_queue.handleNewArraySet(here,ins.getTypeReference(),1,v0);
	 st1.pushStack(v1);
	 break;
      case ARRAYLENGTH :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v0 = st1.popStack();
	 call.setAssociation(AssociationType.THISREF,ins,v0);
	 v1 = fait_control.findAnyValue(fait_control.findDataType("I"));
	 st1.pushStack(v1);
	 break;
      case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v2 = st1.popStack();			// index
	 v0 = st1.popStack();
	 call.setAssociation(AssociationType.THISREF,ins,v0);
	 v1 = flow_queue.handleArrayAccess(here,v0,v2);
	 st1.pushStack(v1);
	 break;
      case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = st1.popStack();
	 call.setAssociation(AssociationType.THISREF,ins,v2);
	 flow_queue.handleArraySet(here,v2,v0,v1);
	 break;

/* FIELD INSTRUCTIONS */
      case GETFIELD :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
         oref = false;
         v0 = st1.popStack();
         if (ins.getPrevious() != null && !call.getMethod().isStatic()) {
            if (v0 == st1.getLocal(0)) oref = true;
          }
         call.setAssociation(AssociationType.THISREF,ins,v0);
         v1 = flow_queue.handleFieldGet(here,st1,oref,v0);
         st1.pushStack(v1);
         break;
      case GETSTATIC :
         v1 = flow_queue.handleFieldGet(here,st1,false,null);
         st1.pushStack(v1);
         break;
      case PUTFIELD :
         st1 = handleAccess(here,st1);
         if (st1 == null) break;
         oref = false;
         v0 = st1.popStack();
         v1 = st1.popStack();
         call.setAssociation(AssociationType.THISREF,ins,v1);
         call.setAssociation(AssociationType.FIELDSET,ins,v0);
         if (!call.getMethod().isStatic()) {
            if (v1 == st1.getLocal(0)) oref = true;
          }
         flow_queue.handleFieldSet(here,st1,oref,v0,v1);
         break;
      case PUTSTATIC :
         v0 = st1.popStack();
         call.setAssociation(AssociationType.FIELDSET,ins,v0);
         flow_queue.handleFieldSet(here,st1,false,v0,null);
	 break;

      default :
	 System.err.println("FAIT: Opcode " + ins.getOpcode() + " not found");
	 break;
    }
   
   if (nins != null && st1 != null) wq.mergeState(st1,nins);
}



/********************************************************************************/
/*										*/
/*	Source helper methods							*/
/*										*/
/********************************************************************************/

private IfaceEntity getLocalEntity(IfaceCall call,FaitInstruction ins)
{
   IfaceEntity ns = call.getBaseEntity(ins);

   if (ns == null) {
      IfacePrototype pt = fait_control.createPrototype(ins.getTypeReference());
      if (pt != null) {
	 ns = fait_control.findPrototypeEntity(ins.getTypeReference(),pt,
	       new FlowLocation(call,ins));
       }
      // might want to create fixed source for non-project methods
      else {
	 ns = fait_control.findLocalEntity(new FlowLocation(call,ins),
	       ins.getTypeReference(),true);
       }
      call.setBaseEntity(ins,ns);
    }

   return ns;
}



private IfaceEntity getUserSource(IfaceCall call,FaitInstruction ins,IfaceValue v)
{
   FaitEntity.UserEntity ent = call.getUserEntity(ins);
   if (ent != null) return (IfaceEntity) ent;
   // create user source if needed
   return null;
}




/********************************************************************************/
/*                                                                              */
/*      Handle accessing a value that must be non-null                          */
/*                                                                              */
/********************************************************************************/

private IfaceState handleAccess(FlowLocation loc,IfaceState st)
{
   FaitInstruction ins = loc.getInstruction();
   
   // First determine which argument
   int act = 0;
   switch (ins.getOpcode()) {
      case GETFIELD :
      case ARRAYLENGTH :
      case MONITORENTER :
      case MONITOREXIT :
      case ATHROW :
	 break;
      case PUTFIELD :
      case AALOAD : case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
	 act = 1;
	 break;
      case AASTORE : case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
	 act = 2;
	 break;
      case INVOKEINTERFACE :
      case INVOKESPECIAL :
      case INVOKEVIRTUAL :
	 FaitMethod mthd = ins.getMethodReference();
	 if (mthd.isStatic() || mthd.isConstructor()) return st;
	 act = mthd.getNumArguments();
	 break;
      default :
	 return st;
    }
   
   // next scan the code to handle any implications
   st = flow_queue.handleAccess(loc,act,st);
   
   // next check the argument itself
   LinkedList<IfaceValue> vl = new LinkedList<IfaceValue>();
   for (int i = 0; i < act; ++i) {
      vl.addFirst(st.popStack());
    }
   IfaceValue v0 = st.popStack();
   if (v0.mustBeNull()) {
      // log
      return null;
    }
   v0 = v0.forceNonNull();
   st.pushStack(v0);
   for (IfaceValue v1 : vl) st.pushStack(v1);
   
   return st;
}
}	// end of class FlowScanner




/* end of FlowScanner.java */

