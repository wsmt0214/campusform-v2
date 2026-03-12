package com.campusform.server.recruiting.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.application.service.ProjectAccessService;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import lombok.RequiredArgsConstructor;

/**
 * 모집 단계 전환 서비스 (Recruiting Context)
 * 
 * Project 상태 흐름: DOCUMENT → INTERVIEW → INTERVIEW_COMPLETE
 *                  DOCUMENT → DOCUMENT_COMPLETE (면접 없이 종료)
 */
@Service
@RequiredArgsConstructor
public class ProjectStageTransitionService {

    private final ProjectAccessService projectAccessService;
    private final ApplicantRepository applicantRepository;

    /**
     * 면접 단계 시작: DOCUMENT → INTERVIEW
     */
    @Transactional
    public ProjectResponse startInterview(Long projectId, Long userId) {
        Project project = projectAccessService.getProjectWithOwnerAccess(projectId, userId);
        project.startInterview();

        return ProjectResponse.from(project);
    }

    /**
     * 서류 단계 종료 (면접 없이 프로젝트 종료): DOCUMENT → DOCUMENT_COMPLETE
     * 서류 심사가 보류(HOLD) 상태인 지원자가 없어야 함 
     */
    @Transactional
    public ProjectResponse completeDocument(Long projectId, Long userId) {
        Project project = projectAccessService.getProjectWithOwnerAccess(projectId, userId);

        long holdCount = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ScreeningResult.HOLD);
        if (holdCount > 0) {
            throw new IllegalStateException(
                    "서류 단계를 종료할 수 없습니다. 서류 심사가 보류(HOLD)인 지원자가 " + holdCount + "명 있습니다. 모두 합격/불합격 처리 후 종료해 주세요.");
        }

        project.completeDocument(userId);

        return ProjectResponse.from(project);
    }

    /**
     * 면접 단계 종료 (프로젝트 전체 종료): INTERVIEW → INTERVIEW_COMPLETE
     *
     * 서류 합격자 중 면접 결과가 보류(HOLD) 상태인 지원자가 없어야 함 
     */
    @Transactional
    public ProjectResponse completeAll(Long projectId, Long userId) {
        Project project = projectAccessService.getProjectWithOwnerAccess(projectId, userId);

        long holdCount = applicantRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(
                projectId, ScreeningResult.PASS, ScreeningResult.HOLD);
        if (holdCount > 0) {
            throw new IllegalStateException(
                    "면접 단계를 종료할 수 없습니다. 면접 결과가 보류(HOLD)인 지원자가 " + holdCount + "명 있습니다. 모두 합격/불합격 처리 후 종료해 주세요.");
        }

        project.completeAll(userId);

        return ProjectResponse.from(project);
    }
}
