package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;

@Repository
public interface ManualInterviewAssignmentJpaRepository
        extends JpaRepository<ManualInterviewAssignment, Long> {

    List<ManualInterviewAssignment> findByProjectId(Long projectId);

    List<ManualInterviewAssignment> findByProjectIdAndApplicantIdIn(Long projectId, List<Long> applicantIds);

    Optional<ManualInterviewAssignment> findByApplicantId(Long applicantId);

    Optional<ManualInterviewAssignment> findByProjectIdAndApplicantId(Long projectId, Long applicantId);

    @Modifying
    @Query("DELETE FROM ManualInterviewAssignment m WHERE m.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
