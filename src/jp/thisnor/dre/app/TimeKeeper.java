package jp.thisnor.dre.app;

import java.util.ArrayList;
import java.util.List;

public class TimeKeeper {
	private List<Long> timeList = new ArrayList<Long>();

	public void tick() {
		timeList.add(System.nanoTime());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < timeList.size() - 1; i++) {
			if (i > 0) sb.append(',');
			sb.append((timeList.get(i + 1) - timeList.get(i)) / 1000);
		}
		return sb.toString();
	}
}
