package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
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
				.andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"text\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo/confirm\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo\"")));
	}

	@Test
	void todoConfirmPageDisplaysSampleTitleAndNavigationLinks() throws Exception {
		mockMvc.perform(get("/todo/confirm"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登録内容の確認")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("サンプルタスク")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo/complete\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo/new\"")));
	}

	@Test
	void todoCompletePageDisplaysSuccessMessageAndListLink() throws Exception {
		mockMvc.perform(get("/todo/complete"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("登録が完了しました")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/todo\"")));
	}

}
