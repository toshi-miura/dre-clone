package jp.thisnor.dre.core;

public interface ProgressListener {
	void progressLoad(int step, int size);
	void progressMeasure(int step, int size);
	void log(String line);
}
