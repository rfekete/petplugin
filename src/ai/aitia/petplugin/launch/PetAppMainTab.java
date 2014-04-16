package ai.aitia.petplugin.launch;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.launch.ui.WebAppMainTab;

public class PetAppMainTab extends WebAppMainTab {
	  
	private static final String ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME = GdtPlugin.PLUGIN_ID + "MainTypeProcessor.PREVIOUSLY_SET_MAIN_TYPE_NAME";
	private static final String MAIN_TYPE = "com.google.gwt.dev.PetDevMode";

	/*@Override
	public void doPerformApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		super.doPerformApply(configuration);
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, MAIN_TYPE);
		configuration.setAttribute(ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME, MAIN_TYPE);
	}*/
	
	@Override
	protected void initializeMainTypeAndName(IJavaElement javaElement,ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, MAIN_TYPE);
	}
	
}
