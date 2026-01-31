package com.campusform.server.recruiting.infrastructure.persistence;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;

/**
 * Spring Data JPA를 위한 InterviewerAvailabilityBlock Repository
 * 
 * 기본 CRUD 메서드와 커스텀 쿼리 메서드를 제공합니다.
 */
@Repository
public interface InterviewerAvailabilityBlockJpaRepository extends JpaRepository<InterviewerAvailabilityBlock, Long> {

    List<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayId(Long adminId, Long interviewDayId);

    List<InterviewerAvailabilityBlock> findByAdminId(Long adminId);

    List<InterviewerAvailabilityBlock> findByInterviewDayId(Long interviewDayId);

    Optional<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayIdAndStartTime(
            Long adminId, Long interviewDayId, LocalTime startTime);

    /**
     * 특정 면접관의 특정 프로젝트 범위 내 블록 목록 조회
     */
    List<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayIdIn(Long adminId, List<Long> interviewDayIds);

    /**
     * 여러 날짜의 모든 면접관 가용 시간 블록 조회
     */
    List<InterviewerAvailabilityBlock> findByInterviewDayIdIn(List<Long> interviewDayIds);
}
