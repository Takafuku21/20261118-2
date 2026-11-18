package com.example.demo;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
		return "todo/form";
	}

	@PostMapping("/confirm")
	public String confirm(
			@RequestParam("title") String title,
			@RequestParam("description") String description,
			@RequestParam("done") Boolean done,
			Model model) {
		model.addAttribute("title", title);
		model.addAttribute("description", description);
		model.addAttribute("done", done);
		return "todo/confirm";
	}

	@GetMapping("/complete")
	public String complete() {
		return "todo/complete";
	}
}
