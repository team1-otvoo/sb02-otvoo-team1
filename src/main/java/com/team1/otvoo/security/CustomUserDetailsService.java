package com.team1.otvoo.security;

import com.team1.otvoo.exception.ErrorCode;
import com.team1.otvoo.exception.RestException;
import com.team1.otvoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) {
    return userRepository.findByEmail(email)
        .map(CustomUserDetails::new)
        .orElseThrow(() -> new RestException(ErrorCode.NOT_FOUND, Map.of("reason", "해당 사용자를 찾을 수 없습니다.")));
  }
}