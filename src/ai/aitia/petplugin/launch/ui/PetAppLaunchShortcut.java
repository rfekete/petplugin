package ai.aitia.petplugin.launch.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ui.PlatformUI;

import ai.aitia.petplugin.launch.PetAppLaunchUtil;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gdt.eclipse.suite.launch.ui.WebAppLaunchShortcut;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;

public class PetAppLaunchShortcut extends WebAppLaunchShortcut 
{

	protected void launch(IResource resource, String mode) {

		resource = ResourceUtils.resolveTargetResource(resource);

		try {
			String startupUrl = "";
			if (startupUrl != null) {
				ILaunchConfiguration config = findOrCreateLaunchConfiguration(resource, startupUrl, false);

				assert (config != null);

				DebugUITools.launch(config, mode);
			}
		} catch (CoreException e) {
			CorePluginLog.logError(e);
		} catch (OperationCanceledException e) { 
		}
	}

	private ILaunchConfiguration loadLaunchConfig(
			String startupUrl, IProject project, boolean isExternal, ILaunchConfiguration[] configs)
					throws CoreException {
		List<ILaunchConfiguration> candidates = new ArrayList<ILaunchConfiguration>();

		for (ILaunchConfiguration config : configs) {
			String configUrl = GWTLaunchConfiguration.getStartupUrl(config);

			if (configUrl.equals(startupUrl)
					&& LaunchConfigurationUtilities.getProjectName(config).equals(project.getName())
					&& WebAppLaunchConfiguration.getRunServer(config) == !isExternal) {
				candidates.add(config);
			}
		}

		if (candidates.isEmpty()) {
			return null;
		} else if (candidates.size() == 1) {
			return candidates.get(0);
		} else {
			return LaunchConfigurationUtilities.chooseConfiguration(candidates, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		}
	}

	@Override
	protected ILaunchConfiguration findLaunchConfiguration(
			IResource resource, String startupUrl, boolean isExternal) throws CoreException {

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType typeid =
				launchManager.getLaunchConfigurationType("ai.aitia.petplugin.petapp");
		ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(typeid);

		return loadLaunchConfig(startupUrl, resource.getProject(), isExternal, configs);
	}

	@Override
	protected ILaunchConfiguration findOrCreateLaunchConfiguration(IResource resource, String startupUrl, boolean isExternal)throws CoreException, OperationCanceledException 
	{
		ILaunchConfiguration config = findLaunchConfiguration(resource, startupUrl, isExternal);

		if (config == null) {
			config = createLaunchConfig(resource, startupUrl, isExternal);
		}

		return config;
	}

	private ILaunchConfiguration createLaunchConfig(IResource resource, String startupUrl, boolean isExternal) throws CoreException, OperationCanceledException {
		String initialName = resource.getName();
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		String launchConfigName = manager.generateLaunchConfigurationName(initialName);

		IProject project = resource.getProject();
		ILaunchConfigurationWorkingCopy wc = PetAppLaunchUtil.createLaunchConfigWorkingCopy(launchConfigName, project, isExternal);
		

		return wc.doSave();
	}


}
