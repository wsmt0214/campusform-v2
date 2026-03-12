package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.project.application.service.ProjectAccessService;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.request.interview.UpsertInterviewSettingRequest;
import com.campusform.server.recruiting.application.dto.response.interview.InterviewSettingResponse;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.model.interview.setup.value.SlotConfiguration;
import com.campusform.server.recruiting.domain.model.interview.setup.value.TimeRange;
import com.campusform.server.recruiting.domain.repository.InterviewSettingRepository;
import lombok.RequiredArgsConstructor;

/**
 * 면접 정보 설정 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewSettingService {

    private final InterviewContextLoader contextLoader;
    private final InterviewSettingRepository interviewSettingRepository;
    private final ProjectAccessService projectAccessService;

    /**
     * 면접 정보 설정 조회 (프로젝트 OWNER/ADMIN만 조회 가능)
     */
    public InterviewSettingResponse getSetting(Long projectId, Long userId) {
        projectAccessService.getProjectWithAdminAccess(projectId, userId);
        contextLoader.loadProjectOrThrow(projectId);

        return interviewSettingRepository.findByProjectId(projectId)
                .map(this::convertToResponse)
                .orElseGet(InterviewSettingResponse::unconfigured);
    }

    /**
     * 면접 정보 설정 저장/수정 (프로젝트 OWNER/ADMIN만 가능)
     */
    @Transactional
    public InterviewSettingResponse saveOrUpdateSetting(Long projectId, Long userId,
            UpsertInterviewSettingRequest request) {
        projectAccessService.getProjectWithAdminAccess(projectId, userId);

        Project project = contextLoader.loadProjectOrThrow(projectId);

        // 면접 단계(INTERVIEW)에서만 면접 정보 설정 저장/수정 가능
        project.validateInterviewStage();

        // 값 객체 생성 -> 도메인 규칙 검증은 값 객체에서 수행
        TimeRange timeRange = TimeRange.of(request.getStartTime(), request.getEndTime());
        SlotConfiguration slotConfig = SlotConfiguration.of(
                request.getSlotDurationMin(),
                request.getSlotBreakMin(),
                request.getMaxApplicantsPerSlot(),
                request.getMinInterviewersPerSlot(),
                request.getMaxInterviewersPerSlot());

        // 날짜 목록: null이면 빈 리스트, 중복 제거 후 정렬
        List<LocalDate> interviewDates = Stream.ofNullable(request.getInterviewDates())
                .flatMap(List::stream)
                .distinct()
                .sorted()
                .toList();

        InterviewSetting setting = interviewSettingRepository.findByProjectId(projectId)
                .orElse(null);

        // Upsert
        if (setting == null) {
            setting = InterviewSetting.create(projectId, timeRange, slotConfig, interviewDates);
            interviewSettingRepository.save(setting);
        } else {
            setting.update(timeRange, slotConfig, interviewDates);
        }

        return convertToResponse(setting);
    }

    /**
     * InterviewSetting 엔티티를 InterviewSettingResponse DTO로 변환
     */
    private InterviewSettingResponse convertToResponse(InterviewSetting setting) {
        List<LocalDate> interviewDates = setting.getDays().stream()
                .map(d -> d.getInterviewDate())
                .distinct()
                .sorted()
                .toList();

        return new InterviewSettingResponse(
                true,
                interviewDates,
                setting.getStartTime(),
                setting.getEndTime(),
                setting.getMaxApplicantsPerSlot(),
                setting.getMinInterviewersPerSlot(),
                setting.getMaxInterviewersPerSlot(),
                setting.getSlotDurationMin(),
                setting.getSlotBreakMin(),
                setting.getInvestigationLink() != null ? setting.getInvestigationLink().getToken() : null);
    }
}
