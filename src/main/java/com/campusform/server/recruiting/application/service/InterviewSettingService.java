package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.request.UpsertInterviewSettingRequest;
import com.campusform.server.recruiting.application.dto.response.InterviewSettingResponse;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.model.interview.setup.value.DateRange;
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

    /**
     * 면접 정보 설정 조회
     */
    public InterviewSettingResponse getSetting(Long projectId, Long userId) {
        Project project = contextLoader.loadProjectOrThrow(projectId);
        project.validateOwnerAccess(userId);

        /**
         * interviewSettingRepository.findByProjectId(projectId)는
         * Optional<InterviewSetting>을 반환
         * 값이 있으면 map() 수행 / 값이 없으면 orElseGet() 수행
         */
        return interviewSettingRepository.findByProjectId(projectId)
                .map(this::convertToResponse)
                .orElseGet(InterviewSettingResponse::unconfigured);
    }

    /**
     * 면접 정보 설정 저장/수정
     */
    @Transactional
    public InterviewSettingResponse saveOrUpdateSetting(Long projectId, Long userId,
            UpsertInterviewSettingRequest request) {
        Project project = contextLoader.loadProjectOrThrow(projectId);
        project.validateOwnerAccess(userId);

        // 값 객체 생성 -> 도메인 규칙 검증은 값 객체에서 수행
        TimeRange timeRange = TimeRange.of(request.getStartTime(), request.getEndTime());
        SlotConfiguration slotConfig = SlotConfiguration
                .of(
                        request.getSlotDurationMin(),
                        request.getSlotBreakMin(),
                        request.getMaxApplicantsPerSlot(),
                        request.getMinInterviewersPerSlot(),
                        request.getMaxInterviewersPerSlot());
        DateRange dateRange = DateRange.of(request.getStartDate(), request.getEndDate());

        InterviewSetting setting = interviewSettingRepository.findByProjectId(projectId)
                .orElse(null);
        // 현재 파라미터는 도메인 규칙을 통해 검증된 상태

        // Upsert
        if (setting == null) {
            setting = InterviewSetting.create(projectId, timeRange, slotConfig, dateRange);
            interviewSettingRepository.save(setting);
        } else {
            setting.update(timeRange, slotConfig, dateRange);
        }

        return convertToResponse(setting);
    }

    /**
     * InterviewSetting 엔티티를 InterviewSettingResponse DTO로 변환
     */
    private InterviewSettingResponse convertToResponse(InterviewSetting setting) {
        List<LocalDate> dates = setting.getDays().stream()
                .map(d -> d.getInterviewDate())
                .distinct()
                .sorted()
                .toList();

        LocalDate startDate = dates.stream().min(Comparator.naturalOrder()).orElse(null);
        LocalDate endDate = dates.stream().max(Comparator.naturalOrder()).orElse(null);

        return new InterviewSettingResponse(
                true,
                startDate,
                endDate,
                dates,
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
