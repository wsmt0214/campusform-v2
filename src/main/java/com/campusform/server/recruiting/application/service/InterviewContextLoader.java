package com.campusform.server.recruiting.application.service;

import org.springframework.stereotype.Component;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.repository.InterviewSettingRepository;

import lombok.RequiredArgsConstructor;

/**
 * 면접 관련 컨텍스트(Project, InterviewSetting) 조회 유틸리티
 * 
 * 여러 서비스에서 중복되는 조회 로직을 한 곳에서 관리합니다.
 */
@Component
@RequiredArgsConstructor
public class InterviewContextLoader {

    private final ProjectRepository projectRepository;
    private final InterviewSettingRepository interviewSettingRepository;

    /**
     * 프로젝트 조회 (없으면 예외)
     */
    public Project loadProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "프로젝트를 찾을 수 없습니다. projectId=" + projectId));
    }

    /**
     * 면접 설정 조회 (없으면 예외)
     */
    public InterviewSetting loadSettingOrThrow(Long projectId) {
        return interviewSettingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException(
                        "면접 정보 설정을 먼저 완료해야 합니다. projectId=" + projectId));
    }

    /**
     * 프로젝트와 면접 설정을 함께 조회
     * 
     * 대부분의 면접 관련 API에서 두 정보가 모두 필요하므로 편의 메서드 제공
     */
    public InterviewContext loadContext(Long projectId) {
        Project project = loadProjectOrThrow(projectId);
        InterviewSetting setting = loadSettingOrThrow(projectId);
        return new InterviewContext(project, setting);
    }

    /**
     * 프로젝트와 면접 설정을 함께 담는 DTO
     */
    public record InterviewContext(Project project, InterviewSetting setting) {
    }
}
