package jp.thisnor.dre.core;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResolvePathTask implements Runnable {
	public static final String RECURSIVE_MARK = File.separator + File.separator;

	private final List<String> pathList;
	private final List<MeasureEntry> entryList;
	private final PathFilter filter;
	private final SynchronizedCounter counter;

	public ResolvePathTask(List<String> pathList, List<MeasureEntry> entryList, PathFilter filter, SynchronizedCounter counter) {
		this.pathList = pathList;
		this.entryList = entryList;
		this.filter = filter != null ? filter : PathFilter.DEFAULT;
		this.counter = counter;
	}

	public void run() {
		try {
			int index;
			while ((index = counter.countup()) < pathList.size()) {
				String path = pathList.get(index);
				boolean recursive = path.endsWith("//") || path.endsWith("\\\\");
				path = recursive ? path.substring(0, path.length() - 2) : path;
				File file = new File(path);
				if (file.isDirectory()) {
					traverseDir(file, recursive);
				} else if (!(path.contains("!") && (recursive || path.charAt(path.length() - 1) == '/'))) {
					checkAndAddPath(path);
				} else {
					int seppos = path.indexOf('!');
					String pathInZip = path.substring(seppos + 1) + (path.charAt(path.length() - 1) == '/' ? "" : "/");
					String zipFilePath = path.substring(0, seppos);
					ZipFile zipFile = null;
					try {
						zipFile =  new ZipFile(zipFilePath);
						if (zipFile.getEntry(pathInZip) != null) {
							for (Enumeration<? extends ZipEntry> it = zipFile.entries(); it.hasMoreElements(); ) {
								String entryPath = it.nextElement().getName();
								if (entryPath.startsWith(pathInZip) && (recursive || entryPath.indexOf('/', pathInZip.length()) == -1)) {
									checkAndAddPath(zipFilePath + '!' + entryPath);
								}
							}
						} else {
							checkAndAddPath(path);
						}
					} catch (IOException e) {
						checkAndAddPath(path);
					} finally {
						if (zipFile != null) try {zipFile.close();} catch (IOException e) {}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void traverseDir(File dir, boolean recursive) {
		for (File f : dir.listFiles()) {
			if (!f.isDirectory()) {
				checkAndAddPath(f.getPath());
			} else if (recursive) {
				traverseDir(f, recursive);
			}
		}
	}

	private void checkAndAddPath(String path) {
		if (filter.accept(path)) {
			synchronized (entryList) {
				entryList.add(new MeasureEntry(new NormalFileEntry(new File(path))));
			}
		}
	}
}
