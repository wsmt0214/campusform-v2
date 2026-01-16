package com.campusform.server.project.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusform.server.project.domain.model.setting.Project;

/**
 * Spring Data JPA를 위한 Project Repository
 * 
 * 기본 CRUD 메서드와 커스텀 쿼리 메서드를 제공합니다.
 */
@Repository
public interface ProjectJpaRepository extends JpaRepository<Project, Long> {

    Optional<Project> findBySheetUrl(String sheetUrl);
}
