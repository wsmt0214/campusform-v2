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
public class InterviewAvailabilityLinkService {

    private final InterviewContextLoader contextLoader;
    private final InterviewerAvailabilityBlockRepository availabilityBlockRepository;
    private final InterviewSlotGenerator slotGenerator = new InterviewSlotGenerator();

    /**
     * 지원자에게 배포할 면접 가능 시간 제출 링크 조회
     */
    public ApplicantInterviewLinkResponse getInvestigationLink(Long projectId, Long userId) {
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
     * 지원자 시간 제출 페이지 설정 조회 (활성화 여부, 안내 문구)
     */
    public ApplicantInterviewLinkConfigResponse getInvestigationLinkConfig(Long projectId, Long userId) {
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
     * 지원자 시간 제출 페이지 설정 수정 (활성화 여부, 안내 문구)
     */
    @Transactional
    public ApplicantInterviewLinkConfigResponse updateInvestigationLinkConfig(
            Long projectId, Long userId, UpdateApplicantLinkConfigRequest request) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        InterviewAvailabilityInvestigationLink link = setting.getInvestigationLink();
        if (link == null) {
            throw new IllegalStateException("지원자 링크가 생성되지 않았습니다. projectId=" + projectId);
        }

        link.updateConfig(request.getEnabled(), request.getGuidanceText());

        return ApplicantInterviewLinkConfigResponse.of(link.getEnabled(), link.getGuidanceText());
    }

    /**
     * 관리자용 전체 면접 슬롯 목록 조회
     * 면접관들이 제출한 가능 시간 기반으로 슬롯 생성 후, 슬롯별 참여 가능 면접관 수 포함
     */
    public InterviewSlotListResponse getInterviewSlotList(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        List<InterviewSlotListResponse.DaySlotSummary> summaries = setting.getDays().stream()
                .sorted(Comparator.comparing(InterviewDay::getInterviewDate))
                .map(day -> {
                    LocalDate date = day.getInterviewDate();
                    Long dayId = day.getId();

                    List<InterviewerAvailabilityBlock> dayBlocks = availabilityBlockRepository
                            .findByInterviewDayId(dayId);

                    List<InterviewSlotGenerator.SlotInfo> domainSlots = slotGenerator.generateSlots(setting, dayBlocks);

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
