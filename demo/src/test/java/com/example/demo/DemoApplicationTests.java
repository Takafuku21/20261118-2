package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void todosTableIsInitializedAtStartup() {
		jdbcTemplate.queryForList("SELECT id, title, completed FROM todos");
	}

	@Test
	void helloPageDisplaysControllerMessage() throws Exception {
		mockMvc.perform(get("/hello"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hello! Spring Boot")));
	}

	@Test
	void todoListPageDisplaysTableAndNewTodoLink() throws Exception {
		mockMvc.perform(get("/todo"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登録済みのToDoはありません。")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo/new\"")));
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
	void todoCompletePageDisplaysSuccessMessageTitleAndListLink() throws Exception {
		mockMvc.perform(post("/todo/complete").param("title", "サンプルタスク"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登録が完了しました")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("サンプルタスク")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo\"")));
	}

}
