package com.campusform.server.recruiting.application.service;

import org.springframework.stereotype.Component;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewAvailabilityInvestigationLink;
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
     * 면접 설정이 없으면 예외를 던집니다.
     */
    public InterviewContext loadContext(Long projectId) {
        Project project = loadProjectOrThrow(projectId);
        InterviewSetting setting = loadSettingOrThrow(projectId);
        return new InterviewContext(project, setting);
    }

    /**
     * 프로젝트와 면접 설정을 함께 조회 (면접 설정이 없어도 가능)
     * 
     * 면접 설정이 선택적인 경우에 사용합니다.
     * 면접 설정이 없으면 Optional.empty()를 반환합니다.
     */
    public java.util.Optional<InterviewContext> loadContextOptional(Long projectId) {
        Project project = loadProjectOrThrow(projectId);
        return interviewSettingRepository.findByProjectId(projectId)
                .map(setting -> new InterviewContext(project, setting));
    }

    /**
     * 토큰으로 프로젝트와 면접 설정을 함께 조회
     * 
     * 공개 API에서 토큰을 통해 접근할 때 사용합니다.
     * DDD 원칙: InterviewAvailabilityInvestigationLink는 InterviewSetting 애그리거트 루트 안에
     * 포함되므로
     * 루트 애그리거트를 통해 접근합니다.
     */
    public InterviewContext loadContextByToken(String token) {
        // 토큰으로 InterviewSetting 조회 (investigationLink 포함)
        InterviewSetting setting = interviewSettingRepository.findByInvestigationLinkToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        // investigationLink가 비활성화되어 있는지 확인
        InterviewAvailabilityInvestigationLink link = setting.getInvestigationLink();
        if (link == null || !link.getEnabled()) {
            throw new IllegalStateException("현재 지원자 응답 제출 링크가 비활성화되어 있습니다.");
        }

        // InterviewSetting에서 projectId를 가져와서 Project 조회
        Long projectId = setting.getProjectId();
        Project project = loadProjectOrThrow(projectId);

        return new InterviewContext(project, setting);
    }

    /**
     * 프로젝트와 면접 설정을 함께 담는 DTO
     */
    public record InterviewContext(Project project, InterviewSetting setting) {
    }
}
