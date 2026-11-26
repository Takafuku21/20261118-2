package com.example.demo;

import java.nio.charset.StandardCharsets;

import com.example.demo.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public RegistrationController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("registrationForm", new UserRegistrationForm());
		return "register";
	}

	@PostMapping("/register")
	public String register(
			@Valid @ModelAttribute("registrationForm") UserRegistrationForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		String username = form.getUsername() == null ? "" : form.getUsername().trim();
		if (form.getPassword() != null
				&& form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
			bindingResult.rejectValue("password", "password.tooLong", "パスワードはUTF-8で72バイト以内にしてください。");
		}
		if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "確認用パスワードが一致しません。");
		}
		if (!username.isBlank() && userMapper.findByUsername(username) != null) {
			bindingResult.rejectValue("username", "username.duplicate", "このユーザー名は既に使用されています。");
		}
		if (bindingResult.hasErrors()) {
			return "register";
		}

		UserAccount user = new UserAccount();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setRole("USER");
		try {
			userMapper.insert(user);
		} catch (DuplicateKeyException exception) {
			bindingResult.rejectValue("username", "username.duplicate", "このユーザー名は既に使用されています。");
			return "register";
		}

		redirectAttributes.addFlashAttribute("registered", true);
		return "redirect:/login?registered";
	}
}
