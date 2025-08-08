package com.team1.otvoo.security;

import com.team1.otvoo.auth.token.AccessTokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtService;
  private final CustomUserDetailsService userDetailsService;
  private final AccessTokenStore accessTokenStore;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String bearerValue = authHeader.substring(7).trim();
      String accessToken = extractAccessToken(bearerValue);

      if (accessToken != null && jwtService.validateToken(accessToken)
          && !accessTokenStore.isBlacklisted(accessToken)) {

        String username = jwtService.getEmailFromToken(accessToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private String extractAccessToken(String bearerValue) {
    if (bearerValue.contains("accessToken=")) {
      for (String pair : bearerValue.split("&")) {
        if (pair.startsWith("accessToken=")) {
          return pair.substring("accessToken=".length());
        }
      }
      return null;
    }

    return bearerValue;
  }
}