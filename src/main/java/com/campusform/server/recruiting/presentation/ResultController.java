package com.campusform.server.recruiting.presentation;


import com.campusform.server.recruiting.application.service.ResultService;
import com.campusform.server.recruiting.application.service.SmsService;
import com.campusform.server.recruiting.application.dto.request.ResultAnnouncementRequest;
import com.campusform.server.recruiting.application.dto.request.SmsTemplateSaveRequest;
import com.campusform.server.recruiting.application.dto.response.ResultListResponse;
import com.campusform.server.recruiting.application.dto.response.SmsPreviewResponse;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @Parameter(description = "조회할 모집 단계") @RequestParam StageStatus stage,
            @Parameter(description = "조회할 지원자 상태 (PASS, FAIL 등)") @RequestParam ApplicantStatus status
    ){
        ResultListResponse response=resultService.getResults(projectId,stage,status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "SMS 템플릿 저장", description = "단계별(서류, 면접) 합격/불합격 통보에 사용할 SMS 문자 메시지 템플릿을 저장합니다.")
    @PostMapping("/sms/templates")
    public ResponseEntity<Void> saveSmsTemplate(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "저장할 템플릿의 모집 단계") @RequestParam StageStatus stage,
            @RequestBody SmsTemplateSaveRequest request
    ) {
        smsService.saveTemplate(projectId, stage, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "SMS 발송 미리보기", description = "특정 지원자에게 발송될 SMS 메시지를 미리 확인합니다.")
    @GetMapping("/applicants/{applicantId}/sms/preview")
    public ResponseEntity<SmsPreviewResponse> getSmsPreview(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "미리보기할 지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "적용할 템플릿의 모집 단계") @RequestParam StageStatus stage,
            @Parameter(description = "적용할 템플릿의 지원자 상태") @RequestParam ApplicantStatus status
    ) {
        SmsPreviewResponse response = smsService.getPreview(projectId, applicantId,stage,status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결과 최종 통보", description = "선택된 지원자들에게 저장된 템플릿을 사용하여 합격/불합격 결과를 SMS로 최종 통보합니다.")
    @PostMapping("/announce")
    public ResponseEntity<Void> announceResult(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @RequestBody ResultAnnouncementRequest request){
        // 1. 만약 요청 데이터가 이상하면 여기서 컷! (Validation)
        if (request.applicantIds() == null || request.applicantIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 2. 서비스 호출
        resultService.announceResults(projectId,request);

        // 3. 클라이언트에게 HTTP 상태 코드(200)로 응답
        return ResponseEntity.ok().build();

    }
}
