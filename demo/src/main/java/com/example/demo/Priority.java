package com.example.demo;

public enum Priority {
	HIGH("高", "bg-danger"),
	MEDIUM("中", "bg-warning text-dark"),
	LOW("低", "bg-success");

	private final String displayName;
	private final String cssClass;

	Priority(String displayName, String cssClass) {
		this.displayName = displayName;
		this.cssClass = cssClass;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getCssClass() {
		return cssClass;
	}
}
