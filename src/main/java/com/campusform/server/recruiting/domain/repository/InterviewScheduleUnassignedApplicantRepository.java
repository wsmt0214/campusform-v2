package com.campusform.server.recruiting.domain.repository;

import java.util.List;

import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduleUnassignedApplicant;

/**
 * 미배정 지원자 애그리거트 Repository
 */
public interface InterviewScheduleUnassignedApplicantRepository {

    void save(InterviewScheduleUnassignedApplicant entity);

    void saveAll(List<InterviewScheduleUnassignedApplicant> entities);

    List<InterviewScheduleUnassignedApplicant> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
