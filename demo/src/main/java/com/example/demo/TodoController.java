package com.example.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.demo.service.AttachmentRejectedException;
import com.example.demo.service.CategoryService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.TodoService;
import com.example.demo.service.TodoAttachmentService;
import com.example.demo.service.VirusDetectedException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/todo")
public class TodoController {

	private static final int PAGE_SIZE = 10;
	private static final DateTimeFormatter CSV_DATE_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Logger logger = LoggerFactory.getLogger(TodoController.class);

	private final TodoService todoService;
	private final CategoryService categoryService;
	private final TodoAttachmentService todoAttachmentService;
	private final FileStorageService fileStorageService;

	public TodoController(
			TodoService todoService,
			CategoryService categoryService,
			TodoAttachmentService todoAttachmentService,
			FileStorageService fileStorageService) {
		this.todoService = todoService;
		this.categoryService = categoryService;
		this.todoAttachmentService = todoAttachmentService;
		this.fileStorageService = fileStorageService;
	}

	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false, defaultValue = "createdAt") String sort,
			@RequestParam(required = false, defaultValue = "desc") String direction,
			@PageableDefault(size = PAGE_SIZE) Pageable pageable,
			@AuthenticationPrincipal AuthenticatedUser user,
			Model model) {
		String normalizedKeyword = normalizeKeyword(keyword);
		String normalizedSort = normalizeSort(sort);
		String normalizedDirection = normalizeDirection(direction);
		Pageable fixedPageable = PageRequest.of(pageable.getPageNumber(), PAGE_SIZE);
		Page<Todo> todoPage = todoService.findPage(
				normalizedKeyword, categoryId, fixedPageable, normalizedSort, normalizedDirection, scopeUserId(user));
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
		model.addAttribute("nextDeadlineDirection",
				"deadline".equals(normalizedSort) && "asc".equals(normalizedDirection) ? "desc" : "asc");
		return "todo/list";
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> export(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false, defaultValue = "createdAt") String sort,
			@RequestParam(required = false, defaultValue = "desc") String direction,
			@AuthenticationPrincipal AuthenticatedUser user) {
		List<Todo> todos = todoService.findForExport(
				normalizeKeyword(keyword),
				categoryId,
				normalizeSort(sort),
				normalizeDirection(direction), scopeUserId(user));
		byte[] csvBytes = ("\uFEFF" + toCsv(todos)).getBytes(StandardCharsets.UTF_8);
		String filename = "todo_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
		return ResponseEntity.ok().headers(headers).body(csvBytes);
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
		model.addAttribute("attachments", todoAttachmentService.findByTodoId(id));
		return "todo/edit";
	}

	@PostMapping(path = "/{todoId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String uploadAttachment(
			@PathVariable Long todoId,
			@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {
		if (todoService.findById(todoId) == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ToDoが見つかりません");
			return "redirect:/todo";
		}
		try {
			todoAttachmentService.upload(todoId, file);
			redirectAttributes.addFlashAttribute("successMessage", "ファイルを添付しました");
		} catch (VirusDetectedException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		} catch (AttachmentRejectedException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		} catch (IOException exception) {
			logger.error("Attachment upload failed for todo {}", todoId, exception);
			redirectAttributes.addFlashAttribute(
					"errorMessage", "ウイルスチェックまたはファイル保存に失敗しました");
		}
		return "redirect:/todo/" + todoId + "/edit";
	}

	@GetMapping("/attachments/{attachmentId}/download")
	public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) throws IOException {
		TodoAttachment attachment = todoAttachmentService.findById(attachmentId);
		if (attachment == null) {
			return ResponseEntity.notFound().build();
		}
		todoService.findById(attachment.getTodoId());
		try {
			Resource resource = fileStorageService.loadAsResource(attachment.getStoredFilename());
			ContentDisposition disposition = ContentDisposition.attachment()
					.filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
					.build();
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(attachment.getContentType()))
					.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
					.body(resource);
		} catch (NoSuchFileException exception) {
			logger.warn("Stored attachment file is missing for attachment {}", attachmentId);
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("/{todoId}/attachments/{attachmentId}/delete")
	public String deleteAttachment(
			@PathVariable Long todoId,
			@PathVariable Long attachmentId,
			RedirectAttributes redirectAttributes) {
		todoService.findById(todoId);
		try {
			if (todoAttachmentService.delete(todoId, attachmentId)) {
				redirectAttributes.addFlashAttribute("successMessage", "添付ファイルを削除しました");
			} else {
				redirectAttributes.addFlashAttribute("errorMessage", "添付ファイルが見つかりません");
			}
		} catch (IOException exception) {
			logger.error("Attachment deletion failed for attachment {}", attachmentId, exception);
			redirectAttributes.addFlashAttribute("errorMessage", "添付ファイルを削除できませんでした");
		}
		return "redirect:/todo/" + todoId + "/edit";
	}

	@PostMapping("/{id}/update")
	public String update(
			@PathVariable Long id,
			@RequestParam("title") String title,
			@RequestParam(name = "completed", defaultValue = "false") Boolean completed,
			@RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) LocalDate deadline,
			RedirectAttributes redirectAttributes,
			@AuthenticationPrincipal AuthenticatedUser user) {
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
		todo.setDeadline(deadline);
		if (todoService.update(id, todo, scopeUserId(user))) {
			redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "更新に失敗しました");
		}
		return "redirect:/todo";
	}

	@PostMapping("/{id}/toggle")
	public String toggleCompleted(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes,
			@AuthenticationPrincipal AuthenticatedUser user) {
		if (!todoService.toggleCompleted(id, scopeUserId(user))) {
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
			@RequestParam(required = false) String author,
			@RequestParam(name = "priority", defaultValue = "MEDIUM") Priority priority,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) LocalDate deadline,
			RedirectAttributes redirectAttributes,
			@AuthenticationPrincipal AuthenticatedUser user) {
		Category category = categoryId == null ? null : categoryService.findById(categoryId);
		if (categoryId != null && category == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "カテゴリを選択し直してください");
			return "redirect:/todo/new";
		}
		Todo todo = new Todo();
		todo.setUserId(user.getId());
		todo.setTitle(title);
		todo.setAuthor(author);
		todo.setCompleted(false);
		todo.setPriority(priority);
		todo.setCategory(category);
		todo.setDeadline(deadline);
		todoService.register(todo);
		return "redirect:/todo";
	}

	@DeleteMapping("/{id}")
	public String delete(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes,
			@AuthenticationPrincipal AuthenticatedUser user) {
		todoService.findById(id); // Enforce ownership before touching attachment files.
		try {
			todoAttachmentService.deleteAllForTodo(id);
		} catch (IOException exception) {
			logger.error("Could not remove attachments for todo {}", id, exception);
			redirectAttributes.addFlashAttribute("errorMessage", "添付ファイルを削除できませんでした");
			return "redirect:/todo";
		}
		if (todoService.deleteById(id, scopeUserId(user))) {
			redirectAttributes.addFlashAttribute("successMessage", "ToDoを削除しました");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました");
		}
		return "redirect:/todo";
	}

	@PostMapping("/bulk-delete")
	public String bulkDelete(
			@RequestParam(name = "ids", required = false) List<Long> ids,
			RedirectAttributes redirectAttributes,
			@AuthenticationPrincipal AuthenticatedUser user) {
		if (ids == null || ids.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "削除するToDoを選択してください");
			return "redirect:/todo";
		}
		// Validate the entire selection before removing any files or records.
		for (Long id : ids) {
			todoService.findById(id);
		}
		try {
			for (Long id : ids) {
				todoAttachmentService.deleteAllForTodo(id);
			}
		} catch (IOException exception) {
			logger.error("Could not remove attachments for selected todos", exception);
			redirectAttributes.addFlashAttribute("errorMessage", "添付ファイルを削除できませんでした");
			return "redirect:/todo";
		}
		int deletedCount = todoService.deleteByIds(ids, scopeUserId(user));
		if (deletedCount > 0) {
			redirectAttributes.addFlashAttribute(
					"successMessage", "選択したToDoを" + deletedCount + "件削除しました");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "選択したToDoを削除できませんでした");
		}
		return "redirect:/todo";
	}

	private static String normalizeKeyword(String keyword) {
		String normalizedKeyword = keyword == null ? null : keyword.trim();
		return normalizedKeyword == null || normalizedKeyword.isEmpty() ? null : normalizedKeyword;
	}

	private static Long scopeUserId(AuthenticatedUser user) {
		return user.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))
				? null : user.getId();
	}

	private static String normalizeSort(String sort) {
		return switch (sort) {
			case "title", "completed", "priority", "deadline" -> sort;
			default -> "createdAt";
		};
	}

	private static String normalizeDirection(String direction) {
		return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
	}

	private static String toCsv(List<Todo> todos) {
		StringBuilder csv = new StringBuilder();
		appendCsvRow(csv, "ID", "タイトル", "登録者", "ステータス", "作成日");
		for (Todo todo : todos) {
			String createdAt = todo.getCreatedAt() == null
					? ""
					: todo.getCreatedAt().format(CSV_DATE_FORMAT);
			appendCsvRow(
					csv,
					todo.getId() == null ? "" : todo.getId().toString(),
					todo.getTitle(),
					todo.getAuthor(),
					todo.isCompleted() ? "完了" : "未完了",
					createdAt);
		}
		return csv.toString();
	}

	private static void appendCsvRow(StringBuilder csv, String... values) {
		for (int index = 0; index < values.length; index++) {
			if (index > 0) {
				csv.append(',');
			}
			csv.append('"')
					.append(values[index] == null ? "" : values[index].replace("\"", "\"\""))
					.append('"');
		}
		csv.append("\r\n");
	}
}
