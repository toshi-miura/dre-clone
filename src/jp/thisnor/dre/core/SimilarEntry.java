package jp.thisnor.dre.core;

import java.util.Comparator;

public class SimilarEntry {
	public static final Comparator<? super SimilarEntry> DISTANCE_COMPARATOR = new Comparator<SimilarEntry>() {
		@Override
		public int compare(SimilarEntry o1, SimilarEntry o2) {
			return Integer.signum(o1.distance - o2.distance);
		}
	};

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
