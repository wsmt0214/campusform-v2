package com.campusform.server.recruiting.application.dto.response.interview;

import java.time.LocalDate;
import java.time.LocalTime;

import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 수동 면접 배정 응답 DTO
 */
@Schema(description = "수동 면접 배정 정보 응답")
@Getter
@RequiredArgsConstructor
public class ManualInterviewAssignmentResponse {

    @Schema(description = "수동 배정 ID", example = "1")
    private final Long id;
    @Schema(description = "프로젝트 ID", example = "1")
    private final Long projectId;
    @Schema(description = "지원자 ID", example = "10")
    private final Long applicantId;
    @Schema(description = "면접 일자", example = "2024-07-01")
    private final LocalDate interviewDate;
    @Schema(description = "면접 시작 시간", example = "14:00")
    private final LocalTime startTime;

    /**
     * ManualInterviewAssignment 엔티티로부터 DTO 생성
     */
    public static ManualInterviewAssignmentResponse from(ManualInterviewAssignment assignment) {
        return new ManualInterviewAssignmentResponse(
                assignment.getId(),
                assignment.getProjectId(),
                assignment.getApplicantId(),
                assignment.getInterviewDate(),
                assignment.getStartTime());
    }
}
