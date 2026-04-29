package com.campusform.server.recruiting.domain.repository;

import java.util.List;

import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduledSlot;

/**
 * 배정된 면접 슬롯 애그리거트 Repository
 */
public interface InterviewScheduledSlotRepository {

    InterviewScheduledSlot save(InterviewScheduledSlot slot);

    void saveAll(List<InterviewScheduledSlot> slots);

    List<InterviewScheduledSlot> findByProjectId(Long projectId);

    /**
     * 프로젝트의 배정된 슬롯을 지원자(applicants)까지 함께 조회합니다.
     * 
     * 조회 API에서 N+1을 방지하기 위한 전용 메서드입니다.
     */
    List<InterviewScheduledSlot> findByProjectIdWithApplicants(Long projectId);

    /**
     * 프로젝트의 배정된 슬롯을 지원자(applicants)까지 함께 조회하되, applicantId 목록에 해당하는 신청만 로딩합니다.
     *
     * - 면접 탭 목록처럼 “현재 조회한 지원자들”에 대해서만 AUTO 배정 정보를 매핑하는 목적
     */
    List<InterviewScheduledSlot> findByProjectIdWithApplicantsFiltered(Long projectId, List<Long> applicantIds);

    void deleteByProjectId(Long projectId);
}
