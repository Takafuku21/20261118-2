package com.example.demo;

import com.example.demo.service.TodoService;
import com.example.demo.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/todo")
public class TodoController {

	private static final int PAGE_SIZE = 10;

	private final TodoService todoService;
	private final CategoryService categoryService;

	public TodoController(TodoService todoService, CategoryService categoryService) {
		this.todoService = todoService;
		this.categoryService = categoryService;
	}

	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false, defaultValue = "createdAt") String sort,
			@RequestParam(required = false, defaultValue = "desc") String direction,
			@PageableDefault(size = PAGE_SIZE) Pageable pageable,
			Model model) {
		String normalizedKeyword = keyword == null ? null : keyword.trim();
		if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
			normalizedKeyword = null;
		}
		String normalizedSort = switch (sort) {
			case "title", "completed", "priority" -> sort;
			default -> "createdAt";
		};
		String normalizedDirection = "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
		Pageable fixedPageable = PageRequest.of(pageable.getPageNumber(), PAGE_SIZE);
		Page<Todo> todoPage = todoService.findPage(
				normalizedKeyword, categoryId, fixedPageable, normalizedSort, normalizedDirection);
		model.addAttribute("todos", todoPage.getContent());
		model.addAttribute("page", todoPage);
		model.addAttribute("startItem", todoPage.isEmpty()
				? 0
				: (long) todoPage.getNumber() * todoPage.getSize() + 1);
		model.addAttribute("endItem", Math.min(todoPage.getTotalElements(),
				(todoPage.getNumber() + 1L) * todoPage.getSize()));
		model.addAttribute("keyword", normalizedKeyword);
		model.addAttribute("categoryId", categoryId);
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("sort", normalizedSort);
		model.addAttribute("direction", normalizedDirection);
		model.addAttribute("nextCreatedAtDirection",
				"createdAt".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		model.addAttribute("nextTitleDirection",
				"title".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		model.addAttribute("nextCompletedDirection",
				"completed".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		model.addAttribute("nextPriorityDirection",
				"priority".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		return "todo/list";
	}

	@GetMapping("/new")
	public String newTodo(Model model) {
		model.addAttribute("todoForm", new TodoForm());
		model.addAttribute("categories", categoryService.findAll());
		return "todo/form";
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable Long id, Model model) {
		Todo todo = todoService.findById(id);
		if (todo == null) {
			return "redirect:/todo";
		}
		model.addAttribute("todo", todo);
		model.addAttribute("categories", categoryService.findAll());
		return "todo/edit";
	}

	@PostMapping("/{id}/update")
	public String update(
			@PathVariable Long id,
			@RequestParam("title") String title,
			@RequestParam(name = "completed", defaultValue = "false") Boolean completed,
			@RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
			@RequestParam(required = false) Long categoryId,
			RedirectAttributes redirectAttributes) {
		Category category = categoryId == null ? null : categoryService.findById(categoryId);
		if (categoryId != null && category == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "カテゴリを選択し直してください");
			return "redirect:/todo";
		}
		Todo todo = new Todo();
		todo.setId(id);
		todo.setTitle(title);
		todo.setCompleted(completed);
		todo.setPriority(priority);
		todo.setCategory(category);
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
			BindingResult bindingResult,
			Model model) {
		Category selectedCategory = todoForm.getCategoryId() == null
				? null
				: categoryService.findById(todoForm.getCategoryId());
		if (todoForm.getCategoryId() != null && selectedCategory == null) {
			bindingResult.rejectValue("categoryId", "category.notFound", "カテゴリを選択し直してください");
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("categories", categoryService.findAll());
			return "todo/form";
		}
		model.addAttribute("selectedCategory", selectedCategory);
		return "todo/confirm";
	}

	@PostMapping("/complete")
	public String complete(
			@RequestParam("title") String title,
			@RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
			@RequestParam(required = false) Long categoryId,
			RedirectAttributes redirectAttributes) {
		Category category = categoryId == null ? null : categoryService.findById(categoryId);
		if (categoryId != null && category == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "カテゴリを選択し直してください");
			return "redirect:/todo/new";
		}
		Todo todo = new Todo();
		todo.setTitle(title);
		todo.setCompleted(false);
		todo.setPriority(priority);
		todo.setCategory(category);
		todoService.register(todo);
		return "redirect:/todo";
	}

	@DeleteMapping("/{id}")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		if (todoService.deleteById(id)) {
			redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
		}
		return "redirect:/todo";
	}
}
