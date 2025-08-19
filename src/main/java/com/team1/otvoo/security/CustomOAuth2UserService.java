package com.team1.otvoo.security;

import com.team1.otvoo.user.entity.Profile;
import com.team1.otvoo.user.entity.User;
import com.team1.otvoo.user.repository.ProfileRepository;
import com.team1.otvoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    String provider = userRequest.getClientRegistration().getRegistrationId();
    Map<String, Object> attributes = oAuth2User.getAttributes();

    String name = extractName(attributes, provider);
    String email = extractEmail(attributes, provider);

    User user = userRepository.findByEmail(email)
        .orElseGet(() -> createUser(email, name));

    return new CustomOAuth2User(user, attributes);
  }

  private User createUser(String email, String name) {
    User user = User.builder()
        .email(email)
        .password(UUID.randomUUID().toString())
        .build();
    user = userRepository.save(user);

    profileRepository.save(new Profile(name, user));
    return user;
  }

  private String extractEmail(Map<String, Object> attributes, String provider) {
    if ("google".equalsIgnoreCase(provider)) {
      String email = Optional.ofNullable(attributes.get("email"))
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .filter(e -> !e.isBlank())
          .orElse("GoogleUser@" + UUID.randomUUID() + ".com");
      return email;
    }

    if ("kakao".equalsIgnoreCase(provider)) {
      Object kakaoIdObj = attributes.get("id");
      String kakaoId = kakaoIdObj != null ? kakaoIdObj.toString() : UUID.randomUUID().toString();

      return kakaoId + "@kakao.com";
    }

    return "OAuthUser@" + provider.toLowerCase() + ".com";
  }

  private String extractName(Map<String, Object> attributes, String provider) {
    if ("google".equalsIgnoreCase(provider)) {
      return Optional.ofNullable(attributes.get("name"))
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .filter(n -> !n.isBlank())
          .orElse("OAuthUser");
    }

    if ("kakao".equalsIgnoreCase(provider)) {
      // 카카오의 경우 이메일을 제공하지 않으므로 카카오 ID를 이용해 임의 이메일 생성
      String nickname = extractKakaoNickname(attributes);
      return nickname != null ? nickname : "OAuthUser";
    }

    return "OAuthUser";
  }

  private String extractKakaoNickname(Map<String, Object> attributes) {
    Object kakaoAccountObj = attributes.get("kakao_account");
    if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
      Object profileObj = kakaoAccount.get("profile");
      if (profileObj instanceof Map<?, ?> profile) {
        Object nicknameObj = profile.get("nickname");
        if (nicknameObj instanceof String nickname && !nickname.isBlank()) {
          return nickname;
        }
      }
    }
    return null;
  }
}