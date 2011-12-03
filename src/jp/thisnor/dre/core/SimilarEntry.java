package jp.thisnor.dre.core;

public class SimilarEntry {
	private final FileEntry fileEntry;
	private final int distance;

	public SimilarEntry(FileEntry fileEntry, int distance) {
		this.fileEntry = fileEntry;
		this.distance = distance;
	}

	public FileEntry getFileEntry() {
		return fileEntry;
	}

	public int getDistance() {
		return distance;
	}
}
