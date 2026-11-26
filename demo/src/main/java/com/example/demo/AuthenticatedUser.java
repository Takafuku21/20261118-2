package com.example.demo;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Authenticated user identity including the database id used for ownership checks. */
public class AuthenticatedUser implements UserDetails {
	private final Long id;
	private final String username;
	private final String password;
	private final List<GrantedAuthority> authorities;
	private final boolean enabled;

	public AuthenticatedUser(UserAccount account, String authority) {
		this.id = account.getId();
		this.username = account.getUsername();
		this.password = account.getPassword();
		this.enabled = account.isEnabled();
		this.authorities = List.of((GrantedAuthority) () -> authority);
	}

	public Long getId() { return id; }
	@Override public String getUsername() { return username; }
	@Override public String getPassword() { return password; }
	@Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }
	@Override public boolean isEnabled() { return enabled; }
}
