package com.campusform.server.project.domain.model.sheet;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 구글 OAuth 토큰 Entity
 * 시트 접근용 OAuth 토큰을 관리합니다.
 * 민감 정보이므로 별도 테이블로 분리 저장됩니다.
 */
@Entity
@Table(name = "google_oauth_tokens",
       indexes = @Index(name = "idx_owner_id", columnList = "owner_id", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GoogleOAuthToken {

    @Id
    @GeneratedValue
    private Long id;

    // 다른 어그리거트 -> 참조 아닌 연관으로 관계 설정
    @Column(name = "owner_id", nullable = false, unique = true)
    private Long ownerId;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expiry_at")
    private LocalDateTime expiryAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
