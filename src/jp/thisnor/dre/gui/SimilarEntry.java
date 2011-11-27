package jp.thisnor.dre.gui;

class SimilarEntry {
	final FileEntry fileEntry;
	final int distance;

	SimilarEntry(FileEntry fileEntry, int distance) {
		this.fileEntry = fileEntry;
		this.distance = distance;
	}
}
