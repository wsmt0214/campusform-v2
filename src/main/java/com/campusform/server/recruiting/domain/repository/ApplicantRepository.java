package com.campusform.server.recruiting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;
/**
 * 도메인 계층의 Repository 인터페이스
 * 
 * 특정 기술에 의존하지 않고 도메인 관점에서 인터페이스를 정의합니다.
 * 구현체는 infrastructure 계층에서 제공됩니다.
 * 이 코드는 "도메인 영역의 요청"을 받아서 "스프링 JPA(DB)"에게 토스해주는 역할을 완벽하게 수행합니다.
 * 도메인 영역이므로 ApplicantJpaRepository에 의존 X
 */
public interface ApplicantRepository{

    // 1. 저장 및 수정
    Applicant save(Applicant applicant);

    // 2. 일괄 저장 (ResultService.announceResults에서 사용)
    void saveAll(List<Applicant> applicants);

    // 3. 단건 조회 (SmsService에서 사용)
    Optional<Applicant> findById(Long id);

    // 4. 다건 ID 조회 (ResultService.announceResults에서 사용)
    List<Applicant> findAllById(List<Long> ids);

    // 프로젝트의 전체 지원자 수 (통계용)
    long countByProjectId(Long projectId);

    // 서류 단계 상태로 조회
    List<Applicant> findByProjectIdAndDocumentStatus(Long projectId, ApplicantStatus status);

    // 면접 단계 상태로 조회
    List<Applicant> findByProjectIdAndInterviewStatus(Long projectId, ApplicantStatus status);

    // 서류(Document) 카운트
    long countByProjectIdAndDocumentStatus(Long projectId, ApplicantStatus status);

    // 면접(Interview) 카운트
    long countByProjectIdAndInterviewStatus(Long projectId, ApplicantStatus status);

    boolean existsById(Long applicantId);

    // 1. 이름 오름차순
    List<Applicant> findByProjectIdOrderByNameAsc(Long projectId);
    // 2. 이름 내림차순
    List<Applicant> findByProjectIdOrderByNameDesc(Long projectId);
    // 3. 찜한 순 (북마크된 것 위로, 나머지는 가나다순)
    List<Applicant> findByProjectIdOrderByBookmarkedDescNameAsc(Long projectId);

    List<Applicant> findByProjectIdAndStage(Long projectId, StageStatus stage);

    /**
     * 프로젝트의 전체 지원자 목록 조회
     *
     * 최종 면접시간(Manual 우선 + Auto fallback) 조회 API에서 사용됩니다.
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
     * 슬롯별 지원자 정보 조회에 사용됩니다.
     */
    List<Applicant> findByIds(List<Long> applicantIds);
}
