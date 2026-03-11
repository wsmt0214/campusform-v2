package com.campusform.server.recruiting.application.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.application.service.ProjectAuthorizationService;
import com.campusform.server.recruiting.application.dto.response.result.ResultListResponse;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;

/**
 * 합격/불합격 결과 조회 전용 서비스 (CQRS 패턴)
 * 기존 읽기 + 쓰기가 함께 구현돼 있어 코드가 비대해짐 -> 쿼리(Query)와 커맨드(Command) 책임이 분리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResultQueryService {

    private final ApplicantRepository applicantRepository;
    private final MessageTemplateRepository templateRepository;
    private final SmsMessageComposer smsMessageComposer;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * 단계별 합격/불합격 명단 및 통계·템플릿 조회
     */
    public ResultListResponse getResults(Long projectId, RecruitmentStage stage, ScreeningResult status,
            Long userId) {
        // 프로젝트 관리자 권한 검증 (OWNER 또는 ADMIN)
        projectAuthorizationService.assertAdmin(projectId, userId);

        // 1. Enum으로 변환
        List<Applicant> applicants;
        // 2. 단계(Stage)에 따라 데이터 조회 분기 처리
        if (stage == RecruitmentStage.DOCUMENT) {
            applicants = applicantRepository.findByProjectIdAndDocumentStatus(projectId, status);
        } else {
            applicants = applicantRepository.findByProjectIdAndDocumentStatusAndInterviewStatus(
                    projectId, ScreeningResult.PASS, status);
        }

        // 3. 통계 데이터 계산
        long totalCount = (stage == RecruitmentStage.DOCUMENT)
                ? applicantRepository.countByProjectId(projectId)
                : applicantRepository.countByProjectIdAndDocumentStatus(projectId, ScreeningResult.PASS);
        long currentPassCount = applicants.size();

        // 경쟁률 (전체 / 현재합격자) - 0으로 나누기 방지
        String competitionRate = currentPassCount > 0
                ? String.format("%.1f:1", (double) totalCount / currentPassCount)
                : "0:1";

        // 성비 계산 (현재 조회된 명단 기준)
        ResultListResponse.GenderRatio genderRatio = calculateGenderRatio(applicants);

        // 4. 저장된 템플릿 가져오기 (없으면 빈 문자열)
        String templateContent = templateRepository.findByProjectId(projectId)
                .map(t -> t.getTemplateContent(stage, status))
                .orElse("");

        // 5. DTO 변환 및 반환 (개인화된 메시지 포함)
        List<ResultListResponse.ApplicantSummary> applicantSummaries = applicants.stream()
                .map(app -> {
                    // 각 지원자별로 개인화된 메시지 생성 (@이름, @포지션 치환)
                    String personalizedMessage = smsMessageComposer.compose(
                            projectId, stage, status,
                            app.getName(),
                            app.getPosition() != null ? app.getPosition() : "-");
                    return ResultListResponse.ApplicantSummary.builder()
                            .applicantId(app.getId())
                            .name(app.getName())
                            .school(app.getSchool())
                            .major(app.getMajor())
                            .position(app.getPosition())
                            .phoneNumber(app.getPhone())
                            .personalizedMessage(personalizedMessage != null ? personalizedMessage : "")
                            .build();
                })
                .collect(Collectors.toList());

        return ResultListResponse.builder()
                .stats(ResultListResponse.ResultStats.builder()
                        .totalApplicantCount(totalCount)
                        .currentStagePassCount(currentPassCount)
                        .competitionRate(competitionRate)
                        .genderRatio(genderRatio)
                        .build())
                .template(ResultListResponse.TemplateInfo.builder().content(templateContent).build())
                .applicants(applicantSummaries)
                .build();
    }

    private ResultListResponse.GenderRatio calculateGenderRatio(List<Applicant> applicants) {
        if (applicants.isEmpty()) {
            return ResultListResponse.GenderRatio.builder()
                    .malePercent(0)
                    .femalePercent(0)
                    .otherPercent(0)
                    .build();
        }
        int totalCount = applicants.size();
        long maleCount = applicants.stream()
                .filter(a -> {
                    String g = a.getGender();
                    return g != null && ("남".equals(g) || "Male".equalsIgnoreCase(g) || "남자".equals(g) || "남성".equals(g));
                })
                .count();
        long femaleCount = applicants.stream()
                .filter(a -> {
                    String g = a.getGender();
                    return g != null && ("여".equals(g) || "Female".equalsIgnoreCase(g) || "여성".equals(g) || "여자".equals(g));
                })
                .count();
        int malePercent = (int) ((maleCount * 100) / totalCount);
        int femalePercent = (int) ((femaleCount * 100) / totalCount);
        int otherPercent = 100 - malePercent - femalePercent;
        return ResultListResponse.GenderRatio.builder()
                .malePercent(malePercent)
                .femalePercent(femalePercent)
                .otherPercent(otherPercent)
                .build();
    }
}
