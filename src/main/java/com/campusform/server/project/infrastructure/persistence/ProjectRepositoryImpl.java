package com.campusform.server.project.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

/**
 * ProjectRepository 구현체
 *
 * Spring Data JPA에 작업을 위임합니다.
 * 향후 Querydsl이 필요하면 여기에 추가할 수 있습니다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository {

    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public void save(Project project) {
        projectJpaRepository.save(project);
    }

    @Override
    public Optional<Project> findById(Long id) {
        return projectJpaRepository.findById(id);
    }

    @Override
    public Optional<Project> findBySheetUrl(String sheetUrl) {
        return projectJpaRepository.findBySheetUrl(sheetUrl);
    }

    @Override
    public List<Project> findAll() {
        return projectJpaRepository.findAll();
    }

    @Override
    public List<Project> findByUserId(Long userId) {
        return projectJpaRepository.findByUserIdAsOwnerOrAdmin(userId);
    }

    @Override
    public void delete(Project project) {
        projectJpaRepository.delete(project);
    }
}
