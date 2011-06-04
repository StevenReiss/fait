/********************************************************************************/
/*										*/
/*		BcodeMethod.java						*/
/*										*/
/*	Byte code definitions method representation				*/
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
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH  HE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.fait.bcode;


import edu.brown.cs.fait.iface.*;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import java.security.*;

import java.util.*;
import java.lang.reflect.*;


class BcodeMethod extends MethodNode implements FaitMethod
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BcodeFactory		bcode_factory;
private BcodeClass		in_class;
private List<BcodeInstruction>	ins_list;
private Map<Label,Integer>	goto_map;
private String			match_name;
private byte [] 		message_digest;
private Collection<FaitMethod>	parent_methods;
private Collection<FaitMethod>	child_methods;
private Collection<FaitTryCatchBlock> try_blocks;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BcodeMethod(BcodeFactory bf,BcodeClass cls,int a,String n,String d,String s,String [] ex)
{
   super(a,n,d,s,ex);
   bcode_factory = bf;
   in_class = cls;
   match_name = null;
   goto_map = null;
   ins_list = null;
   parent_methods = null;
   child_methods = null;
   try_blocks = new ArrayList<FaitTryCatchBlock>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public FaitDataType getDeclaringClass()
{
   return in_class.getDataType();
}



@Override public String getName()
{
   return name;
}


@Override public String getDescription()
{
   return desc;
}


String getMatchName()
{
   if (match_name == null) {
      match_name = name + "." + desc;
    }
   return match_name;
}


@Override public Collection<FaitMethod> getAllCalls(FaitInstruction ins)
{
   return null;
}


@Override public FaitValue getParameterValues(int idx)
{
   return null;
}

@Override public FaitValue getThisValue()
{
   return null;
}


@Override public boolean isInProject()
{
   return in_class.isInProject();
}

@Override public boolean isStaticInitializer()
{
   return getName().equals("<clinit>");
}

@Override public boolean isStatic()
{
   return Modifier.isStatic(access);
}

@Override public boolean isAbstract()
{
   return Modifier.isAbstract(access);
}


@Override public boolean isNative()
{
   return Modifier.isNative(access);
}


@Override public boolean isPrivate()
{
   return Modifier.isPrivate(access);
}


@Override public boolean isSynchronized()
{
   return Modifier.isSynchronized(access);
}

@Override public boolean isConstructor()
{
   return getName().equals("<init>");
}


@Override public FaitDataType getReturnType()
{
   Type rt = Type.getReturnType(desc);
   return in_class.getDataType(rt);
}


@Override public List<FaitDataType> getExceptionTypes()
{
   List<FaitDataType> rslt = new ArrayList<FaitDataType>();
   for (int i = 0; i < exceptions.size(); ++i) {
      String enm = (String) exceptions.get(i);
      FaitDataType fdt = bcode_factory.findClassType(enm);
      if (fdt != null) rslt.add(fdt);
    }
   return rslt;
}



@Override public FaitDataType getArgType(int idx)
{
   Type [] atyps = Type.getArgumentTypes(desc);
   if (idx < 0 || idx >= atyps.length) return null;
   return in_class.getDataType(atyps[idx]);
}

@Override public int getNumArguments()
{
   Type [] atyps = Type.getArgumentTypes(desc);
   return atyps.length;
}



@Override public int getLocalSize()
{
   return maxLocals;
}


@Override public int getNumInstructions()
{
   if (ins_list == null) return 0;
   return ins_list.size();
}

@Override public FaitInstruction getInstruction(int idx)
{
   if (ins_list == null) return null;
   if (idx < 0 || idx >= ins_list.size()) return null;
   return ins_list.get(idx);
}


@Override public int getIndexOf(FaitInstruction ins)
{
   return ins_list.indexOf(ins);
}



/********************************************************************************/
/*										*/
/*	Visitation methods							*/
/*										*/
/********************************************************************************/

@Override public void visitEnd()
{
   super.visitEnd();

   int lno = 0;
   ins_list = new ArrayList<BcodeInstruction>();
   goto_map = new HashMap<Label,Integer>();
   int sz = 0;

   if (instructions.size() == 0) {
      if (!isStatic()) ++maxLocals;
      Type [] atyps = Type.getArgumentTypes(desc);
      for (int i = 0; i < atyps.length; ++i) {
	 maxLocals += 1;
	 switch (atyps[i].getSort()) {
	    case Type.LONG :
	    case Type.DOUBLE :
	       maxLocals += 1;
	       break;
	  }
       }
    }

   InsnList inl = instructions;
   for (int i = 0; i < inl.size(); ++i) {
      AbstractInsnNode ain = inl.get(i);
      switch (ain.getType()) {
	 case AbstractInsnNode.LABEL :
	    LabelNode lnode = (LabelNode) ain;
	    goto_map.put(lnode.getLabel(),sz);
	    break;
	 case AbstractInsnNode.LINE :
	    LineNumberNode lnnode = (LineNumberNode) ain;
	    lno = lnnode.line;
	    break;
	 case AbstractInsnNode.FRAME :
	    // these can be ignored
	    break;
	 default :
	    BcodeInstruction bi = new BcodeInstruction(this,sz,lno,ain);
	    ins_list.add(bi);
	    ++sz;
	    break;
       }
    }

   computeDigest();
}


@Override public void visitTypeInsn(int opc,String typ)
{
   in_class.getFactory().noteClass(typ);
   super.visitTypeInsn(opc,typ);
}


@Override public void visitFieldInsn(int opc,String o,String n,String d)
{
   in_class.getFactory().noteClass(o);
   in_class.getFactory().noteType(d);
   super.visitFieldInsn(opc,o,n,d);
}


@Override public void visitMethodInsn(int opc,String o,String n,String d)
{
   in_class.getFactory().noteClass(o);
   in_class.getFactory().noteType(d);
   super.visitMethodInsn(opc,o,n,d);
}



@Override public void visitTryCatchBlock(Label start,Label end,Label hdlr,String typ)
{
   super.visitTryCatchBlock(start,end,hdlr,typ);

   TryCatchData tcd = new TryCatchData(start,end,hdlr,typ);
   try_blocks.add(tcd);
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

BcodeFactory getFactory()		{ return bcode_factory; }

BcodeInstruction findInstruction(Label l)
{
   if (goto_map == null) return null;
   Integer idx = goto_map.get(l);
   if (idx == null) return null;
   return ins_list.get(idx);
}





/********************************************************************************/
/*										*/
/*	Digest methods							       */
/*										*/
/********************************************************************************/

private void computeDigest()
{
   MessageDigest md = null;
   try {
      md = MessageDigest.getInstance("MD5");
    }
   catch (NoSuchAlgorithmException e) {
      System.err.println("FAIT: Can't find MD5 digest");
      System.exit(1);
    }

   addToDigest(md,name);
   addToDigest(md,desc);
   addToDigest(md,signature);
   for (int i = 0; i < instructions.size(); ++i) {
      AbstractInsnNode ain = instructions.get(i);
      String ins = BcodeInstruction.getString(ain,this);
      addToDigest(md,ins);
    }

   message_digest = md.digest();
}


private void addToDigest(MessageDigest md,String s)
{
   if (s != null) md.update(s.getBytes());
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining method hierarchy				*/
/*										*/
/********************************************************************************/

@Override public synchronized Collection<FaitMethod> getParentMethods()
{
   if (parent_methods != null) return parent_methods;

   parent_methods = in_class.findParentMethods(name,desc,false,false,null);

   return parent_methods;
}



@Override public synchronized Collection<FaitMethod> getChildMethods()
{
   if (child_methods != null) return child_methods;

   if (isPrivate()) {
      child_methods = Collections.emptyList();
    }
   else {
      child_methods = in_class.findChildMethods(name,desc,false,null);
    }

   return child_methods;
}




/********************************************************************************/
/*										*/
/*	Exception handling							*/
/*										*/
/********************************************************************************/

@Override public Collection<FaitTryCatchBlock> getTryCatchBlocks()
{
   return try_blocks;
}



private class TryCatchData implements FaitTryCatchBlock {

   private Label start_label;
   private Label end_label;
   private Label handler_label;
   private String data_type;

   TryCatchData(Label start,Label end,Label handler,String typ) {
      start_label = start;
      end_label = end;
      handler_label = handler;
      data_type = typ;
    }

   @Override public FaitInstruction getStart() {
      return findInstruction(start_label);
    }

   @Override public FaitInstruction getEnd() {
      return findInstruction(end_label);
    }

   @Override public FaitInstruction getHandler() {
      return findInstruction(handler_label);
    }

   @Override public FaitDataType getException() {
      if (data_type == null) return null;
      return bcode_factory.findClassType(data_type);
    }

}	// end of inner class TryCatchData




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return getDeclaringClass().toString() + "." + getName() + getDescription();
}




}	// end of class BcodeMethod




/* end of BcodeMethod.java */

