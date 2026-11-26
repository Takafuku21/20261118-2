package com.example.demo.service;

import com.example.demo.UserAccount;
import com.example.demo.mapper.UserMapper;
import com.example.demo.AuthenticatedUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserMapper userMapper;

	public CustomUserDetailsService(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserAccount account = userMapper.findByUsername(username);
		if (account == null) {
			throw new UsernameNotFoundException("User not found: " + username);
		}

		String authority = account.getRole().startsWith("ROLE_")
				? account.getRole()
				: "ROLE_" + account.getRole();
		return new AuthenticatedUser(account, authority);
	}
}
