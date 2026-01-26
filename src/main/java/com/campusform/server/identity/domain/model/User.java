package com.campusform.server.identity.domain.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자(관리자 계정) Entity
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // protected () -> 임의 객체 생성 제한
@EntityListeners(AuditingEntityListener.class)

public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static User create(String email, String nickname, String profileImageUrl) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.profileImageUrl = profileImageUrl;
        return user;
    }

    /**
     * 프로필 이미지 업데이트
     *
     * @param newProfileImageUrl 새로운 프로필 이미지 URL
     */
    public void updateProfileImage(String newProfileImageUrl) {
        this.profileImageUrl = newProfileImageUrl;
    }

    /**
     * 닉네임 업데이트
     *
     * @param newNickname 새로운 닉네임 (1~12자, 한글/영어만 허용)
     */
    public void updateNickname(String newNickname) {
        String validatedNickname = validateNickname(newNickname);
        this.nickname = validatedNickname;
    }

    /**
     * 닉네임 유효성 검증
     * - 1~12자
     * - 한글, 영어만 허용
     *
     * @return trim된 검증된 닉네임
     */
    private String validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }

        String trimmed = nickname.trim();

        if (trimmed.length() < 1 || trimmed.length() > 12) {
            throw new IllegalArgumentException("닉네임은 1~12자 이내여야 합니다.");
        }

        // 한글, 영어만 허용 (공백 제외)
        if (!trimmed.matches("^[가-힣a-zA-Z]+$")) {
            throw new IllegalArgumentException("닉네임은 한글과 영어만 사용할 수 있습니다.");
        }

        return trimmed;
    }
}
