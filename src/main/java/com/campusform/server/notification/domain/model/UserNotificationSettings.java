package com.campusform.server.notification.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자별 알림 설정 Entity
 * 개인별 알림 수신 설정을 관리합니다.
 */
@Entity
@Table(name = "user_notification_settings", 
       indexes = @Index(name = "idx_user_id", columnList = "user_id", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserNotificationSettings {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ============ Factory Method ============

    /**
     * 사용자 알림 설정 생성 정적 팩토리 메서드
     * 기본값으로 알림 수신이 활성화됩니다.
     *
     * @param userId 사용자 ID (Identity Context)
     * @return 생성된 UserNotificationSettings 객체
     */
    public static UserNotificationSettings create(Long userId) {
        if (userId == null)
            throw new IllegalArgumentException("userId는 필수입니다.");

        UserNotificationSettings settings = new UserNotificationSettings();
        settings.userId = userId;
        settings.notificationEnabled = true;
        return settings;
    }

    // ============ Business Methods ============

    public boolean isNotificationEnabled() {
        return Boolean.TRUE.equals(this.notificationEnabled);
    }

    public void enableNotification() {
        this.notificationEnabled = true;
    }

    public void disableNotification() {
        this.notificationEnabled = false;
    }

    public void toggleNotification() {
        this.notificationEnabled = !this.notificationEnabled;
    }
}
