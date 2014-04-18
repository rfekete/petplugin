package ai.aitia.petplugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

import ai.aitia.petplugin.natures.PetNature;
import ai.aitia.petplugin.projects.PetResourceChangeListener;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements IStartup {

	// The plug-in ID
	public static final String PLUGIN_ID = "ai.aitia.petplugin"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		//registerListeners();
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public static void registerListeners()
	{
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		for(IProject project : projects)
		{
			if(PetNature.isPetProject(project))
			{
				ScopedPreferenceStore petPrefStore = new ScopedPreferenceStore(new ProjectScope(project), "ai.aitia.petplugin");
				String masonProjectName = petPrefStore.getString("mason.project.name");
				if(!masonProjectName.equals("")) workspace.addResourceChangeListener(new PetResourceChangeListener(masonProjectName,project));
			}
		}

	}

	@Override
	public void earlyStartup() {
		registerListeners();		
	}
}
