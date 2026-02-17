package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.ManualInterviewAssignmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 수동 면접 배정 Application Service
 */
@Service
@RequiredArgsConstructor
public class ManualInterviewAssignmentService {

    private final InterviewContextLoader contextLoader;
    private final ManualInterviewAssignmentRepository manualAssignmentRepository;
    private final ApplicantRepository applicantRepository;

    /**
     * 지원자에게 면접 일자 및 시간을 수동으로 배정
     */
    @Transactional
    public void assignInterview(Long projectId, Long applicantId, LocalDate interviewDate,
            LocalTime startTime, Long userId) {
        contextLoader.loadProjectOrThrow(projectId).validateAdminAccess(userId);

        // 지원자 존재 확인 및 프로젝트 소속
        validateApplicantInProject(projectId, applicantId);

        // 기존 수동 배정이 있는지 확인 (프로젝트 범위로 제한)
        Optional<ManualInterviewAssignment> existingAssignment = manualAssignmentRepository
                .findByProjectIdAndApplicantId(projectId, applicantId);

        ManualInterviewAssignment assignment;
        if (existingAssignment.isPresent()) {
            // 기존 배정 업데이트
            assignment = existingAssignment.get();
            assignment.updateInterviewDate(interviewDate);
            assignment.updateStartTime(startTime);
        } else {
            // 새 배정 생성
            assignment = ManualInterviewAssignment.create(
                    projectId, applicantId, interviewDate, startTime);
        }

        manualAssignmentRepository.save(assignment);
    }

    private void validateApplicantInProject(Long projectId, Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("지원자를 찾을 수 없습니다. applicantId=" + applicantId));
        if (!applicant.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("지원자가 해당 프로젝트에 속하지 않습니다. applicantId=" + applicantId
                    + ", projectId=" + projectId);
        }
    }

    /**
     * 지원자의 수동 배정 삭제
     */
    @Transactional
    public void removeAssignment(Long projectId, Long applicantId, Long userId) {
        contextLoader.loadProjectOrThrow(projectId).validateAdminAccess(userId);

        // 지원자가 해당 프로젝트에 속하는지 검증
        validateApplicantInProject(projectId, applicantId);

        ManualInterviewAssignment assignment = manualAssignmentRepository
                .findByProjectIdAndApplicantId(projectId, applicantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "수동 배정을 찾을 수 없습니다. projectId=" + projectId + ", applicantId=" + applicantId));

        manualAssignmentRepository.delete(assignment);
    }

    /**
     * 지원자의 수동 배정 조회
     */
    @Transactional(readOnly = true)
    public Optional<ManualInterviewAssignment> getAssignment(Long projectId, Long applicantId, Long userId) {
        contextLoader.loadProjectOrThrow(projectId).validateAdminAccess(userId);

        // 지원자가 해당 프로젝트에 속하는지 검증
        validateApplicantInProject(projectId, applicantId);

        Optional<ManualInterviewAssignment> assignment = manualAssignmentRepository
                .findByProjectIdAndApplicantId(projectId, applicantId);

        return assignment;
    }

    /**
     * 프로젝트의 모든 수동 배정 조회
     */
    @Transactional(readOnly = true)
    public List<ManualInterviewAssignment> getAllAssignments(Long projectId, Long userId) {
        contextLoader.loadProjectOrThrow(projectId).validateAdminAccess(userId);

        return manualAssignmentRepository.findByProjectId(projectId);
    }
}
