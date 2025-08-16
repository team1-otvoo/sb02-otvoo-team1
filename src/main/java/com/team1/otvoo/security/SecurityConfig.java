package com.team1.otvoo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtLoginSuccessHandler jwtLoginSuccessHandler;
  private final JwtLogoutHandler jwtLogoutHandler;
  private final CustomLoginFailureHandler loginFailureHandler;
  private final CustomUserDetailsService customUserDetailsService;
  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
    http
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/auth/**", "/api/users/**", "/ws/**","/ws/info/**")
            .csrfTokenRequestHandler(requestHandler())
            .csrfTokenRepository(tokenRepository())
            .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**", "/", "/index.html", "/vite.svg", "/assets/**", "/ws/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()                    // 회원가입만 공개
            .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")               // 목록 조회 ADMIN
            .requestMatchers(HttpMethod.PATCH, "/api/users/*/lock").hasRole("ADMIN")          // 잠금 변경 ADMIN
            .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")          // 권한 변경 ADMIN
            .anyRequest().authenticated())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .userDetailsService(customUserDetailsService)
        .addFilterBefore(jsonUsernamePasswordAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .logout(logout -> logout
            .logoutUrl("/api/auth/sign-out")
            .addLogoutHandler(jwtLogoutHandler)
            .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK)));

    return http.build();
  }

  @Bean
  public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
    JsonUsernamePasswordAuthenticationFilter filter = new JsonUsernamePasswordAuthenticationFilter(objectMapper);
    filter.setAuthenticationManager(authenticationManager);
    filter.setAuthenticationSuccessHandler(jwtLoginSuccessHandler);
    filter.setAuthenticationFailureHandler(loginFailureHandler);
    filter.setFilterProcessesUrl("/api/auth/sign-in");

    return filter;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      HttpSecurity http,
      CustomAuthenticationProvider customAuthenticationProvider) throws Exception {
    return http.getSharedObject(AuthenticationManagerBuilder.class)
        .authenticationProvider(customAuthenticationProvider)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CookieCsrfTokenRepository tokenRepository() {
    CookieCsrfTokenRepository repository =
        CookieCsrfTokenRepository.withHttpOnlyFalse(); // 프론트엔드(JS)에서 토큰을 헤더에 포함할 수 있도록 함. (따로 띄우지 않았으므로 CORS는 필요 X)
    repository.setCookieName("XSRF-TOKEN");
    repository.setHeaderName("X-XSRF-TOKEN");

    return repository;
  }
  @Bean
  public CsrfTokenRequestAttributeHandler requestHandler() {
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName("_csrf");
    return requestHandler;
  }
}