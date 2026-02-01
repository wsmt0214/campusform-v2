package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.recruiting.application.dto.request.SubmitSlotsRequest;
import com.campusform.server.recruiting.application.dto.response.InterviewSlotListResponse;
import com.campusform.server.recruiting.application.dto.response.SubmitSlotsResponse;
import com.campusform.server.recruiting.application.service.IntervieweeAvailabilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 면접 가능 시간 조사 공개 API
 * 
 * 토큰 기반으로 지원자가 면접 가능 시간 슬롯을 조회하고 제출합니다.
 * 인증 없이 접근 가능한 공개 API입니다.
 */
@Tag(name = "면접 공개 API", description = "지원자가 면접 시간을 제출하기 위해 사용하는 공개 API (인증 불필요)")
@RestController
@RequestMapping("/api/public/interview")
@RequiredArgsConstructor
public class PublicInterviewController {

    private final IntervieweeAvailabilityService intervieweeAvailabilityService;

    @Operation(summary = "제출 가능한 면접 슬롯 목록 조회 (공개)", description = "지원자가 고유 토큰을 사용하여 제출 가능한 전체 면접 슬롯 목록을 조회합니다.", security = {})
    @GetMapping("/slots")
    public ResponseEntity<InterviewSlotListResponse> getSlots(
            @Parameter(description = "프로젝트의 지원자 링크 생성 시 발급된 고유 토큰", required = true) @RequestParam String token) {
        InterviewSlotListResponse response = intervieweeAvailabilityService.getSlotsByToken(token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 가능 시간 제출 (공개)", description = "지원자가 자신의 정보를 입력하고, 가능한 면접 시간 목록을 선택하여 제출합니다.", security = {})
    @PostMapping("/submit")
    public ResponseEntity<SubmitSlotsResponse> submitSlots(
            @Parameter(description = "프로젝트의 지원자 링크 생성 시 발급된 고유 토큰", required = true) @RequestParam String token,
            @RequestBody SubmitSlotsRequest request) {
        intervieweeAvailabilityService.submitSlots(token, request);
        return ResponseEntity.ok(SubmitSlotsResponse.success());
    }
}
