package com.example.demo;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.mapper.TodoMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TodoMapper todoMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void todosTableIsInitializedAtStartup() {
		jdbcTemplate.queryForList("SELECT id, title, completed FROM todos");
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
	void isCompletedReflectsCompletionFlag() {
		Todo todo = new Todo();

		assertFalse(todo.isCompleted());
		todo.setCompleted(true);
		assertTrue(todo.isCompleted());
		todo.setCompleted(false);
		assertFalse(todo.isCompleted());
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
		todoMapper.insert(incompleteTodo);

		Todo completedTodo = new Todo();
		completedTodo.setTitle("完了一覧テスト");
		completedTodo.setCompleted(true);
		todoMapper.insert(completedTodo);

		mockMvc.perform(get("/todo"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ID")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("タイトル")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ステータス")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("作成日")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(incompleteTodo.getId().toString())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("未完了一覧テスト")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了一覧テスト")))
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
	void todoEditPageDisplaysExistingTitleAndUpdateAction() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("編集前タイトル");
		todo.setCompleted(false);
		todoMapper.insert(todo);

		mockMvc.perform(get("/todo/{id}/edit", todo.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"編集前タイトル\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"completed\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"action=\"/todo/" + todo.getId() + "/update\"")));
	}

	@Test
	void updatingTodoTitlePersistsAndRedirectsToList() throws Exception {
		Todo todo = new Todo();
		todo.setTitle("更新前タイトル");
		todo.setCompleted(false);
		todoMapper.insert(todo);

		mockMvc.perform(post("/todo/{id}/update", todo.getId())
						.param("title", "更新後タイトル")
						.param("completed", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"))
				.andExpect(flash().attribute("successMessage", "更新が完了しました"));

		Todo updatedTodo = todoMapper.findById(todo.getId());
		assertEquals("更新後タイトル", updatedTodo.getTitle());
		assertEquals(true, updatedTodo.getCompleted());
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
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"detail\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/todo/confirm\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo\"")));
	}

	@Test
	void todoConfirmPageDisplaysSubmittedFormValues() throws Exception {
		mockMvc.perform(post("/todo/confirm")
						.param("author", "作成者")
						.param("title", "サンプルタスク")
						.param("detail", "タスクの詳細"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登録内容の確認")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("作成者")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("サンプルタスク")))
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
	void todoCompleteRegistersTodoAndRedirectsToList() throws Exception {
		String title = "登録テスト";
		mockMvc.perform(post("/todo/complete").param("title", title))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"));

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM todos WHERE title = ?", Integer.class, title));
	}

}
