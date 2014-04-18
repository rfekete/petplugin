package ai.aitia.petplugin.projects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;

import ai.aitia.petplugin.natures.PetNature;
import ai.aitia.petplugin.utils.PetFileGenerator;
import ai.aitia.petplugin.utils.PetFileUtils;
import ai.aitia.petplugin.wizards.NewPetProjectWizardPageOne;
import ai.aitia.petplugin.wizards.NewPetProjectWizardPageTwo;

import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gwt.eclipse.core.compile.GWTCompileRunner;
import com.google.gwt.eclipse.core.compile.GWTCompileSettings;
import com.google.gwt.eclipse.core.launch.GWTLaunchAttributes;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

public class PetPluginSupport{

	private static IProject project;
	private static IJavaProject javaProject;
	private static MessageConsole petConsole;
	private static NewPetProjectWizardPageOne pageOne;
	private static NewPetProjectWizardPageTwo pageTwo;

	public static final int DEFAULT_MASON_MODEL = 1;  
	public static final int PROJECT_MASON_MODEL = 2;  
	public static final int JAR_MASON_MODEL = 3;

	public static final String PREF_MASON_MODEL_NAME = "mason.model.name";
	public static final String PREF_MASON_PROJECT_NAME = "mason.project.name";

	private static int masonType = 0;

	static
	{
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		petConsole = new MessageConsole("petConsole", null);
		consoleManager.addConsoles(new IConsole[]{petConsole});
	}

	public static IJavaProject createProject(NewPetProjectWizardPageOne _pageOne) {


		pageOne = _pageOne;
		pageTwo = (NewPetProjectWizardPageTwo)pageOne.getNextPage();
		project = createBaseProject();

		try {
			addNatures(project);
			project.getFolder("src").create(true, true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		javaProject = JavaCore.create(project);
		try {
			javaProject.getPackageFragmentRoot(project.getFolder("src")).createPackageFragment(pageOne.getPackage()+".client", false, null);
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		masonType = pageOne.getMasonType();


		savePreferences(project);

		if(masonType == PROJECT_MASON_MODEL) project.getWorkspace().addResourceChangeListener(new PetResourceChangeListener(getPreference(project ,PREF_MASON_PROJECT_NAME), project));

		copyResources();
		setGwtProperties();
		setClassPath();
		generateFiles();
		createMasonJars();
		Job gwtCompile = new Job("compiling gwt resources...") {
			public IStatus run(IProgressMonitor monitor) {
				compileProject();
				createPetiFile(project);
				createModelDir(project);
				try {
					project.refreshLocal(IProject.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		gwtCompile.schedule();



		try {
			project.refreshLocal(IProject.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return javaProject;
	}

	private PetPluginSupport(){}

	private static IProject createBaseProject() {

		IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(pageOne.getProjectName());

		if (!newProject.exists()) {
			URI projectLocation = pageOne.getProjectLocationURI();
			IProjectDescription desc = newProject.getWorkspace().newProjectDescription(newProject.getName());
			if (projectLocation != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(projectLocation)) {
				projectLocation = null;
			}

			desc.setLocationURI(projectLocation);
			try {
				newProject.create(desc, null);
				if (!newProject.isOpen()) {
					newProject.open(null);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return newProject;
	}
	private static void setClassPath()
	{
		Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
		try {
			entries.add(JavaRuntime.getDefaultJREContainerEntry());
			entries.add(JavaCore.newContainerEntry(new Path("com.google.gwt.eclipse.core.GWT_CONTAINER")));
			entries.add(JavaCore.newSourceEntry(javaProject.getPath().append("/src")));
			entries.add(JavaCore.newLibraryEntry(javaProject.getPath().append("/lib/gwt-visualization.jar"),null,null));
			entries.add(JavaCore.newLibraryEntry(javaProject.getPath().append("/lib/pet2-1.0.jar"),null,null));
			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]),project.getFullPath().append("war/WEB-INF/classes"), null);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void addNatures(IProject project) throws CoreException {
		if (!project.hasNature(PetNature.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 3];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = PetNature.NATURE_ID;
			newNatures[prevNatures.length+1] = JavaCore.NATURE_ID;
			newNatures[prevNatures.length+2] = GWTNature.NATURE_ID;
			description.setNatureIds(newNatures);

			IProgressMonitor monitor = null;
			project.setDescription(description, monitor);
		}
	}

	private static void copyResources()
	{
		copyDirIntoProject("/resources/copy/common");
		if(pageOne.isDefaultMasonProject())
			copyDirIntoProject("/resources/copy/example");
	}

	public static URL getTemplateRootUrl() throws IOException
	{
		Bundle b = Platform.getBundle("ai.aitia.petplugin");
		URL templateUrl = b.getEntry("/ai/aitia/petplugin/templates");
		if(templateUrl!= null) 
			return templateUrl;
		else
			return b.getEntry("/src/ai/aitia/petplugin/templates");
	}

	private static void copyDirIntoProject(String dirPath)
	{
		Bundle b = Platform.getBundle("ai.aitia.petplugin");
		Enumeration<URL> valami = b.findEntries(dirPath, "*", true);
		while(valami.hasMoreElements())
		{
			URL url = valami.nextElement();
			String relativePath = url.getPath().substring(dirPath.length());
			if(url.getPath().endsWith("/"))
			{
				IFolder ifo = project.getFolder(relativePath);
				recursiveCreateIFolder(ifo);

			}
			else
			{
				try {
					IFile ifi = project.getFile(relativePath);
					if(!ifi.getParent().exists() && ifi.getParent() instanceof IFolder)
						recursiveCreateIFolder((IFolder)ifi.getParent());
					ifi.create(url.openStream(), true, null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private static void recursiveCreateIFolder(IFolder folder)
	{
		IResource parent = folder.getParent();
		if(!parent.exists() && parent instanceof IFolder)
			recursiveCreateIFolder((IFolder)parent);
		if(!folder.exists())
			try {
				folder.create(false, true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public static IJavaProject getJavaProject()
	{
		return javaProject;
	}

	public static void setGwtProperties()
	{
		/*GWTRuntime runtime = GWTRuntime.getFactory().newInstance("Gwt Sdk", new Path("f:/Dokumentumok/gwt-2.5.1"));
		GWTRuntime rt = GWTPreferences.getDefaultRuntime();
		String version = runtime.getVersion();*/

		SdkSet<GWTRuntime> prevSdks = GWTPreferences.getSdks();


		for(GWTRuntime sdk : prevSdks)
		{
			if(sdk.getVersion().equals("2.5.1"))
			{
				prevSdks.setDefault(sdk);
				GWTPreferences.setSdks(prevSdks);
				return;
			}
		}
		/*prevSdks.add(runtime);
		prevSdks.setDefault(runtime);
		GWTPreferences.setSdks(prevSdks);*/
	}

	public static MessageConsole getPetConsole()
	{
		return petConsole;
	}

	private static void generateFiles()
	{
		PetFileGenerator.createGwtModule(pageOne);
		PetFileGenerator.createEntryPoint(pageOne);
		PetFileGenerator.createFamilyXml(pageOne);
		PetFileGenerator.createGwtHtmlPage(pageOne);
	}

	private static void compileProject()
	{
		IPath warLocation = project.getFolder("war").getLocation();
		IFile moduleFile = project.getFile(new Path("src/"+pageOne.getPackage().replace(".", "/")
				.concat("/"+pageTwo.getModelName().concat("GUI.gwt.xml"))));
		ModuleFile mf = ModuleUtils.create(moduleFile);
		String moduleName = mf.getQualifiedName();
		GWTCompileSettings compileSettings = new GWTCompileSettings();
		compileSettings.setVmArgs("-Xmx512m");
		compileSettings.setLogLevel(GWTLaunchAttributes.LOG_LEVELS[0]);
		compileSettings.setOutputStyle(GWTLaunchAttributes.OUTPUT_STYLES[0]);
		compileSettings.setExtraArgs("");
		compileSettings.setEntryPointModules(Arrays.asList(new String[]{moduleName}));


		try {
			GWTCompileRunner.compile(javaProject,warLocation, compileSettings,petConsole.newOutputStream(), null);
		} catch (OperationCanceledException | IOException
				| InterruptedException | CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static void createMasonJars()
	{
		if(masonType == JAR_MASON_MODEL)
		{
			copyMasonJars();
			return;
		}
		IJavaProject masonProject = pageOne.getMasonProject();
		if(masonProject!= null)
		{

		}
	}

	private static void copyMasonJars()
	{
		String[] jarPaths = pageOne.getMasonJarNames().split(",");
		for(String jarPath : jarPaths)
		{
			File jarFile = new File(jarPath.trim());
			//TODO talalj ki valami normalis nevet!
			IFile NEMTOM = javaProject.getProject().getFile("war/WEB-INF/lib/"+jarFile.getName());
			if(!NEMTOM.exists())
			{
				FileInputStream srcInput;
				try {
					srcInput = new FileInputStream(jarFile);
					NEMTOM.create(srcInput, true, null);
					srcInput.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private static IJavaProject getJavaProject(String projectName)
	{
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IJavaProject javaProject = null;
		if(project.exists() && project.isOpen())
		{
			javaProject = JavaCore.create(project);
		}
		return javaProject;
	}

	private static void createPetiFile(IProject project)
	{
		HashMap<String, String> filesToJar = new HashMap<>();

		final String petFileName = getPreference(project ,PREF_MASON_MODEL_NAME)+".petii";
		final String dest = project.getFile(petFileName).getLocation()+"";
		final String tempDest = project.getFile("temp/"+getPreference(project ,PREF_MASON_MODEL_NAME)+".jar").getLocation()+"";

		try
		{
			filesToJar.put(project.getFile("model-config/simulation.xml").getLocation()+"" , "simulation.xml");
			filesToJar.put(project.getFile("model-config/family.xml").getLocation()+"" , "family.xml");
			filesToJar.put(project.getFile("war/WEB-INF/lib/mason-all-16.0.0-RELEASE.jar").getLocation()+"", "mason-all-16.0.0-RELEASE.jar");
			String warLocation =  getWarDir().getLocation().toString().replace('/', '\\');
			if(masonType == JAR_MASON_MODEL)
			{
				for(String modelFileName : pageOne.getMasonJarNames().split(","))
				{
					for(File f : new File(warLocation+"\\WEB-INF\\lib").listFiles())
					{
						File modelFile = new File(modelFileName.trim());
						if(modelFile.getName().equals(f.getName()))filesToJar.put(f.getPath(), f.getName());

					}
				}
			}

			if(masonType == PROJECT_MASON_MODEL)
			{
				PetFileUtils.makeJar(PetFileUtils.jarMasonProject(pageOne.getMasonProject(),tempDest), dest);
				String modelName = getPreference(project , PREF_MASON_MODEL_NAME);
				filesToJar.put(project.getFile("temp/"+modelName+".jar").getLocation()+"",modelName+".jar");
			}


			for(File f : new File(warLocation).listFiles())
			{
				if(!f.getPath().startsWith(warLocation+"\\WEB-INF") && !f.getPath().startsWith(warLocation+"\\META-INF"))
					filesToJar.put(f.getPath(), "WebContent/"+f.getName());
			}
			PetFileUtils.makeJar(filesToJar, dest);
			if(masonType == PROJECT_MASON_MODEL)project.getFolder("temp").delete(true, null);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static IFolder getWarDir()
	{
		return project.getFolder("war");
	}


	public static void savePreferences(IProject project)
	{
		ScopedPreferenceStore petPrefStore = new ScopedPreferenceStore(new ProjectScope(project), "ai.aitia.petplugin");
		if(pageOne.getMasonProject()!=null) petPrefStore.putValue("mason.project.name", pageOne.getMasonProject().getProject().getName());
		if(pageTwo.getModelName()!=null) petPrefStore.putValue("mason.model.name", pageTwo.getModelName());
		try {
			petPrefStore.save();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getPreference(IProject project ,String name)
	{
		ScopedPreferenceStore petPrefStore = new ScopedPreferenceStore(new ProjectScope(project), "ai.aitia.petplugin");
		return petPrefStore.getString(name);
	}

	private static void createModelDir(IProject project)
	{
		LinkedList<File> modelFiles = new LinkedList<>();
		String modelName = getPreference(project ,PREF_MASON_MODEL_NAME);
		File petFile = project.getFile(modelName+".petii").getLocation().toFile();
		File modelConfigDir = project.getFolder("model-config").getLocation().toFile();

		try {
			project.getFolder("models/").create(true, true, null);
			project.getFolder("models/"+modelName).create(true, true, null);
			project.getFolder("models/"+modelName+"/models/").create(true, true, null);

			for(File f : modelConfigDir.listFiles())
			{
				if(f.getName().matches("^simulation\\d*.xml$"))
					modelFiles.add(f);
			}
			for(int i=0;i<modelFiles.size();i++)
			{
				IFile familyXml = project.getFile("models/"+modelName+"/models/"+(i+1)+".xml");
				FileInputStream srcInput = new FileInputStream(modelFiles.get(i));
				familyXml.create(srcInput, true, null);
				srcInput.close();
			}
			project.getFile("models/"+modelName+"/"+modelName+".petii").create(new FileInputStream(petFile), true, null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}