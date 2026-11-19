package com.example.demo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Todo {

	private Long id;
	private String title;
	private boolean completed;
	private LocalDateTime createdAt;
	private Priority priority = Priority.MEDIUM;
	private Category category;

	public boolean isCompleted() {
		return completed;
	}

	public boolean getCompleted() {
		return completed;
	}
}
