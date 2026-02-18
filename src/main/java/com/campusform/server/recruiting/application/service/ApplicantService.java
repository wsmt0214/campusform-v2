package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.application.dto.response.ApplicantDetailResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantListResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantResponse;
import com.campusform.server.recruiting.application.dto.response.ApplicantStatusUpdateResponse;
import com.campusform.server.recruiting.application.dto.response.InterviewAssignedTimeResponse;
import com.campusform.server.recruiting.application.dto.response.InterviewTimeSource;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.infrastructure.persistence.CommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantService {
    private final ApplicantRepository applicantRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;
    private final InterviewAssignmentQueryService interviewAssignmentQueryService;

    public ApplicantListResponse getApplicants(Long projectId, String sort, RecruitmentStage stage, Long userId) {
        long total;
        long pending;
        long pass;
        long fail;
        // 2. 단계(Stage)에 따라 대상 범위·집계 기준 결정 (면접 탭은 서류 PASS만 대상)
        if (stage == RecruitmentStage.DOCUMENT) {
            // [서류 탭] 전체 지원자 기준, documentStatus로 카운트
            total = applicantRepository.countByProjectId(projectId);
            pending = applicantRepository.countByProjectIdAndDocumentStatus(projectId,
                    ScreeningResult.HOLD);
            pass = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ScreeningResult.PASS);
            fail = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ScreeningResult.FAIL);
        } else if (stage == RecruitmentStage.INTERVIEW) {
            // [면접 탭] 서류 합격자만 대상, documentStatus=PASS + interviewStatus로 카운트
            total = applicantRepository.countByProjectIdAndDocumentStatus(projectId, ScreeningResult.PASS);
            pending = applicantRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(projectId,
                    ScreeningResult.PASS, ScreeningResult.HOLD);
            pass = applicantRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(projectId,
                    ScreeningResult.PASS, ScreeningResult.PASS);
            fail = applicantRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(projectId,
                    ScreeningResult.PASS, ScreeningResult.FAIL);
        } else {
            throw new IllegalArgumentException("서류 또는 면접 단계 중 하나를 선택해야 합니다.");
        }

        // 3. 단계별 목록 조회 전략
        List<Applicant> applicants;
        if (stage == RecruitmentStage.DOCUMENT) {
            // 서류 탭: 프로젝트의 모든 지원자
            applicants = applicantRepository.findByProjectId(projectId);
        } else if (stage == RecruitmentStage.INTERVIEW) {
            // 면접 탭: 서류 단계에서 합격(PASS)한 지원자만
            applicants = applicantRepository.findByProjectIdAndDocumentStatus(projectId,
                    ScreeningResult.PASS);
        } else {
            // 위에서 이미 예외 처리했지만, 방어적 코드
            throw new IllegalArgumentException("지원자 목록을 조회할 수 없는 단계입니다: " + stage);
        }

        // 4. 정렬 기준 적용 (in-memory)
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
                                        Comparator.nullsLast(Comparator
                                                .naturalOrder())))
                        .toList();
            default -> // 기본: 이름 오름차순
                applicants.stream()
                        .sorted(Comparator.comparing(Applicant::getName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
        };

        // 5. 단계별 댓글 개수 집계 (해당 프로젝트 + 단계 기준)
        Map<Long, Long> commentCountMap = commentRepository
                .findAllByProjectIdAndStageOrderByCreatedAtAsc(projectId, stage)
                .stream()
                .collect(Collectors.groupingBy(
                        Comment::getApplicantId,
                        Collectors.counting()));

        // 6. 면접 단계일 때만 최종 배정 면접 시간 조회
        // 수동 배정은 InterviewSetting 없이도 가능하므로, 면접 설정 유무와 관계없이 항상 조회
        // (getAssignedTimes 내부에서 수동/자동/미배정을 구분해 반환)
        Map<Long, InterviewAssignedTimeResponse> tempAssignedTimeMap = Map.of();
        if (stage == RecruitmentStage.INTERVIEW) {
            List<InterviewAssignedTimeResponse> assignedTimes = interviewAssignmentQueryService
                    .getAssignedTimes(projectId, userId);
            tempAssignedTimeMap = assignedTimes.stream()
                    .collect(Collectors.toMap(
                            InterviewAssignedTimeResponse::getApplicantId,
                            it -> it,
                            (existing, replacement) -> existing));
        }
        final Map<Long, InterviewAssignedTimeResponse> assignedTimeMap = tempAssignedTimeMap;

        // 7. DTO로 변환하기
        List<ApplicantResponse> applicantDtos = applicants.stream()
                .map(applicant -> {
                    // 현재 탭(stage)에 맞는 상태값 선택
                    ScreeningResult currentStatus = (stage == RecruitmentStage.DOCUMENT)
                            ? applicant.getDocumentStatus()
                            : applicant.getInterviewStatus();

                    long commentCount = commentCountMap.getOrDefault(applicant.getId(), 0L);

                    // 면접 단계일 때만 배정 시간 매핑
                    LocalDate interviewDate = null;
                    LocalTime interviewStartTime = null;
                    InterviewTimeSource interviewTimeSource = null;

                    if (stage == RecruitmentStage.INTERVIEW) {
                        InterviewAssignedTimeResponse assigned = assignedTimeMap
                                .get(applicant.getId());
                        if (assigned != null
                                && assigned.getSource() != InterviewTimeSource.NONE) {
                            interviewDate = assigned.getInterviewDate();
                            interviewStartTime = assigned.getStartTime();
                            interviewTimeSource = assigned.getSource();
                        }
                    }

                    return ApplicantResponse.builder()
                            .id(applicant.getId())
                            .name(applicant.getName())
                            .school(applicant.getSchool())
                            .position(applicant.getPosition())
                            .major(applicant.getMajor()) // 학과
                            // 현재 단계 기준 즐겨찾기 여부
                            .bookmarked(applicant.isBookmarkedFor(stage))
                            .status(currentStatus.name()) // 현재 단계 기준 상태
                            .commentCount(commentCount) // 현재 단계 댓글 개수
                            .interviewDate(interviewDate)
                            .interviewStartTime(interviewStartTime)
                            .interviewTimeSource(interviewTimeSource)
                            .build();
                })
                .toList();

        // 4. [추가] 최종 응답 객체(ApplicantListResponse)로 감싸서 반환
        return ApplicantListResponse.builder()
                .status(ApplicantListResponse.ApplicantStatistics.builder()
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
            ScreeningResult status) {
        // 1. 지원자 찾기
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원자입니다."));

        // 2. 프로젝트 상태 검증: 해당 단계가 활성 상태인지 확인
        Project project = projectRepository.findById(applicant.getProjectId())
                .orElseThrow(() -> new IllegalStateException("지원자가 속한 프로젝트를 찾을 수 없습니다."));
        validateStageActive(project, stage);

        // 면접 단계에서는 서류 합격자만 상태 변경 가능 (서류 불합격자는 면접 대상 아님)
        validateDocumentPassForInterview(applicant, stage);

        // 3. 상태 변경 (도메인 로직 호출)
        applicant.updateScreeningResult(stage, status);
        // 3. 변경된 결과 응답 생성 , 현재 상태 확인!
        ScreeningResult updatedStatus = (stage == RecruitmentStage.DOCUMENT)
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

        // 면접 단계에서는 서류 합격자만 즐겨찾기 가능
        validateDocumentPassForInterview(applicant, stage);

        // 현재 탭(서류/면접)에 맞게 단계별 즐겨찾기 필드를 토글
        applicant.toggleBookmark(stage);
    }

    @Transactional(readOnly = true)
    public ApplicantDetailResponse getApplicantDetail(Long applicantId, RecruitmentStage stage) {
        // 1. 지원자 조회 (없으면 예외 발생)
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지원자입니다."));

        // 면접 단계에서는 서류 합격자만 상세 조회 가능
        validateDocumentPassForInterview(applicant, stage);

        // 2. 현재 단계(Stage)에 맞는 합격 상태(Status) 가져오기
        ScreeningResult currentStatus = (stage == RecruitmentStage.DOCUMENT)
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

        // 4. 해당 단계에서의 댓글 개수 조회
        long commentCount = commentRepository.findAllByApplicantIdAndStageOrderByCreatedAtAsc(
                applicantId, stage).size();

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
                // 상세 조회도 단계별 즐겨찾기 여부를 사용
                .isFavorite(applicant.isBookmarkedFor(stage))
                .commentCount(commentCount)
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

    /**
     * 면접 단계 작업 시 서류 합격(PASS) 여부 검증
     * 서류 불합격(FAIL) 지원자는 면접 단계의 대상이 아니므로, 모든 면접 관련 개별 작업을 거부합니다.
     */
    private void validateDocumentPassForInterview(Applicant applicant, RecruitmentStage stage) {
        if (stage == RecruitmentStage.INTERVIEW && applicant.getDocumentStatus() != ScreeningResult.PASS) {
            throw new IllegalArgumentException("서류 합격자만 면접 단계의 대상이 됩니다. 현재 서류 상태: " + applicant.getDocumentStatus());
        }
    }
}
