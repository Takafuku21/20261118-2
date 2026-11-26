package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.Todo;

public interface TodoMapper {

	List<Todo> findAll(
			@Param("userId") Long userId,
			@Param("sort") String sort,
			@Param("direction") String direction);

	List<Todo> findPage(
			@Param("keyword") String keyword,
			@Param("categoryId") Long categoryId,
			@Param("userId") Long userId,
			@Param("sort") String sort,
			@Param("direction") String direction,
			@Param("limit") int limit,
			@Param("offset") long offset);

	List<Todo> findForExport(
			@Param("keyword") String keyword,
			@Param("categoryId") Long categoryId,
			@Param("userId") Long userId,
			@Param("sort") String sort,
			@Param("direction") String direction);

	long countFiltered(
			@Param("keyword") String keyword,
			@Param("categoryId") Long categoryId,
			@Param("userId") Long userId);

	List<Todo> searchByTitle(
			@Param("keyword") String keyword,
			@Param("userId") Long userId,
			@Param("sort") String sort,
			@Param("direction") String direction);

	Todo findById(@Param("id") Long id);

	int insert(Todo todo);

	int deleteById(@Param("id") Long id, @Param("userId") Long userId);

	int deleteAllByUserId(@Param("userId") Long userId);

	int deleteByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

	int update(@Param("todo") Todo todo, @Param("userId") Long userId);

	int toggleCompleted(@Param("id") Long id, @Param("userId") Long userId);
}
