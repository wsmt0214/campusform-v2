package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

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

    /**
     * 프로젝트ID, 이름, 전화번호로 지원자 조회
     */
    Optional<Applicant> findByProjectIdAndNameAndPhone(Long projectId, String name, String phone);

    /**
     * 여러 ID로 지원자 목록 조회
     */
    List<Applicant> findByIdIn(List<Long> applicantIds);
}
