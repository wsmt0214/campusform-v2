package com.campusform.server.recruiting.application.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.application.dto.response.ApplicantDetailResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantListResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantStatusUpdateResponse;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantService {
    private final ApplicantRepository applicantRepository;
    private final ProjectRepository projectRepository;

    public ApplicantListResponse getApplicants(Long projectId, String sort, RecruitmentStage stage) {
        long total = applicantRepository.countByProjectId(projectId);
        long pending = 0;
        long pass = 0;
        long fail = 0;
        // 2. 단계(Stage)에 따라 "누구의 합격 상태"를 셀지 결정 (if문 분기)
        if (stage == RecruitmentStage.DOCUMENT) {
            // [서류 탭] -> documentStatus 기준 카운트
            pending = applicantRepository.countByProjectIdAndDocumentStatus(projectId,
                    ApplicantStatus.HOLD);
            pass = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ApplicantStatus.PASS);
            fail = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ApplicantStatus.FAIL);
        } else if (stage == RecruitmentStage.INTERVIEW) {
            // [면접 탭] -> interviewStatus 기준 카운트
            pending = applicantRepository.countByProjectIdAndInterviewStatus(projectId,
                    ApplicantStatus.HOLD);
            pass = applicantRepository.countByProjectIdAndInterviewStatus(projectId, ApplicantStatus.PASS);
            fail = applicantRepository.countByProjectIdAndInterviewStatus(projectId, ApplicantStatus.FAIL);
        } else {
            // 예외 처리: 단계가 없으면 통계를 낼 수 없음
            throw new IllegalArgumentException("서류 또는 면접 단계 중 하나를 선택해야 합니다.");
        }

        // 3. 조회 대상: 요청된 단계(stage)에 속한 지원자만
        List<Applicant> applicants = applicantRepository.findByProjectIdAndStage(projectId, stage);

        // 4. 정렬 기준 적용 (단계 필터링 이후에 in-memory 정렬)
        applicants = switch (sort) {
            case "name_desc" -> // 이름 내림차순
                applicants.stream()
                        .sorted(Comparator.comparing(Applicant::getName,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();
            case "bookmark" -> // 찜한 순 (현재 단계에서 찜=true 먼저, 그 다음 이름 오름차순)
                applicants.stream()
                        .sorted(Comparator
                                .comparing(
                                        (Applicant applicant) -> applicant.isBookmarkedFor(stage),
                                        Comparator.reverseOrder())
                                .thenComparing(Applicant::getName,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
            default -> // 기본: 이름 오름차순
                applicants.stream()
                        .sorted(Comparator.comparing(Applicant::getName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
        };
        // DTO로 변환하기
        List<ApplicantResponse> applicantDtos = applicants.stream()
                .map(applicant -> ApplicantResponse.builder()
                        .id(applicant.getId())
                        .name(applicant.getName())
                        .major(applicant.getMajor()) // 학과
                        .phone(applicant.getPhone()) // 전화번호
                        // 현재 조회 중인 단계(stage)에 따른 즐겨찾기 여부를 노출
                        .bookmarked(applicant.isBookmarkedFor(stage)) // ★ 찜 여부
                        // 필요한 필드가 더 있다면 여기에 계속 추가
                        // .email(applicant.getEmail())
                        .build())
                .toList();

        // 4. [추가] 최종 응답 객체(ApplicantListResponse)로 감싸서 반환
        return ApplicantListResponse.builder()
                .status(ApplicantListResponse.ApplicantStatus.builder()
                        .totalCount(total)
                        .pendingCount(pending)
                        .passCount(pass)
                        .failCount(fail)
                        .build())
                .applicants(applicantDtos)
                .build();
    }

    @Transactional
    public ApplicantStatusUpdateResponse updateApplicantStatus(Long applicantId, RecruitmentStage stage,
            ApplicantStatus status) {
        // 1. 지원자 찾기
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원자입니다."));

        // 2. 프로젝트 상태 검증: 해당 단계가 활성 상태인지 확인
        Project project = projectRepository.findById(applicant.getProjectId())
                .orElseThrow(() -> new IllegalStateException("지원자가 속한 프로젝트를 찾을 수 없습니다."));
        validateStageActive(project, stage);

        // 3. 상태 변경 (도메인 로직 호출)
        applicant.updateApplicantStatus(stage, status);
        // 3. 변경된 결과 응답 생성 , 현재 상태 확인!
        ApplicantStatus updatedStatus = (stage == RecruitmentStage.DOCUMENT)
                ? applicant.getDocumentStatus()
                : applicant.getInterviewStatus();

        return ApplicantStatusUpdateResponse.builder()
                .applicantId(applicant.getId())
                .currentStatus(updatedStatus.name())
                .updateAt(LocalDateTime.now()) // 혹은 applicant.getUpdatedAt()
                .build();
    }

    // 찜하기 토글
    @Transactional
    public void Bookmark(Long applicantId, RecruitmentStage stage) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("지원자가 존재하지 않습니다."));
        // 현재 탭(서류/면접)에 맞게 단계별 즐겨찾기 필드를 토글
        applicant.toggleBookmark(stage);
    }

    @Transactional(readOnly = true)
    public ApplicantDetailResponse getApplicantDetail(Long applicantId, RecruitmentStage stage) {
        // 1. 지원자 조회 (없으면 예외 발생)
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원자입니다."));

        // 2. 현재 단계(Stage)에 맞는 합격 상태(Status) 가져오기
        ApplicantStatus currentStatus = (stage == RecruitmentStage.DOCUMENT)
                ? applicant.getDocumentStatus()
                : applicant.getInterviewStatus();

        // 3. [수정] 답변 리스트 변환 로직을 Service 내부로 가져옴
        // 시트 헤더 순서대로 정렬 (orderIndex 기준, null인 경우는 맨 뒤로)
        List<ApplicantDetailResponse.AnswerDto> answerDtos = applicant.getExtraAnswers().stream()
                .sorted((a1, a2) -> {
                    Integer idx1 = a1.getOrderIndex();
                    Integer idx2 = a2.getOrderIndex();
                    // null인 경우는 Integer.MAX_VALUE로 처리하여 맨 뒤로
                    int order1 = idx1 != null ? idx1 : Integer.MAX_VALUE;
                    int order2 = idx2 != null ? idx2 : Integer.MAX_VALUE;
                    return Integer.compare(order1, order2);
                })
                .map(answer -> ApplicantDetailResponse.AnswerDto.builder()
                        .question(answer.getQuestionText())
                        .answer(answer.getAnswerText())
                        .build())
                .toList();

        return ApplicantDetailResponse.builder()
                .applicantId(applicant.getId())
                .name(applicant.getName())
                .gender(applicant.getGender())
                .school(applicant.getSchool())
                .major(applicant.getMajor())
                .position(applicant.getPosition())
                .phoneNumber(applicant.getPhone())
                .email(applicant.getEmail())
                .status(currentStatus.name())
                // 현재 단계 기준 즐겨찾기 여부
                .isFavorite(applicant.isBookmarkedFor(stage))
                .answers(answerDtos)
                .build();
    }

    /**
     * 요청한 모집 단계(stage)에 해당하는 프로젝트 상태인지 검증
     * - DOCUMENT: DOCUMENT 상태여야 함
     * - INTERVIEW: INTERVIEW 상태여야 함
     */
    private void validateStageActive(Project project, RecruitmentStage stage) {
        if (stage == RecruitmentStage.DOCUMENT) {
            project.validateDocumentStage();
        } else if (stage == RecruitmentStage.INTERVIEW) {
            project.validateInterviewStage();
        }
    }
}
