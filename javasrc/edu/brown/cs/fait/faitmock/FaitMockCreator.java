/********************************************************************************/
/*										*/
/*		FaitMockCreator.java						*/
/*										*/
/*	Create an InvokeServlets.java file from a web.xml file			*/
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



package edu.brown.cs.fait.faitmock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import java.lang.reflect.Modifier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.xml.IvyXml;

public final class FaitMockCreator implements FaitMockConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   FaitMockCreator fmc = new FaitMockCreator(args);
   fmc.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		output_file;
private List<Element>	input_files;
private VelocityContext velocity_context;
private Map<String,Filter>	filter_map;
private Map<String,Servlet>	servlet_map;
private Map<String,String>	context_map;
private Map<String,Tag> 	tag_map;
private Map<String,Plugin>	plugin_map;
private Map<String,Action>	action_map;
private Map<String,Form>        form_map;
private Map<String,Controller>  controller_map;
private List<Listener>		listener_list;
private List<String>		use_paths;
private Map<String,File>        class_files;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private FaitMockCreator(String [] args)
{
   input_files = new ArrayList<>();

   velocity_context = new VelocityContext();
   velocity_context.put("package",DEFAULT_PACKAGE);
   setTargetClass(DEFAULT_CLASS);
   use_paths = new ArrayList<>();
   class_files = new HashMap<>();
   
   scanArgs(args);
}





/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-o") && i+1 < args.length) {           // -o <output>
	    output_file = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-p") && i+1 < args.length) {      // -p <package>
	    velocity_context.put("package",args[++i]);
	  }
	 else if (args[i].startsWith("-c") && i+1 < args.length) {      // -c <classname>
	    setTargetClass(args[++i]);
	  }
	 else if (args[i].startsWith("-d") && i+1 < args.length) {      // -d <root dir>
	    File dir = new File(args[++i]);
	    findFiles(dir);
	  }
	 else if (args[i].startsWith("-u") && i+1 < args.length) {      // -u <use path>
	    use_paths.add(args[++i]);
	  }
	 else badArgs();
       }
      else {
         findFiles(new File(args[i]));
       }
    }
}


private void badArgs()
{
   System.err.println("FaitMockCreator [-o <outputfile>] { web.xml file }");
   System.exit(1);
}




private void findFiles(File f)
{
   if (f.isDirectory()) {
      for (File f1 : f.listFiles()) {
	 findFiles(f1);
       }
    }
   else if (f.getName().endsWith(".xml") || f.getName().endsWith(".tld") ||
	f.getName().equals("struts-config.xml")) {
      System.err.println("FaitMockCreator: Use file " + f);
      Element e = IvyXml.loadXmlFromFile(f);
      if (e != null) {
	 input_files.add(e);
       }
    }
   else if (f.getName().endsWith(".class")) {
      try { 
         FileInputStream fis = new FileInputStream(f);
         ClassReader cr = new ClassReader(fis);
         String cnm = cr.getClassName();
         String cnm1 = cnm.replace("/",".");
         cnm1 = cnm1.replace("$",".");
         class_files.put(cnm1,f);
       }
      catch (IOException e) { }
    }
}

/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setTargetClass(String cls)
{
   if (output_file != null) {
      File par = output_file.getParentFile();
      output_file = new File(par,cls + ".java");
    }
   else {
      output_file = new File(cls + ".java");
    }

   velocity_context.put("invokeclass",cls);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   velocity_context.put("generate_date",new Date());

   listener_list = new ArrayList<>();
   context_map = new HashMap<>();
   filter_map = new LinkedHashMap<>();
   servlet_map = new LinkedHashMap<>();
   tag_map = new LinkedHashMap<>();
   plugin_map = new LinkedHashMap<>();
   action_map = new LinkedHashMap<>();
   form_map = new LinkedHashMap<>();
   controller_map = new LinkedHashMap<>();
   
   for (Element e : input_files) {
      if (IvyXml.isElement(e,"web-app")) {
	 processWebXmlFile(e);
       }
      else if (IvyXml.isElement(e,"taglib")) {
	 processTagFile(e);
       }
      else if (IvyXml.isElement(e,"struts-config")) {
	 processStrutsFile(e);
       }
    }

   for (Iterator<Form> it = form_map.values().iterator(); it.hasNext(); ) {
      Form f = it.next();
      if (!f.isUsed()) it.remove();
    }
   
   velocity_context.put("parameters",context_map);
   velocity_context.put("listeners",listener_list);
   velocity_context.put("filters",filter_map.values());
   velocity_context.put("servlets",servlet_map.values());
   velocity_context.put("tags",tag_map.values());
   velocity_context.put("plugins",plugin_map.values());
   velocity_context.put("actions",action_map.values());
   velocity_context.put("forms",form_map.values());
   velocity_context.put("controllers",controller_map.values());

   outputResult();
}



/********************************************************************************/
/*										*/
/*	Web.xml file scanning methods						*/
/*										*/
/********************************************************************************/

private void processWebXmlFile(Element xml)
{
   Map<String,String> pmap = loadParameterMap(xml,"context-param");
   if (pmap != null) context_map.putAll(pmap);

   for (Element chld : IvyXml.children(xml,"listener")) {
      Listener lis = new Listener(chld);
      listener_list.add(lis);
    }

   for (Element chld : IvyXml.children(xml,"filter")) {
      Filter f = new Filter(chld);
      String cls = f.getFilterClass();
      if (!validClass(cls)) continue;
      filter_map.put(f.getName(),f);
    }

   for (Element chld : IvyXml.children(xml,"servlet")) {
      Servlet s = new Servlet(chld);
      String cls = s.getServletClass();
      if (!validClass(cls)) continue;
      servlet_map.put(s.getName(),s);
    }
}



private void processTagFile(Element xml)
{
   for (Element chld : IvyXml.children(xml,"tag")) {
      Tag t = new Tag(chld);
      String cls = t.getTagClass();
      if (!validClass(cls)) continue;
      tag_map.put(t.getName(),t);
    }
}



private void processStrutsFile(Element xml)
{
   for (Element chld : IvyXml.children(xml,"plug-in")) {
      Plugin p = new Plugin(chld);
      String cls = p.getPluginClass();
      if (!validClass(cls)) continue;
      plugin_map.put(p.getName(),p);
    }
   Element beans = IvyXml.getChild(xml,"form-beans");
   for (Element chld : IvyXml.children(beans,"form-bean")) {
      Form f = new Form(chld);
      if (f.getName() == null || f.getFormClass() == null) continue;
      form_map.put(f.getName(),f);
    }
   Map<String,String> formmap = new HashMap<>();
   for (Element acts : IvyXml.children(xml,"action-mappings")) {
      for (Element chld : IvyXml.children(acts,"action")) {
	 Action a = new Action(chld);
	 String cls = a.getActionClass();
	 if (!validClass(cls)) continue;
         String fnm = a.getFormName();
         if (fnm != null) formmap.put(cls,fnm);
	 action_map.put(a.getName(),a);
       }
    }
   for (Action a : action_map.values()) {
      String fnm = formmap.get(a.getActionClass());
      if (fnm == null) fnm = a.getFormName();
      if (fnm != null) {
         Form form = form_map.get(fnm);
         if (form != null) {
            a.setForm(form);
            form.setUsed();
          }
       }
      // if (a.getForm() == null)
         // System.err.println("NO FORM " + a.getActionClass() + " " + 
         // a.getName() + " " + a.getFormName());
    }
   
   for (Element ctl : IvyXml.children(xml,"controller")) {
      Controller c = new Controller(ctl);
      String cls = c.getControllerClass();
      if (!validClass(cls)) continue;
      controller_map.put(c.getName(),c);
    }
}



private boolean validClass(String cls)
{
   if (cls == null) return false;
   if (cls.startsWith("org.apache.")) return false;
   if (use_paths.size() > 0) {
      for (String s : use_paths) {
	 if (cls.startsWith(s)) return true;
       }
      return false;
    }
   return true;
}



private static String fixName(String nm)
{
   if (nm == null) return null;

   nm = nm.replace(".","_");

   return nm;
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

static Map<String,String> loadParameterMap(Element e,String id)
{
   Map<String,String> rslt = new HashMap<>();
   for (Element ip : IvyXml.children(e,id)) {
      String nm = IvyXml.getTextElement(ip,"param-name");
      if (nm == null) nm = IvyXml.getTextElement(ip,"property");
      String vl = IvyXml.getTextElement(ip,"param-value");
      if (vl == null) vl = IvyXml.getTextElement(ip,"value");
      if (nm == null || vl == null) continue;
      vl = vl.replace("\n","\\n");
      rslt.put(nm,vl);
    }

   return rslt;
}


private Map<String,String> addFormFields(String cls,Map<String,String> props) 
{
   File cf = class_files.get(cls);
   if (cf == null) return props;
   
   try {
      FileInputStream fis = new FileInputStream(cf);
      ClassReader cr = new ClassReader(fis);
      FindMethodVisitor fmv = new FindMethodVisitor();
      cr.accept(fmv,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
      fis.close();
      Map<String,String> mthd = fmv.getSetters();
      for (Map.Entry<String,String> ent : mthd.entrySet()) {
         props.putIfAbsent(ent.getKey(),ent.getValue()); 
       }
      String sup = cr.getSuperName();
      if (sup != null) {
         sup = sup.replace("/",".");
         addFormFields(sup,props);
       }
    }
   catch (IOException e) { }
   
   return props;   
}


private List<String> addActions(String cls,List<String> rslt) 
{
   if (cls == null) return rslt;
   File cf = class_files.get(cls);
   if (cf == null) return rslt;
   
   try {
      FileInputStream fis = new FileInputStream(cf);
      ClassReader cr = new ClassReader(fis);
      FindMethodVisitor fmv = new FindMethodVisitor();
      cr.accept(fmv,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
      fis.close();
      for (String s : fmv.getActions()) {
         if (!rslt.contains(s)) rslt.add(s);
       }
      String sup = cr.getSuperName();
      if (sup != null) {
         sup = sup.replace("/",".");
         addActions(sup,rslt);
       }
    }
   catch (IOException e) { }
   
   return rslt;   
}


private Map<String,String> addServiceMethods(String cls,Map<String,String> rslt)
{
   if (cls == null) return rslt;
   File cf = class_files.get(cls);
   if (cf == null) return rslt;
   
   try {
      FileInputStream fis = new FileInputStream(cf);
      ClassReader cr = new ClassReader(fis);
      FindMethodVisitor fmv = new FindMethodVisitor();
      cr.accept(fmv,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
      fis.close();
      rslt.putAll(fmv.getServices());
      String sup = cr.getSuperName();
      if (sup != null && !sup.startsWith("javax/servlet/")) {
         sup = sup.replace("/",".");
         addServiceMethods(sup,rslt);
       }
    }
   catch (IOException e) { }
   
   return rslt;   
}



private static class FindMethodVisitor extends ClassVisitor {
   
   private Map<String,String> set_methods;
   private List<String> action_methods;
   private Map<String,String> service_methods;
   
   FindMethodVisitor() {
      super(JcodeConstants.ASM_API);
      set_methods = new HashMap<>();
      action_methods = new ArrayList<>();
      service_methods = new HashMap<>();
    }
   
   Map<String,String> getSetters()                      { return set_methods; }
   List<String> getActions()                            { return action_methods; }
   Map<String,String> getServices()                     { return service_methods; }
   
   @Override public MethodVisitor visitMethod(int acc,String name,String desc,String sgn,String [] exc) {
      if (Modifier.isPublic(acc)) {
         Type [] args = Type.getArgumentTypes(desc);
         Type ret = Type.getReturnType(desc);
         if (ret == Type.VOID_TYPE && args.length == 1) {
            if (name.startsWith("set")) {
               String key = name.substring(3);
               String val = args[0].getClassName();
               // System.err.println("SET " + key + "->" + val);
               set_methods.put(key,val);
             }
          }
         else if (ret == Type.VOID_TYPE && args.length == 2) {
            if (name.startsWith("do")) {
               String s = name.substring(2).toUpperCase();
               service_methods.put(s,name);
             }
            else if (name.equals("service")) {
               service_methods.put("SERVICE",name);
             }
          }
         else if (ret.getClassName().equals("org.apache.struts.action.ActionForward") && args.length == 4) {
            if (args[0].getClassName().equals("org.apache.struts.action.ActionMapping") &&
                  args[1].getClassName().equals("org.apache.struts.action.ActionForm") &&
                  args[2].getClassName().equals("javax.servlet.http.HttpServletRequest") &&
                  args[3].getClassName().equals("javax.servlet.http.HttpServletResponse")) {
                action_methods.add(name);
             }
          }
               
       }
      return null;
    }
   
}       // end of inner class FindMethodVisitor




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputResult()
{
   try {
      InputStream ins = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE);
      Reader fr = null;
      if (ins != null) fr = new InputStreamReader(ins);
      else fr = new FileReader(TEMPLATE_FILE);
      FileWriter fw = new FileWriter(output_file);

      Velocity.evaluate(velocity_context,fw,"invokeservlets",fr);

      fr.close();
      fw.close();
    }
   catch (IOException e) {
      System.err.println("FaitMockCreator: Problem writing " + output_file + ": " + e);
      System.exit(1);
    }
}



/********************************************************************************/
/*										*/
/*	Information about a listener						*/
/*										*/
/********************************************************************************/

public static class Listener {

   private String listener_class;

   Listener(Element xml) {
      listener_class = IvyXml.getTextElement(xml,"listener-class");
    }

   public String getListenerClass()		{ return listener_class; }

}	// end of inner class Listener




/********************************************************************************/
/*										*/
/*	Information about a filter						*/
/*										*/
/********************************************************************************/

public static class Filter {

   private String filter_name;
   private String filter_class;
   private Map<String,String> filter_values;

   Filter(Element xml) {
      filter_name = fixName(IvyXml.getTextElement(xml,"filter-name"));
      filter_class = IvyXml.getTextElement(xml,"filter-class");
      filter_values = loadParameterMap(xml,"init-param");
    }

   public String getName()			{ return filter_name; }
   public String getFilterClass()		{ return filter_class; }
   public Map<String,String> getInitValues()	{ return filter_values; }

}	// end of inner class Filter


/********************************************************************************/
/*										*/
/*	Information about a servlet						*/
/*										*/
/********************************************************************************/

public class Servlet {

   private String servlet_name;
   private String servlet_class;
   private String jsp_file;
   private Map<String,String> servlet_values;
   private Map<String,String> service_methods;

   Servlet(Element xml) {
      servlet_name = fixName(IvyXml.getTextElement(xml,"servlet-name"));
      servlet_class = IvyXml.getTextElement(xml,"servlet-class");
      servlet_values = loadParameterMap(xml,"init-param");
      jsp_file = IvyXml.getTextElement(xml,"jsp-file");
      service_methods = new HashMap<>();
      if (servlet_class != null) {
         service_methods = addServiceMethods(servlet_class,service_methods);
       }
    }

   public String getName()			{ return servlet_name; }
   public String getServletClass()		{ return servlet_class; }
   public Map<String,String> getInitValues()	{ return servlet_values; }
   public String getJspFile()			{ return jsp_file; }
   public Map<String,String> getServices()      { return service_methods; }

}	// end of inner class Servlet



/********************************************************************************/
/*										*/
/*	Information about a tag 						*/
/*										*/
/********************************************************************************/

public class Tag {

   private String tag_name;
   private String tag_class;
   private String tag_content;
   private Map<String,TagAttribute> tag_attrs;
   private Map<String,String> tag_fields;

   Tag(Element xml) {
      tag_name = fixName(IvyXml.getTextElement(xml,"name"));
      tag_class = IvyXml.getTextElement(xml,"tag-class");
      tag_content = IvyXml.getTextElement(xml,"body-content");
      tag_attrs = new LinkedHashMap<>();
      for (Element aelt : IvyXml.children(xml,"attribute")) {
         TagAttribute ta = new TagAttribute(aelt);
         if (ta.getName() != null) tag_attrs.put(ta.getName(),ta);
       }
      tag_fields = new LinkedHashMap<>();
      addFormFields(tag_class,tag_fields);
    }

   public String getName()			        { return tag_name; }
   public String getTagClass()			        { return tag_class; }
   public String getTagContent()		        { return tag_content; }
   public Map<String,TagAttribute> getAttributes()      { return tag_attrs; }
   public Map<String,String> getFields()                { return tag_fields; }

}	// end of inner class Action




public static class TagAttribute {
   
   private String attr_name;
   private boolean is_required;
   private String attr_type;
   
   TagAttribute(Element xml) {
      attr_name = IvyXml.getTextElement(xml,"name");
      attr_type = IvyXml.getTextElement(xml,"type");
      if (attr_type == null) attr_type = "java.lang.String";
      String rq = IvyXml.getTextElement(xml,"required");
      if (rq == null || rq.startsWith("f") || rq.startsWith("F")) is_required = false;
      else is_required = true;
    }
   
   public String getName()                      { return attr_name; }
   public String getType()                      { return attr_type; }
   public boolean isRequired()                  { return is_required; }
   
}       // end of inner class TagAttribute




/********************************************************************************/
/*										*/
/*	Struts Plugin inforamtion						*/
/*										*/
/********************************************************************************/

public static class Plugin {

   private String plugin_name;
   private String plugin_class;
   private Map<String,String> plugin_props;

   Plugin(Element xml) {
      plugin_name = fixName(IvyXml.getAttrString(xml,"name"));
      plugin_class = IvyXml.getAttrString(xml,"className");
      plugin_props = loadParameterMap(xml,"set-property");
      if (plugin_name == null) {
	 int idx = plugin_class.lastIndexOf(".");
	 plugin_name = plugin_class.substring(idx+1);
       }
    }

   public String getName()			{ return plugin_name; }
   public String getPluginClass()		{ return plugin_class; }
   public Map<String,String> getProperties()	{ return plugin_props; }
   
   public String getMethodName(String p) {
      String s1 = "set" +  p.substring(0,1).toUpperCase() + p.substring(1);
      return s1;
    }

}	// end of inner class Plugin


/********************************************************************************/
/*										*/
/*	Struts Action information						*/
/*										*/
/********************************************************************************/

public class Action {

   private String form_name;
   private String action_name;
   private String action_class;
   private Map<String,String> action_props;
   private boolean action_validate;
   private Form action_form;
   private List<String> action_set;

   Action(Element xml) {
      form_name = fixName(IvyXml.getAttrString(xml,"name"));
      action_class = IvyXml.getAttrString(xml,"type");
      action_props = loadParameterMap(xml,"set-property");
      action_validate = IvyXml.getAttrBool(xml,"validate");
      String p = IvyXml.getAttrString(xml,"path");
      action_name = p.replace("/","_");
      action_set = new ArrayList<>();
      if (action_class != null && action_name == null) {
         int idx = action_class.lastIndexOf(".");
         action_name = action_class.substring(idx+1);
       }
      action_set = new ArrayList<>();
      action_set = addActions(action_class,action_set);
      action_form = null;
    }

   public String getName()			{ return action_name; }
   public String getActionClass()		{ return action_class; }
   public Map<String,String> getProperties()	{ return action_props; }
   public boolean getValidate() 		{ return action_validate; }
   public String getFormName()                  { return form_name; }
   
   public Form getForm()                        { return action_form; }
   public void setForm(Form f)                  { action_form = f; }
   
   public List<String> getActionRoutines()      { return action_set; }

}	// end of inner class Action

 

/********************************************************************************/
/*                                                                              */
/*      Struts Form Inforamtion                                                 */
/*                                                                              */
/********************************************************************************/

public class Form {

   private String form_name;
   private String form_class;
   private Map<String,String> form_fields;
   private Map<String,String> form_props;
   private boolean is_used;
   
   Form(Element xml) {
      form_name = fixName(IvyXml.getAttrString(xml,"name"));
      form_class = IvyXml.getAttrString(xml,"type");
      form_fields = new LinkedHashMap<>();
      form_props = new LinkedHashMap<>();
      for (Element fld : IvyXml.children(xml,"form-property")) {
         String nm = IvyXml.getAttrString(fld,"name");
         String typ = IvyXml.getAttrString(fld,"type");
         form_props.put(nm,typ);
       }
      is_used = false;
      addFormFields(form_class,form_fields);
    }
   
   public String getName()                      { return form_name; }
   public String getFormClass()                 { return form_class; }
   public Map<String,String> getFields()        { return form_fields; }
   public Map<String,String> getProps()         { return form_props; }
   public boolean isUsed()                      { return is_used; }
   
   void setUsed()                               { is_used = true; }
   
}       // end of inner class Form



/********************************************************************************/
/*                                                                              */
/*      Controller information\                                                 */
/*                                                                              */
/********************************************************************************/

public static class Controller {

   private String controller_name;
   private String controller_class;
   
   Controller(Element xml) {
      controller_class = IvyXml.getAttrString(xml,"processorClass");
      // action_name might be duplicated for different classes
      if (controller_class != null) {
	 int idx = controller_class.lastIndexOf(".");
	 controller_name = controller_class.substring(idx+1);
       }
    }
   
   public String getName()                      { return controller_name; }
   public String getControllerClass()           { return controller_class; }
   
}       // end of inner class Controller




}
	// end of class FaitMockCreator




/* end of FaitMockCreator.java */

