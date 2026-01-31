package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduleUnassignedApplicant;

@Repository
public interface InterviewScheduleUnassignedApplicantJpaRepository
        extends JpaRepository<InterviewScheduleUnassignedApplicant, Long> {

    List<InterviewScheduleUnassignedApplicant> findByProjectId(Long projectId);

    @Modifying
    @Query("DELETE FROM InterviewScheduleUnassignedApplicant u WHERE u.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
