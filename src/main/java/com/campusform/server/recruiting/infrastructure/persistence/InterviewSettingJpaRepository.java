package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;

/**
 * Spring Data JPA용 InterviewSetting Repository
 */
@Repository
public interface InterviewSettingJpaRepository extends JpaRepository<InterviewSetting, Long> {

    Optional<InterviewSetting> findByProjectId(Long projectId);
}
