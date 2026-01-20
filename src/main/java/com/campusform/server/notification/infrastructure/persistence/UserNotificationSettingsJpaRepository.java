package com.campusform.server.notification.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusform.server.notification.domain.model.UserNotificationSettings;

/**
 * Spring Data JPA를 위한 UserNotificationSettings Repository
 */
@Repository
public interface UserNotificationSettingsJpaRepository extends JpaRepository<UserNotificationSettings, Long> {

    /**
     * 사용자 ID로 알림 설정 조회
     */
    Optional<UserNotificationSettings> findByUserId(Long userId);

    /**
     * 사용자 ID로 알림 설정 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
}
