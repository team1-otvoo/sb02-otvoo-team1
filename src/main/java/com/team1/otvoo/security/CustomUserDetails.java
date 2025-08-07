package com.team1.otvoo.security;

import com.team1.otvoo.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

  private final User user;
  private final boolean usingTemporaryPassword;

  public CustomUserDetails(User user, boolean usingTemporaryPassword) {
    this.user = user;
    this.usingTemporaryPassword = usingTemporaryPassword;
  }

  public CustomUserDetails(User user) {
    this(user, false);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(() -> "ROLE_" + user.getRole().name());
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !user.isLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}