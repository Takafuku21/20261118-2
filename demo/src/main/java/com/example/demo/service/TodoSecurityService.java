package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.AuthenticatedUser;
import com.example.demo.Todo;
import com.example.demo.mapper.TodoMapper;

@Service("todoSecurityService")
public class TodoSecurityService {
	private final TodoMapper todoMapper;

	public TodoSecurityService(TodoMapper todoMapper) {
		this.todoMapper = todoMapper;
	}

	public boolean isOwner(Long id, Object principal) {
		if (id == null || !(principal instanceof AuthenticatedUser user)) {
			return false;
		}
		Todo todo = todoMapper.findById(id);
		return todo != null && (user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
				|| user.getId().equals(todo.getUserId()));
	}
}
