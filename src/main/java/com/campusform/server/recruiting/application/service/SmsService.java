package com.campusform.server.recruiting.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.application.component.MessageGenerator;
import com.campusform.server.recruiting.application.dto.request.SmsTemplateSaveRequest;
import com.campusform.server.recruiting.application.dto.response.SmsPreviewResponse;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.message.MessageTemplate;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmsService {
    // 문자 메시지 내용, 템플릿, 발송 서비스
    private final ApplicantRepository applicantRepository;
    private final MessageTemplateRepository templateRepository;
    private final MessageGenerator messageGenerator;
    private final ProjectRepository projectRepository;

    /**
     * 문자 관련 로직만
     * 템플릿 저장
     *
     * @param projectId
     * @param stage
     * @param request   수정사항 문자열을 Enum으로 변환하여 Type Safety를 확보함
     */
    @Transactional
    public void saveTemplate(Long projectId, RecruitmentStage stage, SmsTemplateSaveRequest request) {
        // 프로젝트 상태 검증: 해당 단계가 활성 상태인지 확인
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));
        validateStageActive(project, stage);

        ScreeningResult applicantStatus = request.getStatus();
        // 1. 없으면 생성, 있으면 가져오기
        MessageTemplate template = templateRepository.findByProjectId(projectId)
                .orElseGet(() -> templateRepository.save(MessageTemplate.createEmpty(projectId)));

        // 2. 내용 업데이트 (엔티티 메서드 활용)
        template.updateTemplate(stage, applicantStatus, request.getContent());
        // Dirty Checking으로 자동 저장됨
    }

    /**
     * 개인별 문자메시지 미리보기
     * 지원자의 현재 상태를 DB에서 조회하여 해당 템플릿을 사용합니다.
     */
    @Transactional(readOnly = true)
    public SmsPreviewResponse getPreview(Long projectId, Long applicantId, RecruitmentStage stage) {
        // 1. 지원자 조회
        Applicant applicant = applicantRepository.findById(applicantId)
                .filter(a -> a.getProjectId().equals(projectId))
                .orElseThrow(() -> new IllegalArgumentException("지원자가 없습니다."));

        // 면접 단계에서는 서류 합격자만 미리보기 가능 (서류 불합격자는 면접 대상 아님)
        if (stage == RecruitmentStage.INTERVIEW && applicant.getDocumentStatus() != ScreeningResult.PASS) {
            throw new IllegalArgumentException("서류 합격자만 면접 단계의 대상이 됩니다. 현재 서류 상태: " + applicant.getDocumentStatus());
        }

        // 2. 지원자의 현재 상태 조회 (DB에서 가져옴)
        ScreeningResult currentStatus = (stage == RecruitmentStage.DOCUMENT)
                ? applicant.getDocumentStatus()
                : applicant.getInterviewStatus();

        // 3. 메시지 생성 (지원자의 현재 상태를 사용)
        String finalContent = messageGenerator.generateMessage(
                projectId,
                stage,
                currentStatus,
                applicant.getName(),
                applicant.getPosition() != null ? applicant.getPosition() : "-");

        // 5. 응답 DTO 생성
        SmsPreviewResponse.PreviewMessage message = SmsPreviewResponse.PreviewMessage.builder()
                .applicantId(applicant.getId())
                .name(applicant.getName())
                .phoneNumber(applicant.getPhone())
                .info(makeInfoString(applicant))
                .content(finalContent)
                .build();

        return SmsPreviewResponse.builder()
                .count(1)
                .messages(List.of(message))
                .build();
    }

    /**
     * 저장된 템플릿 조회
     * 
     * @param projectId 프로젝트 ID
     * @param stage     모집 단계
     * @param status    지원자 상태
     * @return 템플릿 내용 (없으면 빈 문자열)
     */
    @Transactional(readOnly = true)
    public String getTemplate(Long projectId, RecruitmentStage stage, ScreeningResult status) {
        return templateRepository.findByProjectId(projectId)
                .map(t -> t.getTemplateContent(stage, status))
                .orElse("");
    }

    // 미리보기용 정보 문자열 생성 ("학교 / 전공 / 지원분야")
    private String makeInfoString(Applicant applicant) {
        return String.format("%s / %s / %s",
                applicant.getSchool() != null ? applicant.getSchool() : "-",
                applicant.getMajor() != null ? applicant.getMajor() : "-",
                applicant.getPosition() != null ? applicant.getPosition() : "-");
    }

    /**
     * 요청한 모집 단계(stage)에 해당하는 프로젝트 상태인지 검증
     */
    private void validateStageActive(Project project, RecruitmentStage stage) {
        if (stage == RecruitmentStage.DOCUMENT) {
            project.validateDocumentStage();
        } else if (stage == RecruitmentStage.INTERVIEW) {
            project.validateInterviewStage();
        }
    }
}
