package jp.thisnor.dre.core;

import java.util.List;

public class FirstMeasureTask implements Runnable {
	private final Measurer measurer;
	private final List<MeasureEntry> entryList;
	private final MeasureEntry firstEntry;
	private final SynchronizedCounter counter;

	public FirstMeasureTask(List<MeasureEntry> entryList, MeasureEntry firstEntry, Measurer measurer, SynchronizedCounter counter) {
		this.measurer = measurer;
		this.entryList = entryList;
		this.firstEntry = firstEntry;
		this.counter = counter;
	}

	public void run() {
		int index;
		while ((index = counter.countup()) < entryList.size()) {
			MeasureEntry entry = entryList.get(index);
			entry.firstDistance = measurer.measure(firstEntry.data, entry.data);
			if (Thread.interrupted()) return;
		}
	}
}
