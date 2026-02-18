package com.campusform.server.notification.domain.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.campusform.server.notification.domain.model.value.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 Entity
 * 사용자별 알림을 관리합니다.
 */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_receiver_created", columnList = "receiver_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * 알림 문구 작성을 위한 추가 데이터 (JSON 형식)
     * MySQL 8.0의 JSON 타입 지원 활용
     */
    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 읽음 처리 시간 (null이면 안 읽음)
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ============ Factory Method ============

    /**
     * 알림 생성 정적 팩토리 메서드
     *
     * @param receiverId 수신자 ID (Identity Context)
     * @param projectId 프로젝트 ID (Project Context)
     * @param type 알림 타입
     * @param payload 알림 추가 데이터 (JSON 형식)
     * @return 생성된 Notification 객체
     */
    public static Notification create(Long receiverId, Long projectId, NotificationType type, String payload) {
        if (receiverId == null)
            throw new IllegalArgumentException("receiverId는 필수입니다.");
        if (projectId == null)
            throw new IllegalArgumentException("projectId는 필수입니다.");
        if (type == null)
            throw new IllegalArgumentException("type은 필수입니다.");
        if (payload == null || payload.isBlank())
            throw new IllegalArgumentException("payload는 필수입니다.");

        Notification notification = new Notification();
        notification.receiverId = receiverId;
        notification.projectId = projectId;
        notification.type = type;
        notification.payload = payload;
        return notification;
    }

    // ============ Business Methods ============

    /**
     * 알림 읽음 처리
     * 멱등성: 이미 읽은 알림에 대해서는 아무 동작도 하지 않음
     */
    public void markAsRead() {
        if (this.readAt != null)
            return; // 이미 읽은 경우 무시 (멱등성 보장)
        this.readAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    public boolean isOwner(Long userId) {
        return this.receiverId.equals(userId);
    }
}