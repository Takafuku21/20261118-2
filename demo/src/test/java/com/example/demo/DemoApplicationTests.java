package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.mapper.CategoryMapper;
import com.example.demo.mapper.TodoAttachmentMapper;
import com.example.demo.mapper.TodoMapper;
import com.example.demo.service.VirusDetectedException;
import com.example.demo.service.VirusScanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.file-storage-dir=target/test-uploads")
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TodoMapper todoMapper;

	@Autowired
	private TodoAttachmentMapper todoAttachmentMapper;

	@Autowired
	private CategoryMapper categoryMapper;

	@MockitoBean
	private VirusScanner virusScanner;

	@Test
	void contextLoads() {
	}

	@Test
	void todosTableIsInitializedAtStartup() {
		jdbcTemplate.queryForList("SELECT id, title, completed, priority FROM todos");
	}

	@Test
	void categoryMasterIsInitialized() {
		assertEquals(List.of("仕事", "個人", "その他"),
				categoryMapper.findAll().stream().map(Category::getName).toList());
	}

	@Test
	void todoMapperInsertsAndFindsTodos() {
		Todo todo = new Todo();
		todo.setTitle("MyBatisテスト");
		todo.setCompleted(false);

		assertEquals(1, todoMapper.insert(todo));
		assertNotNull(todo.getId());
		assertTrue(todoMapper.findAll("createdAt", "desc").stream()
				.anyMatch(found -> found.getId().equals(todo.getId())
						&& found.getTitle().equals(todo.getTitle())
						&& found.isCompleted() == todo.isCompleted()));
	}

	@Test
	void todoMapperFindsTodoById() {
		Todo todo = new Todo();
		todo.setTitle("ID検索テスト");
		todo.setCompleted(false);
		todoMapper.insert(todo);

		Todo found = todoMapper.findById(todo.getId());

		assertNotNull(found);
		assertEquals(todo.getId(), found.getId());
		assertEquals(todo.getTitle(), found.getTitle());
		assertEquals(Priority.MEDIUM, found.getPriority());
	}

	@Test
	void todoMapperLoadsCategoryWithTodoUsingJoin() {
		Category category = categoryMapper.findAll().get(0);
		Todo todo = new Todo();
		todo.setTitle("JOINカテゴリ取得テスト");
		todo.setCategory(category);
		todoMapper.insert(todo);

		Todo found = todoMapper.findById(todo.getId());

		assertNotNull(found.getCategory());
		assertEquals(category.getId(), found.getCategory().getId());
		assertEquals(category.getName(), found.getCategory().getName());
		assertEquals(category.getColor(), found.getCategory().getColor());
	}

	@Test
	void todoMapperSearchesTitlesByPartialMatch() {
		Todo matchingTodo = new Todo();
		matchingTodo.setTitle("部分一致検索の対象");
		matchingTodo.setCompleted(false);
		todoMapper.insert(matchingTodo);

		Todo nonMatchingTodo = new Todo();
		nonMatchingTodo.setTitle("別のタイトル");
		nonMatchingTodo.setCompleted(false);
		todoMapper.insert(nonMatchingTodo);

		assertTrue(todoMapper.searchByTitle("一致検索", "createdAt", "desc").stream()
				.anyMatch(todo -> todo.getId().equals(matchingTodo.getId())));
		assertFalse(todoMapper.searchByTitle("一致検索", "createdAt", "desc").stream()
				.anyMatch(todo -> todo.getId().equals(nonMatchingTodo.getId())));
	}

	@Test
	void todoMapperSortsByTitleStatusAndCreationDate() {
		Todo newerTodo = new Todo();
		newerTodo.setTitle("Zソートテスト");
		newerTodo.setCompleted(true);
		todoMapper.insert(newerTodo);

		Todo olderTodo = new Todo();
		olderTodo.setTitle("Aソートテスト");
		olderTodo.setCompleted(false);
		todoMapper.insert(olderTodo);

		jdbcTemplate.update("UPDATE todos SET created_at = ? WHERE id = ?",
				Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 0, 0)), newerTodo.getId());
		jdbcTemplate.update("UPDATE todos SET created_at = ? WHERE id = ?",
				Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)), olderTodo.getId());

		assertEquals(List.of(olderTodo.getId(), newerTodo.getId()),
				todoMapper.findAll("title", "asc").stream()
						.filter(todo -> List.of(olderTodo.getId(), newerTodo.getId()).contains(todo.getId()))
						.map(Todo::getId)
						.toList());
		assertEquals(List.of(newerTodo.getId(), olderTodo.getId()),
				todoMapper.findAll("title", "desc").stream()
						.filter(todo -> List.of(olderTodo.getId(), newerTodo.getId()).contains(todo.getId()))
						.map(Todo::getId)
						.toList());
		assertEquals(List.of(olderTodo.getId(), newerTodo.getId()),
				todoMapper.findAll("completed", "asc").stream()
						.filter(todo -> List.of(olderTodo.getId(), newerTodo.getId()).contains(todo.getId()))
						.map(Todo::getId)
						.toList());
		assertEquals(List.of(olderTodo.getId(), newerTodo.getId()),
				todoMapper.findAll("createdAt", "asc").stream()
						.filter(todo -> List.of(olderTodo.getId(), newerTodo.getId()).contains(todo.getId()))
						.map(Todo::getId)
						.toList());
	}

	@Test
	void todoMapperSortsPrioritiesInPriorityOrder() {
		Todo lowPriorityTodo = new Todo();
		lowPriorityTodo.setTitle("低優先度");
		lowPriorityTodo.setPriority(Priority.LOW);
		todoMapper.insert(lowPriorityTodo);

		Todo mediumPriorityTodo = new Todo();
		mediumPriorityTodo.setTitle("中優先度");
		mediumPriorityTodo.setPriority(Priority.MEDIUM);
		todoMapper.insert(mediumPriorityTodo);

		Todo highPriorityTodo = new Todo();
		highPriorityTodo.setTitle("高優先度");
		highPriorityTodo.setPriority(Priority.HIGH);
		todoMapper.insert(highPriorityTodo);

		List<Long> targetIds = List.of(
				highPriorityTodo.getId(), mediumPriorityTodo.getId(), lowPriorityTodo.getId());
		assertEquals(targetIds, todoMapper.findAll("priority", "asc").stream()
				.filter(todo -> targetIds.contains(todo.getId()))
				.map(Todo::getId)
				.toList());
		assertEquals(List.of(lowPriorityTodo.getId(), mediumPriorityTodo.getId(), highPriorityTodo.getId()),
				todoMapper.findAll("priority", "desc").stream()
						.filter(todo -> targetIds.contains(todo.getId()))
						.map(Todo::getId)
						.toList());
	}

	@Test
	void todoMapperSortsByDeadline() {
		Todo laterTodo = new Todo();
		laterTodo.setTitle("期限遅い");
		laterTodo.setDeadline(LocalDate.now().plusDays(2));
		todoMapper.insert(laterTodo);

		Todo soonerTodo = new Todo();
		soonerTodo.setTitle("期限早い");
		soonerTodo.setDeadline(LocalDate.now().plusDays(1));
		todoMapper.insert(soonerTodo);

		List<Long> targetIds = List.of(soonerTodo.getId(), laterTodo.getId());
		assertEquals(targetIds, todoMapper.findAll("deadline", "asc").stream()
				.filter(todo -> targetIds.contains(todo.getId()))
				.map(Todo::getId)
				.toList());
		assertEquals(List.of(laterTodo.getId(), soonerTodo.getId()),
				todoMapper.findAll("deadline", "desc").stream()
						.filter(todo -> targetIds.contains(todo.getId()))
						.map(Todo::getId)
						.toList());
	}

	@Test
	void isCompletedReflectsCompletionFlag() {
		Todo todo = new Todo();

		assertFalse(todo.isCompleted());
		todo.setCompleted(true);
		assertTrue(todo.isCompleted());
		todo.setCompleted(false);
		assertFalse(todo.isCompleted());
	}

	@Test
	void deadlineWarningsDistinguishOverdueAndUpcomingDates() {
		Todo todo = new Todo();

		assertFalse(todo.isOverdue());
		assertFalse(todo.isNearDeadline());

		todo.setDeadline(LocalDate.now().minusDays(1));
		assertTrue(todo.isOverdue());
		assertFalse(todo.isNearDeadline());

		todo.setDeadline(LocalDate.now());
		assertFalse(todo.isOverdue());
		assertTrue(todo.isNearDeadline());

		todo.setDeadline(LocalDate.now().plusDays(3));
		assertFalse(todo.isOverdue());
		assertTrue(todo.isNearDeadline());

		todo.setDeadline(LocalDate.now().plusDays(4));
		assertFalse(todo.isOverdue());
		assertFalse(todo.isNearDeadline());
	}

	@Test
	void todoMapperDeletesTodoById() {
		Todo todo = new Todo();
		todo.setTitle("削除Mapperテスト");
		todo.setCompleted(false);
		todoMapper.insert(todo);

		assertEquals(1, todoMapper.deleteById(todo.getId()));
		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id = ?", Integer.class, todo.getId()));
	}

	@Test
	void todoMapperDeletesTodosByIdsAndPreservesUnselectedTodos() {
		Todo firstTodo = new Todo();
		firstTodo.setTitle("一括削除Mapper対象1");
		todoMapper.insert(firstTodo);

		Todo secondTodo = new Todo();
		secondTodo.setTitle("一括削除Mapper対象2");
		todoMapper.insert(secondTodo);

		Todo remainingTodo = new Todo();
		remainingTodo.setTitle("一括削除Mapper対象外");
		todoMapper.insert(remainingTodo);

		assertEquals(2, todoMapper.deleteByIds(List.of(firstTodo.getId(), secondTodo.getId())));
		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id IN (?, ?)",
				Integer.class, firstTodo.getId(), secondTodo.getId()));
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id = ?", Integer.class, remainingTodo.getId()));
	}

	@Test
	void todoListShowsEmptyStateWhenThereAreNoTodos() throws Exception {
		jdbcTemplate.update("DELETE FROM todos");

		mockMvc.perform(get("/todo"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ToDoがまだ登録されていません")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("新規作成ボタンから")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo/new\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("<table"))));
	}

	@Test
	void helloPageDisplaysControllerMessage() throws Exception {
		mockMvc.perform(get("/hello"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hello! Spring Boot")));
	}

	@Test
	void rootRedirectsToTodoList() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"));
	}

	@Test
	void todoListPageDisplaysDatabaseTodosAndCompletionStatus() throws Exception {
		Todo incompleteTodo = new Todo();
		incompleteTodo.setTitle("未完了一覧テスト");
		incompleteTodo.setCompleted(false);
		incompleteTodo.setDeadline(LocalDate.now().minusDays(1));
		todoMapper.insert(incompleteTodo);

		Todo nearDeadlineTodo = new Todo();
		nearDeadlineTodo.setTitle("期限間近一覧テスト");
		nearDeadlineTodo.setDeadline(LocalDate.now().plusDays(2));
		todoMapper.insert(nearDeadlineTodo);

		Todo completedTodo = new Todo();
		completedTodo.setTitle("完了一覧テスト");
		completedTodo.setCompleted(true);
		completedTodo.setPriority(Priority.HIGH);
		completedTodo.setCategory(categoryMapper.findAll().get(0));
		todoMapper.insert(completedTodo);

		mockMvc.perform(get("/todo"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ID")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("タイトル")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ステータス")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("作成日")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("期限日")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"select-all-todos\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"ids\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"form=\"bulk-delete-form\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"id=\"bulk-delete-trigger\" class=\"btn btn-danger\" disabled")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"選択した項目を削除")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"id=\"bulk-delete-confirmation\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("カテゴリ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(incompleteTodo.getId().toString())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("未完了一覧テスト")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了一覧テスト")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("期限間近一覧テスト")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("overdue-row")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("warning-row")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						LocalDate.now().minusDays(1).format(
								java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日")))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"class=\"badge bg-danger\">高</span>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"class=\"badge bg-warning text-dark\">中</span>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"background-color: #0d6efd;")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("仕事</span>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"class=\"badge bg-success\">完了</span>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"class=\"badge bg-secondary\">未完了</span>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"completed-row\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("未完了")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("操作")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"/todo/" + incompleteTodo.getId() + "/edit")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("btn-outline-primary btn-sm")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"/todo/" + incompleteTodo.getId() + "/toggle")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了にする")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("未完了に戻す")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"text-decoration-line-through text-muted")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"/todo/" + incompleteTodo.getId() + "\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("btn-danger btn-sm")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-delete-trigger")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"data-todo-title=\"未完了一覧テスト\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"id=\"delete-confirmation\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"本当に削除しますか？")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("requestSubmit()")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("ToDoがまだ登録されていません"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo/new\"")));
	}

	@Test
	void todoListSortLinksPreserveSearchAndShowCurrentDirection() throws Exception {
		Todo matchingTodo = new Todo();
		matchingTodo.setTitle("needle sort test");
		matchingTodo.setCompleted(false);
		todoMapper.insert(matchingTodo);

		mockMvc.perform(get("/todo")
						.param("keyword", "needle")
						.param("sort", "title")
						.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"needle\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("タイトル")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("▲")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("keyword=needle")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("sort=completed")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("sort=priority")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("sort=deadline")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("direction=asc")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("sort=title&amp;direction=desc")));
	}

	@Test
	void todoListSearchDisplaysMatchingTodosAndPreservesKeyword() throws Exception {
		Todo matchingTodo = new Todo();
		matchingTodo.setTitle("検索フォーム表示対象");
		matchingTodo.setCompleted(false);
		todoMapper.insert(matchingTodo);

		Todo nonMatchingTodo = new Todo();
		nonMatchingTodo.setTitle("検索対象外タイトル");
		nonMatchingTodo.setCompleted(false);
		todoMapper.insert(nonMatchingTodo);

		mockMvc.perform(get("/todo").param("keyword", "フォーム表示"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"value=\"フォーム表示\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"検索結果")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("全<span>1</span>件中")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"1</span>-<span>1</span>件を表示")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("検索フォーム表示対象")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("検索対象外タイトル"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"href=\"/todo?page=0&amp;sort=createdAt&amp;direction=desc\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("クリア")));
	}

	@Test
	void todoSearchResultsArePaginatedAndPreserveSearchAndSortParameters() throws Exception {
		jdbcTemplate.update("DELETE FROM todos");
		for (int i = 1; i <= 21; i++) {
			Todo todo = new Todo();
			todo.setTitle("ページング検索対象" + "%02d".formatted(i));
			todo.setCompleted(false);
			todoMapper.insert(todo);
		}
		Todo unrelatedTodo = new Todo();
		unrelatedTodo.setTitle("ページング対象外");
		unrelatedTodo.setCompleted(false);
		todoMapper.insert(unrelatedTodo);

		mockMvc.perform(get("/todo")
						.param("keyword", "ページング検索対象")
						.param("sort", "title")
						.param("direction", "asc")
						.param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("全<span>21</span>件中")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"11</span>-<span>20</span>件を表示")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ページング検索対象11")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ページング検索対象20")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("ページング検索対象10"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("ページング対象外"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"class=\"page-item active\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("aria-current=\"page\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("前へ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("次へ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("keyword=")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("sort=title")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("direction=asc")));
	}

	@Test
	void todoListFiltersByCategoryAlongWithTitleSearch() throws Exception {
		List<Category> categories = categoryMapper.findAll();
		Category selectedCategory = categories.get(0);
		Category otherCategory = categories.get(1);

		Todo matchingTodo = new Todo();
		matchingTodo.setTitle("カテゴリ検索対象");
		matchingTodo.setCategory(selectedCategory);
		todoMapper.insert(matchingTodo);

		Todo otherTodo = new Todo();
		otherTodo.setTitle("カテゴリ検索対象外");
		otherTodo.setCategory(otherCategory);
		todoMapper.insert(otherTodo);

		mockMvc.perform(get("/todo")
						.param("keyword", "カテゴリ検索")
						.param("categoryId", selectedCategory.getId().toString())
						.param("sort", "title")
						.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("カテゴリ検索対象</span>")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("カテゴリ検索対象外</span>"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"selected=\"selected\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"categoryId=" + selectedCategory.getId())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("sort=title")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"/todo/export?keyword=%E3%82%AB%E3%83%86%E3%82%B4%E3%83%AA%E6%A4%9C%E7%B4%A2&amp;categoryId="
								+ selectedCategory.getId() + "&amp;sort=title")));
	}

	@Test
	void csvExportIncludesUtf8BomEscapesFieldsAndPreservesFilters() throws Exception {
		List<Category> categories = categoryMapper.findAll();
		Category selectedCategory = categories.get(0);

		Todo matchingTodo = new Todo();
		matchingTodo.setTitle("CSV, \"タイトル\"");
		matchingTodo.setAuthor("作成者, \"A\"");
		matchingTodo.setCompleted(true);
		matchingTodo.setCategory(selectedCategory);
		todoMapper.insert(matchingTodo);

		Todo wrongCategoryTodo = new Todo();
		wrongCategoryTodo.setTitle("CSVカテゴリ対象外");
		wrongCategoryTodo.setCategory(categories.get(1));
		todoMapper.insert(wrongCategoryTodo);

		Todo wrongKeywordTodo = new Todo();
		wrongKeywordTodo.setTitle("別キーワード");
		wrongKeywordTodo.setCategory(selectedCategory);
		todoMapper.insert(wrongKeywordTodo);

		var result = mockMvc.perform(get("/todo/export")
						.param("keyword", "CSV")
						.param("categoryId", selectedCategory.getId().toString())
						.param("sort", "title")
						.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(
						org.springframework.http.MediaType.parseMediaType("text/csv")))
				.andReturn();

		byte[] responseBytes = result.getResponse().getContentAsByteArray();
		assertEquals(0xEF, responseBytes[0] & 0xFF);
		assertEquals(0xBB, responseBytes[1] & 0xFF);
		assertEquals(0xBF, responseBytes[2] & 0xFF);

		String csv = new String(responseBytes, StandardCharsets.UTF_8);
		assertTrue(csv.startsWith("\uFEFF"));
		assertTrue(csv.contains("\"ID\",\"タイトル\",\"登録者\",\"ステータス\",\"作成日\"\r\n"));
		assertTrue(csv.contains("\"" + matchingTodo.getId()
				+ "\",\"CSV, \"\"タイトル\"\"\",\"作成者, \"\"A\"\"\",\"完了\",\""));
		assertFalse(csv.contains("CSVカテゴリ対象外"));
		assertFalse(csv.contains("別キーワード"));

		String contentDisposition = result.getResponse().getHeader("Content-Disposition");
		assertTrue(contentDisposition != null && contentDisposition.contains(
				"todo_" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
						+ ".csv"));
	}

	@Test
	void todoEditPageDisplaysExistingTitleAndUpdateAction() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("編集前タイトル");
		todo.setCompleted(false);
		todo.setCategory(categoryMapper.findAll().get(0));
		todo.setDeadline(LocalDate.of(2026, 12, 1));
		todoMapper.insert(todo);

		mockMvc.perform(get("/todo/{id}/edit", todo.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"編集前タイトル\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"priority\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("selected")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"categoryId\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("仕事")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"value=\"2026-12-01\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"completed\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"enctype=\"multipart/form-data\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"name=\"file\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"accept=\".pdf,.jpg,.jpeg,.png,.txt")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"action=\"/todo/" + todo.getId() + "/update\"")));
	}

	@Test
	void attachmentUploadDownloadAndDeleteUseSanitizedMetadata() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("添付ファイルテスト");
		todoMapper.insert(todo);
		byte[] fileContents = "安全なテキスト".getBytes(StandardCharsets.UTF_8);

		mockMvc.perform(multipart("/todo/{todoId}/attachments", todo.getId())
						.file(new MockMultipartFile(
								"file", "../../secret.txt", "text/plain", fileContents)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo/" + todo.getId() + "/edit"))
				.andExpect(flash().attribute("successMessage", "ファイルを添付しました"));

		TodoAttachment attachment = todoAttachmentMapper.findByTodoId(todo.getId()).get(0);
		assertEquals("secret.txt", attachment.getOriginalFilename());
		assertTrue(attachment.getStoredFilename().endsWith(".txt"));
		assertTrue(Files.exists(Path.of("target/test-uploads", attachment.getStoredFilename())));

		mockMvc.perform(get("/todo/{id}/edit", todo.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("secret.txt")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"/todo/attachments/" + attachment.getId() + "/download")));

		mockMvc.perform(get("/todo/attachments/{id}/download", attachment.getId()))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition",
						org.hamcrest.Matchers.containsString("secret.txt")))
				.andExpect(content().bytes(fileContents));

		mockMvc.perform(post("/todo/{todoId}/attachments/{attachmentId}/delete",
						todo.getId(), attachment.getId()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo/" + todo.getId() + "/edit"))
				.andExpect(flash().attribute("successMessage", "添付ファイルを削除しました"));

		assertNull(todoAttachmentMapper.findById(attachment.getId()));
		assertFalse(Files.exists(Path.of("target/test-uploads", attachment.getStoredFilename())));
	}

	@Test
	void deletingTodoAlsoRemovesItsAttachmentFiles() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("添付付きToDo削除テスト");
		todoMapper.insert(todo);
		mockMvc.perform(multipart("/todo/{todoId}/attachments", todo.getId())
						.file(new MockMultipartFile(
								"file", "attached.txt", "text/plain", "添付内容".getBytes(StandardCharsets.UTF_8))))
				.andExpect(status().is3xxRedirection());

		TodoAttachment attachment = todoAttachmentMapper.findByTodoId(todo.getId()).get(0);
		Path storedFile = Path.of("target/test-uploads", attachment.getStoredFilename());
		assertTrue(Files.exists(storedFile));

		mockMvc.perform(post("/todo/{id}", todo.getId()).param("_method", "delete"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("successMessage", "ToDoを削除しました"));

		assertFalse(Files.exists(storedFile));
		assertNull(todoAttachmentMapper.findById(attachment.getId()));
	}

	@Test
	void attachmentUploadRejectsDisallowedTypesAndMismatchedContent() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("添付形式チェックテスト");
		todoMapper.insert(todo);

		mockMvc.perform(multipart("/todo/{todoId}/attachments", todo.getId())
						.file(new MockMultipartFile(
								"file", "malware.exe", "application/octet-stream", new byte[] {1, 2, 3})))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("errorMessage",
						"許可されている形式はPDF、JPEG、PNG、TXTです"));

		mockMvc.perform(multipart("/todo/{todoId}/attachments", todo.getId())
						.file(new MockMultipartFile(
								"file", "picture.png", "image/png", "not a PNG".getBytes(StandardCharsets.UTF_8))))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("errorMessage", "ファイル内容が拡張子と一致しません"));

		assertTrue(todoAttachmentMapper.findByTodoId(todo.getId()).isEmpty());
	}

	@Test
	void attachmentUploadRejectsFilesFlaggedByVirusScanner() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("ウイルス検査テスト");
		todoMapper.insert(todo);
		doThrow(new VirusDetectedException()).when(virusScanner).scan(any(byte[].class));

		mockMvc.perform(multipart("/todo/{todoId}/attachments", todo.getId())
						.file(new MockMultipartFile(
								"file", "infected.txt", "text/plain", "malware".getBytes(StandardCharsets.UTF_8))))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("errorMessage",
						"ウイルスが検出されたため、添付ファイルを拒否しました"));

		assertTrue(todoAttachmentMapper.findByTodoId(todo.getId()).isEmpty());
	}

	@Test
	void updatingTodoTitlePersistsAndRedirectsToList() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("更新前タイトル");
		todo.setCompleted(false);
		Category category = categoryMapper.findAll().get(0);
		todoMapper.insert(todo);

		mockMvc.perform(post("/todo/{id}/update", todo.getId())
						.param("title", "更新後タイトル")
						.param("completed", "true")
						.param("priority", "HIGH")
						.param("categoryId", category.getId().toString())
						.param("deadline", "2026-12-31"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"))
				.andExpect(flash().attribute("successMessage", "更新が完了しました"));

		Todo updatedTodo = todoMapper.findById(todo.getId());
		assertEquals("更新後タイトル", updatedTodo.getTitle());
		assertEquals(true, updatedTodo.getCompleted());
		assertEquals(Priority.HIGH, updatedTodo.getPriority());
		assertEquals(category.getId(), updatedTodo.getCategory().getId());
		assertEquals(LocalDate.of(2026, 12, 31), updatedTodo.getDeadline());
	}

	@Test
	void updatingMissingTodoShowsErrorFlashMessage() throws Exception {
		mockMvc.perform(post("/todo/{id}/update", Long.MAX_VALUE)
						.param("title", "存在しないToDo"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"))
				.andExpect(flash().attribute("errorMessage", "更新に失敗しました"));
	}

	@Test
	void togglingTodoCompletionFlipsStateAndRedirectsToList() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("完了状態切替テスト");
		todo.setCompleted(false);
		todoMapper.insert(todo);

		mockMvc.perform(post("/todo/{id}/toggle", todo.getId()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"));
		assertTrue(todoMapper.findById(todo.getId()).getCompleted());

		mockMvc.perform(post("/todo/{id}/toggle", todo.getId()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"));
		assertEquals(false, todoMapper.findById(todo.getId()).getCompleted());
	}

	@Test
	void deletingTodoRedirectsToListAndPreservesOtherTodos() throws Exception {
		Todo todoToDelete = new Todo();
		todoToDelete.setTitle("削除対象テスト");
		todoToDelete.setCompleted(false);
		todoMapper.insert(todoToDelete);

		Todo otherTodo = new Todo();
		otherTodo.setTitle("削除対象外テスト");
		otherTodo.setCompleted(false);
		todoMapper.insert(otherTodo);

		mockMvc.perform(post("/todo/{id}", todoToDelete.getId())
						.param("_method", "delete"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"))
				.andExpect(flash().attribute("successMessage", "ToDoを削除しました"));

		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id = ?", Integer.class, todoToDelete.getId()));
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id = ?", Integer.class, otherTodo.getId()));
	}

	@Test
	void deletingMissingTodoShowsErrorFlashMessage() throws Exception {
		mockMvc.perform(delete("/todo/{id}", Long.MAX_VALUE))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"))
				.andExpect(flash().attribute("errorMessage", "削除に失敗しました"));
	}

	@Test
	void bulkDeletingSelectedTodosRemovesOnlySelectedIds() throws Exception {
		Todo firstTodo = new Todo();
		firstTodo.setTitle("一括削除対象1");
		todoMapper.insert(firstTodo);

		Todo secondTodo = new Todo();
		secondTodo.setTitle("一括削除対象2");
		todoMapper.insert(secondTodo);

		Todo remainingTodo = new Todo();
		remainingTodo.setTitle("一括削除対象外");
		todoMapper.insert(remainingTodo);

		mockMvc.perform(post("/todo/bulk-delete")
						.param("ids", firstTodo.getId().toString(), secondTodo.getId().toString()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"))
				.andExpect(flash().attribute("successMessage", "選択したToDoを2件削除しました"));

		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id IN (?, ?)",
				Integer.class, firstTodo.getId(), secondTodo.getId()));
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id = ?", Integer.class, remainingTodo.getId()));
	}

	@Test
	void bulkDeleteWithoutSelectedIdsDoesNotDeleteTodos() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("未選択一括削除テスト");
		todoMapper.insert(todo);

		mockMvc.perform(post("/todo/bulk-delete"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"))
				.andExpect(flash().attribute("errorMessage", "削除するToDoを選択してください"));

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE id = ?", Integer.class, todo.getId()));
	}

	@Test
	void todoListDisplaysFlashMessagesWithAutoDismiss() throws Exception {
		mockMvc.perform(get("/todo").flashAttr("successMessage", "ToDoを削除しました"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("alert-success")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ToDoを削除しました")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("setTimeout")));

		mockMvc.perform(get("/todo").flashAttr("errorMessage", "削除に失敗しました"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("alert-danger")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("削除に失敗しました")));
	}

	@Test
	void todoNewPageDisplaysTitleInputAndNavigationLinks() throws Exception {
		mockMvc.perform(get("/todo/new"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"author\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"title\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"priority\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"categoryId\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"deadline\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"date\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"HIGH\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("高")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("中")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("低")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("仕事")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"detail\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/todo/confirm\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo\"")));
	}

	@Test
	void todoConfirmPageDisplaysSubmittedFormValues() throws Exception {
		Category category = categoryMapper.findAll().get(0);
		mockMvc.perform(post("/todo/confirm")
						.param("author", "作成者")
						.param("title", "サンプルタスク")
						.param("priority", "HIGH")
						.param("categoryId", category.getId().toString())
						.param("deadline", "2026-12-24")
						.param("detail", "タスクの詳細"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登録内容の確認")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("作成者")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("サンプルタスク")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("優先度")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"HIGH\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(">高</p>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("カテゴリ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(">仕事</p>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"name=\"categoryId\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"name=\"author\" value=\"作成者\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("2026年12月24日")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"name=\"deadline\" value=\"2026-12-24\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("タスクの詳細")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/todo/complete\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"title\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("readonly")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("history.back()")));
	}

	@Test
	void todoConfirmPageReturnsFormWithFieldErrors() throws Exception {
		mockMvc.perform(post("/todo/confirm")
						.param("author", "")
						.param("title", "")
						.param("detail", "d".repeat(501)))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("作成者を入力してください")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("タイトルを入力してください")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("詳細は500文字以内で入力してください")));
	}

	@Test
	void todoConfirmRejectsValuesOverMaximumLengths() throws Exception {
		mockMvc.perform(post("/todo/confirm")
						.param("author", "a".repeat(51))
						.param("title", "t".repeat(101))
						.param("detail", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("作成者は50文字以内で入力してください")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("タイトルは100文字以内で入力してください")));
	}

	@Test
	void todoCompleteRegistersTodoWithPriorityCategoryAndDeadline() throws Exception {
		String title = "登録テスト";
		Category category = categoryMapper.findAll().get(0);
		mockMvc.perform(post("/todo/complete")
						.param("title", title)
						.param("author", "登録者テスト")
						.param("priority", "LOW")
						.param("categoryId", category.getId().toString())
						.param("deadline", "2026-12-25"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"));

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE title = ?", Integer.class, title));
		assertEquals("LOW", jdbcTemplate.queryForObject(
				"SELECT priority FROM todos WHERE title = ?", String.class, title));
		assertEquals(category.getId(), jdbcTemplate.queryForObject(
				"SELECT category_id FROM todos WHERE title = ?", Long.class, title));
		Todo registeredTodo = todoMapper.findAll("title", "asc").stream()
				.filter(todo -> todo.getTitle().equals(title))
				.findFirst()
				.orElseThrow();
		assertEquals("登録者テスト", registeredTodo.getAuthor());
		assertEquals(LocalDate.of(2026, 12, 25), registeredTodo.getDeadline());
	}

}
