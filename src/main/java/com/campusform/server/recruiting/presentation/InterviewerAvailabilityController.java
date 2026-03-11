package com.campusform.server.recruiting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.request.interview.SetRequiredInterviewerRequest;
import com.campusform.server.recruiting.application.dto.request.interview.UpdateRequiredInterviewersRequest;
import com.campusform.server.recruiting.application.dto.request.interview.UpsertInterviewerAvailabilityRequest;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewerAvailabilityResponse;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewerAvailabilitySummaryResponse;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewerListResponse;
import com.campusform.server.recruiting.application.dto.response.interview.RequiredInterviewersResponse;
import com.campusform.server.recruiting.application.service.InterviewerAvailabilityService;
import com.campusform.server.recruiting.application.service.InterviewerQueryService;
import com.campusform.server.recruiting.application.service.RequiredInterviewerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 설정 - 2. 면접관 시간 등록
 */
@Tag(name = "면접관 관리", description = "면접관(운영진) 목록 조회, 가능 시간 제출, 필수 면접관 설정 API")
@RestController
@RequestMapping("/api/recruiting/projects")
@RequiredArgsConstructor
public class InterviewerAvailabilityController {

        private final InterviewerAvailabilityService interviewerAvailabilityService;
        private final InterviewerQueryService interviewerQueryService;
        private final RequiredInterviewerService requiredInterviewerService;
        private final AuthService authService;

        @Operation(summary = "면접관(운영진) 목록 조회", description = "프로젝트에 참여하는 모든 면접관(운영진)의 정보를 조회합니다.")
        @GetMapping("/{projectId}/interviewers")
        public ResponseEntity<InterviewerListResponse> getInterviewerList(
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                InterviewerListResponse response = interviewerQueryService.getInterviewerList(projectId, userId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "특정 면접관의 가능 시간 조회", description = "지정된 면접관이 제출한 면접 가능 시간 전체를 조회합니다.")
        @GetMapping("/{projectId}/interviewers/{adminId}/availability")
        public ResponseEntity<InterviewerAvailabilityResponse> getInterviewerAvailability(
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        @Parameter(description = "조회할 면접관의 사용자 ID") @PathVariable Long adminId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                InterviewerAvailabilityResponse response = interviewerAvailabilityService
                                .getInterviewerAvailability(projectId, userId, adminId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "특정 면접관의 가능 시간 저장/수정", description = "지정된 면접관의 면접 가능 시간을 제출(또는 전체 수정)합니다. 기존 데이터는 모두 삭제되고 새로 제출된 시간으로 대체됩니다.")
        @PutMapping("/{projectId}/interviewers/{adminId}/availability")
        public ResponseEntity<InterviewerAvailabilityResponse> replaceInterviewerAvailability(
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        @Parameter(description = "시간을 제출할 면접관의 사용자 ID") @PathVariable Long adminId,
                        Authentication authentication,
                        @Valid @RequestBody UpsertInterviewerAvailabilityRequest request) {
                Long userId = authService.extractUserId(authentication);
                InterviewerAvailabilityResponse response = interviewerAvailabilityService
                                .replaceInterviewerAvailability(projectId, userId, adminId, request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "시간대별 가능 면접관 수 집계 조회", description = "정의된 모든 면접 슬롯 별로 참여 가능한 면접관 인원 수를 집계하여 보여줍니다. (시간표의 '전체' 탭)")
        @GetMapping("/{projectId}/interviewers/availability-summary")
        public ResponseEntity<InterviewerAvailabilitySummaryResponse> getAvailabilitySummary(
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                InterviewerAvailabilitySummaryResponse response = interviewerAvailabilityService
                                .getAvailabilitySummary(projectId, userId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "필수 면접관 목록 조회", description = "스마트 시간표 생성 시, 모든 슬롯에 필수로 배정되어야 하는 면접관 목록을 조회합니다.")
        @GetMapping("/{projectId}/required-interviewers")
        public ResponseEntity<RequiredInterviewersResponse> getRequiredInterviewers(
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        Authentication authentication) {
                Long userId = authService.extractUserId(authentication);
                RequiredInterviewersResponse response = requiredInterviewerService
                                .getRequiredInterviewers(projectId, userId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "필수 면접관 전체 설정", description = "필수 면접관 목록을 새로 제출된 목록으로 전체 교체합니다.")
        @PutMapping("/{projectId}/required-interviewers")
        public ResponseEntity<RequiredInterviewersResponse> replaceRequiredInterviewers(
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        Authentication authentication,
                        @Valid @RequestBody UpdateRequiredInterviewersRequest request) {
                Long userId = authService.extractUserId(authentication);
                RequiredInterviewersResponse response = requiredInterviewerService
                                .replaceAllRequiredInterviewers(projectId, userId, request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "필수 면접관 개별 설정/해제", description = "특정 면접관을 필수 면접관으로 설정하거나 해제합니다.")
        @PutMapping("/{projectId}/required-interviewers/{adminId}")
        public ResponseEntity<RequiredInterviewersResponse> setRequiredInterviewer(
                        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
                        @Parameter(description = "설정할 면접관의 사용자 ID") @PathVariable Long adminId,
                        Authentication authentication,
                        @Valid @RequestBody SetRequiredInterviewerRequest request) {
                Long userId = authService.extractUserId(authentication);
                RequiredInterviewersResponse response = requiredInterviewerService
                                .updateRequiredInterviewerStatus(projectId, userId, adminId, request);
                return ResponseEntity.ok(response);
        }
}