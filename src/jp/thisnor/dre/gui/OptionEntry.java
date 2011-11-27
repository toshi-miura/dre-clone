package jp.thisnor.dre.gui;

import java.util.List;

public class OptionEntry {
	private final String key;
	private String name;
	private String defaultValue;
	private List<String> candidateList;
	private String value;

	OptionEntry(String key) {
		this.key = key;
	}

	String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	String getDefaultValue() {
		return defaultValue;
	}

	void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	List<String> getCandidateList() {
		return candidateList;
	}

	void setCandidateList(List<String> candidateList) {
		this.candidateList = candidateList;
	}

	public String getValue() {
		return (value != null) ? value : defaultValue;
	}

	void setValue(String value) {
		this.value = value;
	}

	public String getKey() {
		return key;
	}
}
