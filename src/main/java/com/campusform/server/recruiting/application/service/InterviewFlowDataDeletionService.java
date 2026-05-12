package com.campusform.server.recruiting.application.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.InterviewScheduleUnassignedApplicantRepository;
import com.campusform.server.recruiting.domain.repository.InterviewScheduledSlotRepository;
import com.campusform.server.recruiting.domain.repository.InterviewSettingRepository;
import com.campusform.server.recruiting.domain.repository.IntervieweeAvailabilitySlotRepository;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;
import com.campusform.server.recruiting.domain.repository.ManualInterviewAssignmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 면접 플로우(설정·수집·배정) 관련 저장 데이터를 한 번에 삭제합니다.
 *
 * <p>{@link ProjectStageTransitionService}의 서류 롤백과 동일한 범위를 재사용합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class InterviewFlowDataDeletionService {

    private final InterviewScheduledSlotRepository interviewScheduledSlotRepository;
    private final ManualInterviewAssignmentRepository manualInterviewAssignmentRepository;
    private final InterviewScheduleUnassignedApplicantRepository interviewScheduleUnassignedApplicantRepository;
    private final InterviewSettingRepository interviewSettingRepository;
    private final IntervieweeAvailabilitySlotRepository intervieweeAvailabilitySlotRepository;
    private final InterviewerAvailabilityBlockRepository interviewerAvailabilityBlockRepository;
    private final ApplicantRepository applicantRepository;

    @Transactional
    public void deleteAllInterviewFlowData(Long projectId) {
        interviewScheduledSlotRepository.deleteByProjectId(projectId);
        manualInterviewAssignmentRepository.deleteByProjectId(projectId);
        interviewScheduleUnassignedApplicantRepository.deleteByProjectId(projectId);

        interviewSettingRepository.findByProjectId(projectId).ifPresent(setting -> {
            List<Long> dayIds = setting.getDays().stream()
                    .map(InterviewDay::getId)
                    .toList();
            if (!dayIds.isEmpty()) {
                intervieweeAvailabilitySlotRepository.deleteAllByInterviewDayIdIn(dayIds);
                interviewerAvailabilityBlockRepository.deleteByInterviewDayIdIn(dayIds);
            }
            interviewSettingRepository.delete(setting);
        });

        applicantRepository.resetInterviewDataByProjectId(projectId);
    }
}
