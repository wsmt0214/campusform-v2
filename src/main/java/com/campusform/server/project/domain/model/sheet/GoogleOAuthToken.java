package com.campusform.server.project.domain.model.sheet;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구글 OAuth 토큰 Entity
 * 시트 접근용 OAuth 토큰을 관리합니다.
 * 민감 정보이므로 별도 테이블로 분리 저장됩니다.
 */
@Entity
@Table(name = "google_oauth_tokens", indexes = @Index(name = "idx_owner_id", columnList = "owner_id", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GoogleOAuthToken {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "owner_id", nullable = false, unique = true)
    private Long ownerId;

    /**
     * Google API를 호출할 때 쓰는 토큰
     * - 유효기간 짧음 (보통 1시간)
     * - Bearer Token 형식식
     * - 이 토큰이 있으면 권한(scope) 범위 내 접근 가능
     */
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    /**
     * accessToken이 만료되면 이 토큰으로 accessToken 갱신용 토큰
     * - 구글 재로그인 불필요요
     * - 유효기간 길음 (보통 1달)
     * - API 호출에는 직접 사용X
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /**
     * accessToken 만료 시간
     */
    @Column(name = "expiry_at")
    private LocalDateTime expiryAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * GoogleOAuthToken 생성 팩토리 메서드
     */
    public static GoogleOAuthToken create(Long ownerId, String accessToken, String refreshToken,
            LocalDateTime expiryAt) {
        GoogleOAuthToken token = new GoogleOAuthToken();
        token.ownerId = ownerId;
        token.accessToken = accessToken;
        token.refreshToken = refreshToken;
        token.expiryAt = expiryAt;
        return token;
    }

    /**
     * 토큰 정보 업데이트
     */
    public void updateToken(String accessToken, String refreshToken, LocalDateTime expiryAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiryAt = expiryAt;
    }
}
