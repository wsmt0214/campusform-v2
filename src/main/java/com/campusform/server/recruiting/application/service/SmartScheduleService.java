package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.ApplicantInfo;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.DaySummary;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.InterviewerInfo;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.SlotInfo;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.Statistics;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.UnassignedApplicantInfo;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduleUnassignedApplicant;
import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduledSlot;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.InterviewScheduleUnassignedApplicantRepository;
import com.campusform.server.recruiting.domain.repository.InterviewScheduledSlotRepository;
import com.campusform.server.recruiting.domain.repository.IntervieweeAvailabilitySlotRepository;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;
import com.campusform.server.recruiting.domain.service.SmartScheduleGenerator;

import lombok.RequiredArgsConstructor;

/**
 * 스마트 시간표 생성 Application Service
 *
 * 도메인 서비스({@link SmartScheduleGenerator})는 {@link SchedulePlan}만 반환하며,
 * application 레이어인 이 클래스에서 {@link SmartScheduleResponse}로 변환합니다.
 */
@Service
@RequiredArgsConstructor
public class SmartScheduleService {

    private final InterviewContextLoader contextLoader;
    private final IntervieweeAvailabilitySlotRepository applicantSlotRepository;
    private final InterviewerAvailabilityBlockRepository interviewerBlockRepository;
    private final ApplicantRepository applicantRepository;
    private final UserRepository userRepository;
    private final InterviewScheduledSlotRepository scheduledSlotRepository;
    private final InterviewScheduleUnassignedApplicantRepository unassignedApplicantRepository;

    private final SmartScheduleGenerator generator = new SmartScheduleGenerator();

    /**
     * 스마트 시간표 미리보기 (저장하지 않음)
     */
    @Transactional(readOnly = true)
    public SmartScheduleResponse generateSchedule(Long projectId, Long userId) {
        validateScheduleNotConfirmed(projectId);
        return toResponse(generateScheduleInternal(projectId));
    }

    /**
     * 스마트 시간표 생성 및 저장
     */
    @Transactional
    public SmartScheduleResponse generateAndSaveSchedule(Long projectId, Long userId) {
        validateScheduleNotConfirmed(projectId);

        Project project = contextLoader.loadContext(projectId).project();
        project.validateOwnerAccess(userId);
        project.validateInterviewStage();

        SchedulePlan plan = generateScheduleInternal(projectId);

        // 기존 결과 삭제 (덮어쓰기)
        scheduledSlotRepository.deleteByProjectId(projectId);
        unassignedApplicantRepository.deleteByProjectId(projectId);

        if (plan.days().isEmpty() && plan.unassignedApplicants().isEmpty()) {
            return toResponse(plan);
        }

        // InterviewDay date -> id 매핑
        InterviewContext context = contextLoader.loadContext(projectId);
        Map<LocalDate, Long> dateToInterviewDayId = context.setting().getDays().stream()
                .collect(Collectors.toMap(InterviewDay::getInterviewDate, InterviewDay::getId));

        // 배정된 슬롯 저장
        List<InterviewScheduledSlot> slots = new ArrayList<>();
        for (SchedulePlan.DayResult day : plan.days()) {
            Long interviewDayId = dateToInterviewDayId.get(day.date());
            if (interviewDayId == null) {
                continue;
            }

            for (SchedulePlan.SlotResult slotResult : day.slots()) {
                InterviewScheduledSlot slot = InterviewScheduledSlot.create(
                        projectId, interviewDayId, day.date(), slotResult.startTime());

                for (SchedulePlan.AssignedApplicant applicant : slotResult.applicants()) {
                    slot.addApplicant(applicant.id());
                }
                for (SchedulePlan.AssignedInterviewer interviewer : slotResult.interviewers()) {
                    slot.addInterviewer(interviewer.id());
                }

                slots.add(slot);
            }
        }
        scheduledSlotRepository.saveAll(slots);

        // 미배정 지원자 저장
        List<InterviewScheduleUnassignedApplicant> unassignedEntities = plan.unassignedApplicants().stream()
                .map(info -> InterviewScheduleUnassignedApplicant.create(
                        projectId, info.id(), info.reason().getMessage()))
                .toList();
        unassignedApplicantRepository.saveAll(unassignedEntities);

        return toResponse(plan);
    }

    /**
     * 이미 스마트 시간표가 확정된 프로젝트인지 검사
     */
    private void validateScheduleNotConfirmed(Long projectId) {
        boolean hasSlots = !scheduledSlotRepository.findByProjectId(projectId).isEmpty();
        boolean hasUnassigned = !unassignedApplicantRepository.findByProjectId(projectId).isEmpty();
        if (hasSlots || hasUnassigned) {
            throw new IllegalStateException("이미 스마트 시간표가 확정된 상태입니다. 미리보기 및 재생성할 수 없습니다.");
        }
    }

    /**
     * 내부용: 시간표 알고리즘 실행 — 도메인 결과 반환
     */
    private SchedulePlan generateScheduleInternal(Long projectId) {
        InterviewContext context = contextLoader.loadContext(projectId);
        InterviewSetting setting = context.setting();
        List<InterviewDay> days = setting.getDays();

        if (days.isEmpty()) {
            return SchedulePlan.empty();
        }

        List<Long> dayIds = days.stream().map(InterviewDay::getId).toList();

        List<IntervieweeAvailabilitySlot> applicantSlots = applicantSlotRepository.findByInterviewDayIdIn(dayIds);

        if (applicantSlots.isEmpty()) {
            return SchedulePlan.empty();
        }

        List<InterviewerAvailabilityBlock> interviewerBlocks = interviewerBlockRepository
                .findByInterviewDayIdIn(dayIds);

        Map<Long, SchedulePlan.AssignedApplicant> applicantInfoMap = buildApplicantInfoMap(applicantSlots);
        Map<Long, SchedulePlan.AssignedInterviewer> interviewerInfoMap = buildInterviewerInfoMap(interviewerBlocks);

        return generator.generate(setting, days, applicantSlots, interviewerBlocks, applicantInfoMap, interviewerInfoMap);
    }

    /**
     * 지원자 ID → AssignedApplicant 매핑
     * 서류 합격(PASS)자만 포함
     */
    private Map<Long, SchedulePlan.AssignedApplicant> buildApplicantInfoMap(
            List<IntervieweeAvailabilitySlot> slots) {
        Set<Long> applicantIds = slots.stream()
                .map(IntervieweeAvailabilitySlot::getApplicantId)
                .collect(Collectors.toSet());

        List<Applicant> applicants = applicantRepository.findByIds(applicantIds.stream().toList()).stream()
                .filter(a -> a.getDocumentStatus() == ScreeningResult.PASS)
                .toList();

        return applicants.stream()
                .collect(Collectors.toMap(
                        Applicant::getId,
                        a -> new SchedulePlan.AssignedApplicant(
                                a.getId(), a.getName(), a.getSchool(), a.getMajor(), a.getPosition())));
    }

    /**
     * 면접관 ID → AssignedInterviewer 매핑
     * 필수 면접관 여부는 알고리즘 내에서 설정되므로 기본값 false
     */
    private Map<Long, SchedulePlan.AssignedInterviewer> buildInterviewerInfoMap(
            List<InterviewerAvailabilityBlock> blocks) {
        Set<Long> interviewerIds = blocks.stream()
                .map(InterviewerAvailabilityBlock::getAdminId)
                .collect(Collectors.toSet());

        List<User> users = userRepository.findByIds(interviewerIds.stream().toList());

        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> new SchedulePlan.AssignedInterviewer(u.getId(), u.getNickname(), false)));
    }

    /**
     * SchedulePlan → SmartScheduleResponse 변환
     * application 레이어의 책임: 도메인 결과를 HTTP 응답 형태로 변환
     */
    private SmartScheduleResponse toResponse(SchedulePlan plan) {
        List<DaySummary> days = plan.days().stream()
                .map(day -> DaySummary.of(
                        day.date(),
                        day.slots().stream()
                                .map(slot -> SlotInfo.of(
                                        slot.startTime(),
                                        slot.endTime(),
                                        slot.applicants().stream()
                                                .map(a -> ApplicantInfo.of(a.id(), a.name(), a.school(), a.major(), a.position()))
                                                .toList(),
                                        slot.interviewers().stream()
                                                .map(i -> InterviewerInfo.of(i.id(), i.name(), i.required()))
                                                .toList()))
                                .toList()))
                .toList();

        List<UnassignedApplicantInfo> unassigned = plan.unassignedApplicants().stream()
                .map(u -> new UnassignedApplicantInfo(
                        u.id(), u.name(), u.school(), u.major(), u.position(),
                        u.reason().getMessage()))
                .toList();

        SchedulePlan.PlanStatistics stats = plan.statistics();
        Statistics statistics = Statistics.of(
                stats.totalApplicants(), stats.assignedApplicants(),
                stats.unassignedApplicants(), stats.usedSlots());

        return SmartScheduleResponse.of(days, unassigned, statistics);
    }
}
