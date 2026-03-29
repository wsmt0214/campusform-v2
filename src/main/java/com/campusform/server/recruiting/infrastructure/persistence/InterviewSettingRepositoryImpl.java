package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.repository.InterviewSettingRepository;

import lombok.RequiredArgsConstructor;

/**
 * InterviewSettingRepository 구현체
 *
 * - Spring Data JPA 레포지토리에 위임합니다.
 */
@Repository
@RequiredArgsConstructor
public class InterviewSettingRepositoryImpl implements InterviewSettingRepository {

    private final InterviewSettingJpaRepository interviewSettingJpaRepository;

    @Override
    public void save(InterviewSetting setting) {
        if (setting == null) {
            // Repository 레벨에서는 null 저장을 명확히 막아 두는 게 디버깅에 유리합니다.
            throw new IllegalArgumentException("InterviewSetting은 null일 수 없습니다.");
        }
        interviewSettingJpaRepository.save(setting);
    }

    @Override
    public Optional<InterviewSetting> findByProjectId(Long projectId) {
        return interviewSettingJpaRepository.findByProjectId(projectId);
    }

    @Override
    public Optional<InterviewSetting> findByInvestigationLinkToken(String token) {
        return interviewSettingJpaRepository.findByInvestigationLinkToken(token);
    }

    @Override
    public void delete(InterviewSetting setting) {
        if (setting == null) {
            throw new IllegalArgumentException("InterviewSetting은 null일 수 없습니다.");
        }
        interviewSettingJpaRepository.delete(setting);
    }
}
