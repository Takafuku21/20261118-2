package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.Category;
import com.example.demo.mapper.CategoryMapper;

@Service
public class CategoryService {

	private final CategoryMapper categoryMapper;

	public CategoryService(CategoryMapper categoryMapper) {
		this.categoryMapper = categoryMapper;
	}

	public List<Category> findAll() {
		return categoryMapper.findAll();
	}

	public Category findById(Long id) {
		return categoryMapper.findById(id);
	}
}
