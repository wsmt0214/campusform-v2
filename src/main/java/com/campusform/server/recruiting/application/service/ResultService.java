package com.campusform.server.recruiting.application.service;

import com.campusform.server.recruiting.application.component.MessageGenerator;
import com.campusform.server.recruiting.application.dto.request.ResultAnnouncementRequest;
import com.campusform.server.recruiting.application.dto.response.ResultListResponse;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResultService {
    private final ApplicantRepository applicantRepository;
    private final MessageTemplateRepository templateRepository;
    private final MessageGenerator messageGenerator;

    // 합,불 명단/ 통계 조회
    public ResultListResponse getResults(Long projectId, StageStatus stage, ApplicantStatus status){

        //1. Enum으로 변환
        List<Applicant> applicants;

        // 2. 단계(Stage)에 따라 데이터 조회 분기 처리
        if (stage==StageStatus.DOCUMENT) {
            applicants = applicantRepository.findByProjectIdAndDocumentStatus(projectId, status);
        } else {
            // INTERVIEW
            applicants = applicantRepository.findByProjectIdAndInterviewStatus(projectId, status);
        }

        // 3. 통계 데이터 계산
        long totalCount;
        if (stage == StageStatus.DOCUMENT) {
            // 서류 단계: 전체 지원자 수
            totalCount = applicantRepository.countByProjectId(projectId);
        } else {
            // 면접 단계: 서류 합격자 수만 (서류 탈락자는 제외)
            totalCount = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ApplicantStatus.PASS);
        }
        long currentPassCount = applicants.size(); // 현재 리스트 개수 (PASS라고 가정)

        // 경쟁률 (전체 / 현재합격자) - 0으로 나누기 방지
        String competitionRate = currentPassCount > 0
                ? String.format("%.1f:1", (double) totalCount / currentPassCount)
                : "0:1";

        // 성비 계산 (현재 조회된 명단 기준)
        ResultListResponse.GenderRatio genderRatio = calculateGenderRatio(applicants);

        // 4. 저장된 템플릿 가져오기 (없으면 빈 문자열)
        //String templateContent = getTemplateContent(projectId, stage, status);
        String templateContent=templateRepository.findByProjectId(projectId)
                .map(t->t.getTemplateContent(stage,status))
                .orElse("");

        // 5. DTO 변환 및 반환 (개인화된 메시지 포함)
        List<ResultListResponse.ApplicantSummary> applicantSummaries = applicants.stream()
                .map(app -> {
                    // 각 지원자별로 개인화된 메시지 생성 (@이름, @포지션 치환)
                    String personalizedMessage = messageGenerator.generateMessage(
                            projectId,
                            stage,
                            status,
                            app.getName(),
                            app.getPosition() != null ? app.getPosition() : "-"
                    );
                    
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
                .template(ResultListResponse.TemplateInfo.builder()
                        .content(templateContent)
                        .build())
                .applicants(applicantSummaries)
                .build();
    }

    // 성비 계산 메서드 (남성, 여성, 기타)
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
                    String gender = a.getGender();
                    return gender != null && ("남".equals(gender) || "Male".equalsIgnoreCase(gender)|| "남자".equals(gender)|| "남성".equals(gender));
                })
                .count();

        long femaleCount = applicants.stream()
                .filter(a -> {
                    String gender = a.getGender();
                    return gender != null && ("여".equals(gender) || "Female".equalsIgnoreCase(gender) || "여성".equals(gender)|| "여자".equals(gender));
                })
                .count();

        long otherCount = totalCount - maleCount - femaleCount;

        int malePercent = (int) ((maleCount * 100) / totalCount);
        int femalePercent = (int) ((femaleCount * 100) / totalCount);
        int otherPercent = 100 - malePercent - femalePercent; // 나머지 비율

        return ResultListResponse.GenderRatio.builder()
                .malePercent(malePercent)
                .femalePercent(femalePercent)
                .otherPercent(otherPercent)
                .build();
    }

    @Transactional
    public void announceResults(Long projectId, ResultAnnouncementRequest request){
        // 1. 대상 지원자 조회
        List<Applicant> applicants = applicantRepository.findAllById(request.applicantIds());

        // 프로젝트 소속 검증
        boolean allBelongToProject = applicants.stream()
                .allMatch(a -> projectId.equals(a.getProjectId()));
        if (!allBelongToProject) {
            throw new IllegalArgumentException("해당 프로젝트에 속하지 않는 지원자가 포함되어 있습니다.");
        }

        StageStatus stage = StageStatus.valueOf(request.stage().toUpperCase());
        //2. 상태 변경 (도메인 로직 실행)
        for (Applicant applicant : applicants){
            applicant.updateApplicantStatus(stage,request.status());
        }

        //3. 저장 ( 이때 update 쿼리가 나가고 registerEvent 했던 이벤트들이 발행 )
        applicantRepository.saveAll(applicants);
    }

}
