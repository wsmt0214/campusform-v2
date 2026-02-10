package com.campusform.server.recruiting.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;

import lombok.RequiredArgsConstructor;

/**
 * Recruiting(모집) 컨텍스트에서 "단계 종료/프로젝트 종료"를 담당하는 서비스
 *
 * 컨텍스트 기준:
 * - Project: 프로젝트 생성/설정(모집 공고의 설정값) 중심
 * - Recruiting: 모집 프로세스(서류/면접 진행, 마감, 결과 확정, 종료) 중심
 *
 * 따라서 "서류 종료", "면접 종료(전체 종료)" 같은 단계 전환은 Recruiting 쪽에서 오케스트레이션합니다.
 */
@Service
@RequiredArgsConstructor
public class RecruitingStageService {

    private final ProjectRepository projectRepository;
    private final ApplicantRepository applicantRepository;

    /**
     * 서류 단계 종료 (면접 없이 프로젝트 종료): DOCUMENT → DOCUMENT_COMPLETE
     *
     * 전제:
     * - 해당 프로젝트에 서류 상태가 HOLD인 지원자가 없어야 합니다.
     * - Project 엔티티의 상태가 DOCUMENT 여야 합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    @Transactional
    public ProjectResponse completeDocument(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // HOLD 상태 지원자 검증
        long holdCount = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ApplicantStatus.HOLD);
        if (holdCount > 0) {
            throw new IllegalStateException(
                    "서류 단계를 종료할 수 없습니다. 서류 심사가 보류(HOLD)인 지원자가 " + holdCount + "명 있습니다. 모두 합격/불합격 처리 후 종료해 주세요.");
        }

        // 상태 전환: DOCUMENT → DOCUMENT_COMPLETE (내부에서 OWNER + 상태 검증 수행)
        project.completeDocument(userId);

        return ProjectResponse.from(project);
    }

    /**
     * 면접 단계 종료 (프로젝트 전체 종료): INTERVIEW → INTERVIEW_COMPLETE
     *
     * 전제:
     * - 해당 프로젝트에 면접 상태가 HOLD인 지원자가 없어야 합니다.
     * - Project 엔티티의 상태가 INTERVIEW 여야 합니다.
     * - 요청한 사용자가 프로젝트의 OWNER여야 합니다.
     */
    @Transactional
    public ProjectResponse completeAll(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // HOLD 상태 지원자 검증
        long holdCount = applicantRepository.countByProjectIdAndInterviewStatus(projectId, ApplicantStatus.HOLD);
        if (holdCount > 0) {
            throw new IllegalStateException(
                    "면접 단계를 종료할 수 없습니다. 면접 결과가 보류(HOLD)인 지원자가 " + holdCount + "명 있습니다. 모두 합격/불합격 처리 후 종료해 주세요.");
        }

        // 상태 전환: INTERVIEW → INTERVIEW_COMPLETE (내부에서 OWNER + 상태 검증 수행)
        project.completeAll(userId);

        return ProjectResponse.from(project);
    }
}
