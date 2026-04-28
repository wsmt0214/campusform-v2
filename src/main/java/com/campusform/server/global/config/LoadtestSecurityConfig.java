package com.campusform.server.global.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@Profile("loadtest")
@EnableWebSecurity
public class LoadtestSecurityConfig {

    public static final String USER_ID_HEADER = "X-Loadtest-UserId";
    public static final String EMAIL_HEADER = "X-Loadtest-Email";

    @Bean
    public SecurityFilterChain loadtestSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new LoadtestAuthHeaderFilter(), AnonymousAuthenticationFilter.class);

        return http.build();
    }

    static class LoadtestAuthHeaderFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            String userId = request.getHeader(USER_ID_HEADER);
            if (userId == null || userId.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = request.getHeader(EMAIL_HEADER);
            if (email == null || email.isBlank()) {
                email = "loadtest@campusform.local";
            }

            Map<String, Object> attributes = Map.of(
                    "userId", Long.parseLong(userId.trim()),
                    "email", email
            );

            DefaultOAuth2User principal = new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "email"
            );

            AbstractAuthenticationToken authentication = new OAuth2AuthenticationToken(
                    principal,
                    principal.getAuthorities(),
                    "loadtest"
            );
            authentication.setAuthenticated(true);

            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);
        }
    }
}

