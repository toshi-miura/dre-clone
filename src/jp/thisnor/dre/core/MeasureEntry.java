package jp.thisnor.dre.core;

import java.util.List;

class MeasureEntry {
	final FileEntry fileEntry;
	volatile Object data;
	volatile int firstDistance;
	volatile List<SimilarEntry> simList;

	MeasureEntry(FileEntry fileEntry) {
		this.fileEntry = fileEntry;
	}
}
