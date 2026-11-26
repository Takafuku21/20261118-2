package com.example.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Todo {

	private Long id;
	private Long userId;
	private String title;
	private String author;
	private boolean completed;
	private LocalDateTime createdAt;
	private LocalDate deadline;
	private Priority priority = Priority.MEDIUM;
	private Category category;

	public boolean isCompleted() {
		return completed;
	}

	public boolean getCompleted() {
		return completed;
	}

	public boolean isOverdue() {
		return deadline != null && deadline.isBefore(LocalDate.now());
	}

	public boolean isNearDeadline() {
		return deadline != null
				&& !isOverdue()
				&& !deadline.isAfter(LocalDate.now().plusDays(3));
	}
}
