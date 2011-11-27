package jp.thisnor.dre.gui;

import java.io.IOException;
import java.io.InputStream;

public interface FileEntry {
	String getPath();
	String getName();
	long getSize();
	long getLastModified();
	int getWidth();
	int getHeight();

	boolean isDirectory();
	FileEntry[] subEntries();
	FileEntry getParent();

	InputStream open() throws IOException;
}
