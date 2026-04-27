package com.campusform.server.identity.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.exception.UserNotFoundException;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.notification.domain.repository.NotificationRepository;
import com.campusform.server.notification.domain.repository.UserNotificationSettingsRepository;
import com.campusform.server.project.application.service.ProjectCommandService;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.GoogleOAuthTokenRepository;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.domain.repository.CommentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴: 소유 프로젝트 및 연관 데이터 삭제, 공동 관리자에서 제외, OAuth·알림·작성 댓글·사용자 행 삭제.
 */
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCommandService projectCommandService;
    private final GoogleOAuthTokenRepository googleOAuthTokenRepository;
    private final UserNotificationSettingsRepository userNotificationSettingsRepository;
    private final NotificationRepository notificationRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        for (Project project : projectRepository.findByUserId(userId)) {
            if (project.getOwnerId().equals(userId)) {
                projectCommandService.deleteProject(project.getId(), userId);
            } else {
                projectCommandService.removeAdmin(project.getId(), project.getOwnerId(), userId);
            }
        }

        googleOAuthTokenRepository.findByOwnerId(userId).ifPresent(googleOAuthTokenRepository::delete);
        userNotificationSettingsRepository.findByUserId(userId)
                .ifPresent(userNotificationSettingsRepository::delete);
        notificationRepository.deleteByReceiverId(userId);
        commentRepository.deleteAllWrittenByAuthorId(userId);

        userRepository.delete(user);
    }
}
