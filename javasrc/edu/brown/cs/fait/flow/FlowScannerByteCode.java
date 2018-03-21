/********************************************************************************/
/*                                                                              */
/*              FlowScannerByteCode.java                                        */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.fait.flow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceImplications;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.ivy.file.Pair;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcode.JcodeInstruction;

class FlowScannerByteCode extends FlowScanner implements FlowConstants, JcodeConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Map<Integer,FaitOperator> op_map;

static {
   op_map = new HashMap<>();
   op_map.put(DADD,FaitOperator.ADD);
   op_map.put(DDIV,FaitOperator.DIV);
   op_map.put(DMUL,FaitOperator.MUL);
   op_map.put(DREM,FaitOperator.MOD);
   op_map.put(DSUB,FaitOperator.SUB);
   op_map.put(FADD,FaitOperator.ADD);
   op_map.put(FDIV,FaitOperator.DIV);
   op_map.put(FMUL,FaitOperator.MUL);
   op_map.put(FREM,FaitOperator.MOD);
   op_map.put(FSUB,FaitOperator.SUB);
   op_map.put(IADD,FaitOperator.ADD);
   op_map.put(IDIV,FaitOperator.DIV);
   op_map.put(IMUL,FaitOperator.MUL);
   op_map.put(IREM,FaitOperator.MOD);
   op_map.put(ISUB,FaitOperator.SUB);
   op_map.put(IAND,FaitOperator.AND);
   op_map.put(IOR,FaitOperator.OR);
   op_map.put(IXOR,FaitOperator.XOR);
   op_map.put(ISHL,FaitOperator.LSH);
   op_map.put(ISHR,FaitOperator.RSH);
   op_map.put(IUSHR,FaitOperator.RSHU);
   op_map.put(LADD,FaitOperator.ADD);
   op_map.put(LDIV,FaitOperator.DIV);
   op_map.put(LMUL,FaitOperator.MUL);
   op_map.put(LREM,FaitOperator.MOD);
   op_map.put(LSUB,FaitOperator.SUB);
   op_map.put(LAND,FaitOperator.AND);
   op_map.put(LOR,FaitOperator.OR);
   op_map.put(LXOR,FaitOperator.XOR);
   op_map.put(LSHL,FaitOperator.LSH);
   op_map.put(LSHR,FaitOperator.RSH);
   op_map.put(LUSHR,FaitOperator.RSHU);
   op_map.put(FNEG,FaitOperator.NEG);
   op_map.put(D2F,FaitOperator.TOFLOAT);
   op_map.put(I2F,FaitOperator.TOFLOAT);
   op_map.put(L2F,FaitOperator.TOFLOAT);
   op_map.put(L2I,FaitOperator.TOINT);
   op_map.put(D2I,FaitOperator.TOINT);
   op_map.put(F2I,FaitOperator.TOINT);
   op_map.put(I2B,FaitOperator.TOBYTE);
   op_map.put(INEG,FaitOperator.NEG);
   op_map.put(IINC,FaitOperator.INCR);
   op_map.put(D2L,FaitOperator.TOLONG);
   op_map.put(F2L,FaitOperator.TOLONG); 
   op_map.put(I2L,FaitOperator.TOLONG);
   op_map.put(LNEG,FaitOperator.NEG);
   op_map.put(LCMP,FaitOperator.COMPARE);
   op_map.put(DNEG,FaitOperator.NEG);
   op_map.put(F2D,FaitOperator.TODOUBLE);
   op_map.put(I2D,FaitOperator.TODOUBLE);
   op_map.put(L2D,FaitOperator.TODOUBLE);
   op_map.put(I2B,FaitOperator.TOBYTE);
   op_map.put(I2C,FaitOperator.TOCHAR);
   op_map.put(I2S,FaitOperator.TOSHORT);
   op_map.put(IF_ACMPEQ,FaitOperator.EQL);
   op_map.put(IF_ACMPNE,FaitOperator.NEQ);
   op_map.put(IF_ICMPEQ,FaitOperator.EQL);
   op_map.put(IF_ICMPNE,FaitOperator.NEQ);
   op_map.put(IF_ICMPLT,FaitOperator.LSS);
   op_map.put(IF_ICMPGE,FaitOperator.GEQ);
   op_map.put(IF_ICMPGT,FaitOperator.GTR);
   op_map.put(IF_ICMPLE,FaitOperator.LEQ);
   op_map.put(IFEQ,FaitOperator.EQL_ZERO);
   op_map.put(IFNE,FaitOperator.NEQ_ZERO);
   op_map.put(IFLT,FaitOperator.LSS_ZERO);
   op_map.put(IFGE,FaitOperator.GEQ_ZERO);
   op_map.put(IFGT,FaitOperator.GTR_ZERO);
   op_map.put(IFLE,FaitOperator.LEQ_ZERO);
   op_map.put(IFNONNULL,FaitOperator.NONNULL);
   op_map.put(IFNULL,FaitOperator.NULL);
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

FlowScannerByteCode(IfaceControl fc,FlowQueue fq)
{
   super(fc,fq);
}



/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

@Override void scanCode(FlowQueueInstanceByteCode wq)
{
   while (!wq.isEmpty()) {
      IfaceProgramPoint fi = wq.getNext();
      try {
	 processInstruction(fi,wq);
       }
      catch (Throwable t) {
	 FaitLog.logE("Problem processing " + wq.getCall().getLogName(),t);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Scan a single instruction						*/
/*										*/
/********************************************************************************/

private void processInstruction(IfaceProgramPoint inspt,FlowQueueInstance wq)
{
   JcodeInstruction ins = inspt.getInstruction();
   IfaceState st1 = wq.getState(inspt);
   IfaceCall call = wq.getCall();
   
   if (FaitLog.isTracing()) {
      FaitLog.logD("Work on " + ins);
    }
   
   if (!flow_queue.checkInitialized(call,inspt)) return;
   
   call.removeErrors(inspt);
   
   st1 = st1.cloneState();
   
   IfaceValue v0,v1,v2;
   int i0;
   boolean oref;
   IfaceEntity ent;
   TestBranch brslt;
   IfaceType dtyp;
   IfaceType jdtyp = inspt.getReferencedType();
   IfaceType rtyp;
   
   if (ins.getIndex() == 0 && call.getMethod().isSynchronized()) {
      if (!call.getMethod().isStatic()) {
	 v0 = st1.getLocal(0);
       }
    }
   
   IfaceProgramPoint nins = call.getMethod().getNext(inspt);
   IfaceProgramPoint pins = nins;
   FlowLocation here = new FlowLocation(flow_queue,call,inspt);
   
   switch (ins.getOpcode()) {
/* OBJECT PROCESSING INSTRUTIONS */
      case NEW :
	 flow_queue.initialize(jdtyp);
	 ent = getLocalEntity(call,inspt);
	 v0 = fait_control.findObjectValue(jdtyp,
	       fait_control.createSingletonSet(ent),
	       FaitAnnotation.NON_NULL,FaitAnnotation.UNDER_INITIALIZATION);
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
         if (FaitLog.isTracing())
            FaitLog.logD1("Set local " + ins.getLocalVariable() + " = " + v0);
	 break;
      case CHECKCAST :
	 v0 = st1.popStack();
	 v0 = v0.restrictByType(jdtyp);
	 if (v0.mustBeNull()) v0 = fait_control.findNullValue(jdtyp);
	 if (!v0.mustBeNull() && v0.isEmptyEntitySet()) nins = null;
         if (FaitLog.isTracing()) FaitLog.logD1("Cast result = " + v0);
	 st1.pushStack(v0);
	 break;
      case DUP :
	 st1.handleDup(false,0);
         JcodeInstruction previns = ins.getPrevious();
         if (previns.getOpcode() == NEW) {
            IfaceValue sv0 = st1.popStack();
            IfaceValue sv1 = st1.popStack();
            sv1 = sv1.forceInitialized(FaitAnnotation.INITIALIZED);
            st1.pushStack(sv1);
            st1.pushStack(sv0);
          }
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
	 break;
      case MONITOREXIT :
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 v0 = st1.popStack();
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
	 v2 = fait_control.findAnyValue(jdtyp);
         rtyp = fait_control.findDataType("boolean");
	 v1 = v0.performOperation(rtyp,v2,FaitOperator.INSTANCEOF,here);
	 st1.pushStack(v1);
	 break;
      case DADD : case DDIV : case DMUL : case DREM : case DSUB :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
         rtyp = fait_control.findDataType("double");
	 v2 = v1.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v2);
	 break;
      case FADD : case FDIV : case FMUL : case FREM : case FSUB :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
         rtyp = fait_control.findDataType("float");
	 v2 = v1.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v2);
	 break;
      case IADD : case IDIV : case IMUL : case IREM : case ISUB :
      case IAND : case IOR : case IXOR :
      case ISHL : case ISHR : case IUSHR :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
         rtyp = fait_control.findDataType("int");
	 v2 = v1.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v2);
	 break;
      case DCMPG : case DCMPL :
      case FCMPG : case FCMPL :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
         rtyp = fait_control.findDataType("int");
	 v2 = fait_control.findRangeValue(rtyp,-1,1);
	 st1.pushStack(v2);
	 break;
      case LADD : case LDIV : case LMUL : case LREM : case LSUB :
      case LAND : case LOR : case LXOR :
      case LSHL : case LSHR : case LUSHR :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
         rtyp = fait_control.findDataType("long");
	 v2 = v1.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v2);
	 break;
      case BIPUSH :
      case SIPUSH :
	 i0 = ins.getIntValue();
         rtyp = fait_control.findDataType("int");
	 v0 = fait_control.findRangeValue(rtyp,i0,i0);
	 st1.pushStack(v0);
	 break;
      case DCONST_0 :
         rtyp = fait_control.findDataType("double");
	 v0 = fait_control.findRangeValue(rtyp,0,0);
	 st1.pushStack(v0);
	 break;
      case DCONST_1 :
         rtyp = fait_control.findDataType("double");
	 v0 = fait_control.findRangeValue(rtyp,1.0,1.0);
	 st1.pushStack(v0);
	 break;
      case DLOAD : case DLOAD_0 : case DLOAD_1 : case DLOAD_2 : case DLOAD_3 :
      case FLOAD : case FLOAD_0 : case FLOAD_1 : case FLOAD_2 : case FLOAD_3 :
      case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
      case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
	 i0 = ins.getLocalVariable();
	 st1.pushStack(st1.getLocal(i0));
	 break;
      case FCONST_0 :
         rtyp = fait_control.findDataType("float");
	 v0 = fait_control.findRangeValue(rtyp,0.0,0.0);
	 st1.pushStack(v0);
	 break;
      case FCONST_1 : 
         rtyp = fait_control.findDataType("float");
	 v0 = fait_control.findRangeValue(rtyp,1.0,1.0);
	 st1.pushStack(v0);
	 break;
      case FCONST_2 :
         rtyp = fait_control.findDataType("float");
	 v0 = fait_control.findRangeValue(rtyp,2.0,2.0);
	 st1.pushStack(v0);
	 break;
      case LCONST_0 : 
         rtyp = fait_control.findDataType("long");
	 v0 = fait_control.findRangeValue(rtyp,0,0);
	 st1.pushStack(v0);
	 break;
      case LCONST_1 :
         rtyp = fait_control.findDataType("long");
	 v0 = fait_control.findRangeValue(rtyp,1,1);
	 st1.pushStack(v0);
	 break;
      case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
      case ICONST_5 : case ICONST_M1 :
	 i0 = ins.getIntValue();
         rtyp = fait_control.findDataType("int");
	 v0 = fait_control.findRangeValue(rtyp,i0,i0);
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
            Float vf = (Float) ov;
            rtyp = fait_control.findDataType("float");
	    v0 = fait_control.findRangeValue(rtyp,vf.doubleValue(),vf.doubleValue());
	  }
	 else if (ov instanceof Double) {
            Double dv = (Double) ov;
            rtyp = fait_control.findDataType("double");
	    v0 = fait_control.findRangeValue(rtyp,dv,dv);
	  }
	 else if (ov instanceof Long) {
	    long l0 = (Long) ov;
            rtyp = fait_control.findDataType("long");
	    v0 = fait_control.findRangeValue(rtyp,l0,l0);
	  }
	 else if (ov instanceof Integer) {
	    i0 = (Integer) ov;
            rtyp = fait_control.findDataType("int");
	    v0 = fait_control.findRangeValue(rtyp,i0,i0);
	  }
	 else {
            rtyp = fait_control.findDataType("java.lang.Class",FaitAnnotation.NON_NULL);
	    v0 = fait_control.findNativeValue(rtyp);
	    v0 = v0.forceNonNull();
	  }
	 st1.pushStack(v0);
	 break;
      case D2F : case FNEG : case I2F : case L2F :
	 v0 = st1.popStack();
         rtyp = fait_control.findDataType("float");
	 v1 = v0.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v1);
	 break;
      case D2I : case F2I : case L2I : case INEG :
	 v0 = st1.popStack();
         rtyp = fait_control.findDataType("int");
	 v1 = v0.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v1);
	 break;
      case IINC :
	 v0 = st1.getLocal(ins.getLocalVariable());
	 i0 = ins.getIntValue();
         rtyp = fait_control.findDataType("int");
	 v1 = fait_control.findRangeValue(rtyp,i0,i0);
	 v2 = v0.performOperation(rtyp,v1,op_map.get(ins.getOpcode()),here);
	 st1.setLocal(ins.getLocalVariable(),v2);
	 break;
      case D2L : case F2L : case I2L : case LNEG :
	 v0 = st1.popStack();
         rtyp = fait_control.findDataType("long");
	 v1 = v0.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v1);
	 break;
      case LCMP :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
         rtyp = fait_control.findDataType("int");
	 v2 = v1.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v2);
	 break;
      case DNEG : case F2D : case I2D : case L2D :
	 v0 = st1.popStack();
         rtyp = fait_control.findDataType("double");
	 v1 = v0.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v1);
	 break;
      case I2B :
	 v0 = st1.popStack();
         rtyp = fait_control.findDataType("byte");
	 v1 = v0.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v1);
	 break;
      case I2C :
	 v0 = st1.popStack();
         rtyp = fait_control.findDataType("char");
	 v1 = v0.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v1);
	 break;
      case I2S :
	 v0 = st1.popStack();
         rtyp = fait_control.findDataType("short");
	 v1 = v0.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
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
	 if (FaitLog.isTracing()) FaitLog.logD1("Set local " + i0 + " = " + v0);
	 break;
         
/* BRANCH INSTRUCTIONS */
      case GOTO :
      case GOTO_W :
	 nins = inspt.getReferencedTarget();
	 break;
      case IF_ACMPEQ : case IF_ACMPNE :
      case IF_ICMPEQ : case IF_ICMPNE :
      case IF_ICMPLT : case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 if (FaitLog.isTracing()) FaitLog.logD1("Compare " + v1 + " :: " + v0);
         Pair<IfaceState,IfaceState> imps = handleImplications(st1,inspt,v0,v1);
	 brslt = v1.branchTest(v0,op_map.get(ins.getOpcode()));
	 if (brslt != TestBranch.NEVER) {
            wq.mergeState(imps.getElement0(),inspt.getReferencedTarget());
          }	 
         else call.addError(inspt,BRANCH_NEVER_TAKEN);
	 if (brslt == TestBranch.ALWAYS) nins = null;
         else st1 = imps.getElement1();
	 break;
      case IFEQ : case IFNE : case IFLT : case IFGE : case IFGT : case IFLE :
      case IFNONNULL : case IFNULL :
	 v0 = st1.popStack();
	 if (FaitLog.isTracing()) FaitLog.logD1("Test Value = " + v0);
	 brslt = v0.branchTest(v0,op_map.get(ins.getOpcode()));
         imps = handleImplications(st1,inspt,v0,null);
	 if (brslt != TestBranch.NEVER) {
            wq.mergeState(imps.getElement0(),inspt.getReferencedTarget());
          }
	 else call.addError(inspt,BRANCH_NEVER_TAKEN);
	 if (brslt == TestBranch.ALWAYS) nins = null;
         else st1 = imps.getElement1();
	 break;
      case LOOKUPSWITCH :
      case TABLESWITCH :
	 v0 = st1.popStack();
	 for (IfaceProgramPoint xin : inspt.getReferencedTargets()) {
	    wq.mergeState(st1,xin);
	  }
	 nins = null;
	 pins = null;
	 break;
         
/* SUBROUTINE CALLS */
         
      case JSR : case JSR_W :
	 st1.pushStack(fait_control.findBadValue());
	 st1.pushReturn(nins);
	 nins = inspt.getReferencedTarget();
	 break;
      case RET :
	 nins = st1.popReturn();
	 break;
         
/* CALL INSTRUCTIONS */
         
      case ARETURN :
      case DRETURN : case FRETURN : case IRETURN : case LRETURN :
	 v0 = st1.popStack();
	 flow_queue.handleReturn(call,v0);
	 nins = null;
	 pins = null;
	 break;
      case RETURN :
         rtyp = fait_control.findDataType("void");
	 v0 = fait_control.findAnyValue(rtyp);
	 flow_queue.handleReturn(call,v0);
	 nins = null;
	 pins = null;
	 break;
      case INVOKEINTERFACE :
      case INVOKESPECIAL :
      case INVOKESTATIC :
      case INVOKEVIRTUAL :
	 IfaceMethod fm = inspt.getReferencedMethod();
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 if (!flow_queue.handleCall(here,st1,wq)) {
	    if (FaitLog.isTracing()) FaitLog.logD1("Unknown RETURN value for " + fm);
	    IfaceCall ncall = call.getMethodCalled(inspt,fm);
	    if (ncall != null && ncall.getCanExit()) pins = null;
	    nins = null;
	  }
	 break;
      case INVOKEDYNAMIC :
         handleDynamicCall(here,st1);  
         break;
      case ATHROW :
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 v0 = st1.popStack();
	 nins = null;
	 pins = null;
	 flow_queue.handleThrow(wq,here,v0,st1);
	 break;
         
/* ARRAY PROCESSING INSTRUCTIONS */
      case AALOAD :
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 v2 = st1.popStack();		// index
	 v0 = st1.popStack();		// array
	 v1 = flow_queue.handleArrayAccess(here,v0,v2);
	 if (FaitLog.isTracing()) FaitLog.logD1("Array " + v0 + " index " + v2 + " = " + v1);
	 st1.pushStack(v1);
	 break;
      case AASTORE :
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = st1.popStack();
	 flow_queue.handleArraySet(here,v2,v0,v1);
	 break;
      case ANEWARRAY :
	 v0 = st1.popStack();
	 flow_queue.initialize(jdtyp);
	 v1 = flow_queue.handleNewArraySet(here,jdtyp,1,v0);
	 st1.pushStack(v1);
	 break;
      case MULTIANEWARRAY :
	 i0 = ins.getIntValue();
	 flow_queue.initialize(jdtyp);
	 dtyp = jdtyp.getArrayType();
	 for (int i = 0; i < ins.getIntValue(); ++i) {
	    v0 = st1.popStack();
	  }
	 v1 = flow_queue.handleNewArraySet(here,dtyp,ins.getIntValue(),null);
	 st1.pushStack(v1);
	 break;
      case NEWARRAY :
	 v0 = st1.popStack();
	 v1 = flow_queue.handleNewArraySet(here,jdtyp,1,v0);
	 st1.pushStack(v1);
	 break;
      case ARRAYLENGTH :
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 v0 = st1.popStack();
         v1 = v0.getArrayLength();
         if (!v1.getDataType().isNumericType()) {
            System.err.println("BAD LENGTH");
            v1 = v0.getArrayLength();
          }
	 st1.pushStack(v1);
	 break;
      case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 v2 = st1.popStack();			// index
	 v0 = st1.popStack();
	 v1 = flow_queue.handleArrayAccess(here,v0,v2);
	 if (FaitLog.isTracing()) FaitLog.logD1("Array " + v0 + " index " + v2 + " = " + v1);
	 st1.pushStack(v1);
	 break;
      case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
	 st1 = handleAccess(here,st1);
	 if (st1 == null) break;
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 v2 = st1.popStack();
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
	 v1 = flow_queue.handleFieldGet(here,st1,oref,v0);
	 if (FaitLog.isTracing()) FaitLog.logD1("Field of " + v0 + " = " + v1);
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
	 if (!call.getMethod().isStatic()) {
	    if (v1 == st1.getLocal(0)) oref = true;
	  }
	 if (FaitLog.isTracing()) FaitLog.logD1("Set field of " + v1 + " = " + v0);
	 flow_queue.handleFieldSet(here,st1,oref,v0,v1);
	 break;
      case PUTSTATIC :
	 v0 = st1.popStack();
	 if (FaitLog.isTracing()) FaitLog.logD1("Static field = " + v0);
	 flow_queue.handleFieldSet(here,st1,false,v0,null);
	 break;
         
      default :
	 FaitLog.logE("FAIT: Opcode " + ins.getOpcode() + " not found");
	 break;
    }
   
   if (nins != null && st1 != null) wq.mergeState(st1,nins);
   else if (pins != null) call.addError(inspt,UNREACHABLE_CODE);
}



/********************************************************************************/
/*										*/
/*	Source helper methods							*/
/*										*/
/********************************************************************************/





 
/********************************************************************************/
/*										*/
/*	Handle accessing a value that must be non-null				*/
/*										*/
/********************************************************************************/

private IfaceState handleAccess(FlowLocation loc,IfaceState st)
{
   IfaceProgramPoint ins = loc.getProgramPoint();
   
   // First determine which argument
   int act = 0;
   switch (ins.getInstruction().getOpcode()) {
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
	 IfaceMethod mthd = ins.getReferencedMethod();
	 if (mthd == null) return st;
	 if (mthd.isStatic() || mthd.isConstructor()) return st;
	 act = mthd.getNumArgs();
	 break;
      default :
	 return st;
    }
   
   // next scan the code to handle any implications
   st = handleAccess(loc,act,st);
   
   // next check the argument itself
   LinkedList<IfaceValue> vl = new LinkedList<IfaceValue>();
   for (int i = 0; i < act; ++i) {
      vl.addFirst(st.popStack());
    }
   IfaceValue v0 = st.popStack();
   if (v0.mustBeNull()) {
      FaitLog.logD1("Access of NULL: can't proceed");
      return null;
    }
   v0 = v0.forceNonNull();
   st.pushStack(v0);
   for (IfaceValue v1 : vl) st.pushStack(v1);
   
   return st;
}



IfaceState handleAccess(IfaceLocation loc,int act,IfaceState st0)
{
   IfaceProgramPoint ins = loc.getProgramPoint();
   boolean inst = !loc.getMethod().isStatic();
   
   IfaceProgramPoint sino = skipWhere(ins.getPrevious(),act);
   
   WhereItem where = getWhere(sino,1);
   
   if (where != null) {
      st0 = where.setNonNull(st0,false,true,inst);
    }
   
   return st0;
}





/********************************************************************************/
/*                                                                              */
/*      Handle implications due to branches                                     */
/*                                                                              */
/********************************************************************************/

IfaceState handleImplications(FlowQueueInstance wq,IfaceProgramPoint ins,
      IfaceState st0,TestBranch brslt)
{
   int act = 1;
   switch (ins.getInstruction().getOpcode()) {
      case IF_ICMPEQ : case IF_ICMPNE : case IF_ICMPLT :
      case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
	 act = 2;
	 break;
    }
   
   boolean inst = !ins.getMethod().isStatic();
   WhereItem where = getWhere(ins.getPrevious(),act);
   
   if (brslt != TestBranch.NEVER) {
      IfaceState st1 = st0;
      if (where != null && brslt != TestBranch.ALWAYS) {
	 switch (ins.getInstruction().getOpcode()) {
	    case IFNULL :
	       st1 = where.setNull(st0,true,inst);
	       break;
	    case IFNONNULL :
	       st1 = where.setNonNull(st0,true,false,inst);
	       break;
	    case IFEQ :
	    case IF_ICMPEQ :
	       st1 = where.setEqual(st0,true,inst);
	       break;
	    case IFNE :
	    case IF_ICMPNE :
	       st1 = where.setNonEqual(st0,true,inst);
	       break;
	  }
       }
      IfaceProgramPoint tins = ins.getReferencedTarget();
      wq.mergeState(st1,tins);
    }
   
   if (brslt == TestBranch.ALWAYS) return null;
   
   if (where != null && brslt != TestBranch.NEVER) {
      switch (ins.getInstruction().getOpcode()) {
	 case IFNULL :
	    st0 = where.setNonNull(st0,false,false,inst);
	    break;
	 case IFNONNULL :
	    st0 = where.setNull(st0,false,inst);
	    break;
	 case IFEQ :
	 case IF_ICMPEQ :
	    st0 = where.setNonEqual(st0,false,inst);
	    break;
	 case IFNE :
	 case IF_ICMPNE :
	    st0 = where.setEqual(st0,false,inst);
	    break;
       }
    }
   
   return st0;
}



private Pair<IfaceState,IfaceState> handleImplications(IfaceState st0,IfaceProgramPoint ins,IfaceValue v0,IfaceValue v1)
{
   Pair<IfaceState,IfaceState> rslt = Pair.createPair(st0,st0);
   int insopc = ins.getInstruction().getOpcode();
   FaitOperator op = op_map.get(insopc);
   int act = 1;
   switch (ins.getInstruction().getOpcode()) {
      case IF_ICMPEQ : case IF_ICMPNE : case IF_ICMPLT :
      case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
      case IF_ACMPEQ : case IF_ACMPNE :
	 act = 2;
	 break;
    }
   
   Pair<WhereItem,WhereItem> where = getSources(ins,act);
   IfaceImplications imps = v0.getImpliedValues(v1,op);
   if (imps == null) return rslt;
   WhereItem lhsw = where.getElement0();
   WhereItem rhsw = where.getElement1();
   if (lhsw == null && rhsw == null) return rslt;
   
   if (lhsw != null) {
      
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Lambdas and related code handling                                       */
/*                                                                              */
/********************************************************************************/

private void handleDynamicCall(IfaceLocation here,IfaceState st)
{
   IfaceProgramPoint inspt = here.getProgramPoint();
   JcodeInstruction ins = inspt.getInstruction();
   String [] args = ins.getDynamicReference();
   IfaceType t1 = fait_control.createFunctionRefType(args[5]);
   IfaceEntity ent = fait_control.findFunctionRefEntity(here,t1,args[4]);
   IfaceEntitySet eset = fait_control.createSingletonSet(ent);
   IfaceValue val = fait_control.findObjectValue(t1,eset,FaitAnnotation.NON_NULL);
   st.pushStack(val);
}




/********************************************************************************/
/*										*/
/*	Methods to find the source for a value					*/
/*										*/
/********************************************************************************/

private WhereItem getWhere(IfaceProgramPoint ins,int act)
{
   if (ins == null) return null;
   
   WhereItem where = null;
   IfaceField fld = null;
   
   while (where == null && ins != null) {
      switch (ins.getInstruction().getOpcode()) {
	 case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
	 case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
	 case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
	    where = new WhereItem(ins.getInstruction().getLocalVariable(),fld);
	    break;
	 case DUP : case DUP_X1 : case DUP_X2 : case DUP2 : case DUP2_X1 : case DUP2_X2 :
	    break;
	 case GETFIELD :
	    if (fld != null) return null;
	    fld = ins.getReferencedField();
	    break;
	 case GETSTATIC :
	    if (fld != null) return null;
	    fld = ins.getReferencedField();
	    where = new WhereItem(-1,fld);
	    break;
	 case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
	 case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :
	 case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
	    IfaceProgramPoint pins = ins.getPrevious();
	    if (pins != null) {
	       switch (pins.getInstruction().getOpcode()) {
		  case DUP : case DUP_X1 : case DUP_X2 : case DUP2 :
		  case DUP2_X1 : case DUP2_X2 :
		     where = new WhereItem(ins.getInstruction().getLocalVariable(),fld);
		     break;
		  case ISUB :
		     // check for iconst 1, dup preceding theis and use value-1
		     return null;
		  default :
		     return null;
		}
	     }
	    break;
	 default :
	    return null;
       }
      ins = ins.getPrevious();
    }
   
   if (where == null) return null;
   
   if (act > 1) {
      switch (ins.getInstruction().getOpcode()) {
	 case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
	 case ICONST_5 : case ICONST_M1 :
	 case LCONST_0 : case LCONST_1 :
	    where.setValue(ins.getInstruction().getIntValue());
	    break;
	 case LDC : case LDC_W :
	    Object o = ins.getInstruction().getObjectValue();
	    if (o instanceof Number) {
	       where.setValue(((Number) o).longValue());
	     }
	    else where = null;
	    break;
	 default :
	    where = null;
	    break;
       }
    }
   
   return where;
}




private Pair<WhereItem,WhereItem> getSources(IfaceProgramPoint ins,int act)
{
   if (ins == null) return null;
   WhereItem [] wheres = new WhereItem[2];
   wheres[0] = wheres[1] = null;
   
   IfaceField fld = null;
   WhereItem where = null;
   boolean done = false;
   boolean cont = true;
   ins = ins.getPrevious(); 
   
   while (act > 0 && cont && ins != null) {
      switch (ins.getInstruction().getOpcode()) {
	 case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
	 case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
	 case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
	    where = new WhereItem(ins.getInstruction().getLocalVariable(),fld);
            done = true;
	    break;
	 case DUP : case DUP_X1 : case DUP_X2 : case DUP2 : case DUP2_X1 : case DUP2_X2 :
            cont = false;
	    break;
	 case GETFIELD :
	    if (fld != null) {
                done = true;
             }
	    else fld = ins.getReferencedField();
	    break;
	 case GETSTATIC :
	    if (fld != null) return null;
	    fld = ins.getReferencedField();
	    where = new WhereItem(-1,fld);
	    break;
	 case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
	 case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :
	 case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
	    IfaceProgramPoint pins = ins.getPrevious();
	    if (pins != null) {
	       switch (pins.getInstruction().getOpcode()) {
		  case DUP : case DUP_X1 : case DUP_X2 : case DUP2 :
		  case DUP2_X1 : case DUP2_X2 :
		     where = new WhereItem(ins.getInstruction().getLocalVariable(),fld);
		     break;
		  default :
		     done = true;
                     break;
		}
	     }
	    break;
         case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
	 case ICONST_5 : case ICONST_M1 :
	 case LCONST_0 : case LCONST_1 :
   	 case LDC : case LDC_W :
            done = true;
	    break;
	 default :
	    cont = false;
            done = true;
       }
      if (where != null || done) {
         wheres[act-1] = where;
         where = null;
         --act;
       }
      ins = ins.getPrevious();
    }
   
   Pair<WhereItem,WhereItem> rslt = Pair.createPair(wheres[0],wheres[1]);
   
   return rslt;
}



private IfaceProgramPoint skipWhere(IfaceProgramPoint ins,int act)
{
   int tot = 0;
   while (ins != null && tot < act) {
      int diff = ins.getInstruction().getStackDiff();
      tot += diff;
      ins = ins.getPrevious();
    }
   
   return ins;
}



/********************************************************************************/
/*										*/
/*	Class representing a value source					*/
/*										*/
/********************************************************************************/

private class WhereItem {
   
   private int var_number;
   private IfaceField field_name;
   private long int_value;
   private boolean is_static;
   
   WhereItem(int var,IfaceField fld) {
      var_number = var;
      field_name = fld;
      is_static = (fld == null ? true : fld.isStatic());
      int_value = 0;
    }
   
   void setValue(long v)			{ int_value = v; }
   
   IfaceState setNull(IfaceState cs,boolean clone,boolean inst) {
      if (field_name == null) {
         if (var_number >= 0) {
            IfaceValue v = cs.getLocal(var_number);
            if (!v.mustBeNull()) {
               if (clone) cs = cs.cloneState();
               cs.setLocal(var_number,fait_control.findNullValue());
             }
          }
       }
      else {
         if (var_number == 0 && inst) {
            IfaceValue v = cs.getFieldValue(field_name);
            if (v == null || !v.mustBeNull()) {
               if (clone) cs = cs.cloneState();
               cs.setFieldValue(field_name,fait_control.findNullValue());
             }
          }
       }
      return cs;
    }
   
   IfaceState setNonNull(IfaceState cs,boolean clone,boolean force,boolean inst) {
      if (field_name == null) {
         if (var_number >= 0) {
            IfaceValue cv = cs.getLocal(var_number);
            IfaceValue cv0 = cv.forceNonNull();
            if (cv != cv0) {
               if (clone) cs = cs.cloneState();
               cs.setLocal(var_number,cv0);
             }
          }
       }
      else {
         if (var_number == 0 && inst && !is_static) {
            IfaceValue cv = cs.getFieldValue(field_name);
            if (cv != null && cv.canBeNull()) {
               cv = cv.forceNonNull();
               if (clone) cs = cs.cloneState();
               cs.setFieldValue(field_name,cv);
             }
          }
       }
      return cs;
    }
   
   IfaceState setEqual(IfaceState cs,boolean clone,boolean inst) {
      IfaceType dt = fait_control.findDataType("int");
      IfaceValue v = fait_control.findRangeValue(dt,int_value,int_value);
      if (field_name == null) {
         if (var_number >= 0) {
            if (clone) cs = cs.cloneState();
            cs.setLocal(var_number,v);
          }
       }
      else if (var_number == 0 && inst) {
         if (cs.getFieldValue(field_name) != v) {
            if (clone) cs = cs.cloneState();
            cs.setFieldValue(field_name,v);
          }
       }
      return cs;
    }
   
   IfaceState setNonEqual(IfaceState cs,boolean cln,boolean inst) {
      return cs;
    }
   
}	// end of inner class WhereItem


}       // end of class FlowScannerByteCode




/* end of FlowScannerByteCode.java */

