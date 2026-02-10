package com.campusform.server.project.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.campusform.server.project.domain.model.setting.ProjectValueMapping;
import com.campusform.server.project.domain.repository.ProjectValueMappingRepository;

import lombok.RequiredArgsConstructor;

/**
 * ProjectValueMappingRepository 구현체
 *
 * Spring Data JPA에 작업을 위임합니다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectValueMappingRepositoryImpl implements ProjectValueMappingRepository {

    private final ProjectValueMappingJpaRepository jpaRepository;

    @Override
    public List<ProjectValueMapping> findByProjectId(Long projectId) {
        return jpaRepository.findByProjectId(projectId);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        jpaRepository.deleteByProjectId(projectId);
    }
}
