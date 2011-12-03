package jp.thisnor.dre.core;

class MeasureEntry {
	final FileEntry fileEntry;
	volatile Object data;
	volatile int firstDistance;

	MeasureEntry(FileEntry fileEntry) {
		this.fileEntry = fileEntry;
	}
}
