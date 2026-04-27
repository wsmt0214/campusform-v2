package com.campusform.server.notification.domain.repository;

import java.util.Optional;

import com.campusform.server.notification.domain.model.UserNotificationSettings;

/**
 * 사용자 알림 설정 Repository 인터페이스 (Domain Layer)
 */
public interface UserNotificationSettingsRepository {

    UserNotificationSettings save(UserNotificationSettings settings);

    Optional<UserNotificationSettings> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void delete(UserNotificationSettings settings);
}
