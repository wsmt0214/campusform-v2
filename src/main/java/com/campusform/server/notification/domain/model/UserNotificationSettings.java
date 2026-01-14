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
}
