package com.campusform.server.recruiting.application.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.application.service.ProjectAuthorizationService;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantStatusUpdateResponse;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 커맨드(Command) 전용 서비스 (CQRS 패턴)
 * 기존 읽기 + 쓰기가 함께 구현돼 있어 코드가 비대해짐 -> 쿼리(Query)와 커맨드(Command) 책임이 분리
 */
@Service
@RequiredArgsConstructor
public class ApplicantCommandService {

    private final ApplicantRepository applicantRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * 지원자 서류/면접 상태(보류·합격·불합격) 변경 
     */
    @Transactional
    public ApplicantStatusUpdateResponse updateApplicantStatus(Long projectId, Long applicantId,
            RecruitmentStage stage, ScreeningResult status, Long userId) {

        projectAuthorizationService.assertAdmin(projectId, userId);

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원자입니다."));
        if (!applicant.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("지원자가 속한 프로젝트와 요청된 프로젝트가 일치하지 않습니다.");
        }

        Project project = projectRepository.findById(applicant.getProjectId())
                .orElseThrow(() -> new IllegalStateException("지원자가 속한 프로젝트를 찾을 수 없습니다."));
        
        // 프로젝트 단계 active 여부
        validateStageActive(project, stage);
        // 서류 합격자인지 검증 
        validateDocumentPassForInterview(applicant, stage);

        applicant.updateScreeningResult(stage, status);

        // 변경된 결과 응답 생성 , 현재 상태 확인
        ScreeningResult updatedStatus = (stage == RecruitmentStage.DOCUMENT)
                ? applicant.getDocumentStatus()
                : applicant.getInterviewStatus();

        return ApplicantStatusUpdateResponse.builder()
                .applicantId(applicant.getId())
                .currentStatus(updatedStatus.name())
                .updateAt(LocalDateTime.now())
                .build();
    }

    /**
     * 지원자 즐겨찾기 토글 (서류/면접 단계별)
     */
    @Transactional
    public void toggleBookmark(Long projectId, Long applicantId, RecruitmentStage stage, Long userId) {

        projectAuthorizationService.assertAdmin(projectId, userId);

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("지원자가 존재하지 않습니다."));

        if (!applicant.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("지원자가 속한 프로젝트와 요청된 프로젝트가 일치하지 않습니다.");
        }
        // 서류 합격자인지 검증 
        validateDocumentPassForInterview(applicant, stage);
        
        applicant.toggleBookmark(stage);
    }

    private void validateStageActive(Project project, RecruitmentStage stage) {
        if (stage == RecruitmentStage.DOCUMENT) {
            project.validateDocumentStage();
        } else if (stage == RecruitmentStage.INTERVIEW) {
            project.validateInterviewStage();
        }
    }

    private void validateDocumentPassForInterview(Applicant applicant, RecruitmentStage stage) {
        if (stage == RecruitmentStage.INTERVIEW && applicant.getDocumentStatus() != ScreeningResult.PASS) {
            throw new IllegalArgumentException(
                    "서류 합격자만 면접 단계의 대상이 됩니다. 현재 서류 상태: " + applicant.getDocumentStatus());
        }
    }
}
