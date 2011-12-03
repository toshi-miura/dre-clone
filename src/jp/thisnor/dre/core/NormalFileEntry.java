package jp.thisnor.dre.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

import jp.thisnor.dre.app.ImageUtils;
import jp.thisnor.dre.app.ImageUtils.ImageSize;

public class NormalFileEntry implements FileEntry {
	private final File file;
	private int width = -1, height = -1;
	private FileEntry[] subEntries;
	private FileEntry parent;

	public NormalFileEntry(File file) {
		this.file = file;
	}

	public String getPath() {
		return file.getPath();
	}

	public String getName() {
		return file.getName();
	}

	public long getSize() {
		return file.length();
	}

	public long getLastModified() {
		return file.lastModified();
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
		return file.isDirectory();
	}

	public FileEntry[] subEntries() {
		if (subEntries == null) {
			if (isZip()) {
				subEntries = new ZippedFileEntry(file, "").subEntries();
			} else {
				File[] files = file.listFiles();
				if (files != null) {
					subEntries = new FileEntry[files.length];
					for (int i = 0; i < files.length; i++) {
						subEntries[i] = new NormalFileEntry(files[i]);
					}
				} else {
					subEntries = new FileEntry[0];
				}
			}
		}
		return subEntries;
	}

	public boolean isZip() {
		if (!file.getName().toLowerCase().endsWith("zip")) return false;
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (zipFile != null) zipFile.close();
			} catch (IOException e) {}
		}
	}

	public FileEntry getParent() {
		if (parent != null) {
			parent = new NormalFileEntry(file.getParentFile());
		}
		return parent;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof NormalFileEntry)) return false;
		NormalFileEntry e = (NormalFileEntry)o;
		return e.file.equals(this.file);
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public String toString() {
		return getPath();
	}

	public InputStream open() throws IOException {
		return new FileInputStream(file);
	}
}
