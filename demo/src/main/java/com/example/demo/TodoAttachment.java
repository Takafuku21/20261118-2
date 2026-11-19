package com.example.demo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TodoAttachment {

	private Long id;
	private Long todoId;
	private String storedFilename;
	private String originalFilename;
	private String contentType;
	private long fileSize;
	private LocalDateTime createdAt;
}
