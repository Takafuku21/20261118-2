package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
			String direction) {
		List<Todo> todos = todoMapper.findPage(
				keyword, categoryId, sort, direction, pageable.getPageSize(), pageable.getOffset());
		return new PageImpl<>(todos, pageable, todoMapper.countFiltered(keyword, categoryId));
	}

	public List<Todo> findForExport(String keyword, Long categoryId, String sort, String direction) {
		return todoMapper.findForExport(keyword, categoryId, sort, direction);
	}

	public Todo findById(Long id) {
		return todoMapper.findById(id);
	}

	public boolean deleteById(Long id) {
		return todoMapper.deleteById(id) > 0;
	}

	public int deleteByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return 0;
		}
		return todoMapper.deleteByIds(ids);
	}

	public boolean update(Todo todo) {
		return todoMapper.update(todo) > 0;
	}

	public boolean toggleCompleted(Long id) {
		Todo todo = todoMapper.findById(id);
		if (todo == null) {
			return false;
		}
		todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
		return todoMapper.update(todo) > 0;
	}
}
