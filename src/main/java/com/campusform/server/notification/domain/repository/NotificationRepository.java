package com.campusform.server.notification.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.campusform.server.notification.domain.model.Notification;

/**
 * 알림 Repository 인터페이스 (Domain Layer)
 */
public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    /**
     * idx_receiver_created 인덱스 활용
     */
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndReadAtIsNull(Long receiverId);

    /**
     * 사용자의 모든 안읽은 알림을 읽음 처리 (벌크 업데이트)
     */
    int markAllAsReadByReceiverId(Long receiverId);

    void deleteByProjectId(Long projectId);

    void deleteByReceiverId(Long receiverId);
}
