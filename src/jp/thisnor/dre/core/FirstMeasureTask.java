package jp.thisnor.dre.core;

import java.util.List;

public class FirstMeasureTask implements Runnable {
	private final Measurer measurer;
	private final List<MeasureEntry> entryList;
	private final MeasureEntry firstEntry;
	private final SynchronizedCounter counter;
	private final ProgressListener logger;

	public FirstMeasureTask(
			List<MeasureEntry> entryList, MeasureEntry firstEntry, Measurer measurer, SynchronizedCounter counter,
			ProgressListener logger) {
		this.measurer = measurer;
		this.entryList = entryList;
		this.firstEntry = firstEntry;
		this.counter = counter;
		this.logger = logger;
	}

	public void run() {
		int index;
		while ((index = counter.countup()) < entryList.size()) {
			MeasureEntry entry = entryList.get(index);
			try {
				entry.firstDistance = measurer.measure(firstEntry.data, entry.data, Integer.MAX_VALUE);
			} catch (Exception e) {
				e.printStackTrace();
				logger.log(String.format("%s: %s (%s)", entry.fileEntry.getPath(), e.getClass(), e.getLocalizedMessage()));
			}
			if (Thread.interrupted()) return;
		}
	}
}
