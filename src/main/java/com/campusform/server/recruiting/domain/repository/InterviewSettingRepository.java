package com.campusform.server.recruiting.domain.repository;

import java.util.Optional;

import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;

/**
 * 면접 설정(InterviewSetting) Repository 인터페이스
 *
 * - 도메인 관점의 인터페이스만 정의하고
 * - 구현은 infrastructure 계층(Spring Data JPA)에서 제공합니다.
 */
public interface InterviewSettingRepository {

    void save(InterviewSetting setting);

    Optional<InterviewSetting> findByProjectId(Long projectId);

    /**
     * 지원자 면접 가능 시간 조사 링크 토큰으로 면접 설정 조회
     * 
     * DDD 원칙: InterviewAvailabilityInvestigationLink는 InterviewSetting 애그리거트 루트 안에
     * 포함되므로
     * 루트 애그리거트를 통해 접근합니다.
     */
    Optional<InterviewSetting> findByInvestigationLinkToken(String token);

    void delete(InterviewSetting setting);
}
