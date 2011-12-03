package jp.thisnor.dre.app;

import java.util.Comparator;

import jp.thisnor.dre.core.FileEntry;

class FileEntryHashComparator implements Comparator<FileEntry> {
	static final FileEntryHashComparator INSTANCE = new FileEntryHashComparator();

	private FileEntryHashComparator() {

	}

	@Override
	public int compare(FileEntry o1, FileEntry o2) {
		if (o1 == null) return 1;
		else if (o2 == null) return -1;
		else if (o1.hashCode() < o2.hashCode()) return -1;
		else if (o1.hashCode() > o2.hashCode()) return 1;
		else return 0;
	}
}
