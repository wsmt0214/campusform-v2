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
import com.campusform.server.recruiting.application.dto.response.SmartScheduleResponse;
import com.campusform.server.recruiting.application.dto.response.SmartScheduleResponse.ApplicantInfo;
import com.campusform.server.recruiting.application.dto.response.SmartScheduleResponse.DaySummary;
import com.campusform.server.recruiting.application.dto.response.SmartScheduleResponse.InterviewerInfo;
import com.campusform.server.recruiting.application.dto.response.SmartScheduleResponse.SlotInfo;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;
import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduleUnassignedApplicant;
import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduledSlot;
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
 */
@Service
@RequiredArgsConstructor
public class SmartScheduleService {

    private final InterviewContextLoader contextLoader;
    private final IntervieweeAvailabilitySlotRepository applicantSlotRepository;
    private final InterviewerAvailabilityBlockRepository interviewerBlockRepository;
    private final ApplicantRepository applicantRepository;
    private final UserRepository userRepository;

    // 각 애그리거트별 Repository
    private final InterviewScheduledSlotRepository scheduledSlotRepository;
    private final InterviewScheduleUnassignedApplicantRepository unassignedApplicantRepository;

    // 도메인 서비스
    private final SmartScheduleGenerator generator = new SmartScheduleGenerator();

    /**
     * 프로젝트의 스마트 시간표 미리보기 (저장하지 않음)
     */
    @Transactional(readOnly = true)
    public SmartScheduleResponse generateSchedule(Long projectId, Long userId) {
        contextLoader.loadContext(projectId).project().validateOwnerAccess(userId);
        return generateScheduleInternal(projectId);
    }

    /**
     * 프로젝트의 스마트 시간표 생성 및 저장
     */
    @Transactional
    public SmartScheduleResponse generateAndSaveSchedule(Long projectId, Long userId) {
        Project project = contextLoader.loadContext(projectId).project();
        project.validateOwnerAccess(userId);

        // 스마트 시간표 생성/저장은 면접 단계(INTERVIEW)에서만 가능
        project.validateInterviewStage();

        // 시간표 생성
        SmartScheduleResponse response = generateScheduleInternal(projectId);

        // 기존 결과 삭제 (덮어쓰기)
        scheduledSlotRepository.deleteByProjectId(projectId);
        unassignedApplicantRepository.deleteByProjectId(projectId);

        if (response.getDays().isEmpty() && response.getUnassignedApplicants().isEmpty()) {
            return response;
        }

        // InterviewDay date -> id 매핑
        InterviewContext context = contextLoader.loadContext(projectId);
        Map<LocalDate, Long> dateToInterviewDayId = context.setting().getDays().stream()
                .collect(Collectors.toMap(InterviewDay::getInterviewDate, InterviewDay::getId));

        // 배정된 슬롯 저장
        List<InterviewScheduledSlot> slots = new ArrayList<>();
        for (DaySummary day : response.getDays()) {
            Long interviewDayId = dateToInterviewDayId.get(day.getDate());
            if (interviewDayId == null) {
                continue;
            }

            for (SlotInfo slotInfo : day.getSlots()) {
                InterviewScheduledSlot slot = InterviewScheduledSlot.create(
                        projectId, interviewDayId, day.getDate(), slotInfo.getStartTime());

                // 지원자 추가
                for (ApplicantInfo applicant : slotInfo.getApplicants()) {
                    slot.addApplicant(applicant.getId());
                }

                // 면접관 추가
                for (InterviewerInfo interviewer : slotInfo.getInterviewers()) {
                    slot.addInterviewer(interviewer.getId());
                }

                slots.add(slot);
            }
        }
        scheduledSlotRepository.saveAll(slots);

        // 미배정 지원자 저장
        List<InterviewScheduleUnassignedApplicant> unassignedEntities = response.getUnassignedApplicants().stream()
                .map(info -> InterviewScheduleUnassignedApplicant.create(
                        projectId, info.getId(), info.getReason()))
                .toList();
        unassignedApplicantRepository.saveAll(unassignedEntities);

        return response;
    }

    /**
     * 내부용: 시간표 생성 로직
     */
    private SmartScheduleResponse generateScheduleInternal(Long projectId) {
        InterviewContext context = contextLoader.loadContext(projectId);
        InterviewSetting setting = context.setting();
        List<InterviewDay> days = setting.getDays();

        if (days.isEmpty()) {
            return SmartScheduleResponse.empty();
        }

        // 면접 일자자
        List<Long> dayIds = days.stream()
                .map(InterviewDay::getId)
                .toList();

        // 지원자 제출 슬롯
        List<IntervieweeAvailabilitySlot> applicantSlots = applicantSlotRepository.findByInterviewDayIdIn(dayIds);

        if (applicantSlots.isEmpty()) {
            return SmartScheduleResponse.empty();
        }

        // 면접관 제출 블록록
        List<InterviewerAvailabilityBlock> interviewerBlocks = interviewerBlockRepository
                .findByInterviewDayIdIn(dayIds);

        // 지원자 id -> ApplicantInfo 생성
        Map<Long, ApplicantInfo> applicantInfoMap = buildApplicantInfoMap(applicantSlots);

        // 면접관 id -> InterviewerInfo 생성
        Map<Long, InterviewerInfo> interviewerInfoMap = buildInterviewerInfoMap(interviewerBlocks);

        // 스마트 시간표 알고리즘
        return generator.generate(
                setting,
                days,
                applicantSlots,
                interviewerBlocks,
                applicantInfoMap,
                interviewerInfoMap);
    }

    /**
     * 지원자 id -> ApplicantInfo 생성
     */
    private Map<Long, ApplicantInfo> buildApplicantInfoMap(List<IntervieweeAvailabilitySlot> slots) {
        Set<Long> applicantIds = slots.stream()
                .map(IntervieweeAvailabilitySlot::getApplicantId)
                .collect(Collectors.toSet());

        // 서류 합격(PASS)자만 스마트 시간표 대상에 포함 (서류 불합격자 제외)
        List<Applicant> applicants = applicantRepository.findByIds(applicantIds.stream().toList()).stream()
                .filter(a -> a.getDocumentStatus() == ScreeningResult.PASS)
                .toList();

        return applicants.stream()
                .collect(Collectors.toMap(
                        Applicant::getId,
                        a -> ApplicantInfo.of(
                                a.getId(),
                                a.getName(),
                                a.getSchool(),
                                a.getMajor(),
                                a.getPosition())));
    }

    /**
     * 면접관 id -> InterviewerInfo 생성
     */
    private Map<Long, InterviewerInfo> buildInterviewerInfoMap(List<InterviewerAvailabilityBlock> blocks) {
        Set<Long> interviewerIds = blocks.stream()
                .map(InterviewerAvailabilityBlock::getAdminId)
                .collect(Collectors.toSet());

        List<User> users = userRepository.findByIds(interviewerIds.stream().toList());

        // 필수 면접관 여부는 SmartScheduleGenerator에서 설정하므로 기본값 false
        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> InterviewerInfo.of(
                                u.getId(),
                                u.getNickname(),
                                false)));
    }
}
