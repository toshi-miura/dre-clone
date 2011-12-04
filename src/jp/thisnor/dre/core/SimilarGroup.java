package jp.thisnor.dre.core;

import java.util.Comparator;
import java.util.List;

public class SimilarGroup {
	public static final Comparator<? super SimilarGroup> FIRST_DISTANCE_COMPARATOR = new Comparator<SimilarGroup>() {
		@Override
		public int compare(SimilarGroup o1, SimilarGroup o2) {
			return Integer.signum(o1.similarList.get(0).getDistance() - o2.similarList.get(0).getDistance());
		}
	};

	private final FileEntry fileEntry;
	private final List<SimilarEntry> similarList;

	public SimilarGroup(FileEntry fileEntry, List<SimilarEntry> similarList) {
		this.fileEntry = fileEntry;
		this.similarList = similarList;
	}

	public FileEntry getFileEntry() {
		return fileEntry;
	}

	public List<SimilarEntry> getSimilarList() {
		return similarList;
	}
}
