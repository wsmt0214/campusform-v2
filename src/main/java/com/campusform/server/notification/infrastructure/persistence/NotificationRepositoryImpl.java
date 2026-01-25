package com.campusform.server.notification.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.campusform.server.notification.domain.model.Notification;
import com.campusform.server.notification.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * NotificationRepository 구현체
 *
 * Spring Data JPA에 작업을 위임합니다.
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationJpaRepository.findById(id);
    }

    @Override
    public Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable) {
        return notificationJpaRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId, pageable);
    }

    @Override
    public long countByReceiverIdAndReadAtIsNull(Long receiverId) {
        return notificationJpaRepository.countByReceiverIdAndReadAtIsNull(receiverId);
    }

    @Override
    public int markAllAsReadByReceiverId(Long receiverId) {
        return notificationJpaRepository.markAllAsReadByReceiverId(receiverId);
    }
}
