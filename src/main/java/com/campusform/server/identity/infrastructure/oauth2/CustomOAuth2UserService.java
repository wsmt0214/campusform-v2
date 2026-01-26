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
 * Google OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 *
 * 신규 사용자인 경우 회원가입 처리, 기존 사용자인 경우 정보 조회
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // OAuth name에서 한글/영어만 추출, 최대 12자로 제한
        String nickname = normalizeNickname(name);

        // 기존 사용자 조회 또는 신규 사용자 생성
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, nickname, picture));

        Map<String, Object> extendedAttributes = new HashMap<>(attributes);
        extendedAttributes.put("userId", user.getId());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                extendedAttributes,
                "email"
        );
    }

    /**
     * 신규 사용자 생성 및 저장
     */
    private User createUser(String email, String nickname, String profileImageUrl) {
        User user = User.create(email, nickname, profileImageUrl);
        userRepository.save(user);
        return user;
    }

    /**
     * OAuth name에서 유효한 닉네임 추출
     * 한글/영어만 필터링, 1~12자 제한
     *
     * @param name Google OAuth name 필드
     * @return 검증 규칙에 맞는 닉네임
     */
    private String normalizeNickname(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "사용자";
        }

        // 한글, 영어만 추출 (공백, 숫자, 특수문자 제거)
        String filtered = name.trim().replaceAll("[^가-힣a-zA-Z]", "");

        // 필터링 후 비어있으면 기본값
        if (filtered.isEmpty()) {
            return "사용자";
        }

        // 12자 초과 시 잘라내기
        return filtered.length() > 12 ? filtered.substring(0, 12) : filtered;
    }
}
