package com.campusform.server.recruiting.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.application.service.ProjectAuthorizationService;
import com.campusform.server.recruiting.application.dto.request.ResultAnnouncementRequest;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import lombok.RequiredArgsConstructor;

/**
 * 합격/불합격 결과 확정(공지) 전용 서비스 (CQRS 패턴)
 * 기존 읽기 + 쓰기가 함께 구현돼 있어 코드가 비대해짐 -> 쿼리(Query)와 커맨드(Command) 책임이 분리
 */
@Service
@RequiredArgsConstructor
public class ResultCommandService {

    private final ApplicantRepository applicantRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * 선택한 지원자들에 대해 합격/불합격 상태를 일괄 확정 (이벤트 발행 포함)
     */
    @Transactional
    public void announceResults(Long projectId, ResultAnnouncementRequest request, Long userId) {
        projectAuthorizationService.assertAdmin(projectId, userId);

        List<Applicant> applicants = applicantRepository.findAllById(request.applicantIds());
        boolean allBelongToProject = applicants.stream().allMatch(a -> projectId.equals(a.getProjectId()));
        if (!allBelongToProject) {
            throw new IllegalArgumentException("해당 프로젝트에 속하지 않는 지원자가 포함되어 있습니다.");
        }

        RecruitmentStage stage = RecruitmentStage.valueOf(request.stage().toUpperCase());
        if (stage == RecruitmentStage.INTERVIEW) {
            boolean hasDocFail = applicants.stream()
                    .anyMatch(a -> a.getDocumentStatus() != ScreeningResult.PASS);
            if (hasDocFail) {
                throw new IllegalArgumentException(
                        "면접 결과 발표 대상에 서류 불합격자가 포함되어 있습니다. 서류 합격자만 면접 결과 발표 대상이 됩니다.");
            }
        }

        for (Applicant applicant : applicants) {
            applicant.updateScreeningResult(stage, request.status());
        }
        applicantRepository.saveAll(applicants);
    }
}
