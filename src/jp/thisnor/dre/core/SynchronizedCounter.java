package jp.thisnor.dre.core;

class SynchronizedCounter {
	private int count;

	SynchronizedCounter() {
		this(0);
	}

	SynchronizedCounter(int initialValue) {
		this.count = initialValue;
	}

	synchronized int countup() {
		return count++;
	}

	synchronized int currentValue() {
		return count;
	}
}
