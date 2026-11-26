package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import com.example.demo.Todo;
import com.example.demo.mapper.TodoMapper;

@Service
public class TodoService {

	private final TodoMapper todoMapper;

	public TodoService(TodoMapper todoMapper) {
		this.todoMapper = todoMapper;
	}

	public void register(Todo todo) {
		todoMapper.insert(todo);
	}

	public Page<Todo> findPage(
			String keyword,
			Long categoryId,
			Pageable pageable,
			String sort,
			String direction,
			Long userId) {
		List<Todo> todos = todoMapper.findPage(
				keyword, categoryId, userId, sort, direction, pageable.getPageSize(), pageable.getOffset());
		return new PageImpl<>(todos, pageable, todoMapper.countFiltered(keyword, categoryId, userId));
	}

	public List<Todo> findForExport(String keyword, Long categoryId, String sort, String direction, Long userId) {
		return todoMapper.findForExport(keyword, categoryId, userId, sort, direction);
	}

	@PreAuthorize("@todoSecurityService.isOwner(#id, principal)")
	public Todo findById(@P("id") Long id) {
		return todoMapper.findById(id);
	}

	@PreAuthorize("@todoSecurityService.isOwner(#id, principal)")
	public boolean deleteById(@P("id") Long id, Long userId) {
		return todoMapper.deleteById(id, userId) > 0;
	}

	public int deleteByIds(List<Long> ids, Long userId) {
		if (ids == null || ids.isEmpty()) {
			return 0;
		}
		return todoMapper.deleteByIds(ids, userId);
	}

	@PreAuthorize("@todoSecurityService.isOwner(#id, principal)")
	public boolean update(@P("id") Long id, Todo todo, Long userId) {
		todo.setId(id);
		return todoMapper.update(todo, userId) > 0;
	}

	@PreAuthorize("@todoSecurityService.isOwner(#id, principal)")
	public boolean toggleCompleted(@P("id") Long id, Long userId) {
		return todoMapper.toggleCompleted(id, userId) > 0;
	}
}
