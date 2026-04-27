package com.campusform.server.notification.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.notification.domain.model.UserNotificationSettings;
import com.campusform.server.notification.domain.repository.UserNotificationSettingsRepository;

import lombok.RequiredArgsConstructor;

/**
 * UserNotificationSettingsRepository 구현체
 *
 * Spring Data JPA에 작업을 위임합니다.
 */
@Repository
@RequiredArgsConstructor
public class UserNotificationSettingsRepositoryImpl implements UserNotificationSettingsRepository {

    private final UserNotificationSettingsJpaRepository settingsJpaRepository;

    @Override
    public UserNotificationSettings save(UserNotificationSettings settings) {
        return settingsJpaRepository.save(settings);
    }

    @Override
    public Optional<UserNotificationSettings> findByUserId(Long userId) {
        return settingsJpaRepository.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return settingsJpaRepository.existsByUserId(userId);
    }

    @Override
    public void delete(UserNotificationSettings settings) {
        settingsJpaRepository.delete(settings);
    }
}
