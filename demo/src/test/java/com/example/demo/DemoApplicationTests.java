package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.mapper.TodoMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
		assertTrue(todoMapper.findAll().stream()
				.anyMatch(found -> found.getId().equals(todo.getId())
						&& found.getTitle().equals(todo.getTitle())
						&& found.getCompleted().equals(todo.getCompleted())));
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
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了状態")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(incompleteTodo.getId().toString())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("未完了一覧テスト")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了一覧テスト")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("未完了")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("操作")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"/todo/" + incompleteTodo.getId() + "/edit")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("btn-outline-primary btn-sm")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"/todo/" + incompleteTodo.getId() + "/delete")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("btn-danger btn-sm")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"return confirm('本当に削除しますか？');")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("ToDoがまだ登録されていません"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo/new\"")));
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
						.param("title", "更新後タイトル"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/todo"));

		assertEquals("更新後タイトル", todoMapper.findById(todo.getId()).getTitle());
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

		mockMvc.perform(post("/todo/{id}/delete", todoToDelete.getId()))
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
		mockMvc.perform(post("/todo/{id}/delete", Long.MAX_VALUE))
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
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"title\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"description\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"checkbox\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"done\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/todo/confirm\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo\"")));
	}

	@Test
	void todoConfirmPageDisplaysSubmittedFormValues() throws Exception {
		mockMvc.perform(post("/todo/confirm")
						.param("title", "サンプルタスク")
						.param("description", "タスクの説明")
						.param("done", "true"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登録内容の確認")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("サンプルタスク")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("タスクの説明")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("完了")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/todo/complete\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"title\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("readonly")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("history.back()")));
	}

	@Test
	void todoConfirmPageTreatsUncheckedDoneAsFalse() throws Exception {
		mockMvc.perform(post("/todo/confirm")
						.param("title", "サンプルタスク")
						.param("description", "タスクの説明"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("未完了")));
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
