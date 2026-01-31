package com.campusform.server.recruiting.presentation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.request.AssignManualInterviewRequest;
import com.campusform.server.recruiting.application.dto.response.ManualInterviewAssignmentResponse;
import com.campusform.server.recruiting.application.service.ManualInterviewAssignmentService;
import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;

import lombok.RequiredArgsConstructor;

/**
 * 수동 면접 배정 API 컨트롤러
 */
@RestController
@RequestMapping("/api/projects/{projectId}/interview/manual-assignments")
@RequiredArgsConstructor
public class ManualInterviewAssignmentController {

    private final ManualInterviewAssignmentService manualAssignmentService;
    private final AuthService authService;

    /**
     * 지원자에게 면접 일자 및 시간을 수동으로 배정
     *
     * <pre>
     * 예시 요청:
     * POST /api/projects/1/interview/manual-assignments
     * {
     *   "applicantId": 10,
     *   "interviewDate": "2024-06-15",
     *   "startTime": "14:00"
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<ManualInterviewAssignmentResponse> assignInterview(
            @PathVariable Long projectId,
            @RequestBody AssignManualInterviewRequest request,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
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

    /**
     * 지원자의 수동 배정 조회
     */
    @GetMapping("/applicants/{applicantId}")
    public ResponseEntity<ManualInterviewAssignmentResponse> getAssignment(
            @PathVariable Long projectId,
            @PathVariable Long applicantId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        Optional<ManualInterviewAssignment> assignment = manualAssignmentService.getAssignment(
                projectId, applicantId, userId);

        /**
         * 값이 있으면, 응답 객체(ManualInterviewAssignmentResponse)로 변환하여 200 OK로 반환하고,
         * 값이 없으면 404 Not Found로 응답한다.
         */
        return assignment
                .map(ManualInterviewAssignmentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 프로젝트의 모든 수동 배정 조회
     */
    @GetMapping
    public ResponseEntity<List<ManualInterviewAssignmentResponse>> getAllAssignments(
            @PathVariable Long projectId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        List<ManualInterviewAssignment> assignments = manualAssignmentService.getAllAssignments(
                projectId, userId);

        List<ManualInterviewAssignmentResponse> responses = assignments.stream()
                .map(ManualInterviewAssignmentResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 지원자의 수동 배정 삭제
     */
    @DeleteMapping("/applicants/{applicantId}")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Long projectId,
            @PathVariable Long applicantId,
            Authentication authentication) {
        Long userId = authService.extractUserId(authentication);
        manualAssignmentService.removeAssignment(projectId, applicantId, userId);
        return ResponseEntity.noContent().build();
    }
}
