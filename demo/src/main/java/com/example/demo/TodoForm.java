package com.example.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TodoForm {

	@NotBlank(message = "作成者を入力してください")
	@Size(max = 50, message = "作成者は50文字以内で入力してください")
	private String author;

	@NotBlank(message = "タイトルを入力してください")
	@Size(max = 100, message = "タイトルは100文字以内で入力してください")
	private String title;

	@Size(max = 500, message = "詳細は500文字以内で入力してください")
	private String detail;

	private Priority priority = Priority.MEDIUM;
}
