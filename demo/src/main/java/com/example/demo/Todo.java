package com.example.demo;

import lombok.Data;

@Data
public class Todo {

	private Long id;
	private String title;
	private boolean completed;

	public boolean isCompleted() {
		return completed;
	}

	public boolean getCompleted() {
		return completed;
	}
}
