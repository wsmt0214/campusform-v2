package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewAssignedTimeResponse;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewTimeSource;
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
         * 
         * 면접 설정이 없어도 동작하도록 수정 (면접 정보는 null로 반환)
         */
        public List<InterviewAssignedTimeResponse> getAssignedTimes(Long projectId, Long userId) {
                // 프로젝트만 조회 (면접 설정은 Optional로 처리)
                Project project = contextLoader.loadProjectOrThrow(projectId);
                project.validateAdminAccess(userId);

                List<Applicant> applicants = applicantRepository.findByProjectId(projectId);

                /**
                 * 면접 시간 수동 설정 가져오기
                 * Map: applicantId -> 수동 설정 정보
                 */
                Map<Long, ManualInterviewAssignment> manualMap = manualAssignmentRepository.findByProjectId(projectId)
                                .stream()
                                .collect(Collectors.toMap(ManualInterviewAssignment::getApplicantId, a -> a));

                // Map: applicantId -> (date, startTime)
                Map<Long, AutoTime> autoMap = scheduledSlotRepository.findByProjectIdWithApplicants(projectId).stream()
                                .sorted(Comparator.comparing(InterviewScheduledSlot::getDate)
                                                .thenComparing(InterviewScheduledSlot::getStartTime))
                                .flatMap(slot -> slot.getApplicants().stream()
                                                .map(a -> new AutoTime(a.getApplicantId(), slot.getDate(),
                                                                slot.getStartTime())))
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

                                                return InterviewAssignedTimeResponse.of(
                                                                applicantId,
                                                                date,
                                                                start,
                                                                InterviewTimeSource.MANUAL);
                                        }

                                        // 알고리즘 결과과
                                        AutoTime auto = autoMap.get(applicantId);
                                        if (auto != null) {
                                                LocalDate date = auto.date();
                                                LocalTime start = auto.startTime();

                                                return InterviewAssignedTimeResponse.of(
                                                                applicantId,
                                                                date,
                                                                start,
                                                                InterviewTimeSource.AUTO);
                                        }

                                        // 설정 안됨
                                        return InterviewAssignedTimeResponse.of(
                                                        applicantId,
                                                        null,
                                                        null,
                                                        InterviewTimeSource.NONE);
                                })
                                .toList();
        }

        /**
         * 특정 지원자(applicantId)의 면접 시간 배정만 조회
         * 상세 조회 등에서 "해당 지원자의" 배정 정보만 필요할 때 사용 (프로젝트 전체 목록 조회 X)
         */
        public Optional<InterviewAssignedTimeResponse> getAssignedTimeForApplicant(
                        Long projectId, Long applicantId, Long userId) {
                Project project = contextLoader.loadProjectOrThrow(projectId);
                project.validateAdminAccess(userId);

                // 수동 배정: 해당 지원자만 조회
                Optional<ManualInterviewAssignment> manual = manualAssignmentRepository
                                .findByProjectIdAndApplicantId(projectId, applicantId);
                if (manual.isPresent()) {
                        ManualInterviewAssignment m = manual.get();
                        return Optional.of(InterviewAssignedTimeResponse.of(
                                        applicantId,
                                        m.getInterviewDate(),
                                        m.getStartTime(),
                                        InterviewTimeSource.MANUAL));
                }

                // 자동 배정: 프로젝트 슬롯에서 해당 지원자 포함된 슬롯 찾기
                AutoTime auto = scheduledSlotRepository.findByProjectIdWithApplicants(projectId).stream()
                                .sorted(Comparator.comparing(InterviewScheduledSlot::getDate)
                                                .thenComparing(InterviewScheduledSlot::getStartTime))
                                .flatMap(slot -> slot.getApplicants().stream()
                                                .filter(a -> applicantId.equals(a.getApplicantId()))
                                                .map(a -> new AutoTime(a.getApplicantId(), slot.getDate(),
                                                                slot.getStartTime())))
                                .findFirst()
                                .orElse(null);
                if (auto != null) {
                        return Optional.of(InterviewAssignedTimeResponse.of(
                                        applicantId,
                                        auto.date(),
                                        auto.startTime(),
                                        InterviewTimeSource.AUTO));
                }

                return Optional.of(InterviewAssignedTimeResponse.of(
                                applicantId, null, null, InterviewTimeSource.NONE));
        }

        /**
         * AUTO 배정 결과를 applicantId 기준으로 매핑하기 위한 내부 record
         */
        private record AutoTime(Long applicantId, LocalDate date, LocalTime startTime) {
        }
}
