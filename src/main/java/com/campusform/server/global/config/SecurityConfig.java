package com.campusform.server.global.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.campusform.server.identity.infrastructure.oauth2.CookieOAuth2AuthorizationRequestRepository;
import com.campusform.server.identity.infrastructure.oauth2.CustomLogoutSuccessHandler;
import com.campusform.server.identity.infrastructure.oauth2.CustomOAuth2UserService;
import com.campusform.server.identity.infrastructure.oauth2.OAuth2AuthenticationFailureHandler;
import com.campusform.server.identity.infrastructure.oauth2.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final OAuth2AuthenticationFailureHandler failureHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;
    private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (세션 기반이지만 API 서버이므로)
                .csrf(csrf -> csrf.disable())

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로
                        .requestMatchers(
                                "/",
                                "/login/**",
                                "/oauth2/**",
                                "/api/auth/**",
                                "/api/test/**", // 테스트용 API
                                "/api/public/**", // 면접자 공개 API
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**")
                        .permitAll()
                        // 나머지 요청은 인증 필요
                        .anyRequest().authenticated())

                // H2 콘솔을 위한 설정
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        // OAuth2 인증 요청을 세션이 아닌 쿠키에 저장
                        // 서브도메인 간 세션 공유 문제 해결
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(
                                        cookieOAuth2AuthorizationRequestRepository)))

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"));

        return http.build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}