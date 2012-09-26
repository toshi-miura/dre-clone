package jp.thisnor.dre.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jp.thisnor.dre.core.DREException;
import jp.thisnor.dre.core.MeasureOptionEntry;
import jp.thisnor.dre.core.MeasurerPackage;
import jp.thisnor.dre.core.PathFilter;
import jp.thisnor.dre.core.ProgressListener;
import jp.thisnor.dre.core.SimilarEntry;
import jp.thisnor.dre.core.SimilarGroup;
import jp.thisnor.dre.core.WholeTask;

class ConsoleApp {
	private String packageName;
	private List<String> targetPathList, storagePathList;
	private PathFilter filter;
	private Map<String, String> optionStrMap;
	private int numThreads = 1;

	private static final ProgressListener STDERR_PROGRESS_LISTENER = new ProgressListener() {
		@Override
		public void progressLoad(int step, int size) {

		}
		@Override
		public void progressMeasure(int step, int size) {

		}
		@Override
		public void log(String line) {
			System.err.println(line);
		}
	};

	ConsoleApp(String[] args) {
		// Parse command line arguments
		for (String arg : args) {
			if (arg.startsWith("--package=")) {
				packageName = arg.substring("--package=".length());
			} else if (arg.startsWith("--storage-path=")) {
				storagePathList = textToList(arg.substring("--storage-path=".length()));
			} else if (arg.startsWith("--filter=")) {
				filter = textToFilter(arg.substring("--filter=".length()));
			} else if (arg.startsWith("--package-option=")) {
				optionStrMap = textToMap(arg.substring("--package-option=".length()));
			} else if (arg.startsWith("--num-threads=")) {
				try {
					numThreads = Integer.parseInt(arg.substring("--num-threads=".length()));
				} catch (NumberFormatException e) {
					System.err.println("WARNING: Need number: " + arg);
					numThreads = 1;
				}
			} else {
				targetPathList = textToList(arg);
			}
		}
	}

	private static List<String> textToList(String text) {
		String[] strs = text.split("\\" + File.pathSeparator);
		List<String> strList = Collections.synchronizedList(new ArrayList<String>(strs.length));
		for (String str : strs) {
			if (str.charAt(0) == '"') str = str.substring(1);
			if (str.charAt(str.length() - 1) == '"') str = str.substring(0, str.length() - 1);
			strList.add(str);
		}
		return strList;
	}

	private static Map<String, String> textToMap(String text) {
		String[] strs = text.split(",");
		Map<String, String> strMap = new HashMap<String, String>(strs.length, 2.0f);
		for (String str : strs) {
			int seppos = str.indexOf('=');
			String key = seppos >= 0 ? str.substring(0, seppos) : str;
			String value = seppos >= 0 ? str.substring(seppos + 1) : "true";
			if (value.charAt(0) == '"') value = value.substring(1);
			if (value.charAt(value.length() - 1) == '"') value = value.substring(0, value.length() - 1);
			strMap.put(key, value);
		}
		return strMap;
	}

	private static PathFilter textToFilter(String text) {
		if (text.charAt(0) == '"') text = text.substring(1);
		if (text.charAt(text.length() - 1) == '"') text = text.substring(0, text.length() - 1);
		final Pattern pattern = Pattern.compile(text);
		return new PathFilter() {
			@Override
			public boolean accept(String path) {
				return pattern.matcher(path).matches();
			}
		};
	}

	boolean run() {
		// Load package
		if (packageName == null) {
			System.err.println("ERROR: Specify package with --package=*");
		}
		System.err.println("Loading the package...");
		MeasurerPackage measurerPackage = null;
		try {
			measurerPackage = MeasurerPackage.importPackage(packageName, Locale.ENGLISH);
		} catch (DREException e) {
			System.err.println("ERROR: Not found specified package: " + packageName);
			return false;
		}
		System.err.println("  package key: " + measurerPackage.getKey());
		System.err.println("  package name: " + measurerPackage.getName());
		System.err.println("  version: " + measurerPackage.getVersion());

		// Load option map
		Map<String, MeasureOptionEntry> optionMap = measurerPackage.getOptionMap();
		if (optionStrMap != null) {
			for (Entry<String, String> e : optionStrMap.entrySet()) {
				MeasureOptionEntry optionEntry = optionMap.get(e.getKey());
				if (optionEntry == null) {
					System.err.println("WARNING: Not found specified option key: " + e.getKey());
					continue;
				}
				List<String> candidates = optionEntry.getCandidateList();
				if (candidates != null && !candidates.contains(e.getValue())) {
					System.err.println("WARNING: Specified option value is not allowed: " + e.getKey() + "=" + e.getValue());
					continue;
				}
				optionEntry.setValue(e.getValue());
			}
		}
		System.err.println("Checking package options...");
		for (MeasureOptionEntry option : optionMap.values()) {
			System.err.printf("  %s: %s%n", option.getKey(), option.getValue());
		}

		System.err.println("Doing detection...");
		WholeTask task = new WholeTask(
				targetPathList,	storagePathList == null ? targetPathList : storagePathList, filter,
				measurerPackage, numThreads,
				STDERR_PROGRESS_LISTENER, Locale.ENGLISH
				);
		List<SimilarGroup> simGroupList = null;
		try {
			simGroupList = task.call();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println("Aborted.");
			return false;
		}

		Map<String, Long> fidMap = new LinkedHashMap<String, Long>(simGroupList.size() * 2, 0.9f);
		long genid = 0;
		for (SimilarGroup simGroup : simGroupList) {
			if (!fidMap.containsKey(simGroup.getFileEntry().getPath()))
				fidMap.put(simGroup.getFileEntry().getPath(), genid++);
			for (SimilarEntry sim : simGroup.getSimilarList()) {
				if (!fidMap.containsKey(sim.getFileEntry().getPath()))
					fidMap.put(sim.getFileEntry().getPath(), genid++);
			}
		}

		System.err.println("Writing the result...");
		printInXML(fidMap, simGroupList);

		return true;
	}

	private void printInXML(Map<String, Long> fidMap, List<SimilarGroup> simGroupList) {
		System.out.println("<?xml version=\"1.0\"?>");
		System.out.println("<result>");
		for (Entry<String, Long> entry : fidMap.entrySet()) {
			System.out.printf("  <file id=\"%d\" path=\"%s\" />%n", entry.getValue(), new File(entry.getKey()).getAbsolutePath());
		}
		for (SimilarGroup simGroup : simGroupList) {
			System.out.printf("  <simgroup file=\"%d\">%n", fidMap.get(simGroup.getFileEntry().getPath()));
			for (SimilarEntry sim : simGroup.getSimilarList()) {
				System.out.printf("    <simitem distance=\"%d\" file=\"%d\" />%n", sim.getDistance(), fidMap.get(sim.getFileEntry().getPath()));
			}
			System.out.println("  </simgroup>");
		}
		System.out.println("</result>");
	}
}