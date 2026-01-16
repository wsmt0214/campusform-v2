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
    @Column(nullable = false)
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
}