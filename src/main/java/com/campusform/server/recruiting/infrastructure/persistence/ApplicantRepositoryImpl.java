package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.projection.ApplicantListRow;
import com.campusform.server.recruiting.domain.repository.projection.ScreeningResultCountRow;
import com.campusform.server.recruiting.domain.repository.projection.ProjectIdCountRow;

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

    @Override
    public List<Applicant> saveAllReturning(List<Applicant> applicants) {
        return applicantJpaRepository.saveAll(applicants);
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

    @Override
    public List<ProjectIdCountRow> countByProjectIdsGroupByProjectId(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return applicantJpaRepository.countByProjectIdsGroupByProjectId(projectIds);
    }

    // 6. 서류 전형 상태별 조회
    @Override
    public List<Applicant> findByProjectIdAndDocumentStatus(Long projectId, ScreeningResult status) {
        return applicantJpaRepository.findByProjectIdAndDocumentStatus(projectId, status);
    }

    // 7. 면접 전형 상태별 조회
    @Override
    public List<Applicant> findByProjectIdAndInterviewStatus(Long projectId, ScreeningResult status) {
        return applicantJpaRepository.findByProjectIdAndInterviewStatus(projectId, status);
    }

    // 8. 서류 합격 + 면접 상태로 조회 (면접 단계에서 서류 불합격자 제외)
    @Override
    public List<Applicant> findByProjectIdAndDocumentStatusAndInterviewStatus(Long projectId,
            ScreeningResult documentStatus, ScreeningResult interviewStatus) {
        return applicantJpaRepository.findByProjectIdAndDocumentStatusAndInterviewStatus(projectId, documentStatus,
                interviewStatus);
    }

    @Override
    public boolean existsById(Long applicantId) {
        return applicantJpaRepository.existsById(applicantId);
    }

    // public long countByProjectIdAndStatus(Long projectId, ScreeningResult
    // applicantStatus){
    // return applicantJpaRepository.countByProjectIdAndStatus(projectId,
    // applicantStatus);
    // }
    @Override
    public long countByProjectIdAndDocumentStatus(Long projectId, ScreeningResult status) {
        return applicantJpaRepository.countByProjectIdAndDocumentStatus(projectId, status);
    }

    @Override
    public long countByProjectIdAndInterviewStatus(Long projectId, ScreeningResult status) {
        return applicantJpaRepository.countByProjectIdAndInterviewStatus(projectId, status);
    }

    @Override
    public long countByProjectIdAndDocumentStatusAndInterviewStatus(Long projectId, ScreeningResult documentStatus,
            ScreeningResult interviewStatus) {
        return applicantJpaRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(projectId, documentStatus,
                interviewStatus);
    }

    @Override
    public List<ScreeningResultCountRow> countDocumentStatusByProjectId(Long projectId) {
        return applicantJpaRepository.countDocumentStatusByProjectId(projectId);
    }

    @Override
    public List<ScreeningResultCountRow> countInterviewStatusByProjectIdWithDocumentStatus(Long projectId,
            ScreeningResult documentStatus) {
        return applicantJpaRepository.countInterviewStatusByProjectIdWithDocumentStatus(projectId, documentStatus);
    }

    @Override
    public List<Applicant> findByProjectId(Long projectId) {
        return applicantJpaRepository.findByProjectId(projectId);
    }

    @Override
    public List<ApplicantListRow> findListRowsByProjectId(Long projectId) {
        return applicantJpaRepository.findListRowsByProjectId(projectId);
    }

    @Override
    public List<ApplicantListRow> findListRowsByProjectIdAndDocumentStatus(Long projectId, ScreeningResult documentStatus) {
        return applicantJpaRepository.findListRowsByProjectIdAndDocumentStatus(projectId, documentStatus);
    }

    @Override
    public List<Applicant> findByProjectIdForSheetSync(Long projectId) {
        return applicantJpaRepository.findByProjectIdForSheetSync(projectId);
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

    @Override
    public List<String> findDistinctPositionValuesByProjectId(Long projectId) {
        return applicantJpaRepository.findDistinctPositionByProjectId(projectId);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        List<Applicant> applicants = applicantJpaRepository.findByProjectId(projectId);
        if (!applicants.isEmpty()) {
            applicantJpaRepository.deleteAll(applicants);
        }
    }

    @Override
    public void resetInterviewDataByProjectId(Long projectId) {
        applicantJpaRepository.resetInterviewDataByProjectId(projectId, ScreeningResult.HOLD);
    }
}
