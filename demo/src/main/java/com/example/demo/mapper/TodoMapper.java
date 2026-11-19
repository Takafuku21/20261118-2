package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.Todo;

public interface TodoMapper {

	List<Todo> findAll(
			@Param("sort") String sort,
			@Param("direction") String direction);

	List<Todo> findPage(
			@Param("keyword") String keyword,
			@Param("categoryId") Long categoryId,
			@Param("sort") String sort,
			@Param("direction") String direction,
			@Param("limit") int limit,
			@Param("offset") long offset);

	long countFiltered(
			@Param("keyword") String keyword,
			@Param("categoryId") Long categoryId);

	List<Todo> searchByTitle(
			@Param("keyword") String keyword,
			@Param("sort") String sort,
			@Param("direction") String direction);

	Todo findById(Long id);

	int insert(Todo todo);

	int deleteById(Long id);

	int deleteByIds(@Param("ids") List<Long> ids);

	int update(Todo todo);
}
