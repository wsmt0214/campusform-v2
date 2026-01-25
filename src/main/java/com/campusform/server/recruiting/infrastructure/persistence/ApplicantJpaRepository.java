package com.campusform.server.recruiting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;

/**
 * Spring Data JPA를 위한 Applicant Repository
 * 
 * 기본 CRUD 메서드를 제공합니다.
 */
@Repository
public interface ApplicantJpaRepository extends JpaRepository<Applicant, Long> {
}
