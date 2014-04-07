package ai.aitia.petplugin.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import ai.aitia.petplugin.projects.PetPluginSupport;

final public class PetZipUtils {
	
	private static Manifest manifest = new Manifest();
	private static String[] masonProperties = new String[]{"content: mason model"};
	
	public static void writeDirToJar(String srcDir, String dest, String prefix) throws IOException
	{
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		JarOutputStream target = new JarOutputStream(new FileOutputStream(dest), manifest);
		writeMasonProperties(target);
		add(new File(srcDir), target, prefix);
		target.close();
	}
	
	public static void writeDirToZip(String srcDir, String dest, String prefix) throws IOException
	{
		ZipOutputStream target = new ZipOutputStream(new FileOutputStream(dest));
		add(new File(srcDir), target, prefix);
		target.close();
	}
	
	public static void writeFilesToZip(Map<File, String> files, String dest)
	{
			try 
			{
				ZipOutputStream target = new ZipOutputStream(new FileOutputStream(dest));
				for(File f : files.keySet())
				{
					add(f, target, files.get(f));
				}	
				target.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	private PetZipUtils(){}
	
	public static Manifest getManifest(){return manifest;}

	private static void add(File source, ZipOutputStream target, String prefix) throws IOException
	{
		add(source, target, source.getParentFile(), prefix);
	}
	
	private static void add(File source, ZipOutputStream target, File base, String prefix) throws IOException
	{
	  BufferedInputStream in = null;
	  try
	  {
		String name = prefix+"/"+base.toURI().relativize(source.toURI()).getPath().replace("\\", "/");
	    if (source.isDirectory())
	    {
	      if (!name.isEmpty())
	      {
	        if (!name.endsWith("/"))
	          name += "/";
	        ZipEntry entry = new ZipEntry(name);
	        entry.setTime(source.lastModified());
	        target.putNextEntry(entry);
	        target.closeEntry();
	      }
	      for (File nestedFile: source.listFiles())
	        add(nestedFile, target, base, prefix);
	      return;
	    }
	    
	    
	    ZipEntry entry = new ZipEntry(name);
	    entry.setTime(source.lastModified());
	    target.putNextEntry(entry);
	    in = new BufferedInputStream(new FileInputStream(source));

	    byte[] buffer = new byte[1024];
	    while (true)
	    {
	      int count = in.read(buffer);
	      if (count == -1)
	        break;
	      target.write(buffer, 0, count);
	    }
	    target.closeEntry();
	  }
	  finally
	  {
	    if (in != null)
	      in.close();
	  }
	}
	
	public static void writeMasonProperties(JarOutputStream target)
	{
		try {
			JarEntry masonPropertyFile = new JarEntry("META-INF/mason.prop");
			masonPropertyFile.setTime(System.currentTimeMillis());
			target.putNextEntry(masonPropertyFile);
			for(String property : masonProperties)
			{
				String entry = String.format("%s%n",property);
				target.write(entry.getBytes(),0,entry.length());
			}
			target.closeEntry();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static URLClassLoader getJarClassLoader(String[] jarPaths) throws FileNotFoundException
	{
			LinkedList<URL> urls = new LinkedList<>();
			for(String jarPath : jarPaths)
			{
					try {
						File f = new File(jarPath.trim());
						if(f.exists())urls.add(f.toURI().toURL());
						else throw(new FileNotFoundException("File \""+jarPath+"\" doesn't exist."));
						File masonJar = new File(PetPluginSupport.getBundleRoot().concat("/resources/copy/common/war/WEB-INF/lib/mason-all-16.0.0-RELEASE.jar"));
						urls.add(masonJar.toURI().toURL());
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			
			return new URLClassLoader(urls.toArray(new URL[urls.size()]));
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
