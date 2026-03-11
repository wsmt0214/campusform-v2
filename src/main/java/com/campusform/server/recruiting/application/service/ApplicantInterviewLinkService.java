package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.application.dto.request.applicant.UpdateApplicantLinkConfigRequest;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantInterviewLinkConfigResponse;
import com.campusform.server.recruiting.application.dto.response.applicant.ApplicantInterviewLinkResponse;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewSlotListResponse;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewAvailabilityInvestigationLink;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;
import com.campusform.server.recruiting.domain.service.InterviewSlotGenerator;

import lombok.RequiredArgsConstructor;

/**
 * 지원자 면접 가능 시간 조사 링크 관리 서비스 (관리자용)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantInterviewLinkService {

    private final InterviewContextLoader contextLoader;
    private final InterviewerAvailabilityBlockRepository availabilityBlockRepository;
    private final InterviewSlotGenerator slotGenerator = new InterviewSlotGenerator(); // 도메인 서비스

    /**
     * 지원자 링크 조회
     */
    public ApplicantInterviewLinkResponse getApplicantLink(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        InterviewAvailabilityInvestigationLink link = setting.getInvestigationLink();
        if (link == null) {
            throw new IllegalStateException("지원자 링크가 생성되지 않았습니다. projectId=" + projectId);
        }

        return ApplicantInterviewLinkResponse.of(link.getToken());
    }

    /**
     * 지원자 페이지 설정 조회
     */
    public ApplicantInterviewLinkConfigResponse getApplicantLinkConfig(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        InterviewAvailabilityInvestigationLink link = setting.getInvestigationLink();
        if (link == null) {
            throw new IllegalStateException("지원자 링크가 생성되지 않았습니다. projectId=" + projectId);
        }

        return ApplicantInterviewLinkConfigResponse.of(link.getEnabled(), link.getGuidanceText());
    }

    /**
     * 지원자 페이지 설정 수정
     */
    @Transactional
    public ApplicantInterviewLinkConfigResponse updateApplicantLinkConfig(
            Long projectId, Long userId, UpdateApplicantLinkConfigRequest request) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        InterviewAvailabilityInvestigationLink link = setting.getInvestigationLink();
        if (link == null) {
            throw new IllegalStateException("지원자 링크가 생성되지 않았습니다. projectId=" + projectId);
        }

        // 변경 감지(Dirty Checking)
        link.updateConfig(request.getEnabled(), request.getGuidanceText());

        return ApplicantInterviewLinkConfigResponse.of(link.getEnabled(), link.getGuidanceText());
    }

    /**
     * 면접 슬롯 목록 조회
     */
    public InterviewSlotListResponse getInterviewSlotList(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        // 면접 정보 설정 기반으로 날짜별 슬롯 생성
        List<InterviewSlotListResponse.DaySlotSummary> summaries = setting.getDays().stream()
                .sorted(Comparator.comparing(InterviewDay::getInterviewDate))
                .map(day -> {
                    LocalDate date = day.getInterviewDate();
                    Long dayId = day.getId();

                    // 해당 날짜의 모든 면접관 가능 시간 블록 조회
                    List<InterviewerAvailabilityBlock> dayBlocks = availabilityBlockRepository
                            .findByInterviewDayId(dayId);

                    // 도메인 서비스를 사용하여 슬롯 생성
                    // 블록 리스트를 직접 전달하여 면접관 중복 카운트 방지
                    List<InterviewSlotGenerator.SlotInfo> domainSlots = slotGenerator.generateSlots(setting,
                            dayBlocks);

                    // 도메인 DTO를 응답 DTO로 변환
                    List<InterviewSlotListResponse.SlotInfo> slots = domainSlots.stream()
                            .map(slot -> InterviewSlotListResponse.SlotInfo.of(
                                    slot.startTime(), slot.endTime(), slot.availableInterviewerCount()))
                            .toList();

                    return InterviewSlotListResponse.DaySlotSummary.of(date, slots);
                })
                .toList();

        return InterviewSlotListResponse.of(summaries);
    }
}
