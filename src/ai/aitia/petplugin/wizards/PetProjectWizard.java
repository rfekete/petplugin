package ai.aitia.petplugin.wizards;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.Bundle;

import ai.aitia.petplugin.Activator;
import ai.aitia.petplugin.projects.PetPluginSupport;


@SuppressWarnings("restriction")
public class PetProjectWizard extends NewElementWizard implements INewWizard{
	
	private NewPetProjectWizardPageOne _pageOne;
	private NewPetProjectWizardPageTwo _pageTwo;

	public PetProjectWizard() {
		_pageOne = new NewPetProjectWizardPageOne();
		_pageTwo = new NewPetProjectWizardPageTwo();
		_pageTwo.setPreviousPage(_pageOne);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("New Pet Project");
		setHelpAvailable(false);
		
		Bundle bundle = Activator.getDefault().getBundle();
		final URL fileURL = bundle.getEntry("resources/pet75x75.png");
		setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(fileURL));
	}

	
	@Override
	public void addPages() {
		// TODO Auto-generated method stub
		//super.addPages();
		addPage(_pageOne);
		addPage(_pageTwo);
	}
	@Override
	public boolean performFinish() {
	    PetPluginSupport.createProject(_pageOne);
	    return true;
	}

	@Override
	protected void finishPage(IProgressMonitor monitor)
			throws InterruptedException, CoreException {
	
	}

	@Override
	public IJavaElement getCreatedElement() {
		return null;
	}


	
}