package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.response.message.SlotApplicantListResponse;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.IntervieweeAvailabilitySlotRepository;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;
import com.campusform.server.recruiting.domain.service.InterviewSlotGenerator;
import lombok.RequiredArgsConstructor;

/**
 * 면접 슬롯별 지원자 목록 조회 서비스 (읽기 전용)
 *
 * 면접관들이 제출한 가능 시간을 바탕으로 생성된 슬롯별로
 * 지원자들의 신청 현황을 조회합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewSlotApplicantQueryService {

    private final InterviewContextLoader contextLoader;
    private final IntervieweeAvailabilitySlotRepository slotRepository;
    private final InterviewerAvailabilityBlockRepository availabilityBlockRepository;
    private final ApplicantRepository applicantRepository;
    private final InterviewSlotGenerator slotGenerator = new InterviewSlotGenerator();

    /**
     * 프로젝트의 모든 면접 슬롯에 대한 지원자 신청 현황 조회
     */
    public SlotApplicantListResponse getAllApplicantsBySlots(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        InterviewSetting setting = ctx.setting();
        Project project = ctx.project();
        project.validateAdminAccess(userId);

        // 모든 날짜별로 슬롯과 지원자 정보 조회
        List<SlotApplicantListResponse.DaySlotSummary> summaries = setting.getDays().stream()
                .sorted(Comparator.comparing(InterviewDay::getInterviewDate))
                .map(day -> {
                    LocalDate date = day.getInterviewDate();
                    Long dayId = day.getId();

                    // 해당 날짜의 모든 면접관 가능 시간 블록 조회
                    List<InterviewerAvailabilityBlock> dayBlocks = availabilityBlockRepository
                            .findByInterviewDayId(dayId);

                    // 해당 날짜의 모든 슬롯 생성
                    List<InterviewSlotGenerator.SlotInfo> domainSlots = slotGenerator.generateSlots(
                            setting,
                            dayBlocks);

                    // 해당 날짜의 모든 슬롯 조회
                    List<IntervieweeAvailabilitySlot> allDaySlots = slotRepository
                            .findByInterviewDayId(dayId);

                    // 모든 슬롯별로 지원자 정보 조회
                    List<SlotApplicantListResponse.SlotApplicantInfo> slotApplicantInfos = domainSlots
                            .stream()
                            .map(domainSlot -> {
                                LocalTime startTime = domainSlot.startTime();
                                LocalTime endTime = domainSlot.endTime();

                                // 해당 슬롯에 신청한 지원자 조회
                                List<IntervieweeAvailabilitySlot> matchedSlots = allDaySlots
                                        .stream()
                                        .filter(slot -> slot.getStartTime().equals(startTime))
                                        .collect(Collectors.toList());

                                // 해당 슬롯에 신청한 지원자가 없으면 빈 리스트 반환
                                if (matchedSlots.isEmpty()) {
                                    return SlotApplicantListResponse.SlotApplicantInfo
                                            .of(startTime, endTime, List.of());
                                }

                                // 해당 슬롯에 신청한 지원자 ID 조회
                                List<Long> applicantIds = matchedSlots.stream()
                                        .map(IntervieweeAvailabilitySlot::getApplicantId)
                                        .distinct()
                                        .collect(Collectors.toList());

                                // 해당 슬롯에 신청한 지원자 조회
                                List<Applicant> applicants = applicantRepository.findByIds(applicantIds);

                                // 해당 슬롯에 신청한 지원자 정보 조회
                                List<SlotApplicantListResponse.ApplicantInfo> applicantInfos = applicants
                                        .stream()
                                        .map(applicant -> SlotApplicantListResponse.ApplicantInfo
                                                .of(applicant.getId(),
                                                        applicant.getName(),
                                                        applicant.getSchool(),
                                                        applicant.getMajor(),
                                                        applicant.getPosition()))
                                        .collect(Collectors.toList());

                                // 해당 슬롯에 신청한 지원자 정보 반환
                                return SlotApplicantListResponse.SlotApplicantInfo.of(
                                        startTime, endTime, applicantInfos);
                            })
                            .collect(Collectors.toList());

                    return SlotApplicantListResponse.DaySlotSummary.of(date, slotApplicantInfos);
                })
                .collect(Collectors.toList());

        return SlotApplicantListResponse.of(summaries);
    }
}
