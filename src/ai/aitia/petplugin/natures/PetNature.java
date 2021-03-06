package ai.aitia.petplugin.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class PetNature implements IProjectNature {

	public static final String NATURE_ID = "petplugin.projectNature";
	
	@Override
	public void configure() throws CoreException {
		// TODO Auto-generated method stub

	}
	
	public static boolean isPetProject(IProject project) {
	    try {
	      return project.isAccessible() && project.hasNature(PetNature.NATURE_ID);
	    } catch (CoreException e) {
	    	e.printStackTrace();
	    }
	    return false;
	  }

	@Override
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public IProject getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProject(IProject project) {
		// TODO Auto-generated method stub

	}

}
