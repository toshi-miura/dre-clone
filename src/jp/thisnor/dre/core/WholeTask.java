package jp.thisnor.dre.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WholeTask implements Callable<List<SimilarGroup>> {
	private final List<String> targetList, storageList;
	private final PathFilter filter;
	private final MeasurerPackage mpack;
	private final int numThreads;
	private final ProgressListener logger;
	private final Locale locale;

	private volatile List<SimilarGroup> simGroupList;

	private static final Comparator<MeasureEntry> MEASURE_ENTRY_COMPARATOR = new Comparator<MeasureEntry>() {
		@Override
		public int compare(MeasureEntry o1, MeasureEntry o2) {
			return Integer.signum(o1.firstDistance - o2.firstDistance);
		}
	};

	public WholeTask(
			List<String> targetList, List<String> storageList, PathFilter filter,
			MeasurerPackage mpack, int numThreads,
			ProgressListener logger, Locale locale) {
		this.targetList = targetList;
		this.storageList = storageList;
		this.filter = filter;
		this.mpack = mpack;
		this.numThreads = numThreads;
		this.logger = logger;
		this.locale = locale;
	}

	public List<SimilarGroup> call() throws DREException, InterruptedException {
		Messages messages = new Messages("jp.thisnor.dre.core.lang", locale, this.getClass().getClassLoader());

		if (mpack == null) throw new DREException(messages.getString("WholeTask.MEASURER_NOT_SPECIFIED"));
		if (targetList == null) throw new DREException(messages.getString("WholeTask.NO_TARGET"));
		if (storageList == null) throw new DREException(messages.getString("WholeTask.NO_STORAGE"));
		if (numThreads <= 0) throw new IllegalArgumentException(messages.getString("WholeTask.ILLEGAL_NUM_THREADS"));

		Measurer measurer = mpack.getHandler();

		ExecutorService executor;
		SynchronizedCounter counter;
		long t0, t1;

		t0 = System.nanoTime();

		// Generate MeasureEntry
		List<MeasureEntry> targetEntryList = new ArrayList<MeasureEntry>();
		List<MeasureEntry> storageEntryList = null;
		executor = Executors.newFixedThreadPool(numThreads);
		if (storageList == targetList) {
			storageEntryList = targetEntryList;
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new ResolvePathTask(targetList, targetEntryList, filter, counter, logger, locale));
			}
		} else {
			storageEntryList = new ArrayList<MeasureEntry>();
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new ResolvePathTask(targetList, targetEntryList, filter, counter, logger, locale));
			}
			counter = new SynchronizedCounter();
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new ResolvePathTask(storageList, storageEntryList, filter, counter, logger, locale));
			}
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			throw e;
		}

		if (targetEntryList.isEmpty()) {
			throw new DREException(messages.getString("WholeTask.EMPTY_TARGET"));
		}
		if (storageEntryList.isEmpty()) {
			throw new DREException(messages.getString("WholeTask.EMPTY_STORAGE"));
		}

		// Init
		measurer.init(mpack);

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
		logger.progressLoad(1, 1);

		// Remove failed entry
		for (Iterator<MeasureEntry> it = targetEntryList.iterator(); it.hasNext(); ) {
			if (it.next().data == null) it.remove();
		}
		if (targetEntryList != storageEntryList) {
			for (Iterator<MeasureEntry> it = storageEntryList.iterator(); it.hasNext(); ) {
				if (it.next().data == null) it.remove();
			}
		}

		if (targetEntryList.isEmpty()) {
			throw new DREException(messages.getString("WholeTask.EMPTY_TARGET"));
		}
		if (storageEntryList.isEmpty()) {
			throw new DREException(messages.getString("WholeTask.EMPTY_STORAGE"));
		}

		t1 = System.nanoTime();
		logger.log(String.format(messages.getString("WholeTask.FINISHED_LOADING"), (double)((t1 - t0) / 1000000) / 1000.0));
		t0 = System.nanoTime();

		// First-measure
		executor = Executors.newFixedThreadPool(numThreads);
		counter = new SynchronizedCounter(1);
		for (int i = 0; i < numThreads; i++) {
			executor.submit(new FirstMeasureTask(targetEntryList, targetEntryList.get(0), measurer, counter, logger));
		}
		if (targetEntryList != storageEntryList) {
			counter = new SynchronizedCounter(0);
			for (int i = 0; i < numThreads; i++) {
				executor.submit(new FirstMeasureTask(storageEntryList, targetEntryList.get(0), measurer, counter, logger));
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
		int threshold = mpack.getOptionMap().containsKey("threshold") ?
				Integer.parseInt(mpack.getOptionMap().get("threshold").getValue()) : Integer.MAX_VALUE;
		executor = Executors.newFixedThreadPool(numThreads);
		counter = new SynchronizedCounter();
		for (int i = 0; i < numThreads; i++) {
			executor.submit(new FullMeasureTask(targetEntryList, storageEntryList, measurer, threshold, counter, logger));
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
		logger.progressMeasure(1, 1);

		// Sort
		simGroupList = new ArrayList<SimilarGroup>();
		for (MeasureEntry mEntry : targetEntryList) {
			if (mEntry.simList != null)
				simGroupList.add(new SimilarGroup(mEntry.fileEntry, mEntry.simList));
		}
		executor = Executors.newFixedThreadPool(numThreads);
		counter = new SynchronizedCounter();
		for (int i = 0; i < numThreads; i++) {
			executor.submit(new SortSimilarListTask(simGroupList, counter));
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			throw e;
		}
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

		t1 = System.nanoTime();
		logger.log(String.format(messages.getString("WholeTask.FINISHED_COMPARING"), (double)((t1 - t0) / 1000000) / 1000.0));

// For precious test
//		outputSimilarPairs(simGroupList);

		return simGroupList;
	}

	public List<SimilarGroup> getResult() {
		return simGroupList;
	}

// For precious test
/*
	private static class SimilarPair {
		final FileEntry e1, e2;
		final int d;
		private SimilarPair(FileEntry e1, FileEntry e2, int d) {
			this.e1 = e1; this.e2 = e2; this.d = d;
		}
	}

	private void outputSimilarPairs(List<SimilarGroup> simGroups) {
		int numPairs = 0;
		for (SimilarGroup g : simGroups) {
			numPairs += g.getSimilarList().size();
		}
		numPairs /= 2;
		List<SimilarPair> pairs = new ArrayList<SimilarPair>(numPairs);
		for (SimilarGroup g : simGroups) {
			FileEntry e1 = g.getFileEntry();
			for (SimilarEntry sim : g.getSimilarList()) {
				FileEntry e2 = sim.getFileEntry();
				int d = sim.getDistance();
				if (e1.getPath().compareTo(e2.getPath()) < 0) {
					pairs.add(new SimilarPair(e1, e2, d));
				}
			}
		}
		Collections.sort(pairs, new Comparator<SimilarPair>() {
			@Override
			public int compare(SimilarPair o1, SimilarPair o2) {
				return Integer.signum(o1.d - o2.d);
			}
		});
		PrintWriter pw = null;
		try {
			pw = new PrintWriter("tmp/result.txt");
			for (SimilarPair pair : pairs) {
				pw.printf("%-13s, %-13s, % 3d%n", pair.e1.getName(), pair.e2.getName(), pair.d);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) pw.close();
		}
	}
	*/
}
