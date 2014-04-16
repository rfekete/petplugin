package ai.aitia.petplugin.launch;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.google.gdt.eclipse.suite.launch.ui.WebAppTabGroup;

public class PetAppTabGroup extends WebAppTabGroup {
	
@Override
public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
	// TODO Auto-generated method stub
	super.createTabs(dialog, mode);
	ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[getTabs().length];
	System.arraycopy(getTabs(), 1, tabs, 1, getTabs().length-1);
	tabs[0]=new PetAppMainTab();
	setTabs(tabs);
}
}
