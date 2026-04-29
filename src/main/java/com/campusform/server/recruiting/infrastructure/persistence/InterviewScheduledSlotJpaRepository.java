package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduledSlot;

@Repository
public interface InterviewScheduledSlotJpaRepository extends JpaRepository<InterviewScheduledSlot, Long> {

    List<InterviewScheduledSlot> findByProjectId(Long projectId);

    /**
     * OneToMany(LAZY)로 인한 N+1을 방지하기 위한 fetch join 전용 메서드입니다.
     */
    @Query("SELECT DISTINCT s FROM InterviewScheduledSlot s " +
            "LEFT JOIN FETCH s.applicants " +
            "WHERE s.projectId = :projectId")
    List<InterviewScheduledSlot> findByProjectIdWithApplicants(@Param("projectId") Long projectId);

    @Query("SELECT DISTINCT s FROM InterviewScheduledSlot s " +
            "LEFT JOIN FETCH s.applicants a " +
            "WHERE s.projectId = :projectId AND a.applicantId IN :applicantIds")
    List<InterviewScheduledSlot> findByProjectIdWithApplicantsFiltered(
            @Param("projectId") Long projectId,
            @Param("applicantIds") List<Long> applicantIds
    );
}
