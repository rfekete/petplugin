package ai.aitia.petplugin.wizards;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.lang.model.SourceVersion;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import ai.aitia.petplugin.utils.PetFileUtils;
import ai.aitia.petplugin.wizards.dialogfields.ButtonDialogField;

@SuppressWarnings("restriction")
public class NewPetProjectWizardPageTwo extends WizardPage implements IPageChangedListener {

	private final NameGroup fNameGroup;
	private final SimClassGroup fSimClassGroup;
	private final Validator fValidator;
	private ArrayList<String> javaClasses = new ArrayList<>();
		
	private final class NameGroup extends Observable implements IDialogFieldListener {

		protected final StringDialogField fNameField;

		public NameGroup() {
			fNameField= new StringDialogField();
			fNameField.setLabelText("Model name:");
			fNameField.setDialogFieldListener(this);
		}

		public Control createControl(Composite composite) {
			Composite nameComposite= new Composite(composite, SWT.NONE);
			nameComposite.setFont(composite.getFont());
			nameComposite.setLayout(new GridLayout(1, false));

			fNameField.doFillIntoGrid(nameComposite, 2);
			LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
			return nameComposite;
		}

		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}

		public String getName() {
			return fNameField.getText().trim();
		}

		public void postSetFocus() {
			fNameField.postSetFocusOnDialogField(getShell().getDisplay());
		}

		public void setName(String name) {
			fNameField.setText(name);
		}

		public void dialogFieldChanged(DialogField field) {
			fireEvent();
		}
	}
	
	private final class SimClassGroup extends Observable implements IStringButtonAdapter, IDialogFieldListener {

		protected final ButtonDialogField fSimClassField;
		private Class<?> simState = null;
		
		public SimClassGroup() {


			fSimClassField= new ButtonDialogField(this);
			fSimClassField.setButtonLabel("Browse");
			fSimClassField.setText("");//$NON-NLS-1$
			fSimClassField.setDialogFieldListener(this);
		}

		
		public Control createControl(Composite composite) {
			final int numColumns = 2;

			final Composite locationComposite= new Composite(composite, SWT.NONE);
			locationComposite.setLayout(new GridLayout(numColumns, false));
			
			Label simClassLabel = new Label(locationComposite, SWT.NONE);
			simClassLabel.setText("Simulation class:");
			new Label(locationComposite, SWT.NONE);

			fSimClassField.doFillIntoGrid(locationComposite, 2);

			LayoutUtil.setHorizontalGrabbing(fSimClassField.getTextControl(composite));
			BidiUtils.applyBidiProcessing(fSimClassField.getTextControl(null), StructuredTextTypeHandlerFactory.FILE);

			return locationComposite;
		}

		public void changeControlPressed(DialogField field) {
			fSimClassField.setText(chooseSimClass());
			fireEvent();
		}
		
		private String getName()
		{
			return fSimClassField.getText();
		}
		
		private boolean isValidClass()
		{
			return javaClasses.contains(getName());
		}
				
		private String chooseSimClass() {
			ILabelProvider labelProvider = new LabelProvider();
		    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
		        getShell(), labelProvider);
		    dialog.setTitle("Project Selection");
		    dialog.setMessage("Choose a project to compile");
		    dialog.setElements(getJavaClasses().toArray());
		    dialog.setHelpAvailable(false);
		    if (dialog.open() == Window.OK) {
		      return ((String)dialog.getFirstResult());
		    }
		    return "";
	}
		
		private void loadClassesFromProject()
		{
			IJavaProject javaProject = null;
			HashSet<String> entriesToCheck = new HashSet<>();
		    IProject project = ((NewPetProjectWizardPageOne)getPreviousPage()).getMasonProject().getProject();
		    if(project==null)return;
			ClassLoader cl = constructClassLoader();

		    if (project.isOpen() && JavaProject.hasJavaNature(project)) {
		      javaProject = JavaCore.create(project); 
		      IClasspathEntry[] classpathEntries = null; 
		      try {
				classpathEntries = javaProject.getRawClasspath();
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		      for (IClasspathEntry entry : classpathEntries)
		      {
		    	  if(entry.getEntryKind() == IClasspathEntry.CPE_SOURCE)
					{
						if(entry.getOutputLocation()!=null)
						{
							entriesToCheck.add(project.getWorkspace().getRoot().getLocation()+""+entry.getOutputLocation());
						}
					}
		      }
		      try {
				entriesToCheck.add(project.getWorkspace().getRoot().getLocation()+""+javaProject.getOutputLocation());
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		      checkEntries(entriesToCheck);
		    }
		}
		
		private void checkEntries(Set<String> entries)
		{
			try (final URLClassLoader cl = constructClassLoader()){
			
			final Class<?> simClass = cl.loadClass("sim.engine.SimState");
			
			for(String entry : entries)
			{
				final java.nio.file.Path src = Paths.get(entry);
				if(!Files.isDirectory(src))
				{
					if(src.endsWith(".class"))
					{
						String relativePath = Paths.get(entry).relativize(src).toString();
						try {
							Class<?> c = cl.loadClass(relativePath.substring(0, relativePath.length()-6));
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				else
				{
					try {
						Files.walkFileTree(src, new SimpleFileVisitor<java.nio.file.Path>(){
							@Override
							public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
								if(file.toString().endsWith(".class"))
								{
									String filePath = src.relativize(file).toString().replace("\\", ".");
									String className = filePath.substring(0, filePath.length()-6);
									Class<?> currentClass;
									try {
										currentClass = cl.loadClass(className);
										if(simClass.isAssignableFrom(currentClass))javaClasses.add(currentClass.getName());
									} catch (ClassNotFoundException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								return FileVisitResult.CONTINUE;
							}
							
							
						});
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			
		}
		
		private void loadClassesFromJars()
		{
			String[] jarPaths = ((NewPetProjectWizardPageOne)getPreviousPage()).getMasonJarNames().split(",");
			if(jarPaths[0].equals(""))return;
			try (URLClassLoader loader = PetFileUtils.getJarClassLoader(((NewPetProjectWizardPageOne)getPreviousPage()).getMasonJarNames().split(","))) 
			{
				simState = loader.loadClass("sim.engine.SimState");
				for(String jarPath: jarPaths)
				{
					ZipFile modelJar = new ZipFile(jarPath.trim());
					Enumeration<? extends ZipEntry> entries = modelJar.entries();
					while(entries.hasMoreElements())
					{
						ZipEntry entry = entries.nextElement();
						if(entry.getName().endsWith(".class"))
						{
							Class<?> currentClass = loader.loadClass(entry.getName().replace("/", ".").substring(0, entry.getName().length()-6));
							if(simState!= null && simState.isAssignableFrom(currentClass))
							{
								javaClasses.add(currentClass.getName());
							}
						}

					}
					modelJar.close();
				}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

		}
		
		private URLClassLoader constructClassLoader()
		{
		    URLClassLoader classLoader = null;
		    IJavaProject javaProject = null ;
		    IProject project = ((NewPetProjectWizardPageOne)getPreviousPage()).getMasonProject().getProject();
		    if (project.isOpen() && JavaProject.hasJavaNature(project))
			      javaProject = JavaCore.create(project); 
		    
		    try {
				String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
				List<URL> urlList = new ArrayList<URL>();
				for (int i = 0; i < classPathEntries.length; i++) {
				 String entry = classPathEntries[i];
				 IPath path = new Path(entry);
				 URL url = path.toFile().toURI().toURL();
				 urlList.add(url);
				}
				ClassLoader parentClassLoader = project.getClass().getClassLoader();
				URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
				classLoader = new URLClassLoader(urls, parentClassLoader);
				simState = classLoader.loadClass("sim.engine.SimState");
				}catch ( MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				}catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    return classLoader;
		}
		
		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}
		
		public void dialogFieldChanged(DialogField field) {
			fireEvent();
		}
	}
	
	private final class Validator implements Observer {

		public void update(Observable o, Object arg) {

		
			final String modelName= fNameGroup.getName();
			final String simClassName = fSimClassGroup.getName();
			setPageComplete(true);
			((WizardPage)getPreviousPage()).setPageComplete(true);

			if (modelName.length() == 0) {
				setErrorMessage(null);
				setMessage("Enter a project name.");
				setPageComplete(false);
				return;
			}
			
			if(!SourceVersion.isIdentifier(modelName))
			{
				setErrorMessage(modelName+" is not a valid identifier.");
				setPageComplete(false);
				return;
			}
			
			if(simClassName.length() == 0)
			{
				setErrorMessage(null);
				setMessage("Enter the fully quailified simulation class name.");
				setPageComplete(false);
				return;
			}
			
			if(!fSimClassGroup.isValidClass())
			{
				setErrorMessage(simClassName+" is not a mason simulation class.");
				setPageComplete(false);
				return;
			}
			
			setErrorMessage(null);
			setMessage(null);
		}
		
	}

	public NewPetProjectWizardPageTwo()
	{
		super("masonProperties");
		fNameGroup= new NameGroup();
		fSimClassGroup = new SimClassGroup();
		fValidator= new Validator();
		
		setTitle("Create a Pet Project");
		setDescription("Configure model properties.");
		setPageComplete(true);
		
		fNameGroup.addObserver(fValidator);
		fSimClassGroup.addObserver(fValidator);
	}
	
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite= new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		composite.setLayout(initGridLayout(new GridLayout(1, false), true));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		final IWizardContainer container = this.getContainer();
		  if (container instanceof IPageChangeProvider) {
		    ((IPageChangeProvider)container).addPageChangedListener(this);
		  }
		
		Control nameControl= createNameControl(composite);
		nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control simClassControl= createSimClassControl(composite);
		simClassControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		setControl(composite);
	}
	
	protected Control createNameControl(Composite composite) {
		return fNameGroup.createControl(composite);
	}
	
	protected Control createSimClassControl(Composite composite) {
		return fSimClassGroup.createControl(composite);
	}
	
	private GridLayout initGridLayout(GridLayout layout, boolean margins) {
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		//layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.verticalSpacing = 0;
		
		if (margins) {
			layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		} else {
			layout.marginWidth= 0;
			layout.marginHeight= 0;
		}
		return layout;
	}


	public ArrayList<String> getJavaClasses() {
		return javaClasses;
	}


	public void setJavaFileNames(ArrayList<String> javaFileNames) {
		this.javaClasses = javaFileNames;
	}
	
	public String getModelName()
	{
		if(fNameGroup.getName().equals(""))return "Example";
		else return fNameGroup.getName();
	}

	public String getSimClassName()
	{
		if(fSimClassGroup.getName().equals(""))return "ai.aitia.mason.example.DummyModel";
		else return fSimClassGroup.getName();
	}
	
	@Override
	public void pageChanged(PageChangedEvent event) {
		if(event.getSelectedPage().toString().equals("masonProperties"))
		{
			Job gatheringSimClasses = new Job("gathering simulation classes...") {
				public IStatus run(IProgressMonitor monitor) {
					//PetZipUtils.jarMasonProject(((NewPetProjectWizardPageOne)getPreviousPage()).getMasonProject(), new File("majom.jar"));
					javaClasses.clear();
					fSimClassGroup.loadClassesFromProject();
					fSimClassGroup.loadClassesFromJars();
					return Status.OK_STATUS;
				}
			};
			gatheringSimClasses.schedule();
		}
	}
}
