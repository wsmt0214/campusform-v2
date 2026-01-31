package com.campusform.server.recruiting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;

/**
 * 면접관 가능 시간 블록 Repository 인터페이스
 * 
 * 도메인 계층의 infrastructure 인터페이스
 * 특정 기술에 의존하지 않고 도메인 관점에서 인터페이스를 정의합니다.
 */
public interface InterviewerAvailabilityBlockRepository {

    void save(InterviewerAvailabilityBlock block);

    void saveAll(List<InterviewerAvailabilityBlock> blocks);

    void delete(InterviewerAvailabilityBlock block);

    void deleteAll(List<InterviewerAvailabilityBlock> blocks);

    /**
     * 특정 면접관의 특정 날짜 가능 시간 블록 목록 조회
     */
    List<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayId(Long adminId, Long interviewDayId);

    /**
     * 특정 면접관의 모든 가능 시간 블록 목록 조회
     */
    List<InterviewerAvailabilityBlock> findByAdminId(Long adminId);

    /**
     * 특정 날짜의 모든 면접관 가능 시간 블록 목록 조회 (집계용)
     */
    List<InterviewerAvailabilityBlock> findByInterviewDayId(Long interviewDayId);

    /**
     * 특정 면접관의 특정 날짜/시간 블록 조회
     */
    Optional<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayIdAndStartTime(
            Long adminId, Long interviewDayId, java.time.LocalTime startTime);

    /**
     * 특정 면접관의 특정 프로젝트 범위 내 블록 목록 조회
     * 
     * @param adminId         면접관 ID
     * @param interviewDayIds 프로젝트의 InterviewDay ID 목록
     * @return 해당 면접관의 프로젝트 범위 내 블록 목록
     */
    List<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayIdIn(Long adminId, List<Long> interviewDayIds);

    /**
     * 영속성 컨텍스트의 변경사항을 즉시 DB에 반영
     * 
     * 쓰기 지연으로 인한 unique 제약 조건 위반을 방지하기 위해 사용합니다.
     */
    void flush();

    /**
     * 여러 날짜의 모든 면접관 가용 시간 블록 조회
     * 스마트 시간표 알고리즘에서 사용합니다.
     */
    List<InterviewerAvailabilityBlock> findByInterviewDayIdIn(List<Long> interviewDayIds);
}
