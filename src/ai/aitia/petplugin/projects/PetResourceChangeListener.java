package ai.aitia.petplugin.projects;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import ai.aitia.petplugin.utils.PetFileUtils;

public class PetResourceChangeListener implements IResourceChangeListener {

	private IJavaProject masonJavaProject;
	private IJavaProject petJavaProject;

	public PetResourceChangeListener(String masonProjectName, IProject petProject) {
		IProject masonProject = ResourcesPlugin.getWorkspace().getRoot().getProject(masonProjectName);

		try {
			if(masonProject.isOpen() && masonProject.hasNature(JavaCore.NATURE_ID))
				masonJavaProject = JavaCore.create(masonProject);
			if(petProject.isOpen())
				petJavaProject = JavaCore.create(petProject);

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		final HashMap<String, String> resourcesToWatch = PetFileUtils.getFilesToJar(masonJavaProject);
		final LinkedList<IResource> resourcesToWrite = new LinkedList<>(); 
		final LinkedList<IResource> resourcesToDelete = new LinkedList<>(); 
		if(event.getType() == IResourceChangeEvent.POST_CHANGE)
		{
			IResourceDelta rootDelta = event.getDelta();
			IResourceDelta d = rootDelta.findMember(new Path(masonJavaProject.getProject().getName()));
			IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) {

					IResource resource = delta.getResource();
					if (resource.getType() == IResource.FILE) {
						Iterator<String> it = resourcesToWatch.keySet().iterator();
						while(it.hasNext())
						{
							java.nio.file.Path filePath = Paths.get(it.next());
							java.nio.file.Path resourcePath = Paths.get(resource.getLocationURI());
							if(resourcePath.startsWith(filePath))
							{
								trukk(masonJavaProject);
								java.nio.file.Path dest = filePath.getParent().relativize(resourcePath);
							}
						}

					}
					return true;
				}
			};
			try {
				if(d!=null)d.accept(visitor);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected void trukk(IJavaProject masonJavaProject) 
	{
		String masonProjectName = PetPluginSupport.getPreference(petJavaProject.getProject(), PetPluginSupport.PREF_MASON_MODEL_NAME);
		String petJarPath = petJavaProject.getProject().getFile(masonProjectName+".petii").getLocation()+"";
		try(FileSystem fs = PetFileUtils.createJarFileSystem(petJarPath, false))
		{
			java.nio.file.Path masonJarName = fs.getPath(masonProjectName+".jar");
			java.nio.file.Path tempJarPath = Paths.get(masonJavaProject.getProject().getFile("temp/"+masonProjectName+".jar").getFullPath()+"");
			Files.copy(masonJarName, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

