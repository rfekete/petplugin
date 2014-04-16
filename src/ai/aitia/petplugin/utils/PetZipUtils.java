package ai.aitia.petplugin.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.osgi.framework.Bundle;

import ai.aitia.petplugin.Activator;
import ai.aitia.petplugin.projects.PetPluginSupport;

final public class PetZipUtils {


	public static URLClassLoader getJarClassLoader(String[] jarPaths) throws FileNotFoundException
	{
		LinkedList<URL> urls = new LinkedList<>();
		for(String jarPath : jarPaths)
		{
			try {
				File f = new File(jarPath.trim());
				if(f.exists())urls.add(f.toURI().toURL());
				else throw(new FileNotFoundException("File \""+jarPath+"\" doesn't exist."));
				Bundle b = Activator.getDefault().getBundle();
				URL masonJarUrl = b.getEntry("/resources/copy/common/war/WEB-INF/lib/mason-all-16.0.0-RELEASE.jar");
				urls.add(masonJarUrl);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		return new URLClassLoader(urls.toArray(new URL[urls.size()]));
	}

	@SuppressWarnings("restriction")
	public static HashMap<String, String> jarMasonProject(IProject masonProject)
	{
		IClasspathEntry[] classpathEntries=null;
		HashMap<String, String> filesToJar = new HashMap<>();
		HashMap<String, String> allFiles = new HashMap<>();
		IJavaProject javaProject = null;
		String workspaceRoot = masonProject.getWorkspace().getRoot().getLocation()+"";

		if (masonProject.isOpen() && JavaProject.hasJavaNature(masonProject)) {
			javaProject = JavaCore.create(masonProject);
			try {

				classpathEntries = javaProject.getRawClasspath();

				for (IClasspathEntry entry : classpathEntries)
				{
					if(entry.getEntryKind() == IClasspathEntry.CPE_SOURCE)
					{
						if(entry.getOutputLocation()!=null)
						{
							Path dirPath = Paths.get(workspaceRoot+entry.getPath());
							for(Path path : Files.newDirectoryStream(dirPath))
							{
								filesToJar.put(path.toString(), path.getFileName().toString());
							}
						}
					}
					if(entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
					{
						if(entry.getPath().toString().toLowerCase().endsWith(".jar")){
							Path filePath = Paths.get(workspaceRoot+entry.getPath());
							allFiles.put(filePath+"", filePath.getFileName()+"");
						}
						else
						{
							Path filePath = Paths.get(workspaceRoot+entry.getPath());
							filesToJar.put(filePath+"", filePath.getFileName()+"");
						}
					}
				}

				Path dirPath = Paths.get(workspaceRoot+"/"+javaProject.getOutputLocation()+"");
				for(Path path : Files.newDirectoryStream(dirPath))
				{
					filesToJar.put(path.toString(), path.getFileName().toString());
				}
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			PetPluginSupport.getJavaProject().getProject().getFolder("temp").create(true, true, null);
			String jarName = PetPluginSupport.getJavaProject().getProject().getFolder("temp").getLocation()+"/"+PetPluginSupport.getMasonModelName()+".jar";
			makeJar(filesToJar,jarName);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return allFiles;
	}

	/**
	 * @param fileNames array of filenames to jar
	 * @param absolute path of the created jar in the filesystem
	 * @param rootDir root directory of the jarred files
	 * @param prefix prefix attached to the jarred files
	 * @return
	 * @throws IOException
	 */
	//	public static String makeJar(Map<String, String> fileNames,String dest, String rootDir, String prefix) throws IOException {
	public static String makeJar(Map<String, String> fileNames,String dest) throws IOException {
		String jarFileName = dest;
		try (FileSystem jarFileSystem = createJarFileSystem(jarFileName, true)) {
			final Path root = jarFileSystem.getPath("/");
			Iterator<String> it = fileNames.keySet().iterator();
			while(it.hasNext())
			{
				final String source;
				final String innerDest;
				source = (String)it.next();
				innerDest = fileNames.get(source);
				final Path src = Paths.get(source);

				if(!Files.isDirectory(src)){
					final Path p = jarFileSystem.getPath(root.toString(),innerDest);
					final Path parent = p.getParent();
					if(Files.notExists(parent)){
						Files.createDirectories(parent);
					}
					Files.copy(src, p, StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
				}
				else{
					Files.walkFileTree(src, new SimpleFileVisitor<Path>(){
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							final Path relativeDest = Paths.get(source).relativize(file);
							final Path legjobbPath = jarFileSystem.getPath(innerDest, relativeDest+"");
							Files.copy(file, legjobbPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							final Path dirToCreate = Paths.get(source).relativize(dir);
							final Path legjobbPath = jarFileSystem.getPath(innerDest, dirToCreate+"");
							if(Files.notExists(legjobbPath)){
								Files.createDirectories(legjobbPath);
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}
			}
		}
		return jarFileName;
	}
	private static FileSystem createJarFileSystem(String zipFilename, boolean create) throws IOException 
	{
		final Path path = Paths.get(zipFilename);
		final URI uri = URI.create("jar:file:" + path.toUri().getPath());

		final Map<String, String> env = new HashMap<>();
		if (create) {
			env.put("create", "true");
		}
		return FileSystems.newFileSystem(uri, env);
	}

	public static void copyFiles(String[] files, String root, final String dest)
	{
		final int rootLen = root.length();
		for(String file : files)
		{
			try {
				final Path src = Paths.get(file.trim());
				if(!Files.isDirectory(src)){
					final Path fileName = Paths.get(dest,file.substring(rootLen,file.length()));//replace original file root directory with destination
					final Path parent = fileName.getParent();
					if(Files.notExists(parent))
					{
						Files.createDirectories(parent);
					}
					Files.copy(src, fileName, StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
				}
				else{
					Files.walkFileTree(src, new SimpleFileVisitor<Path>(){
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							final Path fileName = Paths.get(dest,file.toString().substring(rootLen, file.toString().length()));
							Files.copy(file, fileName, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							String dirName = dir.toString().substring(rootLen, dir.toString().length());
							if(!dirName.equals(""))
							{
								IFolder dirToCreate = PetPluginSupport.getJavaProject().getProject().getFolder(dirName);
								if(!dirToCreate.exists()){
									try {
										dirToCreate.create(true, true, null);
									} catch (CoreException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static String isMasonModel(String[] files)
	{
		String error = "No simulation class found.";
		try {

			URLClassLoader loader = getJarClassLoader(files);
			Class<?> c = loader.loadClass("sim.engine.SimState");
			for(String fileName: files)
			{
				ZipFile modelJar = new ZipFile(fileName.trim());
				Enumeration<? extends ZipEntry> entries = modelJar.entries();
				while(entries.hasMoreElements())
				{
					ZipEntry entry = entries.nextElement();
					if(entry.getName().endsWith(".class"))
					{
						Class<?> currentClass = loader.loadClass(entry.getName().replace("/", ".").substring(0, entry.getName().length()-6));
						if(c.isAssignableFrom(currentClass))
						{
							error = "";
						}
					}

				}
				modelJar.close();
			}

			loader.close();
			return error;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			error = e.getMessage();
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
			error = e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			error = e.getMessage();
		} catch(NoClassDefFoundError e) {
			error = "Missing dependency: "+e.getMessage().replace("/", ".");
		}

		return error;
	}

}
