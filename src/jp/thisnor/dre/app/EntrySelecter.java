package jp.thisnor.dre.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.thisnor.dre.core.FileEntry;
import jp.thisnor.dre.core.SimilarEntry;
import jp.thisnor.dre.core.SimilarGroup;

interface EntrySelecter {
	Set<FileEntry> select(List<SimilarGroup> simGroupList);
}

class AllEntrySelecter implements EntrySelecter {
	static final AllEntrySelecter INSTANCE = new AllEntrySelecter();
	private AllEntrySelecter() {}
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		for (SimilarGroup simGroup : simGroupList) {
			resultSet.add(simGroup.getFileEntry());
			for (SimilarEntry similar : simGroup.getSimilarList()) {
				resultSet.add(similar.getFileEntry());
			}
		}
		return resultSet;
	}
}

class TargetEntrySelecter implements EntrySelecter {
	static final TargetEntrySelecter INSTANCE = new TargetEntrySelecter();
	private TargetEntrySelecter() {}
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		for (SimilarGroup simGroup : simGroupList) {
			resultSet.add(simGroup.getFileEntry());
		}
		return resultSet;
	}
}

class StorageEntrySelecter implements EntrySelecter {
	static final StorageEntrySelecter INSTANCE = new StorageEntrySelecter();
	private StorageEntrySelecter() {}
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		for (SimilarGroup simGroup : simGroupList) {
			for (SimilarEntry sim : simGroup.getSimilarList()) {
				resultSet.add(sim.getFileEntry());
			}
		}
		return resultSet;
	}
}

class PickOneEntrySelecter implements EntrySelecter {
	static final PickOneEntrySelecter INSTANCE = new PickOneEntrySelecter();
	private PickOneEntrySelecter() {}
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		groupList:
		for (SimilarGroup simGroup : simGroupList) {
			if (resultSet.contains(simGroup.getFileEntry())) continue;
			for (SimilarEntry sim : simGroup.getSimilarList()) {
				if (resultSet.contains(sim.getFileEntry())) continue groupList;
			}
			resultSet.add(simGroup.getFileEntry());
		}
		return resultSet;
	}
}

abstract class MaxBasedEntrySelecter<T extends Comparable<? super T>> implements EntrySelecter {
	private final int order;
	protected MaxBasedEntrySelecter() {
		this(1);
	}
	protected MaxBasedEntrySelecter(int order) {
		this.order = order;
	}
	protected abstract T getContentScore(FileEntry file);
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		for (SimilarGroup simGroup : simGroupList) {
			FileEntry maxFile = simGroup.getFileEntry();
			T max = getContentScore(maxFile);
			for (SimilarEntry similar : simGroup.getSimilarList()) {
				FileEntry file = similar.getFileEntry();
				T val = getContentScore(file);
				int comp = val.compareTo(max) * order;
				if (comp > 0 || comp == 0 && resultSet.contains(file)) {
					maxFile = file;
					max = val;
				}
			}
			resultSet.add(maxFile);
		}
		return resultSet;
	}
}

class CompressEntrySelecter extends MaxBasedEntrySelecter<Double> {
	static CompressEntrySelecter
		MAX_SELECTER = new CompressEntrySelecter(1),
		MIN_SELECTER = new CompressEntrySelecter(-1);
	private CompressEntrySelecter(int order) { super(order); }
	protected Double getContentScore(FileEntry file) {
		return (file.getWidth() >= 0 && file.getSize() > 0) ? ((double)file.getWidth() * file.getHeight() / file.getSize()) : Double.NEGATIVE_INFINITY;
	}
}

class SizeEntrySelecter extends MaxBasedEntrySelecter<Integer> {
	static SizeEntrySelecter
		MAX_SELECTER = new SizeEntrySelecter(1),
		MIN_SELECTER = new SizeEntrySelecter(-1);
	private SizeEntrySelecter(int order) { super(order); }
	protected Integer getContentScore(FileEntry file) {
		return (file.getWidth() >= 0) ? (file.getWidth() * file.getHeight()) : Integer.MIN_VALUE;
	}
}

class FileSizeEntrySelecter extends MaxBasedEntrySelecter<Long> {
	static FileSizeEntrySelecter
		MAX_SELECTER = new FileSizeEntrySelecter(1),
		MIN_SELECTER = new FileSizeEntrySelecter(-1);
	private FileSizeEntrySelecter(int order) { super(order); }
	protected Long getContentScore(FileEntry file) {
		return file.getSize();
	}
}

class LastModifiedEntrySelecter extends MaxBasedEntrySelecter<Long> {
	static LastModifiedEntrySelecter
		LAST_SELECTER = new LastModifiedEntrySelecter(1),
		EARLIEST_SELECTER = new LastModifiedEntrySelecter(-1);
	private LastModifiedEntrySelecter(int order) { super(order); }
	protected Long getContentScore(FileEntry file) {
		return file.getLastModified();
	}
}

class FilenameEntrySelecter extends MaxBasedEntrySelecter<String> {
	static FilenameEntrySelecter
		LAST_SELECTER = new FilenameEntrySelecter(1),
		EARLIEST_SELECTER = new FilenameEntrySelecter(-1);
	private FilenameEntrySelecter(int order) { super(order); }
	protected String getContentScore(FileEntry file) {
		return file.getName();
	}
}

abstract class ThresholdBasedEntrySelecter implements EntrySelecter {
	protected abstract double getContentScore(FileEntry file);
}

class LargerDistanceEntrySelecter implements EntrySelecter {
	private final int threshold;
	LargerDistanceEntrySelecter(int threshold) {
		this.threshold = threshold;
	}
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		for (SimilarGroup simGroup : simGroupList) {
			for (SimilarEntry similar : simGroup.getSimilarList()) {
				if (similar.getDistance() >= threshold) resultSet.add(similar.getFileEntry());
			}
		}
		return resultSet;
	}
}

class SmallerDistanceEntrySelecter implements EntrySelecter {
	private final int threshold;
	SmallerDistanceEntrySelecter(int threshold) {
		this.threshold = threshold;
	}
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		for (SimilarGroup simGroup : simGroupList) {
			for (SimilarEntry similar : simGroup.getSimilarList()) {
				if (similar.getDistance() <= threshold) resultSet.add(similar.getFileEntry());
			}
		}
		return resultSet;
	}
}

class PathFilterEntrySelecter implements EntrySelecter {
	private final String filterPath;
	PathFilterEntrySelecter(String path) {
		this.filterPath = path.toLowerCase();
	}
	public Set<FileEntry> select(List<SimilarGroup> simGroupList) {
		Set<FileEntry> resultSet = new HashSet<FileEntry>();
		for (SimilarGroup simGroup : simGroupList) {
			FileEntry file = simGroup.getFileEntry();
			if (hit(file)) resultSet.add(file);
			for (SimilarEntry similar : simGroup.getSimilarList()) {
				FileEntry simFile = similar.getFileEntry();
				if (hit(simFile)) resultSet.add(simFile);
			}
		}
		return resultSet;
	}
	private boolean hit(FileEntry file) {
		return file.getPath().toLowerCase().startsWith(filterPath);
	}
}