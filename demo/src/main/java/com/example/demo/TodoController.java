package com.example.demo;

import com.example.demo.service.TodoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/todo")
public class TodoController {

	private final TodoService todoService;

	public TodoController(TodoService todoService) {
		this.todoService = todoService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("todos", todoService.findAll());
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
			@RequestParam(name = "done", defaultValue = "false") Boolean done,
			Model model) {
		model.addAttribute("title", title);
		model.addAttribute("description", description);
		model.addAttribute("done", done);
		return "todo/confirm";
	}

	@PostMapping("/complete")
	public String complete(@RequestParam("title") String title) {
		Todo todo = new Todo();
		todo.setTitle(title);
		todo.setCompleted(false);
		todoService.register(todo);
		return "redirect:/todo";
	}
}
