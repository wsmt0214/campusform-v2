package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.application.dto.response.InterviewAssignedTimeResponse;
import com.campusform.server.recruiting.application.dto.response.InterviewTimeSource;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduledSlot;
import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.InterviewScheduledSlotRepository;
import com.campusform.server.recruiting.domain.repository.ManualInterviewAssignmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 최종 면접시간 조회 Application Service
 * 우선 순위: Manual -> Auto -> None
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewAssignmentQueryService {

    private final InterviewContextLoader contextLoader;
    private final ApplicantRepository applicantRepository;
    private final ManualInterviewAssignmentRepository manualAssignmentRepository;
    private final InterviewScheduledSlotRepository scheduledSlotRepository;

    /**
     * 프로젝트 내 전체 지원자의 최종 면접시간 조회
     */
    public List<InterviewAssignedTimeResponse> getAssignedTimes(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);

        int slotDurationMin = ctx.setting().getSlotDurationMin();

        List<Applicant> applicants = applicantRepository.findByProjectId(projectId);

        /**
         * 면접 시간 수동 설정 가져오기
         * Map: applicantId -> 수동 설정 정보
         */
        Map<Long, ManualInterviewAssignment> manualMap = manualAssignmentRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(ManualInterviewAssignment::getApplicantId, a -> a));

        // Map: applicantId -> (date, startTime)
        Map<Long, AutoTime> autoMap = scheduledSlotRepository.findByProjectIdWithApplicants(projectId).stream()
                .sorted(Comparator.comparing(InterviewScheduledSlot::getDate)
                        .thenComparing(InterviewScheduledSlot::getStartTime))
                .flatMap(slot -> slot.getApplicants().stream()
                        .map(a -> new AutoTime(a.getApplicantId(), slot.getDate(), slot.getStartTime())))
                .collect(Collectors.toMap(AutoTime::applicantId, t -> t));

        // 최종 구성
        return applicants.stream()
                .map(applicant -> {
                    Long applicantId = applicant.getId();

                    // 수동 설정 우선
                    ManualInterviewAssignment manual = manualMap.get(applicantId);
                    if (manual != null) {
                        LocalDate date = manual.getInterviewDate();
                        LocalTime start = manual.getStartTime();
                        LocalTime end = (start == null) ? null : start.plusMinutes(slotDurationMin);

                        return InterviewAssignedTimeResponse.of(
                                applicantId,
                                applicant.getName(),
                                applicant.getSchool(),
                                applicant.getMajor(),
                                applicant.getPosition(),
                                date,
                                start,
                                end,
                                InterviewTimeSource.MANUAL);
                    }

                    // 알고리즘 결과과
                    AutoTime auto = autoMap.get(applicantId);
                    if (auto != null) {
                        LocalDate date = auto.date();
                        LocalTime start = auto.startTime();
                        LocalTime end = start.plusMinutes(slotDurationMin);

                        return InterviewAssignedTimeResponse.of(
                                applicantId,
                                applicant.getName(),
                                applicant.getSchool(),
                                applicant.getMajor(),
                                applicant.getPosition(),
                                date,
                                start,
                                end,
                                InterviewTimeSource.AUTO);
                    }

                    // 설정 안됨
                    return InterviewAssignedTimeResponse.of(
                            applicantId,
                            applicant.getName(),
                            applicant.getSchool(),
                            applicant.getMajor(),
                            applicant.getPosition(),
                            null,
                            null,
                            null,
                            InterviewTimeSource.NONE);
                })
                .toList();
    }

    /**
     * AUTO 배정 결과를 applicantId 기준으로 매핑하기 위한 내부 record
     */
    private record AutoTime(Long applicantId, LocalDate date, LocalTime startTime) {
    }
}
