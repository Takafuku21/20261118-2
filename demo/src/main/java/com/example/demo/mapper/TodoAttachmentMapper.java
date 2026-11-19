package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.TodoAttachment;

public interface TodoAttachmentMapper {

	List<TodoAttachment> findByTodoId(@Param("todoId") Long todoId);

	TodoAttachment findById(@Param("id") Long id);

	int insert(TodoAttachment attachment);

	int deleteById(@Param("id") Long id);
}
