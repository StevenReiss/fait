/********************************************************************************/
/*										*/
/*		FlowScannerByteCode.java					*/
/*										*/
/*	description of class							*/
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;

import edu.brown.cs.fait.iface.FaitAnnotation;
import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.fait.iface.IfaceAnnotation;
import edu.brown.cs.fait.iface.IfaceBackElement;
import edu.brown.cs.fait.iface.IfaceCall;
import edu.brown.cs.fait.iface.IfaceControl;
import edu.brown.cs.fait.iface.IfaceEntity;
import edu.brown.cs.fait.iface.IfaceEntitySet;
import edu.brown.cs.fait.iface.IfaceField;
import edu.brown.cs.fait.iface.IfaceImplications;
import edu.brown.cs.fait.iface.IfaceLocation;
import edu.brown.cs.fait.iface.IfaceMethod;
import edu.brown.cs.fait.iface.IfaceProgramPoint;
import edu.brown.cs.fait.iface.IfaceStackMarker;
import edu.brown.cs.fait.iface.IfaceValue;
import edu.brown.cs.fait.iface.IfaceState;
import edu.brown.cs.fait.iface.IfaceType;
import edu.brown.cs.ivy.file.Pair;
import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcode.JcodeInstruction;

class FlowScannerByteCode extends FlowScanner implements FlowConstants, JcodeConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
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
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

FlowScannerByteCode(IfaceControl fc,FlowQueue fq,FlowQueueInstanceByteCode wq)
{
   super(fc,fq,wq);
}



/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

@Override int scanCode()
{
   int ctr = 0;
   while (!work_queue.isEmpty()) {
      IfaceProgramPoint fi = work_queue.getNext();
      try {
	 processInstruction(fi);
	 ++ctr;
       }
      catch (Throwable t) {
	 FaitLog.logE("Problem processing " + work_queue.getCall().getLogName() + ": " + t,t);
       }
    }

   return ctr;
}



int scanBack()
{
   int ctr = 0;

   while (!work_queue.isBackEmpty()) {
      IfaceBackElement be = work_queue.getNextBack();
      try {
	 IfaceProgramPoint pt = be.getProgramPoint();
	 if (pt != null) {
	    processBackInstruction(pt,be.getReference(),be.getSetType());
	    ++ctr;
	  }
       }
      catch (Throwable t) {
	 FaitLog.logE("Problem ast back processing " + work_queue.getCall().getLogName(),t);
       }
    }

   return ctr;
}



/********************************************************************************/
/*										*/
/*	Scan a single instruction						*/
/*										*/
/********************************************************************************/

private void processInstruction(IfaceProgramPoint inspt)
{
   JcodeInstruction ins = inspt.getInstruction();
   IfaceState st1 = work_queue.getState(inspt);
   IfaceCall call = work_queue.getCall();

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
   boolean notenext = true;
   FlowLocation here = new FlowLocation(flow_queue,call,inspt);

   switch (ins.getOpcode()) {
/* OBJECT PROCESSING INSTRUTIONS */
      case NEW :
	 IfaceType newt0 = jdtyp;
	 IfaceAnnotation [] nannots = fait_control.getAnnotations(inspt);
         if (newt0 == null) {
            FaitLog.logI("Unknown type for NEW: " + ins);
            nins = null;
            break; 
          }
	 if (nannots != null) newt0 = newt0.getAnnotatedType(nannots);
         newt0 = newt0.getAnnotatedType(FaitAnnotation.NON_NULL);
	 IfaceType newt1 = newt0.getComputedType(FaitTypeOperator.STARTINIT);
	 flow_queue.initialize(newt0);
         if (fait_control.isSingleAllocation(newt1,false)) 
            ent = fait_control.findFixedEntity(newt1);
         else
            ent = getLocalEntity(call,here,newt1);
	 v0 = fait_control.findObjectValue(newt1,fait_control.createSingletonSet(ent));
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
	 IfaceAnnotation [] annots = fait_control.getAnnotations(inspt);
	 IfaceType casttype = jdtyp;
	 if (annots != null) casttype = casttype.getAnnotatedType(annots);
	 v0 = v0.restrictByType(casttype);
	 if (v0.mustBeNull()) v0 = fait_control.findNullValue(casttype);
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
	    // sv1 = sv1.forceInitialized(FaitAnnotation.INITIALIZED);
	    IfaceType dupt0 = sv1.getDataType();
	    IfaceType dupt1 = dupt0.getComputedType(FaitTypeOperator.DONEINIT);
	    if (dupt0 != dupt1) {
	       sv1 = sv1.changeType(dupt1);
	     }
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
	 if (v0 == null || v1 == null) {
	    nins = null;
	    break;
	  }
	 rtyp = fait_control.findDataType("int");
	 v2 = v1.performOperation(rtyp,v0,op_map.get(ins.getOpcode()),here);
	 st1.pushStack(v2);
	 break;
      case DCMPG : case DCMPL :
      case FCMPG : case FCMPL :
	 v0 = st1.popStack();
	 v1 = st1.popStack();
	 rtyp = fait_control.findDataType("int");
	 v2 = fait_control.findRangeValue(rtyp,(Long)(-1L),(Long) 1L);
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
	 v0 = fait_control.findConstantValue(rtyp,i0);
	 st1.pushStack(v0);
	 break;
      case DCONST_0 :
	 rtyp = fait_control.findDataType("double");
	 v0 = fait_control.findConstantValue(rtyp,0);
	 st1.pushStack(v0);
	 break;
      case DCONST_1 :
	 rtyp = fait_control.findDataType("double");
	 v0 = fait_control.findConstantValue(rtyp,1.0);
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
	 v0 = fait_control.findConstantValue(rtyp,0.0);
	 st1.pushStack(v0);
	 break;
      case FCONST_1 :
	 rtyp = fait_control.findDataType("float");
	 v0 = fait_control.findConstantValue(rtyp,1.0);
	 st1.pushStack(v0);
	 break;
      case FCONST_2 :
	 rtyp = fait_control.findDataType("float");
	 v0 = fait_control.findConstantValue(rtyp,2.0);
	 st1.pushStack(v0);
	 break;
      case LCONST_0 :
	 rtyp = fait_control.findDataType("long");
	 v0 = fait_control.findConstantValue(rtyp,0);
	 st1.pushStack(v0);
	 break;
      case LCONST_1 :
	 rtyp = fait_control.findDataType("long");
	 v0 = fait_control.findConstantValue(rtyp,1);
	 st1.pushStack(v0);
	 break;
      case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
      case ICONST_5 : case ICONST_M1 :
	 i0 = ins.getIntValue();
	 rtyp = fait_control.findDataType("int");
	 v0 = fait_control.findConstantValue(rtyp,i0);
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
	    v0 = fait_control.findConstantValue(rtyp,vf.doubleValue());
	  }
	 else if (ov instanceof Double) {
	    Double dv = (Double) ov;
	    rtyp = fait_control.findDataType("double");
	    v0 = fait_control.findConstantValue(rtyp,dv);
	  }
	 else if (ov instanceof Long) {
	    long l0 = (Long) ov;
	    rtyp = fait_control.findDataType("long");
	    v0 = fait_control.findConstantValue(rtyp,l0);
	  }
	 else if (ov instanceof Integer) {
	    i0 = (Integer) ov;
	    rtyp = fait_control.findDataType("int");
	    v0 = fait_control.findConstantValue(rtyp,i0);
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
	 v1 = fait_control.findConstantValue(rtyp,i0);
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
	    FlowLocation nloc = newLocation(inspt.getReferencedTarget());
	    work_queue.mergeState(imps.getElement0(),nloc);
	  }
	 else call.addError(inspt,BRANCH_NEVER_TAKEN);
	 if (brslt == TestBranch.ALWAYS) nins = null;
	 else st1 = imps.getElement1();
	 break;
      case IFEQ : case IFNE : case IFLT : case IFGE : case IFGT : case IFLE :
      case IFNONNULL : case IFNULL :
	 v0 = st1.popStack();
	 if (FaitLog.isTracing()) FaitLog.logD1("Test Value = " + v0);
	 if (v0 == null) {
	    nins = null;
	    break;
	  }
	 brslt = v0.branchTest(v0,op_map.get(ins.getOpcode()));
	 imps = handleImplications(st1,inspt,v0,null);
	 if (brslt != TestBranch.NEVER) {
	    FlowLocation nloc = newLocation(inspt.getReferencedTarget());
	    work_queue.mergeState(imps.getElement0(),nloc);
	  }
	 else call.addError(inspt,BRANCH_NEVER_TAKEN);
	 if (brslt == TestBranch.ALWAYS) nins = null;
	 else st1 = imps.getElement1();
	 break;
      case LOOKUPSWITCH :
      case TABLESWITCH :
	 v0 = st1.popStack();
	 for (IfaceProgramPoint xin : inspt.getReferencedTargets()) {
	    FlowLocation nloc = newLocation(xin);
	    work_queue.mergeState(st1,nloc);
	  }
	 nins = null;
	 pins = null;
	 break;

/* SUBROUTINE CALLS */

      case JSR : case JSR_W :
	 nins = inspt.getReferencedTarget();
	 st1.pushStack(fait_control.findMarkerValue(nins,inspt));
	 break;
      case RET :
	 i0 = ins.getLocalVariable();
	 IfaceValue rmrk = st1.getLocal(i0);
         st1.setLocal(i0,null);
	 IfaceStackMarker smrk = (IfaceStackMarker) rmrk;
	 Set<Object> rtvs = smrk.getMarkerValues();
	 nins = null;
	 if (rtvs != null) {
	    for (Object rtpt : rtvs) {
	       IfaceProgramPoint cpt = (IfaceProgramPoint) rtpt;
	       IfaceProgramPoint pt = cpt.getNext();
	       IfaceState stnew = st1.cloneState();
	       IfaceState stprv = work_queue.getState(cpt);
	       // want to copy all unchanged items here
	       stnew.copyStackFrom(stprv);
	       FlowLocation nloc = newLocation(pt);
	       work_queue.mergeState(stnew,nloc);
	     }
	  }
	 break;

/* CALL INSTRUCTIONS */

      case ARETURN :
      case DRETURN : case FRETURN : case IRETURN : case LRETURN :
	 v0 = st1.popStack();
	 flow_queue.handleReturn(call,v0,st1,here);
	 nins = null;
	 pins = null;
	 break;
      case RETURN :
	 rtyp = fait_control.findDataType("void");
	 v0 = fait_control.findAnyValue(rtyp);
	 flow_queue.handleReturn(call,v0,st1,here);
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
	 switch (flow_queue.handleCall(here,st1,work_queue,-1)) {
	    case NOT_DONE :
	       if (FaitLog.isTracing()) FaitLog.logD1("Unknown RETURN value for " + fm);
	       IfaceCall ncall = call.getMethodCalled(inspt,fm);
	       if (ncall != null && ncall.getCanExit()) pins = null;
	       nins = null;
	       break;
	    case NO_RETURN :
	       ncall = call.getMethodCalled(inspt,fm);
	       if (ncall != null && ncall.getCanExit()) pins = null;
	       nins = null;
	       notenext = false;
	       break;
	    case CONTINUE :
	       break;
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
	 flow_queue.handleThrow(work_queue,here,v0,st1);
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
	 flow_queue.handleArraySet(here,v2,v0,v1,0);
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
	 dtyp = jdtyp;
	 for (int i = 0; i < i0; ++i) {
	    v0 = st1.popStack();
	  }
	 int ndim = 0;
	 while (dtyp.isArrayType()) {
	    dtyp = dtyp.getBaseType();
	    ++ndim;
	  }
	 v1 = flow_queue.handleNewArraySet(here,dtyp,ndim,null);
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
	 v1 = flow_queue.handleArrayLength(here,v0);
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
	 flow_queue.handleArraySet(here,v2,v0,v1,0);
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
	 flow_queue.handleFieldSet(here,st1,oref,v0,v1,0);
	 break;
      case PUTSTATIC :
	 v0 = st1.popStack();
	 if (FaitLog.isTracing()) FaitLog.logD1("Static field = " + v0);
	 flow_queue.handleFieldSet(here,st1,false,v0,null,0);
	 break;

      default :
	 FaitLog.logE("FAIT: Opcode " + ins.getOpcode() + " not found");
	 break;
    }

   if (nins != null && st1 != null) {
      FlowLocation nloc = newLocation(nins);
      work_queue.mergeState(st1,nloc);
    }
   else if (pins != null && notenext) call.addError(inspt,UNREACHABLE_CODE);
}



 /********************************************************************************/
/*										*/
/*	Backward scan a single instruction					*/
/*										*/
/********************************************************************************/

private void processBackInstruction(IfaceProgramPoint inspt,IfaceValue ref,IfaceType settype)
{
   JcodeInstruction ins = inspt.getInstruction();
   IfaceState st1 = work_queue.getState(inspt);
   if (FaitLog.isTracing()) {
      FaitLog.logD("Work BACK on " + ins + " " + ref + " " + settype + " " + ref.getRefSlot() + " " + ref.getRefStack() + " " + ref.getRefField());
    }

   if (st1.getPriorState(0) == work_queue.getCall().getStartState()) {
      return;
    }

   IfaceValue nextref = null;
   IfaceValue v0,v1;

   int stk = ref.getRefStack();
   int var = ref.getRefSlot();

   switch (ins.getOpcode()) {
/* OBJECT PROCESSING INSTRUTIONS */
      case DLOAD : case DLOAD_0 : case DLOAD_1 : case DLOAD_2 : case DLOAD_3 :
      case FLOAD : case FLOAD_0 : case FLOAD_1 : case FLOAD_2 : case FLOAD_3 :
      case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
      case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
      case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
	 if (stk == 0) {
	    nextref = fait_control.findRefValue(settype,ins.getLocalVariable());
	  }
	 else nextref = adjustRef(ref,0,1);
	 break;
      case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
      case DSTORE : case DSTORE_0 : case DSTORE_1 : case DSTORE_2 : case DSTORE_3 :
      case FSTORE : case FSTORE_0 : case FSTORE_1 : case FSTORE_2 : case FSTORE_3 :
      case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
      case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :
	 if (var == ins.getLocalVariable()) {
	    nextref = fait_control.findRefStackValue(settype,0);
	  }
	 else nextref = adjustRef(ref,1,0);
	 break;
      case DUP :
	 if (stk == 0 || stk == 1) {
	    nextref = fait_control.findRefStackValue(settype,0);
	  }
	 else nextref = adjustRef(ref,1,2);
	 break;
      case DUP_X1 :
	 if (stk == 2) {
	    nextref = fait_control.findRefStackValue(settype,0);
	  }
	 else if (stk == 0 || stk == 1) {
	    nextref = ref;
	  }
	 else nextref = adjustRef(ref,2,3);
	 break;
      case DUP_X2 :
	 // need to take category 2 values into account
	 if (stk == 3) {
	    nextref = fait_control.findRefStackValue(settype,0);
	  }
	 else if (stk == 0 || stk == 1 || stk == 2) {
	    nextref = ref;
	  }
	 else nextref = adjustRef(ref,3,4);
	 break;
      case DUP2 :
	 v0 = st1.getStack(0);
	 if (v0.isCategory2()) {
	    if (stk == 0 || stk == 1) {
	       nextref = fait_control.findRefStackValue(settype,0);
	     }
	    else nextref = adjustRef(ref,1,2);
	  }
	 else {
	    if (stk == 0 || stk == 2) {
	       nextref = fait_control.findRefStackValue(settype,0);
	     }
	    else if (stk == 1 || stk == 3) {
	       nextref = fait_control.findRefStackValue(settype,1);
	     }
	    else {
	       nextref = adjustRef(ref,2,4);
	     }
	  }
	 break;
      case DUP2_X1 :
	 v0 = st1.getStack(0);
	 if (v0.isCategory2()) {
	    if (stk == 2) {
	       nextref = fait_control.findRefStackValue(settype,0);
	     }
	    else if (stk == 0 || stk == 1) {
	       nextref = ref;
	     }
	    else nextref = adjustRef(ref,2,3);
	  }
	 else {
	    if (stk == 3) {
	       nextref = fait_control.findRefStackValue(settype,0);
	     }
	    else if (stk == 4) {
	       nextref = fait_control.findRefStackValue(settype,1);
	     }
	    else if (stk == 0 || stk == 1 || stk == 2) {
	       nextref = ref;
	     }
	    else nextref = adjustRef(ref,3,5);
	  }
	 break;
      case DUP2_X2 :
	 v0 = st1.getStack(0);
	 if (v0.isCategory2()) {
	    v1 = st1.getStack(1);
	    if (v1.isCategory2()) {
	       if (stk == 2) {
		  nextref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 0 || stk == 1) {
		  nextref = ref;
		}
	       else nextref = adjustRef(ref,2,3);
	     }
	    else {
	       if (stk == 3) {
		  nextref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 0 || stk == 1 || stk == 2) {
		  nextref = ref;
		}
	       else nextref = adjustRef(ref,3,4);
	     }
	  }
	 else {
	    v1 = st1.getStack(2);
	    if (v1.isCategory2()) {
	       if (stk == 3) {
		  nextref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 4) {
		  nextref = fait_control.findRefStackValue(settype,1);
		}
	       else if (stk == 0 || stk == 1 || stk == 2) nextref = ref;
	       else nextref = adjustRef(ref,3,5);
	     }
	    else {
	       if (stk == 4) {
		  nextref = fait_control.findRefStackValue(settype,0);
		}
	       else if (stk == 5) {
		  nextref = fait_control.findRefStackValue(settype,1);
		}
	       else if (stk == 0 || stk == 1 || stk == 2 || stk == 3) {
		  nextref = ref;
		}
	       else nextref = adjustRef(ref,4,6);
	     }
	  }
	 break;
      case MONITORENTER :
      case MONITOREXIT :
      case POP :
	 nextref = adjustRef(ref,1,0);
	 break;
      case POP2 :
	 v0 = st1.getStack(0);
	 if (v0.isCategory2()) nextref = adjustRef(ref,0,1);
	 else nextref = adjustRef(ref,0,2);
	 break;
      case SWAP :
	 if (stk == 0) {
	    nextref = fait_control.findRefStackValue(settype,1);
	  }
	 else if (stk == 1) {
	    nextref = fait_control.findRefStackValue(settype,0);
	  }
	 else nextref = adjustRef(ref,2,2);
	 break;

/* ARITHMETIC INSTRUCTIONS */
      case DADD : case DDIV : case DMUL : case DREM : case DSUB :
      case FADD : case FDIV : case FMUL : case FREM : case FSUB :
      case IADD : case IDIV : case IMUL : case IREM : case ISUB :
      case IAND : case IOR : case IXOR :
      case ISHL : case ISHR : case IUSHR :
      case LADD : case LDIV : case LMUL : case LREM : case LSUB :
      case LAND : case LOR : case LXOR :
      case LSHL : case LSHR : case LUSHR :
      case DCMPG : case DCMPL :
      case FCMPG : case FCMPL :
      case LCMP :
	 if (ref.getRefStack() == 0) {
	    IfaceValue vl = st1.getStack(1);
	    IfaceValue vr = st1.getStack(0);
	    List<IfaceType> ntyps = settype.getBackTypes(op_map.get(ins.getOpcode()),vl,vr);
	    if (ntyps != null) {
	       FlowLocation here = new FlowLocation(flow_queue,work_queue.getCall(),inspt);
	       IfaceType ntyp = ntyps.get(0);
	       if (ntyp != null) {
		  IfaceValue nref = fait_control.findRefStackValue(vl.getDataType(),1);
		  queueBackRefs(here,st1,nref,ntyp);
		}
	       ntyp = ntyps.get(1);
	       if (ntyp != null) {
		  IfaceValue nref = fait_control.findRefStackValue(vr.getDataType(),0);
		  queueBackRefs(here,st1,nref,ntyp);
		}
	     }
	    nextref = null;
	  }
	 else nextref = adjustRef(ref,2,1);
	 break;
      case BIPUSH :
      case SIPUSH :
      case NEW :
      case ACONST_NULL :
      case DCONST_0 :
      case DCONST_1 :
      case FCONST_0 :
      case FCONST_1 :
      case FCONST_2 :
      case LCONST_0 :
      case LCONST_1 :
      case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
      case ICONST_5 : case ICONST_M1 :
      case LDC :
      case LDC_W :
      case LDC2_W :
	 nextref = adjustRef(ref,0,1);
	 break;
      case D2F : case FNEG : case I2F : case L2F :
      case D2I : case F2I : case L2I : case INEG :
      case D2L : case F2L : case I2L : case LNEG :
      case DNEG : case F2D : case I2D : case L2D :
      case I2B : case I2C : case I2S :
      case INSTANCEOF :
      case CHECKCAST :
	 if (ref.getRefStack() == 0) {
	    IfaceValue vl = st1.getStack(0);
	    List<IfaceType> ntyps = settype.getBackTypes(op_map.get(ins.getOpcode()),vl);
	    if (ntyps != null) {
	       FlowLocation here = new FlowLocation(flow_queue,work_queue.getCall(),inspt);
	       IfaceType ntyp = ntyps.get(0);
	       if (ntyp != null) {
		  IfaceValue nref = fait_control.findRefStackValue(vl.getDataType(),0);
		  queueBackRefs(here,st1,nref,ntyp);
		}
	     }
	    nextref = null;
	  }
	 else nextref = adjustRef(ref,1,1);
	 break;
      case IINC :
	 if (var == ins.getLocalVariable()) nextref = null;
	 break;
      case NOP :
	 nextref = ref;
	 break;

/* BRANCH INSTRUCTIONS */
      case GOTO :
      case GOTO_W :
	 nextref = ref;
	 break;
      case IF_ACMPEQ : case IF_ACMPNE :
      case IF_ICMPEQ : case IF_ICMPNE :
      case IF_ICMPLT : case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :
	 nextref = adjustRef(ref,2,0);
	 break;
      case IFEQ : case IFNE : case IFLT : case IFGE : case IFGT : case IFLE :
      case IFNONNULL : case IFNULL :
	 nextref = adjustRef(ref,1,0);
	 break;
      case LOOKUPSWITCH :
      case TABLESWITCH :
	 nextref = adjustRef(ref,1,0);
	 break;

/* SUBROUTINE CALLS */
      case JSR : case JSR_W :
	 nextref = adjustRef(ref,0,1);
	 break;
      case RET :
	 nextref = ref;
	 break;

/* CALL INSTRUCTIONS */
      case ARETURN :
      case DRETURN : case FRETURN : case IRETURN : case LRETURN :
      case ATHROW :
	 nextref = adjustRef(ref,1,0);
	 break;
      case RETURN :
	 nextref = ref;
	 break;
      case INVOKEINTERFACE :
      case INVOKESPECIAL :
      case INVOKESTATIC :
      case INVOKEVIRTUAL :
	 IfaceMethod fm = inspt.getReferencedMethod();
	 int act = fm.getNumArgs();
	 if (!fm.isStatic()) act += 1;
	 if (fm.getReturnType() == null || fm.getReturnType().isVoidType()) {
	    nextref = adjustRef(ref,act,0);
	  }
	 else if (ref.getRefStack() == 0) {
	    FlowLocation here = new FlowLocation(flow_queue,work_queue.getCall(),inspt);
	    work_queue.getCall().backFlowReturn(here,settype);
	    nextref = null;
	  }
	 else if (ref.getRefStack() < 0 && ref.getRefSlot() < 0) nextref = null;
	 else nextref = adjustRef(ref,act,1);
	 break;
      case INVOKEDYNAMIC :
	 nextref = null;
	 break;

/* ARRAY PROCESSING INSTRUCTIONS */
      case AALOAD :
      case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
	 if (stk == 0) {
	    // nextref = fait_control.findRefValue(ref.getDataType(),st1.getStack(1),
		  // st1.getStack(0));
	    nextref = null;
	  }
	 else nextref = adjustRef(ref,2,1);
	 break;
      case AASTORE :
      case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
	 nextref = adjustRef(ref,3,0);
	 break;
      case ANEWARRAY :
      case NEWARRAY :
	 nextref = adjustRef(ref,1,1);
	 break;
      case MULTIANEWARRAY :
	 nextref = adjustRef(ref,ins.getIntValue(),1);
	 break;
      case ARRAYLENGTH :
	 nextref = adjustRef(ref,1,1);
	 break;

/* FIELD INSTRUCTIONS */
      case GETFIELD :
	 if (stk == 0) {
	    nextref = fait_control.findRefValue(ref.getDataType(),st1.getStack(0),
		  inspt.getReferencedField());
	  }
	 else nextref = adjustRef(ref,1,1);
	 break;
      case GETSTATIC :
	 if (stk == 0) {
	    nextref = fait_control.findRefValue(ref.getDataType(),null,
		  inspt.getReferencedField());
	  }
	 else nextref = adjustRef(ref,0,1);
	 break;
      case PUTFIELD :
	 nextref = adjustRef(ref,2,0);
	 break;
      case PUTSTATIC :
	 nextref = adjustRef(ref,1,0);
	 break;

      default :
	 FaitLog.logE("FAIT: Opcode " + ins.getOpcode() + " not found");
	 break;
    }

   if (nextref != null) {
      FlowLocation here = new FlowLocation(flow_queue,work_queue.getCall(),inspt);
      queueBackRefs(here,st1,nextref,settype);
      if (ins.getIndex() == 0) {
	 work_queue.getCall().backFlowParameter(nextref,settype);
       }
    }
}





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
   int vct = -1;

   switch (ins.getInstruction().getOpcode()) {
      case GETFIELD :
      case ARRAYLENGTH :
      case MONITORENTER :
      case MONITOREXIT :
      case ATHROW :
	 break;
      case PUTFIELD :
	 act = 1;
	 vct = 0;
	 break;
      case AALOAD : case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
	 act = 1;
	 break;
      case AASTORE : case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
	 vct = 1;
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

   IfaceValue v0 = st.getStack(act);
   if (v0 == null || v0.mustBeNull()) {
      if (FaitLog.isTracing()) FaitLog.logD1("Access of NULL: can't proceed");
      return null;
    }
   IfaceValue v1 = null;
   if (vct >= 0) {
      v1 = st.getStack(vct);
    }

   checkBackPropagation(loc,st,act,v0,FaitOperator.DEREFERENCE,v1);

   // set the value in the state after the dereference
   v0 = v0.forceNonNull();
   st.setStack(act,v0);

   return st;
}




/********************************************************************************/
/*										*/
/*	Handle implications due to branches					*/
/*										*/
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
      FlowLocation nloc = new FlowLocation(flow_queue,wq.getCall(),tins);
      wq.mergeState(st1,nloc);
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
   if (v0 == null) return rslt;

   Pair<WhereItem,WhereItem> where = getSources(ins,act);
   IfaceImplications imps = v0.getImpliedValues(v1,op);
   if (imps == null || where == null) return rslt;
   WhereItem lhsw = where.getElement0();
   WhereItem rhsw = where.getElement1();
   if (lhsw == null && rhsw == null) return rslt;

   if (lhsw != null) {

    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Lambdas and related code handling					*/
/*										*/
/********************************************************************************/

private void handleDynamicCall(IfaceLocation here,IfaceState st)
{
   IfaceProgramPoint inspt = here.getProgramPoint();
   JcodeInstruction ins = inspt.getInstruction();
   String [] args = ins.getDynamicReference();

   if (args[2].startsWith("java/lang/invoke/LambdaMetafactory.metafactory")) {
      IfaceType t1 = fait_control.createFunctionRefType(args[5],args[1]);
      if (!args[1].startsWith("()")) {
	 Type [] ar = Type.getArgumentTypes(args[1]);
	 for (int i = 0; i < ar.length; ++i) {
	    IfaceValue v0 = st.popStack();
	    if (FaitLog.isTracing()) {
	       FaitLog.logD1("Pop non-static value off stack: " + v0);
	     }
	  }
       }
      IfaceEntity ent = here.getCall().getBaseEntity(here.getProgramPoint());
      if (ent == null) {
         ent = fait_control.findFunctionRefEntity(here,t1,args[4]);
         here.getCall().setBaseEntity(here.getProgramPoint(),ent);
       }
      IfaceEntitySet eset = fait_control.createSingletonSet(ent);
      IfaceValue val = fait_control.findObjectValue(t1,eset,FaitAnnotation.NON_NULL);
      st.pushStack(val);
    }
   else if (args[2].startsWith("java/lang/invoke/StringConcatFactory.makeConcat")) {
      IfaceType rslttyp = fait_control.findDataType("java.lang.String");
      Type [] ar = Type.getArgumentTypes(args[1]);
      for (int i = 0; i < ar.length; ++i) {
	 IfaceValue v0 = st.popStack();
	 if (FaitLog.isTracing()) {
	    FaitLog.logD1("Pop non-static value off stack: " + v0);
	  }
       }
      IfaceValue val = fait_control.findAnyValue(rslttyp);
      st.pushStack(val);
    }
   else {
      String mtyp = args[1];
      int idx = mtyp.lastIndexOf(")");
      String rtypnm = mtyp.substring(idx+1);
      IfaceType rtyp = fait_control.findDataType(rtypnm);
      IfaceValue val = fait_control.findAnyValue(rtyp);
      st.pushStack(val);
      FaitLog.logE("HANDLE dynamic call" + args[2]);
    }
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
      IfaceValue v = fait_control.findConstantValue(dt,int_value);
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




/********************************************************************************/
/*										*/
/*	Getting location data							*/
/*										*/
/********************************************************************************/

private FlowLocation newLocation(IfaceProgramPoint pt)
{
   return new FlowLocation(flow_queue,work_queue.getCall(),pt);
}

}	// end of class FlowScannerByteCode




/* end of FlowScannerByteCode.java */

