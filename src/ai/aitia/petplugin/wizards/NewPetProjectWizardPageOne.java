package ai.aitia.petplugin.wizards;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.lang.model.SourceVersion;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import ai.aitia.petplugin.utils.PetZipUtils;


@SuppressWarnings("restriction")
public class NewPetProjectWizardPageOne extends WizardPage{
	
	private final NameGroup fNameGroup;
	private final LocationGroup fLocationGroup;
	private final PackageGroup fPackageGroup;
	private final MasonProjectGroup fMasonProjectGroup;
	private final Validator fValidator;
	private boolean isNameAndPackageValid;
	
	
	private final class NameGroup extends Observable implements IDialogFieldListener {

		protected final StringDialogField fNameField;

		public NameGroup() {
			fNameField= new StringDialogField();
			fNameField.setLabelText("Project name:");
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

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			fireEvent();
		}
	}
	
	private final class PackageGroup extends Observable implements IDialogFieldListener {

		protected final StringDialogField fNameField;

		public PackageGroup() {
			// text field for project name
			fNameField= new StringDialogField();
			fNameField.setLabelText("Package: (e.g. ai.aitia.pet.example)");
			fNameField.setDialogFieldListener(this);
		}

		public Control createControl(Composite composite) {
			Composite nameComposite= new Composite(composite, SWT.NONE);
			nameComposite.setFont(composite.getFont());
			nameComposite.setLayout(new GridLayout(1, false));

			//nameComposite.setText("Project name:");
			fNameField.doFillIntoGrid(nameComposite, 2);
			LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
			nameComposite.pack();

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

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			fireEvent();
		}
	}
	
	private final class LocationGroup extends Observable implements Observer, IStringButtonAdapter, IDialogFieldListener {

		protected final SelectionButtonDialogField fUseDefaults;
		protected final StringButtonDialogField fLocation;

		private String fPreviousExternalLocation;

		private static final String DIALOGSTORE_LAST_EXTERNAL_LOC= JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$

		public LocationGroup() {
			fUseDefaults= new SelectionButtonDialogField(SWT.CHECK);
			fUseDefaults.setDialogFieldListener(this);
			fUseDefaults.setLabelText("Use default");

			fLocation= new StringButtonDialogField(this);
			fLocation.setDialogFieldListener(this);
			fLocation.setLabelText("Directory: ");
			fLocation.setButtonLabel("Browse");

			fUseDefaults.setSelection(true);

			fPreviousExternalLocation= ""; //$NON-NLS-1$
		}

		public Control createControl(Composite composite) {
			final int numColumns = 3;

			final Group locationComposite= new Group(composite, SWT.NONE);
			locationComposite.setLayout(new GridLayout(numColumns, false));
			
			locationComposite.setText("Location");
			
			fUseDefaults.doFillIntoGrid(locationComposite, numColumns);
			fLocation.doFillIntoGrid(locationComposite, numColumns);
			LayoutUtil.setHorizontalGrabbing(fLocation.getTextControl(null));
			BidiUtils.applyBidiProcessing(fLocation.getTextControl(null), StructuredTextTypeHandlerFactory.FILE);

			return locationComposite;
		}

		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}

		protected String getDefaultPath(String name) {
			final IPath path= Platform.getLocation().append(name);
			return path.toOSString();
		}

		/* (non-Javadoc)
		 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
		 */
		public void update(Observable o, Object arg) {
			if (isUseDefaultSelected()) {
				fLocation.setText(getDefaultPath(fNameGroup.getName()));
			}
			fireEvent();
		}

		public IPath getLocation() {
			if (isUseDefaultSelected()) {
				return Platform.getLocation();
			}
			return Path.fromOSString(fLocation.getText().trim());
		}

		public boolean isUseDefaultSelected() {
			return fUseDefaults.isSelected();
		}

		public void setLocation(IPath path) {
			fUseDefaults.setSelection(path == null);
			if (path != null) {
				fLocation.setText(path.toOSString());
			} else {
				fLocation.setText(getDefaultPath(fNameGroup.getName()));
			}
			fireEvent();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			final DirectoryDialog dialog= new DirectoryDialog(getShell());
			dialog.setMessage("Choose a project directory: ");
			String directoryName = fLocation.getText().trim();
			if (directoryName.length() == 0) {
				String prevLocation= JavaPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LAST_EXTERNAL_LOC);
				if (prevLocation != null) {
					directoryName= prevLocation;
				}
			}

			if (directoryName.length() > 0) {
				final File path = new File(directoryName);
				if (path.exists())
					dialog.setFilterPath(directoryName);
			}
			final String selectedDirectory = dialog.open();
			if (selectedDirectory != null) {
				String oldDirectory= new Path(fLocation.getText().trim()).lastSegment();
				fLocation.setText(selectedDirectory);
				String lastSegment= new Path(selectedDirectory).lastSegment();
				if (lastSegment != null && (fNameGroup.getName().length() == 0 || fNameGroup.getName().equals(oldDirectory))) {
					fNameGroup.setName(lastSegment);
				}
				JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fUseDefaults) {
				final boolean checked= fUseDefaults.isSelected();
				if (checked) {
					fPreviousExternalLocation= fLocation.getText();
					fLocation.setText(getDefaultPath(fNameGroup.getName()));
					fLocation.setEnabled(false);
				} else {
					fLocation.setText(fPreviousExternalLocation);
					fLocation.setEnabled(true);
				}
			}
			fireEvent();
		}
	}
	
	private final class MasonProjectGroup extends Observable implements SelectionListener, IDialogFieldListener, IStringButtonAdapter {

		private static final String DIALOGSTORE_LAST_EXTERNAL_LOC= JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$
		
		private final SelectionButtonDialogField masonProjectRadio, masonFileRadio,fUseDefaults;
		private final StringButtonDialogField masonProjectField, masonFileField;
		private Group fGroup;
		private IProject selectedProject = null;
		private LinkedList<IJavaProject> validMasonProjects = new LinkedList<>();
		private String previousExternalJarLocation;

		public MasonProjectGroup() {
			
			fUseDefaults= new SelectionButtonDialogField(SWT.CHECK);
			fUseDefaults.setDialogFieldListener(this);
			fUseDefaults.setLabelText("Use existing mason project");
			fUseDefaults.setSelection(false);
			
			masonProjectField = new StringButtonDialogField(this);
			masonFileField = new StringButtonDialogField(this);
			
			masonProjectField.setDialogFieldListener(this);
			masonProjectField.setLabelText("");
			masonProjectField.setText("");
			masonProjectField.setButtonLabel("Browse");
			
			masonFileField.setDialogFieldListener(this);
			masonFileField.setLabelText("");
			masonFileField.setButtonLabel("Browse");
			
			masonProjectRadio= new SelectionButtonDialogField(SWT.RADIO);
			masonProjectRadio.setLabelText("Mason project from workspace:");
						
			masonFileRadio= new SelectionButtonDialogField(SWT.RADIO);
			masonFileRadio.setLabelText("Mason project archive (*.jar) location:");

			masonProjectRadio.setDialogFieldListener(this);
			masonFileRadio.setDialogFieldListener(this);
			
			previousExternalJarLocation = "";
			
			masonProjectRadio.setSelection(true);
			initValidProjects();
		}
		
		  private void initValidProjects() {
			  IJavaProject[] javaProjects;

			    try {
			      javaProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
			    } catch (JavaModelException e) {
			      javaProjects = new IJavaProject[0];
			    }
			    
			    URLClassLoader classLoader = null;
				String[] classPathEntries;
				
				for(IJavaProject javaProject : javaProjects)
				{
					try {
						classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
						List<URL> urlList = new ArrayList<URL>();
						for (int i = 0; i < classPathEntries.length; i++) {
						 String entry = classPathEntries[i];
						 IPath path = new Path(entry);
						 URL url = path.toFile().toURI().toURL();
						 urlList.add(url);
						}
						URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
						classLoader = new URLClassLoader(urls);
						Class<?> c = classLoader.loadClass("sim.engine.SimState");
						if(c!=null)validMasonProjects.add(javaProject);
					} catch (CoreException | MalformedURLException | ClassNotFoundException e) {
						//e.printStackTrace();
					}
					finally
					{
						try {
							if(classLoader!=null)classLoader.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
				}
		  }

		  private IProject chooseProject() {

			  ILabelProvider labelProvider = new JavaElementLabelProvider(
					  JavaElementLabelProvider.SHOW_DEFAULT);
			  ElementListSelectionDialog dialog = new ElementListSelectionDialog(
					  getShell(), labelProvider);
			  dialog.setTitle("Project Selection");
			  dialog.setMessage("Choose your mason project");
			  dialog.setElements(validMasonProjects.toArray(new IJavaProject[validMasonProjects.size()]));
			  dialog.setHelpAvailable(false);
			  if (dialog.open() == Window.OK) {
				  return ((IJavaProject)dialog.getFirstResult()).getProject();
			  }
			  return null;
		  }

		  
		  
		public Control createContent(Composite composite) {
			fGroup= new Group(composite, SWT.NONE);
			fGroup.setFont(composite.getFont());
			fGroup.setLayout(new GridLayout(4,false));
			fGroup.setText("Mason Project");
			
			fUseDefaults.doFillIntoGrid(fGroup, 4);
			
			new Label(fGroup, SWT.NONE);
			masonProjectRadio.attachDialogField(masonProjectField);
			masonProjectRadio.doFillIntoGrid(fGroup, 3);
			masonProjectField.doFillIntoGrid(fGroup, 4);
			
			new Label(fGroup, SWT.NONE);
			masonFileRadio.attachDialogField(masonFileField);
			masonFileRadio.doFillIntoGrid(fGroup, 3);
			masonFileField.doFillIntoGrid(fGroup, 4);
			
			LayoutUtil.setHorizontalGrabbing(masonProjectField.getTextControl(null));
			LayoutUtil.setHorizontalGrabbing(masonFileField.getTextControl(null));
			
			setControlState(false);
			
			return fGroup;
		}


		protected void fireEvent() {
			setChanged();
			notifyObservers();
		}
		
		public boolean isImportFileSelected()
		{
			return masonFileRadio.isSelected() && fUseDefaults.isSelected();
		}
		
		public boolean isImportProjectSelected()
		{
			return masonProjectRadio.isSelected() && fUseDefaults.isSelected();
		}
		
		public String getFileLocation()
		{
			return masonFileField.getText().trim();
		}
		
		public String getProjectLocation()
		{
			return masonProjectField.getText().trim();
		}
		
		/**
		 * Return <code>true</code> if the user specified to create 'source' and 'bin' folders.
		 *
		 * @return returns <code>true</code> if the user specified to create 'source' and 'bin' folders.
		 */

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e) {
			//widgetDefaultSelected(e);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
		public boolean isValidProject()
		{
			for(IJavaProject javaProject : validMasonProjects)
			{
				if(javaProject.getProject().getName().equals(masonProjectField.getText()))
					{
						selectedProject = javaProject.getProject();
						return true;
					}
			}
			return false;
		}
		
		public void setControlState(boolean enabled)
		{
			if(!enabled)
			{
				previousExternalJarLocation = masonFileField.getText();
				
				masonProjectField.setText("");
				masonFileField.setText("");
				
				masonProjectRadio.setEnabled(false);
				masonProjectField.setEnabled(false);
				masonFileRadio.setEnabled(false);
				masonFileField.setEnabled(false);
			}
			else
			{
				masonProjectRadio.setEnabled(true);
				masonFileRadio.setEnabled(true);
				if(masonProjectRadio.isSelected()){
					masonProjectField.setEnabled(true);
					masonProjectField.setText(selectedProject==null ? "" : selectedProject.getProject().getName());
				}
				
				else masonFileField.setEnabled(true);
				masonFileField.setText(previousExternalJarLocation);
			}
		}


		@Override
		public void dialogFieldChanged(DialogField field) {
			if (field == fUseDefaults) {
				setControlState(fUseDefaults.isSelected());
			}
			if(field == masonProjectRadio)
			{
				if(masonProjectRadio.isSelected())
				{
					previousExternalJarLocation = masonFileField.getText();
					masonFileField.setText("");
					masonProjectField.setText(selectedProject==null ? "" : selectedProject.getProject().getName());
				}
				else
				{
					masonProjectField.setText("");
					masonFileField.setText(previousExternalJarLocation);
				}
			}
			fireEvent();
		}
		
		@Override
		public void changeControlPressed(DialogField field) {
			if(masonProjectRadio.isSelected())
			{
				selectedProject = chooseProject();
				if(selectedProject!=null)masonProjectField.setText(selectedProject.getProject().getName());
			}
			else if(masonFileRadio.isSelected())
			{
				final FileDialog dialog = new FileDialog(getShell(), SWT.MULTI);
				dialog.setFilterExtensions(new String[]{"*.jar"});
				final String selectedFile = dialog.open();
				if (selectedFile != null) {
					String prefix = selectedFile.substring(0,selectedFile.length()-dialog.getFileName().length()).replace("\\", "/");
					String allfiles = prefix+dialog.getFileNames()[0];
					for(int i = 1;i<dialog.getFileNames().length;i++)
					{
						allfiles+=", "+prefix+dialog.getFileNames()[i];
					}
					masonFileField.setText(allfiles);
					JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, allfiles);
				}
			}
			
		}
	}
	
	private final class Validator implements Observer {

		public void update(Observable o, Object arg) {

			final IWorkspace workspace= JavaPlugin.getWorkspace();

			final String projectName= fNameGroup.getName();
			final String packageName = fPackageGroup.getName();
			setPageComplete(true);
			isNameAndPackageValid = false;

			// check whether the project name field is empty
			if (projectName.length() == 0) {
				setErrorMessage(null);
				setMessage("Enter a project name.");
				setPageComplete(false);
				return;
			}

			// check whether the project name is valid
			final IStatus nameStatus= workspace.validateName(projectName, IResource.PROJECT);
			if (!nameStatus.isOK()) {
				setErrorMessage(nameStatus.getMessage());
				setPageComplete(false);
				return;
			}
			
					
			if (packageName.length() == 0) {
				setErrorMessage(null);
				setMessage("Enter a package name.");
				setPageComplete(false);
				return;
			}
			
			for(String id : fPackageGroup.getName().split("\\."))
			{
				if(!SourceVersion.isIdentifier(id))
				{
					setErrorMessage(id+" is not a valid identifier.");
					setPageComplete(false);
					return;
				}
			}
			
			
			// check whether project already exists
			final IProject handle= workspace.getRoot().getProject(projectName);
			if (handle.exists()) {
				setErrorMessage("A project with this name already exists.");
				setPageComplete(false);
				return;
			}


			final String location= fLocationGroup.getLocation().toOSString();

			// check whether location is empty
			if (location.length() == 0) {
				setErrorMessage(null);
				setMessage("Enter location for the project.");
				setPageComplete(false);
				return;
			}

			// check whether the location is a syntactically correct path
			if (!Path.EMPTY.isValidPath(location)) {
				setErrorMessage("Invalid directory.");
				setPageComplete(false);
				return;
			}

			IPath projectPath= null;
			if (!fLocationGroup.isUseDefaultSelected()) {
				projectPath= Path.fromOSString(location);
				if (!projectPath.toFile().exists()) {
					// check non-existing external location
					if (!canCreate(projectPath.toFile())) {
						setErrorMessage("Cannot create project at given external location.");
						setPageComplete(false);
						return;
					}
				}
			}
			
			final IStatus locationStatus= workspace.validateProjectLocation(handle, projectPath);
			if (!locationStatus.isOK()) {
				setErrorMessage(locationStatus.getMessage());
				setPageComplete(false);
				return;
			}
			
			isNameAndPackageValid = true;
			
			if(fMasonProjectGroup.fUseDefaults.isSelected())setPageComplete(false);
			
			if(fMasonProjectGroup.isImportFileSelected() && fMasonProjectGroup.getFileLocation().isEmpty())
			{
				setErrorMessage(null);
				setMessage("Enter the location of your mason model file.");
				setPageComplete(false);
				return;
			}
			
			if(fMasonProjectGroup.isImportProjectSelected() && fMasonProjectGroup.getProjectLocation().isEmpty())
			{
				setErrorMessage(null);
				setMessage("Enter the location of your mason model directory.");
				setPageComplete(false);
				return;
			}
			
			if(fMasonProjectGroup.isImportProjectSelected())
			{
				if(!fMasonProjectGroup.isValidProject())
				{
					setErrorMessage("Not a valid mason project.");
					return;
				}
			}
			
			if(fMasonProjectGroup.isImportFileSelected())
			{
				String[] fileNames = fMasonProjectGroup.getFileLocation().split(",");
					String error = PetZipUtils.isMasonModel(fileNames);
					if(!error.equals(""))
					{
						setErrorMessage(error);
						return;
					}
			}


			setErrorMessage(null);
			setMessage(null);
		}

		private boolean canCreate(File file) {
			while (!file.exists()) {
				file= file.getParentFile();
				if (file == null)
					return false;
			}

			return file.canWrite();
		}
	}
	
	public NewPetProjectWizardPageOne()
	{
		super("defaultSettings");
		setPageComplete(false);
		setTitle("Create a Pet Project");
		setDescription("Create a Pet project in the workspace or in an external location.");
		
		fNameGroup= new NameGroup();
		fLocationGroup = new LocationGroup();
		fPackageGroup = new PackageGroup();
		fMasonProjectGroup = new MasonProjectGroup();
		
		fNameGroup.addObserver(fLocationGroup);
		fNameGroup.notifyObservers();
		
		fValidator= new Validator();
		fNameGroup.addObserver(fValidator);
		fLocationGroup.addObserver(fValidator);
		fPackageGroup.addObserver(fValidator);
		fMasonProjectGroup.addObserver(fValidator);
		
		setProjectName("");
	}
	
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite= new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		composite.setLayout(initGridLayout(new GridLayout(1, false), true));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		// create UI elements
		Control nameControl= createNameControl(composite);
		nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control packageControl= createPackageControl(composite);
		packageControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control locationControl= createLocationControl(composite);
		locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control masonProjectControl = createMasonProjectControl(composite);
		masonProjectControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		setControl(composite);
	}
	
	private Control createPackageControl(Composite composite) {
		return fPackageGroup.createControl(composite);
	}

	protected Control createNameControl(Composite composite) {
		return fNameGroup.createControl(composite);
	}

	protected Control createLocationControl(Composite composite) {
		return fLocationGroup.createControl(composite);
	}
	protected Control createMasonProjectControl(Composite composite) {
		return fMasonProjectGroup.createContent(composite);
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
	
	public void setProjectName(String name) {
		if (name == null)
			throw new IllegalArgumentException();

		fNameGroup.setName(name);
	}
	public String getProjectName() {
		return fNameGroup.getName();
	}
	
	public URI getProjectLocationURI() {
		if (fLocationGroup.isUseDefaultSelected()) {
			return null;
		}
		return URIUtil.toURI(fLocationGroup.getLocation());
	}
	
	public String getPackage()
	{
		return fPackageGroup.getName();
	}
	
	public IProject getMasonProject()
	{
		return fMasonProjectGroup.masonProjectField.getText().equals("") ? null : fMasonProjectGroup.selectedProject;
	}
	
	public String getMasonJarNames()
	{
		return fMasonProjectGroup.getFileLocation();
	}
	
	@Override
	public boolean canFlipToNextPage() {
		String error = PetZipUtils.isMasonModel(fMasonProjectGroup.masonFileField.getText().split(","));
		return isNameAndPackageValid && fMasonProjectGroup.fUseDefaults.isSelected() && (fMasonProjectGroup.isValidProject() || error.equals(""));
	}
	
	public boolean isDefaultMasonProject()
	{
		return !fMasonProjectGroup.fUseDefaults.isSelected();
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fNameGroup.postSetFocus();
		}
	}


}
