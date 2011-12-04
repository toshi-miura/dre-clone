package jp.thisnor.dre.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WholeTask implements Callable<List<SimilarGroup>> {
	private final Measurer measurer;
	private final List<String> targetList, storageList;
	private final PathFilter filter;
	private final Map<String, MeasureOptionEntry> optionMap;
	private final int numThreads;
	private final ProgressListener logger;

	private volatile List<SimilarGroup> simGroupList;

	private static final Comparator<MeasureEntry> MEASURE_ENTRY_COMPARATOR = new Comparator<MeasureEntry>() {
		@Override
		public int compare(MeasureEntry o1, MeasureEntry o2) {
			return Integer.signum(o1.firstDistance - o2.firstDistance);
		}
	};

	public WholeTask(
			List<String> targetList, List<String> storageList, PathFilter filter,
			Measurer measurer, Map<String, MeasureOptionEntry> optionMap, int numThreads,
			ProgressListener logger) {
		if (measurer == null) throw new IllegalArgumentException("Need measurer");
		if (targetList == null) throw new IllegalArgumentException("Need target path list");
		if (numThreads <= 0) throw new IllegalArgumentException("numThreads must be >= 1, got " + numThreads);
		this.targetList = targetList;
		this.storageList = storageList;
		this.filter = filter;
		this.measurer = measurer;
		this.optionMap = new HashMap<String, MeasureOptionEntry>(optionMap);
		this.numThreads = numThreads;
		this.logger = logger;
	}

	public List<SimilarGroup> call() throws InterruptedException {
		ExecutorService executor;
		SynchronizedCounter counter;

		// Generate MeasureEntry
		List<MeasureEntry> targetEntryList = new ArrayList<MeasureEntry>();
		List<MeasureEntry> storageEntryList = null;
		executor = Executors.newFixedThreadPool(numThreads);
		if (storageList == null || storageList == targetList) {
			storageEntryList = targetEntryList;
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new ResolvePathTask(targetList, targetEntryList, filter, counter));
			}
		} else {
			storageEntryList = new ArrayList<MeasureEntry>();
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new ResolvePathTask(targetList, targetEntryList, filter, counter));
			}
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new ResolvePathTask(storageList, storageEntryList, filter, counter));
			}
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			throw e;
		}

		// Init
		measurer.init(optionMap);

		// Load
		int fullFileCount = targetEntryList.size() + (targetEntryList.size() == storageEntryList.size() ? 0 : storageEntryList.size());
		{
			executor = Executors.newFixedThreadPool(numThreads);
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new LoadTask(targetEntryList, measurer, counter, logger));
			}
			executor.shutdown();
			try {
				while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
					logger.progressLoad(Math.min(counter.currentValue(), targetEntryList.size()), fullFileCount);
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				throw e;
			}
		}
		if (targetEntryList != storageEntryList) {
			executor = Executors.newFixedThreadPool(numThreads);
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new LoadTask(storageEntryList, measurer, counter, logger));
			}
			executor.shutdown();
			try {
				while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
					logger.progressLoad(Math.min(targetEntryList.size() + counter.currentValue(), fullFileCount), fullFileCount);
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				throw e;
			}
		}
		logger.progressLoad(fullFileCount, fullFileCount);

		// Remove failed entry
		for (Iterator<MeasureEntry> it = targetEntryList.iterator(); it.hasNext(); ) {
			if (it.next().data == null) it.remove();
		}
		if (targetEntryList != storageEntryList) {
			for (Iterator<MeasureEntry> it = storageEntryList.iterator(); it.hasNext(); ) {
				if (it.next().data == null) it.remove();
			}
		}

		// First-measure
		executor = Executors.newFixedThreadPool(numThreads);
		counter = new SynchronizedCounter(1);
		for (int i = 0; i < numThreads; i++) {
			executor.submit(new FirstMeasureTask(targetEntryList, targetEntryList.get(0), measurer, counter));
		}
		if (targetEntryList != storageEntryList) {
			counter = new SynchronizedCounter(0);
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new FirstMeasureTask(storageEntryList, targetEntryList.get(0), measurer, counter));
			}
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			throw e;
		}

		// Sort
		Collections.sort(targetEntryList, MEASURE_ENTRY_COMPARATOR);
		if (targetEntryList != storageEntryList)
			Collections.sort(storageEntryList, MEASURE_ENTRY_COMPARATOR);

		// Full-measure
		simGroupList = Collections.synchronizedList(new ArrayList<SimilarGroup>());
		int threshold = optionMap.containsKey("threshold") ?
				Integer.parseInt(optionMap.get("threshold").getValue()) : Integer.MAX_VALUE;
		executor = Executors.newFixedThreadPool(numThreads);
		counter = new SynchronizedCounter();
		for (int i = 0; i < numThreads; i++) {
			executor.submit(new FullMeasureTask(targetEntryList, storageEntryList, simGroupList, measurer, threshold, counter));
		}
		executor.shutdown();
		try {
			while (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
				logger.progressMeasure(counter.currentValue(), targetEntryList.size());
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			throw e;
		}
		logger.progressMeasure(targetEntryList.size(), targetEntryList.size());

		// Sort
		Collections.sort(simGroupList, SimilarGroup.FIRST_DISTANCE_COMPARATOR);

		// Dispose
		measurer.dispose();
		for (MeasureEntry entry : targetEntryList) {
			entry.data = null;
		}
		if (targetEntryList != storageEntryList) {
			for (MeasureEntry entry : storageEntryList) {
				entry.data = null;
			}
		}

		return simGroupList;
	}

	public List<SimilarGroup> getResult() {
		return simGroupList;
	}
}
