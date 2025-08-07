package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.TemporaryPassword;
import com.team1.otvoo.auth.token.TemporaryPasswordStore;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

  private final UserRepository userRepository;
  private final TemporaryPasswordStore temporaryPasswordStore;
  private final PasswordEncoder passwordEncoder;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String email = authentication.getName();
    String rawPassword = authentication.getCredentials().toString();

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("❌ 사용자 없음: {}", email);
          return new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        });

    TemporaryPassword tempPassword = temporaryPasswordStore.get(email);
    boolean usingTempPassword = false;

    if (tempPassword != null) {
      if (tempPassword.isExpired()) {
        if (tempPassword.getPassword().equals(rawPassword)) {
          log.warn("⏳ 임시 비밀번호 만료: {}", email);
          throw new CredentialsExpiredException("임시 비밀번호가 만료되었습니다. 비밀번호 재설정을 다시 시도해주세요.");
        } else {
          log.warn("❌ 임시 비밀번호 불일치 (만료된 상태): {}", email);
          throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
      }
      if (!tempPassword.getPassword().equals(rawPassword)) {
        log.warn("❌ 임시 비밀번호 불일치: {}", email);
        throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
      }
      usingTempPassword = true;
    } else {
      if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
        log.warn("❌ 비밀번호 불일치: {}", email);
        throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
      }
    }

    CustomUserDetails userDetails = new CustomUserDetails(user, usingTempPassword);

    log.info("✅ 인증 성공: {}", email);

    return new UsernamePasswordAuthenticationToken(userDetails, rawPassword, userDetails.getAuthorities());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}