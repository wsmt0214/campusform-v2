package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 수동 면접 배정 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class ManualInterviewAssignmentResponse {

    private final Long id;
    private final Long projectId;
    private final Long applicantId;
    private final LocalDate interviewDate;
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
