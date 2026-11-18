package com.example.demo;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/todo")
public class TodoController {

	@GetMapping
	public String list(Model model) {
		model.addAttribute("todos", List.of());
		return "todo/list";
	}

	@GetMapping("/new")
	public String newTodo() {
		return "todo/new";
	}

	@GetMapping("/confirm")
	public String confirm() {
		return "todo/confirm";
	}

	@GetMapping("/complete")
	public String complete() {
		return "todo/complete";
	}
}
