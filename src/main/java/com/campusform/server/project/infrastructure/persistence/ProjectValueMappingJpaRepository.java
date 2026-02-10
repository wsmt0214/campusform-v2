package com.campusform.server.project.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.campusform.server.project.domain.model.setting.ProjectValueMapping;

/**
 * Spring Data JPA - ProjectValueMapping Repository
 */
@Repository
public interface ProjectValueMappingJpaRepository extends JpaRepository<ProjectValueMapping, Long> {

    List<ProjectValueMapping> findByProjectId(Long projectId);

    @Modifying
    void deleteByProjectId(Long projectId);
}
