package jp.thisnor.dre.core;

import java.util.List;

public class LoadTask implements Runnable {
	private final Measurer measurer;
	private final List<MeasureEntry> entryList;
	private final SynchronizedCounter counter;
	private final ProgressListener logger;

	public LoadTask(List<MeasureEntry> entryList, Measurer measurer, SynchronizedCounter counter, ProgressListener logger) {
		this.measurer = measurer;
		this.entryList = entryList;
		this.counter = counter;
		this.logger = logger;
	}

	public void run() {
		int index;
		while ((index = counter.countup()) < entryList.size()) {
			MeasureEntry entry = entryList.get(index);
			try {
				entry.data = measurer.convert(entry.fileEntry);
			} catch (Exception e) {
				logger.log(String.format("%s: %s (%s)", entry.fileEntry.getPath(), e.getClass(), e.getLocalizedMessage()));
			}
			if (Thread.interrupted()) return;
		}
	}
}
