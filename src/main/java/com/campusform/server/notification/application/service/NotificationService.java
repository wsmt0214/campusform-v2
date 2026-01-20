package com.campusform.server.notification.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.notification.application.dto.response.NotificationListResponse;
import com.campusform.server.notification.application.dto.response.NotificationResponse;
import com.campusform.server.notification.application.dto.response.UnreadCountResponse;
import com.campusform.server.notification.domain.exception.NotificationAccessDeniedException;
import com.campusform.server.notification.domain.exception.NotificationNotFoundException;
import com.campusform.server.notification.domain.model.Notification;
import com.campusform.server.notification.domain.model.UserNotificationSettings;
import com.campusform.server.notification.domain.model.value.NotificationType;
import com.campusform.server.notification.domain.repository.NotificationRepository;
import com.campusform.server.notification.domain.repository.UserNotificationSettingsRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;
    private final UserNotificationSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    /**
     * 알림 생성
     *
     * 정책: 알림 수신 설정 OFF 시에도 알림은 생성됨.
     * 조회 시에만 표시가 제한됨.
     *
     * @param receiverId 수신자 ID
     * @param projectId 프로젝트 ID
     * @param type 알림 타입
     * @param payload 알림 추가 데이터 (JSON)
     * @return 생성된 알림 정보
     */
    @Transactional
    public NotificationResponse createNotification(Long receiverId, Long projectId,
                                                    NotificationType type, String payload) {
        validateUserExists(receiverId);

        Notification notification = Notification.create(receiverId, projectId, type, payload);
        Notification saved = notificationRepository.save(notification);

        return NotificationResponse.from(saved);
    }

    /**
     * 사용자별 알림 목록 조회 (최신순, 페이징)
     *
     * 정책: 알림 수신 설정과 무관하게 항상 조회 가능
     *      (알림 수신 설정은 푸시/실시간 알림 수신 여부만 제어)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 알림 목록 (페이징 정보 포함)
     */
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, int page, int size) {
        int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(page, pageSize);

        Page<Notification> notificationPage = notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(userId, pageable);

        return NotificationListResponse.from(notificationPage);
    }

    /**
     * 특정 알림 읽음 처리
     *
     * 멱등성: 이미 읽은 알림에 대해서는 예외 없이 성공 응답 반환
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 정보
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = findNotificationById(notificationId);

        validateNotificationOwner(notification, userId, notificationId);

        notification.markAsRead();
        // JPA dirty checking으로 자동 저장

        return NotificationResponse.from(notification);
    }

    /**
     * 안읽은 알림 개수 조회
     *
     * 정책: 알림 수신 설정과 무관하게 항상 조회 가능
     *      (알림 수신 설정은 푸시/실시간 알림 수신 여부만 제어)
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByReceiverIdAndReadAtIsNull(userId);
        return UnreadCountResponse.of(count);
    }

    /**
     * 모든 알림 읽음 처리
     *
     * @return 읽음 처리된 알림 개수
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByReceiverId(userId);
    }

    /**
     * 알림 수신 설정 변경
     *
     * @param userId 사용자 ID
     * @param enabled 활성화 여부
     * @return 변경된 설정 상태
     */
    @Transactional
    public boolean updateNotificationSetting(Long userId, boolean enabled) {
        UserNotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> settingsRepository.save(UserNotificationSettings.create(userId)));

        if (enabled) {
            settings.enableNotification();
        } else {
            settings.disableNotification();
        }

        return settings.isNotificationEnabled();
    }

    /**
     * 알림 수신 설정 조회
     */
    @Transactional(readOnly = true)
    public boolean getNotificationSetting(Long userId) {
        return isNotificationEnabled(userId);
    }

    // ============ Private Helper Methods ============

    /**
     * 알림 수신 설정 확인
     * 설정이 없으면 기본값(true) 반환
     */
    private boolean isNotificationEnabled(Long userId) {
        return settingsRepository.findByUserId(userId)
                .map(UserNotificationSettings::isNotificationEnabled)
                .orElse(true); // 설정이 없으면 기본값 true
    }

    /**
     * 알림 ID로 알림 조회
     */
    private Notification findNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }

    /**
     * 알림 소유권 검증
     */
    private void validateNotificationOwner(Notification notification, Long userId, Long notificationId) {
        if (!notification.isOwner(userId)) {
            throw new NotificationAccessDeniedException(notificationId, userId);
        }
    }

    /**
     * 사용자 존재 여부 확인
     */
    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId);
        }
    }
}
