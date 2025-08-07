package com.team1.otvoo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .ignoringRequestMatchers("/api/auth/**", "/api/users/**"))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**", "/api/users/**", "/", "/index.html", "/vite.svg", "/assets/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
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
}