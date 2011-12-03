package jp.thisnor.dre.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FullMeasureTask implements Runnable {
	private final Measurer measurer;
	private final List<MeasureEntry> targetList, storageList;
	private final Map<FileEntry, List<SimilarEntry>> similarMap;
	private final int threshold;
	private final SynchronizedCounter counter;

	public FullMeasureTask(
			List<MeasureEntry> targetList,
			List<MeasureEntry> storageList,
			Map<FileEntry, List<SimilarEntry>> similarMap,
			Measurer measurer,
			int threshold,
			SynchronizedCounter counter) {
		this.measurer = measurer;
		this.targetList = targetList;
		this.storageList = storageList;
		this.similarMap = similarMap;
		this.threshold = threshold;
		this.counter = counter;
	}

	public void run() {
		int indexInDbList0 = findIndex(storageList, targetList.get(0).firstDistance);
		int index;
		while ((index = counter.countup()) < targetList.size()) {
			MeasureEntry tarEntry = targetList.get(index);
			MeasureEntry stEntry;
			while ((stEntry = storageList.get(indexInDbList0)).firstDistance < tarEntry.firstDistance - threshold) {
				indexInDbList0++;
			}
			int indexInDbList = indexInDbList0;
			List<SimilarEntry> simList = null;
			while ((stEntry = storageList.get(indexInDbList)).firstDistance <= tarEntry.firstDistance + threshold) {
				stEntry = storageList.get(indexInDbList);
				if (!stEntry.fileEntry.getPath().equals(tarEntry.fileEntry.getPath())) {
					int realDistance = measurer.measure(stEntry.data, tarEntry.data);
					if (realDistance <= threshold) {
						if (simList == null) simList = Collections.synchronizedList(new ArrayList<SimilarEntry>(4));
						simList.add(new SimilarEntry(stEntry.fileEntry, realDistance));
					}
				}
				indexInDbList++;
			}
			if (simList != null) {
				similarMap.put(tarEntry.fileEntry, simList);
			}
			if (Thread.interrupted()) return;
		}
	}

	private static int findIndex(List<MeasureEntry> entryList, int targetFirstSim) {
		int lo = 0, hi = entryList.size();
		while (lo < hi) {
			int m = (hi - lo) / 2 + lo;
			int fsim = entryList.get(m).firstDistance;
			if (fsim > targetFirstSim) {
				hi = m;
			} else if (fsim < targetFirstSim) {
				lo = m + 1;
			} else {
				break;
			}
		}
		while (lo > 0 && entryList.get(lo - 1).firstDistance >= targetFirstSim)
			--lo;
		return lo;
	}
}
