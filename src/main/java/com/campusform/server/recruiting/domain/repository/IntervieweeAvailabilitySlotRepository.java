package com.campusform.server.recruiting.domain.repository;

import java.time.LocalTime;
import java.util.List;

import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;

/**
 * 지원자 면접 가능 슬롯 Repository 인터페이스
 * 
 * 도메인 계층의 infrastructure 인터페이스
 * 특정 기술에 의존하지 않고 도메인 관점에서 인터페이스를 정의합니다.
 */
public interface IntervieweeAvailabilitySlotRepository {

    void save(IntervieweeAvailabilitySlot slot);

    void saveAll(List<IntervieweeAvailabilitySlot> slots);

    /**
     * 특정 지원자의 모든 면접 가능 슬롯 목록 조회
     */
    List<IntervieweeAvailabilitySlot> findByApplicantId(Long applicantId);

    /**
     * 특정 지원자의 모든 면접 가능 슬롯 삭제
     */
    void deleteByApplicantId(Long applicantId);

    /**
     * 특정 날짜와 시작 시간으로 면접 가능 슬롯 목록 조회
     * 슬롯별 지원자 정보 조회에 사용됩니다.
     */
    List<IntervieweeAvailabilitySlot> findByInterviewDayIdAndStartTime(Long interviewDayId, LocalTime startTime);

    /**
     * 특정 날짜의 모든 면접 가능 슬롯 목록 조회
     * 모든 슬롯별 지원자 정보 조회에 사용됩니다.
     */
    List<IntervieweeAvailabilitySlot> findByInterviewDayId(Long interviewDayId);
}
