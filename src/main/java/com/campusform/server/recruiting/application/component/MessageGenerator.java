package com.campusform.server.recruiting.application.component;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.repository.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessageGenerator {
    private final MessageTemplateRepository messageTemplateRepository;

    // 기본 템플릿 (DB에 없을 경우 사용)
    private static final Map<String, String> DEFAULT_TEMPLATES = Map.of(
            "DOCUMENT_PASS", "[CAMPUS:FORM] @이름님, 축하합니다! 서류 전형에 합격하셨습니다. (포지션: @포지션)",
            "DOCUMENT_FAIL", "[CAMPUS:FORM] @이름님, 아쉽게도 이번 서류 전형에서 모시지 못하게 되었습니다.",
            "INTERVIEW_PASS", "[CAMPUS:FORM] @이름님, 축하합니다! 면접 전형에 합격하셨습니다. (포지션: @포지션)",
            "INTERVIEW_FAIL", "[CAMPUS:FORM] @이름님, 아쉽게도 이번 면접 전형에서 모시지 못하게 되었습니다."
    );
    /**
     * 메시지 생성 메인 함수
     * 1. DB에서 프로젝트별 커스텀 템플릿 조회
     * 2. 없으면 기본 템플릿 사용
     * 3. 변수 치환 (@지원자이름, @포지션)
     */
    public String generateMessage(Long projectId,RecruitmentStage stage, ApplicantStatus status, String applicantName, String positionName) {
        // 1. 템플릿 내용 가져오기
        String template = getTemplateContent(projectId, stage, status);

        // 2. 템플릿이 없거나 비어있으면 null 반환 (발송 안함)
        if (template == null || template.isBlank()) {
            return null;
        }

        // 3. 변수 치환 (@이름, @포지션)
        return template
                .replace("@이름", applicantName != null ? applicantName : "")
                .replace("@포지션", positionName != null ? positionName : "-");
    }

    /**
     * 템플릿 조회: DB 우선 -> 없으면 기본값
     * @param projectId
     * @param stage
     * @param status
     * @return
     */
    private String getTemplateContent(Long projectId, RecruitmentStage stage, ApplicantStatus status) {
        String dbTemplate = messageTemplateRepository.findByProjectId(projectId)
                .map(t->t.getTemplateContent(stage, status))
                .orElse(null);
        if (dbTemplate != null && !dbTemplate.isBlank()) {
            return dbTemplate;
        }
        return getDefaultTemplate(stage,status);
    }

    private String getDefaultTemplate(RecruitmentStage stage, ApplicantStatus status) {
        String key = stage.name() +"_"+status.name();
        return DEFAULT_TEMPLATES.get(key);
    }
}
