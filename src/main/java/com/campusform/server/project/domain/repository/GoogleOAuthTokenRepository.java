package com.campusform.server.project.domain.repository;

import java.util.Optional;

import com.campusform.server.project.domain.model.sheet.GoogleOAuthToken;

/**
 * Google OAuth Token Repository 인터페이스
 * 
 * 도메인 계층의 Repository 인터페이스로, 특정 기술에 의존하지 않고
 * 도메인 관점에서 인터페이스를 정의합니다.
 */
public interface GoogleOAuthTokenRepository {

    /**
     * Google OAuth Token 저장
     */
    GoogleOAuthToken save(GoogleOAuthToken token);

    /**
     * ownerId로 토큰 조회
     */
    Optional<GoogleOAuthToken> findByOwnerId(Long ownerId);

    /**
     * 토큰 삭제
     */
    void delete(GoogleOAuthToken token);
}
