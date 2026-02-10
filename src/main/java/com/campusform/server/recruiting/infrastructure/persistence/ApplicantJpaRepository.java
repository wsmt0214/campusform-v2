package com.campusform.server.recruiting.infrastructure.persistence;


import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;

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
    // JPA가 이름만 보고 자동으로 쿼리를 만들어줌.
    long countByProjectId(Long projectId);
    //long countByProjectIdAndStatus(Long projectId, ApplicantStatus applicantStatus);
    List<Applicant> findByProjectIdOrderByBookmarkedDescNameAsc(Long projectId);
    List<Applicant> findByProjectIdOrderByNameDesc(Long projectId);

    List<Applicant> findByProjectIdAndDocumentStatus(Long projectId, ApplicantStatus documentStatus);
    List<Applicant> findByProjectIdAndInterviewStatus(Long projectId, ApplicantStatus interviewStatus);

    // JPA가 알아서 쿼리를 만들어줍니다.
    long countByProjectIdAndDocumentStatus(Long projectId, ApplicantStatus status);
    long countByProjectIdAndInterviewStatus(Long projectId, ApplicantStatus status);

    List<Applicant> findByProjectIdAndStage(Long projectId, RecruitmentStage stage);
  
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
}
