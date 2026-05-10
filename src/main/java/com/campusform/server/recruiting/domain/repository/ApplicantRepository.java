package com.campusform.server.recruiting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.repository.projection.ApplicantListRow;
import com.campusform.server.recruiting.domain.repository.projection.ScreeningResultCountRow;
import com.campusform.server.recruiting.domain.repository.projection.ProjectIdCountRow;

/**
 * 도메인 계층의 Repository 인터페이스
 * 
 * 특정 기술에 의존하지 않고 도메인 관점에서 인터페이스를 정의합니다.
 * 구현체는 infrastructure 계층에서 제공됩니다.
 * 이 코드는 "도메인 영역의 요청"을 받아서 "스프링 JPA(DB)"에게 토스해주는 역할을 완벽하게 수행합니다.
 * 도메인 영역이므로 ApplicantJpaRepository에 의존 X
 */
public interface ApplicantRepository {

    // 1. 저장 및 수정
    Applicant save(Applicant applicant);

    // 2. 일괄 저장 (ResultCommandService.announceResults에서 사용)
    void saveAll(List<Applicant> applicants);

    /**
     * 일괄 저장 후 저장된 엔티티 반환
     *
     * 시트 동기화처럼 "신규 생성 후 생성된 id를 즉시 응답에 포함"해야 하는 유즈케이스에서 사용
     */
    List<Applicant> saveAllReturning(List<Applicant> applicants);

    // 3. 단건 조회 (SmsService에서 사용)
    Optional<Applicant> findById(Long id);

    // 4. 다건 ID 조회 (ResultCommandService.announceResults에서 사용)
    List<Applicant> findAllById(List<Long> ids);

    // 프로젝트의 전체 지원자 수 (통계용)
    long countByProjectId(Long projectId);

    /**
     * 여러 프로젝트에 대한 지원자 수 집계
     *
     * - 프로젝트 목록 화면에서 프로젝트별 count를 N번 호출하지 않기 위한 목적
     */
    List<ProjectIdCountRow> countByProjectIdsGroupByProjectId(List<Long> projectIds);

    // 서류 단계 상태로 조회
    List<Applicant> findByProjectIdAndDocumentStatus(Long projectId, ScreeningResult status);

    // 면접 단계 상태로 조회
    List<Applicant> findByProjectIdAndInterviewStatus(Long projectId, ScreeningResult status);

    /**
     * 서류 합격 + 면접 상태 조건으로 지원자 목록 조회 (면접 탭: 서류 불합격자 제외)
     */
    List<Applicant> findByProjectIdAndDocumentStatusAndInterviewStatus(Long projectId,
            ScreeningResult documentStatus, ScreeningResult interviewStatus);

    // 서류(Document) 카운트
    long countByProjectIdAndDocumentStatus(Long projectId, ScreeningResult status);

    // 면접(Interview) 카운트
    long countByProjectIdAndInterviewStatus(Long projectId, ScreeningResult status);

    /**
     * 서류 합격 + 면접 상태 조건 카운트 (면접 탭 통계용: 서류 PASS인 지원자만 대상)
     */
    long countByProjectIdAndDocumentStatusAndInterviewStatus(Long projectId, ScreeningResult documentStatus,
            ScreeningResult interviewStatus);

    /**
     * 서류 상태별 통계 집계
     *
     * - projectId 기준으로 documentStatus별 count를 1회 쿼리로 집계하는 목적
     */
    List<ScreeningResultCountRow> countDocumentStatusByProjectId(Long projectId);

    /**
     * 면접 상태별 통계 집계 (서류 PASS 대상만)
     *
     * - projectId + documentStatus=PASS 조건에서 interviewStatus별 count를 1회 쿼리로 집계하는 목적
     */
    List<ScreeningResultCountRow> countInterviewStatusByProjectIdWithDocumentStatus(Long projectId,
            ScreeningResult documentStatus);

    boolean existsById(Long applicantId);

    /**
     * 프로젝트의 전체 지원자 목록 조회
     *
     * 최종 면접시간(Manual 우선 + Auto fallback) 조회 API에서 사용됩니다.
     */
    List<Applicant> findByProjectId(Long projectId);

    /**
     * 목록 화면 전용 projection 조회
     *
     * - 엔티티 전체 로딩을 피하고, 목록에 필요한 최소 컬럼만 조회하는 목적
     */
    List<ApplicantListRow> findListRowsByProjectId(Long projectId);

    /**
     * 목록 화면 전용 projection 조회 (서류 상태 조건 포함)
     *
     * - 면접 탭에서 서류 PASS 대상만 조회하는 목적
     */
    List<ApplicantListRow> findListRowsByProjectIdAndDocumentStatus(Long projectId, ScreeningResult documentStatus);

    /**
     * 시트 동기화 전용 preload 조회
     *
     * 동기화 로직의 변경 감지 단계에서 extraAnswers 접근이 포함되므로,
     * extraAnswers까지 함께 로딩하는 전용 조회로 분리
     */
    List<Applicant> findByProjectIdForSheetSync(Long projectId);

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

    /**
     * 프로젝트 지원자들의 포지션 컬럼에 등장하는 고유값 목록 조회 (null·공백 제외, 정렬)
     */
    List<String> findDistinctPositionValuesByProjectId(Long projectId);

    /**
     * 프로젝트에 속한 지원자 전체 삭제 (추가 답변 등 cascade는 영속성 컨텍스트 기준)
     */
    void deleteAllByProjectId(Long projectId);

    /**
     * 프로젝트 지원자의 면접 관련 데이터(interviewStatus, interviewBookmarked)를 초기값으로 일괄 리셋
     */
    void resetInterviewDataByProjectId(Long projectId);
}
