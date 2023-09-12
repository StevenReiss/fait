/********************************************************************************/
/*										*/
/*		ServerBuildTestDriver.java					*/
/*										*/
/*	Build a test driver for a project					*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
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



package edu.brown.cs.fait.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.fait.iface.FaitLog;
import edu.brown.cs.ivy.jcode.JcodeAnnotation;
import edu.brown.cs.ivy.jcode.JcodeClass;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcode.JcodeMethod;

class ServerBuildTestDriver implements ServerConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ServerProject	for_project;
private StringBuffer	output_file;
private List<String>	test_methods;

private static final String OBJECT = "testobject";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ServerBuildTestDriver(ServerProject sp)
{
   for_project = sp;
   output_file = null;
   test_methods = new ArrayList<>();
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

ServerFile process(List<String> classes)
{
   JcodeFactory jf = for_project.getJcodeFactory();
   for (String s : classes) {
      JcodeClass jc = jf.findClass(s);
      if (jc != null) processClass(jf,jc);
    }

   if (output_file == null) return null;
   
   outputTrailer();
   
   FaitLog.logD("Generate tests: " + output_file.toString());
   
   return new ServerFile(new File(TEST_FILE_NAME),output_file.toString(),"\n");
}



/********************************************************************************/
/*										*/
/*	Get the set of test methods						*/
/*										*/
/********************************************************************************/

private void processClass(JcodeFactory jf,JcodeClass jc)
{
   List<JcodeMethod> beforeclass = new ArrayList<>();
   List<JcodeMethod> before = new ArrayList<>();
   List<JcodeMethod> tests = new ArrayList<>();
   List<JcodeMethod> after = new ArrayList<>();
   List<JcodeMethod> afterclass = new ArrayList<>();

   for (JcodeMethod jm : jc.getMethods()) {
       List<JcodeAnnotation> annots = jm.getReturnAnnotations();
       if (annots != null) {
	  for (JcodeAnnotation ja : annots) {
	     String nm = ja.getDescription();
	     if (nm.startsWith("org.junit.")) {
		int idx = nm.lastIndexOf(".");
		nm = nm.substring(idx+1);
		switch (nm) {
		   case "BeforeClass" :
		      beforeclass.add(jm);
		      break;
		   case "Before" :
		      before.add(jm);
		      break;
		   case "Test" :
		      tests.add(jm);
		      break;
		   case "After" :
		      after.add(jm);
		      break;
		   case "AfterClass" :
		      afterclass.add(jm);
		      break;
		   default :
		      break;
		 }
	      }
	   }
	}
    }

   if (tests.isEmpty()) {
      // handle JUNIT 3 tests
      boolean checkmethods = false;
      if (!jc.isInterface() && !jc.isEnum() && jc.isPublic()) {
         for (JcodeClass jx = jc; jx != null; ) {
            String sup = jx.superName;
            if (sup == null) break;
            if (sup.endsWith("TestCase")) {
               checkmethods = true;
               break;
             }
            jx = jf.findClass(sup);
          }
       }
      if (checkmethods) {
         Boolean cnstok = null;
         for (JcodeMethod jm : jc.getMethods()) {
            if (jm.isStaticInitializer()) continue;
            else if (jm.isConstructor()) {
               if (jm.isPublic() && jm.getNumArguments() <= 1) cnstok = true;
               else if (cnstok == null) cnstok = false;
             }
            else if (jm.isAbstract() || jm.isNative()) continue;
            if (jm.getName().equals("setUp") && !jm.isPrivate() && jm.getNumArguments() == 0) {
               before.add(jm);
             }
            else if (jm.getName().equals("tearDown") && !jm.isPrivate() && jm.getNumArguments() == 0) {
               after.add(jm);
             }
            else if (jm.getName().startsWith("test") && jm .isPublic() && jm.getNumArguments() == 0) {
               tests.add(jm);
             }
          }
         FaitLog.logD("CHECK TEST CLASS " + jc.getName() + " " + cnstok + " " + tests.size());
         if (cnstok == Boolean.FALSE) {
            tests.clear();
          }
       }
    }
   
   if (tests.isEmpty()) return;
   augmentClass(jf,jc.superName,beforeclass,before,afterclass,after);

   outputClassTests(jc,tests,beforeclass,before,afterclass,after);
}



private void augmentClass(JcodeFactory jf,String supnm,
      List<JcodeMethod> beforeclass,List<JcodeMethod> before,
      List<JcodeMethod> afterclass,List<JcodeMethod> after)
{
   if (supnm == null) return;
   JcodeClass jc = jf.findClass(supnm);
   if (jc == null) return;

   for (JcodeMethod jm : jc.getMethods()) {
      List<JcodeAnnotation> annots = jm.getReturnAnnotations();
      if (annots != null) {
	 for (JcodeAnnotation ja : annots) {
	    String nm = ja.getDescription();
	    if (nm.startsWith("org.junit.")) {
	       int idx = nm.lastIndexOf(".");
	       nm = nm.substring(idx+1);
	       switch (nm) {
		  case "BeforeClass" :
		     beforeclass.add(0,jm);
		     break;
		  case "Before" :
		     before.add(0,jm);
		     break;
		  case "After" :
		     after.add(jm);
		     break;
		  case "AfterClass" :
		     afterclass.add(jm);
		     break;
		  default :
		     break;
		}
	     }
	  }
       }
    }

   augmentClass(jf,jc.superName,beforeclass,before,afterclass,after);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputClassTests(JcodeClass jc,List<JcodeMethod> tests,
      List<JcodeMethod> beforeclass,List<JcodeMethod> before,	   List<JcodeMethod> afterclass,List<JcodeMethod> after)
{
   if (output_file == null) {
      output_file = new StringBuffer();
      outputHeader();
    }

   String nm = deriveTestName(jc);
   test_methods.add(nm);
   outputTestStart(nm);
   outputStaticCalls(beforeclass);
   outputStartTests(jc);
   outputTests(tests,before,after);
   outputDoneTests(jc);
   outputStaticCalls(afterclass);
   outputTestEnd(nm);
}



private void outputHeader()
{
   output_file.append("package fait.test;\n");
   output_file.append("public class FAIT_TEST_CLASS_GENERATED {\n");
   output_file.append("\n");
}



private void outputTrailer()
{
   output_file.append("public static void main(String [] args)\n");
   output_file.append("{\n");
   output_file.append("int k = args.length;\n");
   output_file.append("switch (k) {\n");
   int ct = 0;
   for (String s : test_methods) {
      output_file.append("case " + ct + ":\n");
      output_file.append("   " + s + "(k);\n");
      output_file.append("break;\n");
      ++ct;
    }
   output_file.append("}\n");                   // end switch
   output_file.append("}\n\n");                 // end main
   output_file.append("}\n");                   // end class
}


private void outputTestStart(String nm)
{
   output_file.append("\n");
   output_file.append("public static void " + nm + "(int k)\n");
   output_file.append("{\n");
}

private void outputTestEnd(String nm)
{
   output_file.append("}\n");
   output_file.append("\n");
}


private void outputStartTests(JcodeClass jc)
{
   output_file.append(outputName(jc) + " " + OBJECT + " = new " + outputName(jc) + "();\n");
   output_file.append("switch (k) {\n");
}



private void outputTests(List<JcodeMethod> tests,List<JcodeMethod> before,
      List<JcodeMethod> after)
{
   for (int i = 0; i < tests.size(); ++i) {
      JcodeMethod test = tests.get(i);
      output_file.append("case " + i + ":\n");
      outputCalls(before);
      outputCall(test);
      outputCalls(after);
      output_file.append("break;\n");
    }
}

private void outputDoneTests(JcodeClass jc)
{
   output_file.append("}\n");                   // end of switch
}




private void outputStaticCalls(List<JcodeMethod> calls)
{
   for (JcodeMethod jm : calls) {
      output_file.append(jm.getFullName() + "();\n");
    }
}


private void outputCalls(List<JcodeMethod> calls)
{
   for (JcodeMethod jm : calls) {
      outputCall(jm);
    }
}



private void outputCall(JcodeMethod jm)
{
   output_file.append(OBJECT + "." + jm.getName() + "();\n");
}

/********************************************************************************/
/*										*/
/*	Name methods								*/
/*										*/
/********************************************************************************/

private String deriveTestName(JcodeClass jc)
{
   String nm = jc.getName();
   nm = nm.replace("$",".");
   nm = nm.replace("/",".");
   nm = nm.replace(".","__");
   return "TEST_" + nm;
}


private String outputName(JcodeClass jc)
{
   String nm = jc.getName();
   nm = nm.replace("$",".");
   nm = nm.replace("/",".");
   return nm;
}



}	// end of class ServerBuildTestDriver




/* end of ServerBuildTestDriver.java */

