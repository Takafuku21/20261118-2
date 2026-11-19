package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.Category;

public interface CategoryMapper {

	List<Category> findAll();

	Category findById(@Param("id") Long id);
}
