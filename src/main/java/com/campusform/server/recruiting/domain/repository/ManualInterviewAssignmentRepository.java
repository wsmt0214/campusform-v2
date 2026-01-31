package com.campusform.server.recruiting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;

/**
 * 수동 면접 배정 Repository 인터페이스
 * 
 * 도메인 계층의 Repository 인터페이스로, 특정 기술에 의존하지 않고
 * 도메인 관점에서 인터페이스를 정의합니다.
 * 구현체는 infrastructure 계층에서 제공됩니다.
 */
public interface ManualInterviewAssignmentRepository {

    /**
     * 수동 배정 저장
     */
    ManualInterviewAssignment save(ManualInterviewAssignment assignment);

    /**
     * 수동 배정 목록 저장
     */
    void saveAll(List<ManualInterviewAssignment> assignments);

    /**
     * 프로젝트 ID로 수동 배정 목록 조회
     */
    List<ManualInterviewAssignment> findByProjectId(Long projectId);

    /**
     * 지원자 ID로 수동 배정 조회
     * 
     * @param applicantId 지원자 ID
     * @return 수동 배정 정보 (없으면 Optional.empty())
     */
    Optional<ManualInterviewAssignment> findByApplicantId(Long applicantId);

    /**
     * 프로젝트 ID와 지원자 ID로 수동 배정 조회
     * 
     * @param projectId   프로젝트 ID
     * @param applicantId 지원자 ID
     * @return 수동 배정 정보 (없으면 Optional.empty())
     */
    Optional<ManualInterviewAssignment> findByProjectIdAndApplicantId(Long projectId, Long applicantId);

    /**
     * 수동 배정 삭제
     */
    void delete(ManualInterviewAssignment assignment);

    /**
     * 프로젝트 ID로 모든 수동 배정 삭제
     */
    void deleteByProjectId(Long projectId);
}
