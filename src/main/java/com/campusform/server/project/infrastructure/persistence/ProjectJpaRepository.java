package com.campusform.server.project.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 사용자가 Owner이거나 Admin인 프로젝트 목록 조회
     *
     * @param userId 사용자 ID
     * @return 사용자가 속한 프로젝트 목록
     */
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.admins a WHERE p.ownerId = :userId OR a.adminId = :userId")
    List<Project> findByUserIdAsOwnerOrAdmin(@Param("userId") Long userId);
}
