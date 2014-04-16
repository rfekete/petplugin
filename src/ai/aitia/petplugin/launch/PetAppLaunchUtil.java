package ai.aitia.petplugin.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfigurationWorkingCopy;
import com.google.gdt.eclipse.suite.launch.WebAppLaunchUtil;
import com.google.gdt.eclipse.suite.launch.processors.WarArgumentProcessor;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;

public class PetAppLaunchUtil {

	public static ILaunchConfigurationWorkingCopy createLaunchConfigWorkingCopy(String launchConfigName, final IProject project,boolean isExternal) throws CoreException, OperationCanceledException {

		    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		    ILaunchConfigurationType petType = manager.getLaunchConfigurationType("ai.aitia.petplugin.petapp");
		    ILaunchConfigurationType gwtType = manager.getLaunchConfigurationType(WebAppLaunchConfiguration.TYPE_ID);
		    
		    
		    final ILaunchConfigurationWorkingCopy gwtWorkingCopy = gwtType.newInstance(null,launchConfigName);
		    final ILaunchConfigurationWorkingCopy petWorkingCopy = petType.newInstance(null,launchConfigName);
		    		    
		    WebAppLaunchUtil.setDefaults(gwtWorkingCopy, project);
		    WebAppLaunchUtil.setDefaults(petWorkingCopy, project);
		    LaunchConfigurationUtilities.setProjectName(gwtWorkingCopy, project.getName());
		    LaunchConfigurationUtilities.setProjectName(petWorkingCopy, project.getName());
		    if (isExternal) {
		      WebAppLaunchConfigurationWorkingCopy.setRunServer(gwtWorkingCopy, false);
		      WebAppLaunchConfigurationWorkingCopy.setRunServer(petWorkingCopy, false);
		    }
		    GWTLaunchConfigurationWorkingCopy.setStartupUrl(petWorkingCopy, "");

		    IPath warDir = null;
		    if (WebAppUtilities.hasManagedWarOut(project)) {
		      warDir = WebAppUtilities.getManagedWarOut(project).getLocation();
		    }

		    if (warDir != null) {
		    	WarArgumentProcessor warArgProcessor = new WarArgumentProcessor();
		    	warArgProcessor.setWarDirFromLaunchConfigCreation(warDir.toOSString());
		    	LaunchConfigurationProcessorUtilities.updateViaProcessor(warArgProcessor,gwtWorkingCopy);
		    	LaunchConfigurationProcessorUtilities.updateViaProcessor(warArgProcessor,petWorkingCopy);

		    }

		    gwtWorkingCopy.setMappedResources(new IResource[] {project});
		    petWorkingCopy.setMappedResources(new IResource[] {project});

		    petWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, gwtWorkingCopy.doSave().getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""));
		    petWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "com.google.gwt.dev.PetDevMode");
		    
		    return petWorkingCopy;
		  }
	
}
