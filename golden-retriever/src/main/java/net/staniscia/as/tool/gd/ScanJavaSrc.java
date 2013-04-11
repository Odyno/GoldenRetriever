import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ScanJavaSrc {
	
	public static final String USAGE = 
			"Using: \n" +
			"java ScanJavaSrc <java_src_folder> <jars_file_dat> [true|false <- sort list of jars by filename]\n" +
			"Example:\n" +
			"java ScanJavaSrc C:\\java_src jars.dat true\n";
		
	private static String[] importPrefixToReject = 
			new String[] {
				"java.", "javax.swing"
			};
	
	private static final String IMPORT_REGEX = "[\\s]*import[\\s]+([a-zA-Z0-9\\.\\$\\_\\*]+).*";
	private static Pattern importPattern = Pattern.compile(IMPORT_REGEX);

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		if (args.length != 2 && args.length != 3) {
			System.out.println(USAGE);
			System.exit(1);
		}
		
		File javaSrcFolder = new File(args[0]);
		if (!javaSrcFolder.exists() || !javaSrcFolder.isDirectory()) {
			System.out.println(args[0] + " is not a directory");
			System.out.println(USAGE);
			System.exit(1);
		}
	
		File scanJarFileDat = new File(args[1]) ;
		if (!scanJarFileDat.exists() || !scanJarFileDat.isFile()) {
			System.out.println(args[1] + " is not a file");
			System.out.println(USAGE);
			System.exit(1);
		}
		boolean sortJarsByFileName = false;
		if (args.length == 3) {
			sortJarsByFileName = Boolean.parseBoolean(args[2]);
		}
		startScan(javaSrcFolder, scanJarFileDat, sortJarsByFileName);
	}

	private static void startScan(File javaSrcFolder, File scanJarFileDat, boolean sortJarsByFileName) throws IOException, ClassNotFoundException {
		
		Map<String, List<String>> jarMap = getJarMapFromFile(scanJarFileDat);
		Set<String> jarsContainingImports = new TreeSet<String>();
		Map<String, List<String>> javaSrcWithImportsNotFoundMap = new HashMap<String, List<String>>();
		
		scanFolder(javaSrcFolder, jarMap, jarsContainingImports, javaSrcWithImportsNotFoundMap);
		
		outputResults(jarsContainingImports, javaSrcWithImportsNotFoundMap, sortJarsByFileName);
	}

	private static void outputResults(Set<String> jarsContainingImports,
			Map<String, List<String>> javaSrcWithImportsNotFoundMap, boolean sortJarsByFileName) {
		System.out.println("List of JARs containing imports:");
		System.out.println("--------------------------------");
		List<String> jarsContainingImportsToList = new ArrayList<String>();
		for (String jar : jarsContainingImports) {
			jarsContainingImportsToList.add(jar);
		}
		if (sortJarsByFileName) {
			///// start sorting for file name
			Collections.sort(jarsContainingImportsToList, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					o1 = o1.substring(o1.lastIndexOf(File.separator) + 1);
					o2 = o2.substring(o2.lastIndexOf(File.separator) + 1);
					return o1.compareTo(o2);
				}
			});
			///// end sorting for file name
		}
		for (String jar : jarsContainingImportsToList) {
			System.out.println(jar);
		}
		
		if (javaSrcWithImportsNotFoundMap.size() > 0) {
			System.out.println("List of Java sources with imports not found in JARs:");
			System.out.println("----------------------------------------------------");
			Set<String> javaSrcSet = javaSrcWithImportsNotFoundMap.keySet();
			for (String javaSrc : javaSrcSet) {
				System.out.print(javaSrc + ": ");
				List<String> xxxx = javaSrcWithImportsNotFoundMap.get(javaSrc);
				for (String className : xxxx) {
					System.out.print(className + ", ");
				}
				System.out.println();
			}
		}
	}

	private static void scanFolder(File folder,
			Map<String, List<String>> jarMap, Set<String> jarsContainingImports, 
			Map<String, List<String>> javaSrcWithImportsNotFoundMap) throws IOException {
		
		String[] folderContents = folder.list();
		File f = null;
		for (String item : folderContents) {
			f = new File(folder, item);
			if (f.isDirectory()) {
				scanFolder(f, jarMap, jarsContainingImports, javaSrcWithImportsNotFoundMap);
			} else if (f.isFile()) {
				if (f.getAbsolutePath().endsWith(".java")) {
					System.out.println("Scanning imports for: " + f.getAbsolutePath());
					scanJavaImports(f, jarMap, jarsContainingImports, javaSrcWithImportsNotFoundMap);
				}
			}
		}		
		
	}

	private static void scanJavaImports(File f, Map<String, List<String>> jarMap, 
			Set<String> jarsContainingImports, Map<String, 
			List<String>> javaSrcWithImportsNotFoundMap) throws IOException {
		FileReader fis = new FileReader(f);
		BufferedReader br = new BufferedReader(fis);
		String line = null;
		Matcher m = null;
		String importInJava = null;
		List<String> importsNotFound = new ArrayList<String>();
		while ( (line = br.readLine()) != null) {
			line = line.trim();
			m = importPattern.matcher(line);
			if (m.matches()) {
				importInJava = m.group(1);
				System.out.println("Found import for: " + m.group(1));
				if (!startsWith(importInJava, importPrefixToReject)) {
					List<String> listOfJarsContainingClass = jarMap.get(importInJava);
					if (listOfJarsContainingClass != null) {
						// populate jarsContainingImports
						for (String jarFile : listOfJarsContainingClass) {
							jarsContainingImports.add(jarFile);
						}
					} else {
						importsNotFound.add(importInJava);
					}
				}
			}
		}
		br.close();
		fis.close();
		if (importsNotFound.size() > 0) {
			//javaSrcWithImportsNotFoundMap.put(f.getAbsolutePath(), importsNotFound);
			javaSrcWithImportsNotFoundMap.put(f.getName(), importsNotFound);
		}
	}

	private static Map<String, List<String>> getJarMapFromFile(
			File scanJarFileDat) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(scanJarFileDat);
		ObjectInputStream ois = new ObjectInputStream(fis);
		@SuppressWarnings("unchecked")
		Map<String, List<String>> jarMap = (Map<String, List<String>>) ois.readObject();
		ois.close();
		fis.close();
		return jarMap;
	}

	private static boolean startsWith(String s, String[] prefixes) {
		boolean result = false;
		for (String prefix : prefixes) {
			if (s.startsWith(prefix)) {
				result = true;
				break;
			}
		}
		return result;
	}
}
	