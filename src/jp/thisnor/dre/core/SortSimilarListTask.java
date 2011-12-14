package jp.thisnor.dre.core;

import java.util.Collections;
import java.util.List;

class SortSimilarListTask implements Runnable {
	private final List<SimilarGroup> simGroupList;
	private final SynchronizedCounter counter;

	SortSimilarListTask(List<SimilarGroup> simGroupList, SynchronizedCounter counter) {
		this.simGroupList = simGroupList;
		this.counter = counter;
	}

	public void run() {
		int index;
		while ((index = counter.countup()) < simGroupList.size()) {
			Collections.sort(simGroupList.get(index).getSimilarList(), SimilarEntry.DISTANCE_COMPARATOR);
		}
	}
}
