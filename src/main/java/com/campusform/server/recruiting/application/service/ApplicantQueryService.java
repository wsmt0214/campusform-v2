package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.application.service.ProjectAuthorizationService;
import com.campusform.server.recruiting.domain.exception.ApplicantNotFoundException;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantDetailResponse;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantListResponse;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantResponse;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewAssignedTimeResponse;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewTimeSource;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 쿼리(Query) 전용 서비스 (CQRS 패턴)
 * 기존 읽기 + 쓰기가 함께 구현돼 있어 코드가 비대해짐 -> 쿼리(Query)와 커맨드(Command) 책임이 분리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantQueryService {

    private final ApplicantRepository applicantRepository;
    private final CommentRepository commentRepository;
    private final InterviewAssignmentQueryService interviewAssignmentQueryService;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * 프로젝트별 지원자 목록 조회
     */
    public ApplicantListResponse getApplicants(Long projectId, RecruitmentStage stage, Long userId) {

        projectAuthorizationService.assertAdmin(projectId, userId);

        long total;
        long pending;
        long pass;
        long fail;
        // Stage에 따라 대상 범위·집계 기준 결정
        if (stage == RecruitmentStage.DOCUMENT) {
            // [서류 탭] 전체 지원자 기준, documentStatus로 카운트
            total = applicantRepository.countByProjectId(projectId);
            pending = applicantRepository.countByProjectIdAndDocumentStatus(projectId,
                    ScreeningResult.HOLD);
            pass = applicantRepository.countByProjectIdAndDocumentStatus(projectId,
                    ScreeningResult.PASS);
            fail = applicantRepository.countByProjectIdAndDocumentStatus(projectId,
                    ScreeningResult.FAIL);
        } else if (stage == RecruitmentStage.INTERVIEW) {
            // [면접 탭] 서류 합격자만 대상, documentStatus=PASS + interviewStatus로 카운트
            total = applicantRepository.countByProjectIdAndDocumentStatus(projectId,
                    ScreeningResult.PASS);
            pending = applicantRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(
                    projectId, ScreeningResult.PASS, ScreeningResult.HOLD);
            pass = applicantRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(
                    projectId, ScreeningResult.PASS, ScreeningResult.PASS);
            fail = applicantRepository.countByProjectIdAndDocumentStatusAndInterviewStatus(
                    projectId, ScreeningResult.PASS, ScreeningResult.FAIL);
        } else {
            throw new IllegalArgumentException("서류 또는 면접 단계 중 하나를 선택해야 합니다.");
        }

        // 목록 조회 전략
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

        // 단계별 댓글 개수 집계 (해당 프로젝트 + 단계 기준)
        Map<Long, Long> commentCountMap = commentRepository
                .findAllByProjectIdAndStageOrderByCreatedAtAsc(projectId, stage).stream()
                .collect(Collectors.groupingBy(Comment::getApplicantId, Collectors.counting()));

        // 면접 단계일 때만 최종 배정 면접 시간 조회
        // 수동 배정은 InterviewSetting 없이도 가능하므로, 면접 설정 유무와 관계없이 항상 조회
        // (getAssignedTimes 내부에서 수동/자동/미배정을 구분해 반환)
        Map<Long, InterviewAssignedTimeResponse> assignedTimeMap = Map.of();
        if (stage == RecruitmentStage.INTERVIEW) {
            List<InterviewAssignedTimeResponse> assignedTimes =
                    interviewAssignmentQueryService.getAssignedTimes(projectId, userId);
            assignedTimeMap = assignedTimes.stream()
                    .collect(Collectors.toMap(InterviewAssignedTimeResponse::getApplicantId,
                            it -> it, (existing, replacement) -> existing));
        }
        final Map<Long, InterviewAssignedTimeResponse> assignedTimeMapFinal = assignedTimeMap;

        // DTO로 변환하기
        List<ApplicantResponse> applicantDtos = applicants.stream().map(applicant -> {
            // 현재 탭(stage)에 맞는 상태값 선택
            ScreeningResult currentStatus =
                    (stage == RecruitmentStage.DOCUMENT) ? applicant.getDocumentStatus()
                            : applicant.getInterviewStatus();
            long commentCount = commentCountMap.getOrDefault(applicant.getId(), 0L);
            LocalDate interviewDate = null;
            LocalTime interviewStartTime = null;
            InterviewTimeSource interviewTimeSource = null;
            if (stage == RecruitmentStage.INTERVIEW) {
                // 면접 단계일 때만 배정 시간 매핑
                InterviewAssignedTimeResponse assigned =
                        assignedTimeMapFinal.get(applicant.getId());
                if (assigned != null && assigned.getSource() != InterviewTimeSource.NONE) {
                    interviewDate = assigned.getInterviewDate();
                    interviewStartTime = assigned.getStartTime();
                    interviewTimeSource = assigned.getSource();
                }
            }
            return ApplicantResponse.builder().id(applicant.getId()).name(applicant.getName())
                    .school(applicant.getSchool()).position(applicant.getPosition())
                    .major(applicant.getMajor())
                    // 현재 단계 기준 즐겨찾기 여부
                    .bookmarked(applicant.isBookmarkedFor(stage))
                    // 현재 단계 기준 상태
                    .status(currentStatus.name())
                    // 현재 단계 댓글 개수
                    .commentCount(commentCount).interviewDate(interviewDate)
                    .interviewStartTime(interviewStartTime).interviewTimeSource(interviewTimeSource)
                    .build();
        }).toList();

        // [추가] 최종 응답 객체(ApplicantListResponse)로 감싸서 반환
        return ApplicantListResponse.builder()
                .status(ApplicantListResponse.ApplicantStatistics.builder().totalCount(total)
                        .pendingCount(pending).passCount(pass).failCount(fail).build())
                .applicants(applicantDtos).build();
    }

    /**
     * 지원자 상세 조회 (답변·댓글 수·면접 배정 시간 포함)
     */
    public ApplicantDetailResponse getApplicantDetail(Long projectId, Long applicantId,
            RecruitmentStage stage, Long userId) {
        projectAuthorizationService.assertAdmin(projectId, userId);

        // 1. 지원자 조회 (없으면 예외 발생)
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new ApplicantNotFoundException(applicantId));
        // 면접 단계에서는 서류 합격자만 상세 조회 가능
        validateDocumentPassForInterview(applicant, stage);

        // 2. 현재 단계(Stage)에 맞는 합격 상태(Status) 가져오기
        ScreeningResult currentStatus =
                (stage == RecruitmentStage.DOCUMENT) ? applicant.getDocumentStatus()
                        : applicant.getInterviewStatus();

        // 3. [수정] 답변 리스트 변환 로직을 Service 내부로 가져옴
        // 시트 헤더 순서대로 정렬 (orderIndex 기준, null인 경우는 맨 뒤로)
        List<ApplicantDetailResponse.AnswerDto> answerDtos =
                applicant.getExtraAnswers().stream().sorted((a1, a2) -> {
                    int order1 =
                            a1.getOrderIndex() != null ? a1.getOrderIndex() : Integer.MAX_VALUE;
                    int order2 =
                            a2.getOrderIndex() != null ? a2.getOrderIndex() : Integer.MAX_VALUE;
                    return Integer.compare(order1, order2);
                }).map(a -> ApplicantDetailResponse.AnswerDto.builder()
                        .question(a.getQuestionText()).answer(a.getAnswerText()).build()).toList();

        // 4. 해당 단계에서의 댓글 개수 조회
        long commentCount = commentRepository
                .findAllByApplicantIdAndStageOrderByCreatedAtAsc(applicantId, stage).size();

        // 5. 면접 단계일 때만 해당 지원자(applicantId)의 면접 시간 배정만 조회 (수동/자동/미배정)
        LocalDate interviewDate = null;
        LocalTime interviewStartTime = null;
        InterviewTimeSource interviewTimeSource = null;
        if (stage == RecruitmentStage.INTERVIEW) {
            Optional<InterviewAssignedTimeResponse> assignedOpt = interviewAssignmentQueryService
                    .getAssignedTimeForApplicant(projectId, applicantId, userId);
            if (assignedOpt.isPresent()) {
                InterviewAssignedTimeResponse assigned = assignedOpt.get();
                interviewTimeSource = assigned.getSource();
                if (assigned.getSource() != InterviewTimeSource.NONE) {
                    interviewDate = assigned.getInterviewDate();
                    interviewStartTime = assigned.getStartTime();
                }
            }
        }

        return ApplicantDetailResponse.builder().applicantId(applicant.getId())
                .name(applicant.getName()).gender(applicant.getGender())
                .school(applicant.getSchool()).major(applicant.getMajor())
                .position(applicant.getPosition()).phoneNumber(applicant.getPhone())
                .email(applicant.getEmail()).status(currentStatus.name())
                .isFavorite(applicant.isBookmarkedFor(stage)).commentCount(commentCount)
                .interviewDate(interviewDate).interviewStartTime(interviewStartTime)
                .interviewTimeSource(interviewTimeSource).answers(answerDtos).build();
    }

    private void validateDocumentPassForInterview(Applicant applicant, RecruitmentStage stage) {
        if (stage == RecruitmentStage.INTERVIEW
                && applicant.getDocumentStatus() != ScreeningResult.PASS) {
            throw new IllegalArgumentException(
                    "서류 합격자만 면접 단계의 대상이 됩니다. 현재 서류 상태: " + applicant.getDocumentStatus());
        }
    }
}
