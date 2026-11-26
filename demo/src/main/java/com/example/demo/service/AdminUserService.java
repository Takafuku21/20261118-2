package com.example.demo.service;

import java.util.List;

import com.example.demo.UserAccount;
import com.example.demo.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
	private final UserMapper userMapper;

	public AdminUserService(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	@PreAuthorize("hasRole('ADMIN')")
	public List<UserAccount> findAll() {
		return userMapper.findAll();
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public boolean update(Long id, String role, boolean enabled, Long actingUserId) {
		if (id == null || !("ADMIN".equals(role) || "USER".equals(role))) {
			return false;
		}
		UserAccount target = userMapper.findById(id);
		if (target == null || id.equals(actingUserId)) {
			return false;
		}
		if ("ADMIN".equals(target.getRole()) && target.isEnabled()
				&& (!"ADMIN".equals(role) || !enabled) && userMapper.countActiveAdmins() <= 1) {
			return false;
		}
		target.setRole(role);
		target.setEnabled(enabled);
		return userMapper.updateAdminFields(target) == 1;
	}
}
