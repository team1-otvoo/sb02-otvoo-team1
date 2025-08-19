package com.team1.otvoo.security;

import com.team1.otvoo.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

  private final User user;
  private final Map<String, Object> attributes;

  public CustomOAuth2User(User user, Map<String, Object> attributes) {
    this.user = user;
    Map<String, Object> modifiedAttributes = new HashMap<>(attributes);
    modifiedAttributes.put("email", user.getEmail());
    this.attributes = modifiedAttributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
  }

  @Override
  public String getName() {
    return user.getEmail();
  }

  public User getUser() {
    return user;
  }
}