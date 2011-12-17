package jp.thisnor.dre.core;

import java.util.List;

public class MeasureOptionEntry {
	private final String key;
	private String name;
	private String defaultValue;
	private List<String> candidateList;
	private String value;
	private boolean hidden;

	public MeasureOptionEntry(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public List<String> getCandidateList() {
		return candidateList;
	}

	public void setCandidateList(List<String> candidateList) {
		this.candidateList = candidateList;
	}

	public String getValue() {
		return (value != null) ? value : defaultValue;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public String getKey() {
		return key;
	}
}
