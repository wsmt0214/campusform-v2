package com.campusform.server.recruiting.presentation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.campusform.server.global.security.CurrentUserId;
import com.campusform.server.recruiting.application.dto.request.interview.AssignManualInterviewRequest;
import com.campusform.server.recruiting.application.dto.response.interview.ManualInterviewAssignmentResponse;
import com.campusform.server.recruiting.application.service.ManualInterviewAssignmentService;
import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 수동 면접 배정 API 컨트롤러
 */
@Tag(name = "수동 면접 배정", description = "스마트 시간표와 별개로, 특정 지원자에게 면접 시간을 수동으로 배정하는 API")
@RestController
@RequestMapping("/api/projects/{projectId}/interview/manual-assignments")
@RequiredArgsConstructor
public class ManualInterviewAssignmentController {

    private final ManualInterviewAssignmentService manualAssignmentService;

    @Operation(summary = "지원자 면접 시간 수동 배정", description = "특정 지원자에게 면접 날짜와 시간을 수동으로 배정합니다. 스마트 시간표 결과보다 우선 적용됩니다.")
    @PostMapping
    public ResponseEntity<ManualInterviewAssignmentResponse> assignInterview(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @RequestBody AssignManualInterviewRequest request,
            @CurrentUserId Long userId) {
        manualAssignmentService.assignInterview(
                projectId,
                request.getApplicantId(),
                request.getInterviewDate(),
                request.getStartTime(),
                userId);

        // 배정 후 조회하여 응답
        Optional<ManualInterviewAssignment> assignment = manualAssignmentService.getAssignment(
                projectId, request.getApplicantId(), userId);
        return ResponseEntity.ok(ManualInterviewAssignmentResponse.from(assignment.orElseThrow()));
    }

    @Hidden
    @Operation(summary = "지원자의 수동 배정 정보 조회", description = "특정 지원자에게 수동으로 배정된 면접 시간 정보를 조회합니다.")
    @GetMapping("/applicants/{applicantId}")
    public ResponseEntity<ManualInterviewAssignmentResponse> getAssignment(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "조회할 지원자 ID") @PathVariable Long applicantId,
            @CurrentUserId Long userId) {
        Optional<ManualInterviewAssignment> assignment = manualAssignmentService.getAssignment(
                projectId, applicantId, userId);
        return assignment
                .map(ManualInterviewAssignmentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Hidden
    @Operation(summary = "프로젝트의 모든 수동 배정 정보 조회", description = "해당 프로젝트에 수동으로 배정된 모든 면접 시간 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ManualInterviewAssignmentResponse>> getAllAssignments(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        List<ManualInterviewAssignment> assignments = manualAssignmentService.getAllAssignments(
                projectId, userId);
        List<ManualInterviewAssignmentResponse> responses = assignments.stream()
                .map(ManualInterviewAssignmentResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "지원자의 수동 배정 정보 삭제", description = "특정 지원자에게 수동으로 배정된 면접 시간 정보를 삭제합니다.")
    @DeleteMapping("/applicants/{applicantId}")
    public ResponseEntity<Void> removeAssignment(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "삭제할 지원자 ID") @PathVariable Long applicantId,
            @CurrentUserId Long userId) {
        manualAssignmentService.removeAssignment(projectId, applicantId, userId);
        return ResponseEntity.noContent().build();
    }
}
