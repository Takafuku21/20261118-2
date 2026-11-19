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

	public Page<Todo> findAll(Pageable pageable, String sort, String direction) {
		List<Todo> todos = todoMapper.findPage(
				sort, direction, pageable.getPageSize(), pageable.getOffset());
		return new PageImpl<>(todos, pageable, todoMapper.countAll());
	}

	public Page<Todo> searchByTitle(String keyword, Pageable pageable, String sort, String direction) {
		List<Todo> todos = todoMapper.searchPageByTitle(
				keyword, sort, direction, pageable.getPageSize(), pageable.getOffset());
		return new PageImpl<>(todos, pageable, todoMapper.countByTitle(keyword));
	}

	public Todo findById(Long id) {
		return todoMapper.findById(id);
	}

	public boolean deleteById(Long id) {
		return todoMapper.deleteById(id) > 0;
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
