package jp.thisnor.dre.core;

public class SynchronizedCounter {
	private int count;

	public SynchronizedCounter() {
		this(0);
	}

	public SynchronizedCounter(int initialValue) {
		this.count = initialValue;
	}

	public int countup() {
		synchronized (this) {
			return count++;
		}
	}

	public int countdown() {
		synchronized (this) {
			return --count;
		}
	}

	public int currentValue() {
		synchronized (this) {
			return count;
		}
	}
}
