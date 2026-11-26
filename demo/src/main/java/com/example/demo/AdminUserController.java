package com.example.demo;

import com.example.demo.service.AdminUserService;
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
}
