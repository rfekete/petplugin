package ai.aitia.petplugin.utils;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;

import ai.aitia.petplugin.projects.PetPluginSupport;
import ai.aitia.petplugin.wizards.NewPetProjectWizardPageOne;
import ai.aitia.petplugin.wizards.NewPetProjectWizardPageTwo;

import com.google.gwt.eclipse.core.runtime.GWTRuntime;

public final class PetFileGenerator {

	private static final String epcName = "GUI";

	private PetFileGenerator()
	{}

	public static String generateFile(String templateName, Map<String, String> context)
	{
		CharArrayWriter w = new CharArrayWriter();
		try {
			VelocityEngine ve = new VelocityEngine();
			VelocityContext vc = new VelocityContext(context);
			//ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateDir.replace("\\", "/"));
			Properties props = new Properties();
			props.put("resource.loader","url");
			props.put("url.resource.loader.class", "org.apache.velocity.runtime.resource.loader.URLResourceLoader");
			props.put("url.resource.loader.root",PetPluginSupport.getTemplateRootUrl().toString());
			ve.init(props);
			ve.mergeTemplate(templateName, vc,w);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return w.toString();

	}
	public static void createGwtModule(NewPetProjectWizardPageOne _pageOne) {

		IJavaProject javaProject = PetPluginSupport.getJavaProject();
		IProject project = javaProject.getProject();
		String modelName = ((NewPetProjectWizardPageTwo)_pageOne.getNextPage()).getModelName();
		IFile module = project.getFile(new Path("src/"+_pageOne.getPackage().replace(".", "/").concat("/"+modelName.concat("GUI.gwt.xml"))));
		if(module.exists())return;
		HashMap<String, String> context = new HashMap<>();
		context.put("version", GWTRuntime.findSdkFor(javaProject).getVersion());
		context.put("rename-to", modelName.concat("GUI"));
		context.put("entrypoint",_pageOne.getPackage()+".client.GUI" );
		String xmlContents = generateFile("moduleXML.vm", context);
		try {
			module.create(new ByteArrayInputStream(xmlContents.getBytes()), true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public static void createGwtHtmlPage(NewPetProjectWizardPageOne _pageOne)
	{
		IJavaProject javaProject = PetPluginSupport.getJavaProject();
		IProject project = javaProject.getProject();
		String modelName = ((NewPetProjectWizardPageTwo)_pageOne.getNextPage()).getModelName();

		IFile htmlFile = project.getFile(new Path("war/index.html"));
		if(htmlFile.exists())return;
		HashMap<String, String> context = new HashMap<>();
		context.put("gui", modelName.concat("GUI"));
		String htmlContents = PetFileGenerator.generateFile("index.html.vm", context);
		try {
			htmlFile.create(new ByteArrayInputStream(htmlContents.getBytes()), true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void createFamilyXml(NewPetProjectWizardPageOne _pageOne)
	{
		IJavaProject javaProject = PetPluginSupport.getJavaProject();
		IProject project = javaProject.getProject();

		String simClassString = ((NewPetProjectWizardPageTwo)_pageOne.getNextPage()).getSimClassName();

		IFile family = project.getFile(new Path("model-config/family.xml"));
		if(family.exists())return;
		HashMap<String, String> context = new HashMap<>();
		context.put("ui", "index.html");
		context.put("admin_ui", "index.html");
		context.put("creationFunction", "start");
		context.put("simclassname", simClassString);
		String familyXml = PetFileGenerator.generateFile("family.xml.vm", context);
		try {
			family.create(new ByteArrayInputStream(familyXml.getBytes()), true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public static void createEntryPoint(NewPetProjectWizardPageOne _pageOne)
	{
		IJavaProject javaProject = PetPluginSupport.getJavaProject();
		IProject project = javaProject.getProject();
		IFile family = project.getFile(new Path("src/"+_pageOne.getPackage().replace(".", "/").concat("/client/GUI.java")));
		if(family.exists())return;
		HashMap<String, String> context = new HashMap<>();
		context.put("packagename", _pageOne.getPackage());
		context.put("classname", epcName);
		String entryPointClass = PetFileGenerator.generateFile("GUI.java.vm", context);
		try {
			family.create(new ByteArrayInputStream(entryPointClass.getBytes()), true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
