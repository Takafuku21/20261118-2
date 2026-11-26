package com.example.demo;

import com.example.demo.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapUserInitializer {

	private static final Logger logger = LoggerFactory.getLogger(BootstrapUserInitializer.class);

	@Bean
	ApplicationRunner createBootstrapUser(
			UserMapper userMapper,
			PasswordEncoder passwordEncoder,
			@Value("${app.security.bootstrap.username:}") String username,
			@Value("${app.security.bootstrap.password:}") String password) {
		return arguments -> {
			if (username.isBlank() || password.isBlank()) {
				logger.info("Bootstrap user creation skipped because credentials are not configured");
				return;
			}
			UserAccount existingUser = userMapper.findByUsername(username);
			if (existingUser != null) {
				if (!"ADMIN".equals(existingUser.getRole())) {
					userMapper.promoteToAdmin(existingUser.getId());
					logger.info("Configured bootstrap user was promoted to ADMIN");
				}
				logger.info("Bootstrap user already exists; leaving its password unchanged");
				return;
			}

			UserAccount user = new UserAccount();
			user.setUsername(username);
			user.setPassword(passwordEncoder.encode(password));
			user.setRole("ADMIN");
			userMapper.insert(user);
			logger.info("Bootstrap user created with BCrypt password encoding");
		};
	}
}
