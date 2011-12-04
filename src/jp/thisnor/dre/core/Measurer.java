package jp.thisnor.dre.core;

import java.util.Map;


public interface Measurer {
	void init(Map<String, MeasureOptionEntry> optionMap);
	Object convert(FileEntry entry) throws Exception;
	int measure(Object data1, Object data2, int threshold);
	void dispose();
}
