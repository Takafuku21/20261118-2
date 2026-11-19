package com.example.demo;

import com.example.demo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false, defaultValue = "createdAt") String sort,
			@RequestParam(required = false, defaultValue = "desc") String direction,
			Model model) {
		String normalizedKeyword = keyword == null ? null : keyword.trim();
		if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
			normalizedKeyword = null;
		}
		String normalizedSort = switch (sort) {
			case "title", "completed" -> sort;
			default -> "createdAt";
		};
		String normalizedDirection = "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
		model.addAttribute("todos", normalizedKeyword == null
				? todoService.findAll(normalizedSort, normalizedDirection)
				: todoService.searchByTitle(normalizedKeyword, normalizedSort, normalizedDirection));
		model.addAttribute("keyword", normalizedKeyword);
		model.addAttribute("sort", normalizedSort);
		model.addAttribute("direction", normalizedDirection);
		model.addAttribute("nextCreatedAtDirection",
				"createdAt".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		model.addAttribute("nextTitleDirection",
				"title".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		model.addAttribute("nextCompletedDirection",
				"completed".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		return "todo/list";
	}

	@GetMapping("/new")
	public String newTodo(Model model) {
		model.addAttribute("todoForm", new TodoForm());
		return "todo/form";
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable Long id, Model model) {
		Todo todo = todoService.findById(id);
		if (todo == null) {
			return "redirect:/todo";
		}
		model.addAttribute("todo", todo);
		return "todo/edit";
	}

	@PostMapping("/{id}/update")
	public String update(
			@PathVariable Long id,
			@RequestParam("title") String title,
			@RequestParam(name = "completed", defaultValue = "false") Boolean completed,
			RedirectAttributes redirectAttributes) {
		Todo todo = new Todo();
		todo.setId(id);
		todo.setTitle(title);
		todo.setCompleted(completed);
		if (todoService.update(todo)) {
			redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "更新に失敗しました");
		}
		return "redirect:/todo";
	}

	@PostMapping("/{id}/toggle")
	public String toggleCompleted(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		if (!todoService.toggleCompleted(id)) {
			redirectAttributes.addFlashAttribute("errorMessage", "完了状態の更新に失敗しました");
		}
		return "redirect:/todo";
	}

	@PostMapping("/confirm")
	public String confirm(
			@Valid @ModelAttribute("todoForm") TodoForm todoForm,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return "todo/form";
		}
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

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		if (todoService.deleteById(id)) {
			redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
		}
		return "redirect:/todo";
	}
}
