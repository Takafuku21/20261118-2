package com.example.demo.service;

import java.util.List;

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

	public List<Todo> findAll() {
		return todoMapper.findAll();
	}

	public Todo findById(Long id) {
		return todoMapper.findById(id);
	}

	public boolean deleteById(Long id) {
		return todoMapper.deleteById(id) > 0;
	}

	public boolean updateTitle(Todo todo) {
		return todoMapper.updateTitle(todo) > 0;
	}
}
