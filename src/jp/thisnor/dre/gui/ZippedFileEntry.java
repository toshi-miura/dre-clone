package jp.thisnor.dre.gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jp.thisnor.dre.gui.ImageUtils.ImageSize;

class ZippedFileEntry implements FileEntry {
	private final File file;
	private final String pathInZip;
	private String name;
	private long size;
	private long lastModified;
	private int width = -1, height = -1;
	private FileEntry[] subEntries;

	ZippedFileEntry(File file, String pathInZip) {
		this.file = file;
		this.pathInZip = pathInZip;
	}

	public String getPath() {
		return file.getPath() + File.separator + pathInZip;
	}

	public String getName() {
		if (name == null) {
			int sepPos = Math.max(pathInZip.lastIndexOf('/', pathInZip.length() - 2), pathInZip.lastIndexOf('\\', pathInZip.length() - 2));
			name = pathInZip.substring(sepPos + 1);
			if (name.endsWith("/") || name.endsWith("\\")) name = name.substring(0, name.length() - 1);
		}
		return name;
	}

	public long getSize() {
		if (size == 0) {
			ZipFile zip = null;
			try {
				zip = new ZipFile(file);
				size = zip.getInputStream(zip.getEntry(pathInZip)).available();
			} catch (IOException e) {
			} finally {
				try {
					if (zip != null) zip.close();
				} catch (IOException e) {}
			}
		}
		return size;
	}

	public long getLastModified() {
		if (lastModified == 0) {
			ZipFile zip = null;
			try {
				zip = new ZipFile(file);
				lastModified = zip.getEntry(pathInZip).getTime();
			} catch (IOException e) {
			} finally {
				try {
					if (zip != null) zip.close();
				} catch (IOException e) {}
			}
		}
		return lastModified;
	}

	public int getWidth() {
		if (width == -1) {
			ImageSize size = ImageUtils.getImageSize(this);
			if (size != null) {
				width = size.width;
				height = size.height;
			} else {
				width = Integer.MIN_VALUE;
				height = Integer.MIN_VALUE;
			}
		}
		return width;
	}

	public int getHeight() {
		if (height == -1) {
			ImageSize size = ImageUtils.getImageSize(this);
			if (size != null) {
				width = size.width;
				height = size.height;
			} else {
				width = Integer.MIN_VALUE;
				height = Integer.MIN_VALUE;
			}
		}
		return height;
	}

	public boolean isDirectory() {
		return pathInZip.endsWith("/") || pathInZip.endsWith("\\") || pathInZip.isEmpty();
	}

	public FileEntry[] subEntries() {
		if (subEntries == null) {
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(file);
				Enumeration<? extends ZipEntry> subEntryEnum = zipFile.entries();
				Set<String> subDirEntrySet = new HashSet<String>();
				List<FileEntry> subEntryList = new ArrayList<FileEntry>();
				while (subEntryEnum.hasMoreElements()) {
					ZipEntry e = subEntryEnum.nextElement();
					String name = e.getName();
					if (name.length() > this.pathInZip.length() && name.startsWith(pathInZip)) {
						int sepPos = Math.max(name.indexOf("/", pathInZip.length()), name.indexOf("\\", pathInZip.length()));
						if (sepPos == -1) {
							subEntryList.add(new ZippedFileEntry(file, name));
						} else {
							String dirName = name.substring(0, sepPos + 1);
							if (!subDirEntrySet.contains(dirName)) {
								subEntryList.add(new ZippedFileEntry(file, dirName));
								subDirEntrySet.add(dirName);
							}
						}
					}
				}
				subEntries = subEntryList.toArray(new FileEntry[subEntryList.size()]);
			} catch (IOException e) {
				return new FileEntry[0];
			} finally {
				try {
					if (zipFile != null) zipFile.close();
				} catch (IOException e) {}
			}
		}
		return subEntries;
	}

	public FileEntry getParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ZippedFileEntry)) return false;
		ZippedFileEntry e = (ZippedFileEntry)o;
		return e.file.equals(this.file) && e.pathInZip.equals(this.pathInZip);
	}

	@Override
	public int hashCode() {
		return file.hashCode() * 31 + pathInZip.hashCode();
	}

	@Override
	public String toString() {
		return getPath();
	}

	public InputStream open() throws IOException {
		return new ZippedFileEntryInputStream(file, pathInZip);
	}

	private static class ZippedFileEntryInputStream extends InputStream {
		private final ZipFile zipFile;
		private final InputStream in;

		private ZippedFileEntryInputStream(File file, String path) throws IOException {
			this.zipFile = new ZipFile(file);
			this.in = zipFile.getInputStream(zipFile.getEntry(path));
		}

		public int read() throws IOException {
			return in.read();
		}

		public int hashCode() {
			return in.hashCode();
		}

		public int read(byte[] b) throws IOException {
			return in.read(b);
		}

		public boolean equals(Object obj) {
			return in.equals(obj);
		}

		public int read(byte[] b, int off, int len) throws IOException {
			return in.read(b, off, len);
		}

		public long skip(long n) throws IOException {
			return in.skip(n);
		}

		public int available() throws IOException {
			return in.available();
		}

		public String toString() {
			return in.toString();
		}

		public void close() throws IOException {
			in.close();
		}

		public void mark(int readlimit) {
			in.mark(readlimit);
		}

		public void reset() throws IOException {
			in.reset();
		}

		public boolean markSupported() {
			return in.markSupported();
		}
	}
}
