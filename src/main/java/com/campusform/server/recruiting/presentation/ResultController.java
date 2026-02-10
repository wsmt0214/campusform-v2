package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.recruiting.application.dto.request.SmsTemplateSaveRequest;
import com.campusform.server.recruiting.application.dto.response.ResultListResponse;
import com.campusform.server.recruiting.application.dto.response.SmsPreviewResponse;
import com.campusform.server.recruiting.application.service.ResultService;
import com.campusform.server.recruiting.application.service.SmsService;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "합불 결과", description = "합격/불합격자 조회, 문자 템플릿 및 결과 통보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}")
public class ResultController {
    private final ResultService resultService;
    private final SmsService smsService;

    @Operation(summary = "단계별 합격/불합격자 명단 조회", description = "특정 단계(서류, 면접)의 합격 또는 불합격자 명단을 조회합니다.")
    @GetMapping("/results")
    public ResponseEntity<ResultListResponse> getResultList(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "조회할 모집 단계") @RequestParam RecruitmentStage stage,
            @Parameter(description = "조회할 지원자 상태 (PASS, FAIL 등)") @RequestParam ScreeningResult status) {
        ResultListResponse response = resultService.getResults(projectId, stage, status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "SMS 템플릿 저장", description = "단계별(서류, 면접) 합격/불합격 통보에 사용할 SMS 문자 메시지 템플릿을 저장합니다.")
    @PostMapping("/sms/templates")
    public ResponseEntity<Void> saveSmsTemplate(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "저장할 템플릿의 모집 단계") @RequestParam RecruitmentStage stage,
            @RequestBody SmsTemplateSaveRequest request) {
        smsService.saveTemplate(projectId, stage, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "SMS 발송 미리보기", description = "특정 지원자에게 발송될 SMS 메시지를 미리 확인합니다. (템플릿의 @이름, @포지션이 실제 값으로 치환된 메시지)")
    @GetMapping("/applicants/{applicantId}/sms/preview")
    public ResponseEntity<SmsPreviewResponse> getSmsPreview(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "미리보기할 지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "적용할 템플릿의 모집 단계") @RequestParam RecruitmentStage stage) {
        SmsPreviewResponse response = smsService.getPreview(projectId, applicantId, stage);
        return ResponseEntity.ok(response);
    }
}
