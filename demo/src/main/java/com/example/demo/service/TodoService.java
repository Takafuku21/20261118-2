package com.example.demo.service;

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
}
