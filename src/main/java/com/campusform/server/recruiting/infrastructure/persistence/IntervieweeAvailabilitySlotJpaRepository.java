package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;

/**
 * Spring Data JPA를 위한 IntervieweeAvailabilitySlot Repository
 * 
 * 기본 CRUD 메서드와 커스텀 쿼리 메서드를 제공합니다.
 */
@Repository
public interface IntervieweeAvailabilitySlotJpaRepository extends JpaRepository<IntervieweeAvailabilitySlot, Long> {

    /**
     * 특정 지원자의 모든 면접 가능 슬롯 목록 조회
     */
    List<IntervieweeAvailabilitySlot> findByApplicantId(Long applicantId);

    /**
     * 특정 지원자의 모든 면접 가능 슬롯 삭제
     */
    @Modifying
    @Query("DELETE FROM IntervieweeAvailabilitySlot s WHERE s.applicantId = :applicantId")
    void deleteByApplicantId(@Param("applicantId") Long applicantId);

    /**
     * 특정 날짜와 시작 시간으로 면접 가능 슬롯 목록 조회
     */
    List<IntervieweeAvailabilitySlot> findByInterviewDayIdAndStartTime(Long interviewDayId,
            java.time.LocalTime startTime);

    /**
     * 특정 날짜의 모든 면접 가능 슬롯 목록 조회
     */
    List<IntervieweeAvailabilitySlot> findByInterviewDayId(Long interviewDayId);
}
