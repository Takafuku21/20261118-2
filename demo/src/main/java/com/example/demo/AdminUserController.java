package com.example.demo;

import com.example.demo.service.AdminUserService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
	private final AdminUserService adminUserService;

	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("users", adminUserService.findAll());
		return "admin/users";
	}

	@GetMapping("/new")
	public String newUser(Model model) {
		model.addAttribute("registrationForm", new UserRegistrationForm());
		model.addAttribute("role", "USER");
		model.addAttribute("enabled", true);
		return "admin/user-create";
	}

	@PostMapping("/create")
	public String create(
			@Valid @org.springframework.web.bind.annotation.ModelAttribute("registrationForm") UserRegistrationForm form,
			org.springframework.validation.BindingResult bindingResult,
			@RequestParam(defaultValue = "USER") String role,
			@RequestParam(defaultValue = "false") boolean enabled,
			Model model,
			RedirectAttributes redirectAttributes) {
		String username = form.getUsername() == null ? "" : form.getUsername().trim();
		if (form.getPassword() != null && form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
			bindingResult.rejectValue("password", "password.tooLong", "Password must be no longer than 72 UTF-8 bytes.");
		}
		if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
		}
		if (!"USER".equals(role) && !"ADMIN".equals(role)) {
			bindingResult.reject("role.invalid", "Select USER or ADMIN.");
		}
		if (bindingResult.hasErrors()) {
			modelCreateState(role, enabled, model);
			return "admin/user-create";
		}
		try {
			if (!adminUserService.create(username, form.getPassword(), role, enabled)) {
				bindingResult.rejectValue("username", "username.duplicate", "That username is already in use.");
				modelCreateState(role, enabled, model);
				return "admin/user-create";
			}
		} catch (DuplicateKeyException exception) {
			bindingResult.rejectValue("username", "username.duplicate", "That username is already in use.");
			modelCreateState(role, enabled, model);
			return "admin/user-create";
		}
		redirectAttributes.addFlashAttribute("successMessage", "User created.");
		return "redirect:/admin/users";
	}

	@GetMapping("/{id}/edit")
	public String edit(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
		UserAccount user = adminUserService.findById(id);
		if (user == null) return "redirect:/admin/users";
		model.addAttribute("user", user);
		return "admin/user-edit";
	}

	@PostMapping("/{id}/edit")
	public String edit(
			@org.springframework.web.bind.annotation.PathVariable Long id,
			@RequestParam String role,
			@RequestParam(defaultValue = "false") boolean enabled,
			@RequestParam(required = false) String newPassword,
			@RequestParam(required = false) String confirmPassword,
			Model model,
			RedirectAttributes redirectAttributes) {
		UserAccount user = adminUserService.findById(id);
		if (user == null) return "redirect:/admin/users";
		if (!"USER".equals(role) && !"ADMIN".equals(role)) {
			model.addAttribute("errorMessage", "Select USER or ADMIN.");
		} else if (newPassword != null && !newPassword.isBlank()
				&& (newPassword.length() < 8 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72)) {
			model.addAttribute("errorMessage", "New password must be at least 8 characters and at most 72 UTF-8 bytes.");
		} else if (newPassword != null && !newPassword.isBlank() && !newPassword.equals(confirmPassword)) {
			model.addAttribute("errorMessage", "Passwords do not match.");
		}
		if (model.containsAttribute("errorMessage")) {
			user.setRole(role);
			user.setEnabled(enabled);
			model.addAttribute("user", user);
			return "admin/user-edit";
		}
		if (!adminUserService.updateAccount(id, role, enabled, newPassword)) {
			model.addAttribute("errorMessage", "Could not update user. The last active admin must remain enabled.");
			user.setRole(role);
			user.setEnabled(enabled);
			model.addAttribute("user", user);
			return "admin/user-edit";
		}
		redirectAttributes.addFlashAttribute("successMessage", "User updated.");
		return "redirect:/admin/users";
	}

	@PostMapping("/{id}/delete")
	public String delete(
			@org.springframework.web.bind.annotation.PathVariable Long id,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			RedirectAttributes redirectAttributes) {
		if (adminUserService.delete(id, currentUser.getId())) {
			redirectAttributes.addFlashAttribute("successMessage", "User and related ToDos deleted.");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Could not delete this user. You cannot delete yourself or the last active admin.");
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/update")
	public String update(
			@RequestParam Long id,
			@RequestParam String role,
			@RequestParam(defaultValue = "false") boolean enabled,
			@AuthenticationPrincipal AuthenticatedUser currentUser,
			RedirectAttributes redirectAttributes) {
		boolean updated = adminUserService.update(id, role, enabled, currentUser.getId());
		redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage", updated
				? "User settings updated."
				: "Could not update user. You cannot edit your own account or disable the last active admin.");
		return "redirect:/admin/users";
	}

	private static void modelCreateState(String role, boolean enabled, Model model) {
		model.addAttribute("role", role);
		model.addAttribute("enabled", enabled);
	}
}
