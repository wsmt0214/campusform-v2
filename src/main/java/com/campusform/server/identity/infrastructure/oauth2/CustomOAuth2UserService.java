package com.campusform.server.identity.infrastructure.oauth2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 * Google OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 *
 * 1. 사용자가 프론트엔드에서 "Google로 로그인" 버튼 클릭
 * 
 * 2. 프론트엔드가 Spring Security가 제공하는 OAuth2 로그인 Endpoint(예: /oauth2/authorization/google)로 리다이렉트
 *     - Spring Security가 자동으로 Google OAuth2 Authorization Endpoint로 리다이렉트 
 *         - 예: https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&scope=...&response_type=code&...
 *     - 이 시점에서 사용자는 구글 계정으로 로그인
 *     - scope 에는 필요한 범위의 권한을 명시 
 * 
 * 3. 로그인 완료 시 구글은 등록된 redirectUri로 authorization code 전달
 *     - 예: /login/oauth2/code/google?code=4/0AeanS...&state=...
 *     - 이때 code는 accessToken으로 교환하기 위한 일회용 코드 
 *     - 이 리다이렉트는 OAuth2LoginAuthenticationFilter가 처리 
 * 
 * 4. Spring Security가 받은 authorization code를 이용해 Google OAuth2 Token Endpoint로 서버간 통신하여 access_token 획득
 *     - 내부적으로 HTTP POST로 https://oauth2.googleapis.com/token에 요청 
 *       (파라미터: code, client_id, client_secret, grant_type=authorization_code, redirect_uri 등)
 *     - 이 과정은 모두 Spring Security OAuth2 Client가 자동으로 처리.
 *     - 구글은 응답으로 accessToken, idToken, refreshToken(옵션) 등 발급
 * 
 * 5. access_token, id_token 등을 확보한 뒤, Spring Security가 Google UserInfo Endpoint를 호출해 사용자 정보를 받아옴
 *    - 예: https://openidconnect.googleapis.com/v1/userinfo
 *    - 응답 정보: sub(고유ID), email, name, picture 등
 *    - 바로 이 시점에 loadUser(OAuth2UserRequest userRequest) 메서드가 호출됨!
 *    - 최종적으로 OAuth2User 객체를 리턴하면 Spring Security가 해당 객체를 SecurityContext에 등록
 * 
 * [참고]
 * - Google Sheets 권한은 회원가입 시가 아닌, 구글 시트 데이터를 가져오려고 할 때 별도로 획득
 * - 권한 획득은 /api/projects/google-oauth/authorize-url API 통함.
 * </pre>
 */

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * <pre>
      * OAuth2 사용자 정보 로드 및 처리
      * 
      * [호출 시점]
      * - Spring Security OAuth2 로그인 플로우 도중 자동 호출
      * - Google OAuth2 UserInfo Endpoint에서 사용자 정보를 받은 후 실행
      * 
      * OAuth2UserRequest: OAuth2 인증 세션 정보 포함 객체
      * - 주요 필드:
      *     - getAccessToken(): Google에서 발급한 access token
      *         - 사용자의 정보 (email, name, picture 등)를 조회하는 용도로만 일시적으로 사용
      *         - 구글 시트 등 별도 API 접근 토큰은 별도로 획득 필요
      *     - getClientRegistration(): 클라이언트(client)의 등록 정보(Google Client ID, Secret, Provider 정보 등)
      *     - getAdditionalParameters(): 인증 요청 과정에서 전달된 추가 파라미터
      * 
      * @throws OAuth2AuthenticationException OAuth2 인증 실패 시 예외 발생
     * </pre>
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // super.loadUser() 통해 Google로부터 받은 사용자 정보 조회
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String picture = (String) attributes.get("picture");

        // 최초 가입 시 닉네임은 저장하지 않음 (프론트에서 온보딩으로 직접 설정하도록 유도)
        // nickname이 null이면 "첫 로그인"으로 판단 가능
        String nickname = null;

        /**
         * 기존 사용자 조회 또는 신규 사용자 생성
         */
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 신규 사용자의 경우
                    User newUser = createUser(email, nickname, picture);

                    /**
                     * 회원가입 이벤트 발행 (선택적)
                     * 구글 시트 권한은 회원가입 시가 아닌 연동 시도 시에 획득하므로 이벤트 발행 필요없음
                     */
                    // eventPublisher.publishEvent(new UserRegisteredEvent(newUser.getId(), email));

                    return newUser;
                });

        /**
         * SecurityContext 내 Authentication 세션에 저장할 OAuth2User 객체 생성
         * 이후에는 SecurityContextHolder.getContext().getAuthentication() 등으로 접근 가능
         */
        Map<String, Object> extendedAttributes = new HashMap<>(attributes);
        extendedAttributes.put("userId", user.getId()); // 조회의 편리성을 위해 userId 추가

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), // ROLE_USER 권한 부여
                extendedAttributes,
                "email");
    }

    /**
     * 신규 사용자 생성 및 저장
     */
    private User createUser(String email, String nickname, String profileImageUrl) {
        User user = User.create(email, nickname, profileImageUrl);
        userRepository.save(user);
        return user;
    }
}
