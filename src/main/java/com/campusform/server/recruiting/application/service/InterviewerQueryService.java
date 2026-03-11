package com.campusform.server.recruiting.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.ProjectAdmin;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewerListResponse;

import lombok.RequiredArgsConstructor;

/**
 * 면접관 조회 전용 Application Service
 * 
 * 면접관 목록 조회와 같은 조회 전용 비즈니스 로직을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewerQueryService {

    private final InterviewContextLoader contextLoader;
    private final UserRepository userRepository;

    /**
     * 프로젝트의 모든 관리자(OWNER + ADMIN) 목록 조회
     */
    public InterviewerListResponse getInterviewerList(Long projectId, Long userId) {
        Project project = contextLoader.loadProjectOrThrow(projectId);
        project.validateAdminAccess(userId);

        // 프로젝트의 모든 관리자 ID 목록 수집
        Set<Long> adminIds = collectAdminIds(project);

        // User 정보 조회 및 응답 생성
        List<InterviewerListResponse.AdminInfo> interviewers = adminIds.stream()
                .map(adminId -> {
                    User user = userRepository.findById(adminId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + adminId));
                    return InterviewerListResponse.AdminInfo.of(
                            user.getId(),
                            user.getNickname(),
                            user.getEmail(),
                            user.getProfileImageUrl());
                })
                .toList();

        return InterviewerListResponse.of(interviewers);
    }

    /**
     * 프로젝트의 모든 관리자 ID 목록 수집 (OWNER + ADMIN)
     */
    private Set<Long> collectAdminIds(Project project) {
        Set<Long> adminIds = new LinkedHashSet<>();
        adminIds.add(project.getOwnerId());
        for (ProjectAdmin admin : project.getAdmins()) {
            adminIds.add(admin.getAdminId());
        }
        return adminIds;
    }
}
