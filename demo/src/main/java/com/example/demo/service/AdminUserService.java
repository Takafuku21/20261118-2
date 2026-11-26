package com.example.demo.service;

import java.util.List;
import java.io.IOException;

import com.example.demo.UserAccount;
import com.example.demo.mapper.TodoAttachmentMapper;
import com.example.demo.mapper.TodoMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AdminUserService {
	private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);
	private final UserMapper userMapper;
	private final TodoMapper todoMapper;
	private final TodoAttachmentMapper attachmentMapper;
	private final PasswordEncoder passwordEncoder;
	private final FileStorageService fileStorageService;

	public AdminUserService(UserMapper userMapper, TodoMapper todoMapper, TodoAttachmentMapper attachmentMapper,
			PasswordEncoder passwordEncoder, FileStorageService fileStorageService) {
		this.userMapper = userMapper;
		this.todoMapper = todoMapper;
		this.attachmentMapper = attachmentMapper;
		this.passwordEncoder = passwordEncoder;
		this.fileStorageService = fileStorageService;
	}

	@PreAuthorize("hasRole('ADMIN')")
	public List<UserAccount> findAll() {
		return userMapper.findAll();
	}

	@PreAuthorize("hasRole('ADMIN')")
	public UserAccount findById(Long id) {
		return userMapper.findById(id);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public boolean create(String username, String rawPassword, String role, boolean enabled) {
		if (username == null || username.isBlank() || rawPassword == null
				|| !("ADMIN".equals(role) || "USER".equals(role)) || userMapper.findByUsername(username) != null) {
			return false;
		}
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.setPassword(passwordEncoder.encode(rawPassword));
		account.setRole(role);
		account.setEnabled(enabled);
		return userMapper.insert(account) == 1;
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

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public boolean updateAccount(Long id, String role, boolean enabled, String rawPassword) {
		if (id == null || !("ADMIN".equals(role) || "USER".equals(role))) {
			return false;
		}
		UserAccount target = userMapper.findById(id);
		if (target == null || isLastActiveAdmin(target, role, enabled)) {
			return false;
		}
		target.setRole(role);
		target.setEnabled(enabled);
		if (rawPassword == null || rawPassword.isBlank()) {
			return userMapper.updateAdminFields(target) == 1;
		}
		target.setPassword(passwordEncoder.encode(rawPassword));
		return userMapper.updateAdminAccount(target) == 1;
	}

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public boolean delete(Long id, Long actingUserId) {
		if (id == null || id.equals(actingUserId)) {
			return false;
		}
		UserAccount target = userMapper.findById(id);
		if (target == null || isLastActiveAdmin(target, null, false)) {
			return false;
		}
		List<String> storedFiles = attachmentMapper.findStoredFilenamesByUserId(id);
		todoMapper.deleteAllByUserId(id);
		if (userMapper.deleteById(id) != 1) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return false;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				for (String storedFile : storedFiles) {
					try {
						fileStorageService.deleteStoredFile(storedFile);
					} catch (IOException | RuntimeException exception) {
						logger.warn("Could not remove attachment file {} after deleting user {}", storedFile, id, exception);
					}
				}
			}
		});
		return true;
	}

	private boolean isLastActiveAdmin(UserAccount target, String nextRole, boolean nextEnabled) {
		return "ADMIN".equals(target.getRole()) && target.isEnabled()
				&& (!"ADMIN".equals(nextRole) || !nextEnabled) && userMapper.countActiveAdmins() <= 1;
	}
}
