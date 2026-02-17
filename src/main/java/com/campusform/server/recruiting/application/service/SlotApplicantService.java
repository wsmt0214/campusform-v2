package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.response.SlotApplicantListResponse;
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
 * 슬롯별 지원자 정보 조회 서비스
 * 
 * 프로젝트의 모든 날짜의 모든 슬롯에 제출한 지원자들의 정보를 조회합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotApplicantService {

    private final InterviewContextLoader contextLoader;
    private final IntervieweeAvailabilitySlotRepository slotRepository;
    private final InterviewerAvailabilityBlockRepository availabilityBlockRepository;
    private final ApplicantRepository applicantRepository;
    private final InterviewSlotGenerator slotGenerator = new InterviewSlotGenerator();

    /**
     * 모든 슬롯별 지원자 목록 조회
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

                    // 도메인 서비스를 사용하여 슬롯 생성
                    List<InterviewSlotGenerator.SlotInfo> domainSlots = slotGenerator.generateSlots(
                            setting,
                            dayBlocks);

                    // 해당 날짜의 모든 지원자 슬롯 조회
                    List<IntervieweeAvailabilitySlot> allDaySlots = slotRepository
                            .findByInterviewDayId(dayId);

                    // 슬롯별로 지원자 정보 매핑
                    List<SlotApplicantListResponse.SlotApplicantInfo> slotApplicantInfos = domainSlots
                            .stream()
                            .map(domainSlot -> {
                                LocalTime startTime = domainSlot.startTime();
                                LocalTime endTime = domainSlot.endTime();

                                // 해당 슬롯에 제출한 지원자 슬롯 필터링
                                List<IntervieweeAvailabilitySlot> slotSlots = allDaySlots
                                        .stream()
                                        .filter(slot -> slot.getStartTime()
                                                .equals(startTime))
                                        .collect(Collectors.toList());

                                // 지원자가 없는 경우 빈 리스트
                                if (slotSlots.isEmpty()) {
                                    return SlotApplicantListResponse.SlotApplicantInfo
                                            .of(startTime, endTime,
                                                    List.of());
                                }

                                // 지원자 ID 목록 추출
                                List<Long> applicantIds = slotSlots.stream()
                                        .map(IntervieweeAvailabilitySlot::getApplicantId)
                                        .distinct()
                                        .collect(Collectors.toList());

                                // 지원자 정보 조회
                                List<Applicant> applicants = applicantRepository
                                        .findByIds(applicantIds);

                                // 응답 DTO로 변환
                                List<SlotApplicantListResponse.ApplicantInfo> applicantInfos = applicants
                                        .stream()
                                        .map(applicant -> SlotApplicantListResponse.ApplicantInfo
                                                .of(applicant.getId(),
                                                        applicant.getName(),
                                                        applicant.getSchool(),
                                                        applicant.getMajor(),
                                                        applicant.getPosition()))
                                        .collect(Collectors.toList());

                                return SlotApplicantListResponse.SlotApplicantInfo.of(
                                        startTime, endTime,
                                        applicantInfos);
                            })
                            .collect(Collectors.toList());

                    return SlotApplicantListResponse.DaySlotSummary.of(date, slotApplicantInfos);
                })
                .collect(Collectors.toList());

        return SlotApplicantListResponse.of(summaries);
    }
}
