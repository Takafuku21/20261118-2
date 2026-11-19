package com.example.demo.service;

import java.io.IOException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.TodoAttachment;
import com.example.demo.mapper.TodoAttachmentMapper;

@Service
public class TodoAttachmentService {

	private final TodoAttachmentMapper attachmentMapper;
	private final FileStorageService fileStorageService;

	public TodoAttachmentService(
			TodoAttachmentMapper attachmentMapper,
			FileStorageService fileStorageService) {
		this.attachmentMapper = attachmentMapper;
		this.fileStorageService = fileStorageService;
	}

	public List<TodoAttachment> findByTodoId(Long todoId) {
		return attachmentMapper.findByTodoId(todoId);
	}

	public TodoAttachment findById(Long id) {
		return attachmentMapper.findById(id);
	}

	public TodoAttachment upload(Long todoId, MultipartFile upload) throws IOException {
		TodoAttachment attachment = fileStorageService.store(todoId, upload);
		try {
			if (attachmentMapper.insert(attachment) != 1) {
				throw new IllegalStateException("Attachment metadata was not inserted");
			}
		} catch (DataAccessException | IllegalStateException exception) {
			try {
				fileStorageService.deleteStoredFile(attachment.getStoredFilename());
			} catch (IOException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}
		return attachment;
	}

	public boolean delete(Long todoId, Long attachmentId) throws IOException {
		TodoAttachment attachment = attachmentMapper.findById(attachmentId);
		if (attachment == null || !todoId.equals(attachment.getTodoId())) {
			return false;
		}
		fileStorageService.deleteStoredFile(attachment.getStoredFilename());
		return attachmentMapper.deleteById(attachmentId) > 0;
	}

	public void deleteAllForTodo(Long todoId) throws IOException {
		for (TodoAttachment attachment : attachmentMapper.findByTodoId(todoId)) {
			fileStorageService.deleteStoredFile(attachment.getStoredFilename());
			attachmentMapper.deleteById(attachment.getId());
		}
	}
}
