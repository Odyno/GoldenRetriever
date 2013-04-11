import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class ScanJars {
	
	public static String USAGE = 
			"Using: \n" +
			"java ScanJars <jars_folder> <result_file_to_save> \n" +
			"Example:\n" +
			"java ScanJars C:\\jars scan.dat\n";
	
	public static int CLASS_SUFFIX_LENGTH = ".class".length();

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			System.out.println(USAGE);
			System.exit(1);
		}
		
		File jarFolder = new File(args[0]);
		if (!jarFolder.exists() || !jarFolder.isDirectory()) {
			System.out.println(args[0] + " is not a directory");
			System.out.println(USAGE);
			System.exit(1);
		}
	
		startScan(jarFolder, args[1]);
	}

	private static void startScan(File jarFolder, String outputFile) throws IOException {
		
		Map<String, List<String>> jarMap = new HashMap<String, List<String>>();
		scanFolder(jarFolder, jarMap);
		saveMapToFile(jarMap, outputFile);
	}

	private static void saveMapToFile(Map<String, List<String>> jarMap,
			String outputFile) throws IOException {

		System.out.println("Saving file " + outputFile + "...");
		FileOutputStream fos = new FileOutputStream(outputFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(jarMap);
		oos.close();
		fos.close();
	}

	private static void scanFolder(File folder,
			Map<String, List<String>> jarMap) throws IOException {

		String[] folderContents = folder.list();
		File f = null;
		for (String item : folderContents) {
			f = new File(folder, item);
			if (f.isDirectory()) {
				scanFolder(f, jarMap);
			} else if (f.isFile()) {
				if (f.getAbsolutePath().endsWith(".jar")) {
					System.out.println("Scanning entries for: " + f.getAbsolutePath());
					scanJarEntries(f, jarMap);
				}
			}
		}
	}

	private static void scanJarEntries(File f, Map<String, List<String>> jarMap) throws IOException {
		JarFile jar = new JarFile(f);
		JarEntry entry = null;
		Enumeration<JarEntry> jarEntries = jar.entries();
		while (jarEntries.hasMoreElements()) {
			entry = jarEntries.nextElement();
			String name = entry.getName();
			if (name.endsWith(".class")) {
				String className = convertJarEntryNameInClassName(name);
				addClassToJarMap(className, f, jarMap);
			}
		}
	}

	private static void addClassToJarMap(String className, File jarFile,
			Map<String, List<String>> jarMap) {

		List<String> jarContainingClassList = jarMap.get(className);
		if (jarContainingClassList == null) {
			jarContainingClassList = new ArrayList<String>();
			jarMap.put(className, jarContainingClassList);
		}
		jarContainingClassList.add(jarFile.getAbsolutePath());
	}

	private static String convertJarEntryNameInClassName(String name) {
		name = name.substring(0, name.length() - CLASS_SUFFIX_LENGTH);
		name = name.replaceAll("/", ".");
		return name;
	}

}
	