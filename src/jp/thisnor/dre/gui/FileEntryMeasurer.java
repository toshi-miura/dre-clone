package jp.thisnor.dre.gui;

import java.util.Map;

public interface FileEntryMeasurer {
	void init(Map<String, OptionEntry> optionMap);
	Object convert(FileEntry entry) throws Exception;
	int measure(Object data1, Object data2);
	void dispose();
}
