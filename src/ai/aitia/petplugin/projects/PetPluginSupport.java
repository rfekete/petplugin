package ai.aitia.petplugin.projects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import ai.aitia.petplugin.Activator;
import ai.aitia.petplugin.natures.ProjectNature;
import ai.aitia.petplugin.utils.PetFileGenerator;
import ai.aitia.petplugin.utils.PetZipUtils;
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

public class PetPluginSupport {

	private static String bundleRoot;
	private static IProject project;
	private static IJavaProject javaProject;
	private static MessageConsole petConsole;
	private static NewPetProjectWizardPageOne pageOne;

	static
	{
		URL url = Activator.getDefault().getBundle().getEntry("");
		try {
			bundleRoot = FileLocator.resolve(url).getPath();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		petConsole = new MessageConsole("petConsole", null);
		consoleManager.addConsoles(new IConsole[]{petConsole});
	}

	public static IJavaProject createProject(NewPetProjectWizardPageOne _pageOne) {


		pageOne = _pageOne;
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
		setGwtProperties();
		setClassPath();
		copyResources();
		generateFiles();
		copyMasonJars();
		Job gwtCompile = new Job("compiling gwt resources...") {
			public IStatus run(IProgressMonitor monitor) {
				compileProject();
				createPetiFile();
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


	/**
	 * Just do the basics: create a basic project.
	 *
	 * @param location
	 * @param projectName
	 */
	private static IProject createBaseProject() {
		// it is acceptable to use the ResourcesPlugin class
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
		if (!project.hasNature(ProjectNature.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 3];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = ProjectNature.NATURE_ID;
			newNatures[prevNatures.length+1] = JavaCore.NATURE_ID;
			newNatures[prevNatures.length+2] = GWTNature.NATURE_ID;
			description.setNatureIds(newNatures);

			IProgressMonitor monitor = null;
			project.setDescription(description, monitor);
		}
	}

	private static void copyResources()
	{
		File root = new File(bundleRoot+"resources/copy/common");
		copyFile(root);
		if(pageOne.isDefaultMasonProject())
		{
			root = new File(bundleRoot+"resources/copy/example");
			copyFile(root);
		}
	}

	private static void copyFile(File src)
	{
		copyFile(src,src);
	}

	private static void copyFile(File src, File root)
	{
		try
		{
			String name = root.toURI().relativize(src.toURI()).getPath();
			if (src.isDirectory())
			{
				if(!name.equals(""))
				{
					IFolder folder = project.getFolder(name);
					if(!folder.exists())folder.create(true, true, null);
				}
				for (File nestedFile: src.listFiles())
					copyFile(nestedFile, root);
				return;
			}
			IFile file = project.getFile(name);
			if(!file.exists())
			{
				FileInputStream srcInput = new FileInputStream(src);
				file.create(srcInput, true, null);
				srcInput.close();
			}

		}
		catch(CoreException | IOException e)
		{
			e.printStackTrace();
		}

	}
	public static File generatePetProperties(File defaultProperties)
	{
		Properties petProperties = new Properties();
		try {
			petProperties.load(new FileInputStream(defaultProperties));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new File("");
	}

	public static String getBundleRoot()
	{
		return bundleRoot;
	}

	public static IJavaProject getJavaProject()
	{
		return javaProject;
	}

	public static void setGwtProperties()
	{
		GWTRuntime runtime = GWTRuntime.getFactory().newInstance("Gwt Sdk", new Path("f:/Dokumentumok/gwt-2.5.1"));
		String version = runtime.getVersion();

		SdkSet<GWTRuntime> prevSdks = GWTPreferences.getSdks();


		for(GWTRuntime sdk : prevSdks)
		{
			if(sdk.getVersion().equals(version))
			{
				prevSdks.setDefault(runtime);
				GWTPreferences.setSdks(prevSdks);
				return;
			}
		}
		prevSdks.add(runtime);
		prevSdks.setDefault(runtime);
		GWTPreferences.setSdks(prevSdks);
	}

	public MessageConsole getPetConsole()
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
				.concat("/"+((NewPetProjectWizardPageTwo)pageOne.getNextPage()).getModelName().concat("GUI.gwt.xml"))));
		ModuleFile mf = ModuleUtils.create(moduleFile);
		String qualifiedModuleName = mf.getQualifiedName();
		GWTCompileSettings compileSettings = new GWTCompileSettings();
		compileSettings.setVmArgs("-Xmx512m");
		compileSettings.setLogLevel(GWTLaunchAttributes.LOG_LEVELS[0]);
		compileSettings.setOutputStyle(GWTLaunchAttributes.OUTPUT_STYLES[0]);
		compileSettings.setExtraArgs("");
		compileSettings.setEntryPointModules(Arrays.asList(new String[]{qualifiedModuleName}));

		try {
			GWTCompileRunner.compile(javaProject,warLocation, compileSettings,petConsole.newOutputStream(), null);
		} catch (OperationCanceledException | IOException
				| InterruptedException | CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	private static void createPetiFile()
	{
		HashMap<File, String> files = new HashMap<>();
		files.put(project.getFile("model-config/simulation.xml").getLocation().toFile(), "");
		files.put(project.getFile("model-config/family.xml").getLocation().toFile(), "");
		files.put(project.getFile("war/WEB-INF/lib/mason-all-16.0.0-RELEASE.jar").getLocation().toFile(), "");

		String warLocation =  project.getFolder("war").getLocation().toString().replace('/', '\\');
		for(File f : new File(warLocation).listFiles())
		{
			if(!f.getPath().startsWith(warLocation+"\\WEB-INF") && !f.getPath().startsWith(warLocation+"\\META-INF"))
				files.put(f, "WebContent");
		}

		for(String modelFileName : pageOne.getMasonJarNames().split(","))
		{
			for(File f : new File(warLocation+"\\WEB-INF\\lib").listFiles())
			{
				File modelFile = new File(modelFileName.trim());
				if(modelFile.getName().equals(f.getName()))files.put(f, "");
			}
		}
		String petFileName = ((NewPetProjectWizardPageTwo)pageOne.getNextPage()).getModelName()+"petii";
		PetZipUtils.writeFilesToZip(files, project.getFile(petFileName).getLocation()+"");
	}


}