package com.campusform.server.recruiting.infrastructure.persistence;


import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;

/**
 * Spring Data JPA를 위한 Applicant Repository
 * 
 * 기본 CRUD 메서드를 제공합니다.
 */
@Repository
public interface ApplicantJpaRepository extends JpaRepository<Applicant, Long> {
    // JPA가 이름만 보고 자동으로 쿼리를 만들어줌.
    long countByProjectId(Long projectId);
    //long countByProjectIdAndStatus(Long projectId, ScreeningResult applicantStatus);

    List<Applicant> findByProjectIdAndDocumentStatus(Long projectId, ScreeningResult documentStatus);
    List<Applicant> findByProjectIdAndInterviewStatus(Long projectId, ScreeningResult interviewStatus);

    /** 서류 합격 + 면접 상태 조건으로 지원자 목록 조회 (면접 탭에서 서류 불합격자 제외) */
    List<Applicant> findByProjectIdAndDocumentStatusAndInterviewStatus(Long projectId,
            ScreeningResult documentStatus, ScreeningResult interviewStatus);

    // JPA가 알아서 쿼리를 만들어줍니다.
    long countByProjectIdAndDocumentStatus(Long projectId, ScreeningResult status);
    long countByProjectIdAndInterviewStatus(Long projectId, ScreeningResult status);

    long countByProjectIdAndDocumentStatusAndInterviewStatus(Long projectId, ScreeningResult documentStatus,
            ScreeningResult interviewStatus);

    /**
     * 프로젝트의 전체 지원자 목록 조회
     */
    List<Applicant> findByProjectId(Long projectId);

    /**
     * 프로젝트ID, 이름, 전화번호로 지원자 조회
     */
    Optional<Applicant> findByProjectIdAndNameAndPhone(Long projectId, String name, String phone);

    /**
     * 프로젝트ID, 이름, 이메일로 지원자 조회
     */
    Optional<Applicant> findByProjectIdAndNameAndEmail(Long projectId, String name, String email);

    /**
     * 여러 ID로 지원자 목록 조회
     */
    List<Applicant> findByIdIn(List<Long> applicantIds);

    /**
     * 프로젝트 지원자들의 position 컬럼 고유값 목록 (null·공백 제외, 오름차순)
     */
    @Query("SELECT DISTINCT a.position FROM Applicant a WHERE a.projectId = :projectId AND a.position IS NOT NULL AND TRIM(a.position) != '' ORDER BY a.position")
    List<String> findDistinctPositionByProjectId(@Param("projectId") Long projectId);
}
