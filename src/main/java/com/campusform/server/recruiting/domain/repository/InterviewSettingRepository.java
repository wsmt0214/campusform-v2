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
}
