package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;

import lombok.RequiredArgsConstructor;

/**
 * ApplicantRepository 구현체
 * 
 * Spring Data JPA에 작업을 위임합니다.
 * 향후 Querydsl이 필요하면 여기에 추가할 수 있습니다.
 */
@Repository
@RequiredArgsConstructor
public class ApplicantRepositoryImpl implements ApplicantRepository {

    private final ApplicantJpaRepository applicantJpaRepository;

    // 1. 저장 (단건)
    @Override
    public Applicant save(Applicant applicant) {
        return applicantJpaRepository.save(applicant);
    }

    // 2. 저장 (여러 건)
    @Override
    public void saveAll(List<Applicant> applicants) {
        applicantJpaRepository.saveAll(applicants);
    }

    // 3. ID로 조회 (여러 건)
    @Override
    public List<Applicant> findAllById(List<Long> ids) {
        return applicantJpaRepository.findAllById(ids);
    }

    // 4. ID로 조회 (단건) - SmsService 등에서 사용
    @Override
    public Optional<Applicant> findById(Long id) {
        return applicantJpaRepository.findById(id);
    }

    // --- 아래는 아까 추가한 통계/조회용 메서드들 (이게 없어서 에러가 났던 겁니다!) ---

    // 5. 프로젝트별 전체 지원자 수
    @Override
    public long countByProjectId(Long projectId) {
        return applicantJpaRepository.countByProjectId(projectId);
    }

    // 6. 서류 전형 상태별 조회
    @Override
    public List<Applicant> findByProjectIdAndDocumentStatus(Long projectId, ApplicantStatus status) {
        return applicantJpaRepository.findByProjectIdAndDocumentStatus(projectId, status);
    }

    // 7. 면접 전형 상태별 조회
    @Override
    public List<Applicant> findByProjectIdAndInterviewStatus(Long projectId, ApplicantStatus status) {
        return applicantJpaRepository.findByProjectIdAndInterviewStatus(projectId, status);
    }

    @Override
    public boolean existsById(Long applicantId) {
        return applicantJpaRepository.existsById(applicantId);
    }

    @Override
    public List<Applicant> findByProjectIdOrderByNameAsc(Long projectId) {
        // 현재 코드베이스에서는 in-memory 정렬을 사용하고 있어 JPA 정렬 메서드를 제공하지 않습니다.
        throw new UnsupportedOperationException("이름 오름차순 정렬은 ApplicantService에서 in-memory 정렬로 처리합니다.");
    }

    @Override
    public List<Applicant> findByProjectIdOrderByNameDesc(Long projectId) {
        return applicantJpaRepository.findByProjectIdOrderByNameDesc(projectId);
    }

    @Override
    public List<Applicant> findByProjectIdAndStage(Long projectId, RecruitmentStage stage) {
        return applicantJpaRepository.findByProjectIdAndStage(projectId, stage);
    }

    // public long countByProjectIdAndStatus(Long projectId, ApplicantStatus
    // applicantStatus){
    // return applicantJpaRepository.countByProjectIdAndStatus(projectId,
    // applicantStatus);
    // }
    @Override
    public long countByProjectIdAndDocumentStatus(Long projectId, ApplicantStatus status) {
        return applicantJpaRepository.countByProjectIdAndDocumentStatus(projectId, status);
    }

    @Override
    public long countByProjectIdAndInterviewStatus(Long projectId, ApplicantStatus status) {
        return applicantJpaRepository.countByProjectIdAndInterviewStatus(projectId, status);
    }

    @Override
    public List<Applicant> findByProjectId(Long projectId) {
        return applicantJpaRepository.findByProjectId(projectId);
    }

    @Override
    public Optional<Applicant> findByProjectIdAndNameAndPhone(Long projectId, String name, String phone) {
        return applicantJpaRepository.findByProjectIdAndNameAndPhone(projectId, name, phone);
    }

    @Override
    public Optional<Applicant> findByProjectIdAndNameAndEmail(Long projectId, String name, String email) {
        return applicantJpaRepository.findByProjectIdAndNameAndEmail(projectId, name, email);
    }

    @Override
    public List<Applicant> findByIds(List<Long> applicantIds) {
        return applicantJpaRepository.findByIdIn(applicantIds);
    }
}
