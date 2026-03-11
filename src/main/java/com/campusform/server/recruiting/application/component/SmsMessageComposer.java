package com.campusform.server.recruiting.application.component;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

/**
 * SMS 발송 문자 조합기
 * 지원자 이름·포지션 변수를 치환한 최종 SMS 문자를 생성하는 헬퍼 컴포넌트
 * SmsService, ResultQueryService, ApplicantEventHandler 등 여러 곳에서 공유해서 사용됨
 */
@Component
@RequiredArgsConstructor
public class SmsMessageComposer {

    private final MessageTemplateRepository messageTemplateRepository;

    /** DB에 커스텀 템플릿이 없을 경우 사용하는 기본 문자 내용 */
    private static final Map<String, String> DEFAULT_TEMPLATES = Map.of(
            "DOCUMENT_PASS", "[CAMPUS:FORM] @이름님, 축하합니다! 서류 전형에 합격하셨습니다. (포지션: @포지션)",
            "DOCUMENT_FAIL", "[CAMPUS:FORM] @이름님, 아쉽게도 이번 서류 전형에서 모시지 못하게 되었습니다.",
            "INTERVIEW_PASS", "[CAMPUS:FORM] @이름님, 축하합니다! 면접 전형에 합격하셨습니다. (포지션: @포지션)",
            "INTERVIEW_FAIL", "[CAMPUS:FORM] @이름님, 아쉽게도 이번 면접 전형에서 모시지 못하게 되었습니다."
    );

    /**
     * 최종 SMS 문자 생성
     * 1. DB에서 프로젝트별 커스텀 템플릿 조회
     * 2. 없으면 기본 템플릿 사용
     * 3. @이름, @포지션 변수 치환
     */
    public String compose(Long projectId, RecruitmentStage stage, ScreeningResult status,
            String applicantName, String positionName) {
        String template = resolveTemplate(projectId, stage, status);

        if (template == null || template.isBlank()) {
            return null;
        }

        return template
                .replace("@이름", applicantName != null ? applicantName : "")
                .replace("@포지션", positionName != null ? positionName : "-");
    }

    /**
     * 템플릿 조회: DB 커스텀 템플릿 우선, 없으면 기본 템플릿 반환
     */
    private String resolveTemplate(Long projectId, RecruitmentStage stage, ScreeningResult status) {
        String dbTemplate = messageTemplateRepository.findByProjectId(projectId)
                .map(t -> t.getTemplateContent(stage, status))
                .orElse(null);

        if (dbTemplate != null && !dbTemplate.isBlank()) {
            return dbTemplate;
        }

        return DEFAULT_TEMPLATES.get(stage.name() + "_" + status.name());
    }
}
