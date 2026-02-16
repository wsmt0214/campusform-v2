package com.campusform.server.recruiting.application.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.request.UpsertInterviewerAvailabilityRequest;
import com.campusform.server.recruiting.application.dto.response.InterviewerAvailabilityResponse;
import com.campusform.server.recruiting.application.dto.response.InterviewerAvailabilitySummaryResponse;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;

import lombok.RequiredArgsConstructor;

/**
 * 면접관 가능 시간 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewerAvailabilityService {

    private final InterviewContextLoader contextLoader;
    private final InterviewerAvailabilityBlockRepository availabilityBlockRepository;
    private final UserRepository userRepository;

    /**
     * 특정 면접관의 가능 시간 조회
     */
    public InterviewerAvailabilityResponse getInterviewerAvailability(Long projectId, Long userId, Long adminId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(adminId);

        InterviewSetting setting = ctx.setting();

        // User 정보 조회
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + adminId));

        // 프로젝트의 모든 InterviewDay ID 수집
        List<Long> projectDayIds = setting.getDays().stream()
                .map(InterviewDay::getId)
                .toList();

        // 해당 면접관의 프로젝트 범위 내 블록만 조회 (DB 레벨 필터링)
        List<InterviewerAvailabilityBlock> blocks = availabilityBlockRepository
                .findByAdminIdAndInterviewDayIdIn(adminId, projectDayIds);

        /**
         * 해당 면접관의 프로젝트 범위 내 가능 시간 블록을 InterviewDay ID별로 그룹화
         * 예시: {1L=[블록1, 블록2], 2L=[블록3]}
         */
        Map<Long, List<InterviewerAvailabilityBlock>> dayIdToBlocks = blocks.stream()
                .collect(Collectors.groupingBy(InterviewerAvailabilityBlock::getInterviewDayId));

        /**
         * InterviewDay ID -> LocalDate 매핑 생성
         * 예시: {1L=2024-07-01, 2L=2024-07-02}
         */
        Map<Long, LocalDate> dayIdToDate = setting.getDays().stream()
                .collect(Collectors.toMap(InterviewDay::getId, InterviewDay::getInterviewDate));

        List<InterviewerAvailabilityResponse.DayAvailability> availabilities = dayIdToDate.entrySet().stream()
                // 해당 날짜에 등록된 블록이 있는 경우만 선택
                .filter(entry -> dayIdToBlocks.containsKey(entry.getKey()))
                .map(entry -> {
                    LocalDate date = entry.getValue(); // 날짜
                    Long dayId = entry.getKey(); // InterviewDay ID
                    List<InterviewerAvailabilityBlock> dayBlocks = dayIdToBlocks.get(dayId); // 그날의
                                                                                             // 블록들

                    // 각 블록을 30분 단위 TimeBlock으로 변환 (시간순 정렬)
                    List<InterviewerAvailabilityResponse.TimeBlock> timeBlocks = dayBlocks.stream()
                            .sorted(Comparator.comparing(
                                    InterviewerAvailabilityBlock::getStartTime))
                            .map(block -> InterviewerAvailabilityResponse.TimeBlock.of(
                                    block.getStartTime(),
                                    block.getStartTime().plusMinutes(30)))
                            .toList();

                    // 날짜와 해당 블록 리스트를 DayAvailability로 반환
                    return InterviewerAvailabilityResponse.DayAvailability.of(date, timeBlocks);
                })
                // 날짜 오름차순 정렬
                .sorted(Comparator.comparing(InterviewerAvailabilityResponse.DayAvailability::getDate))
                .toList();

        return InterviewerAvailabilityResponse.of(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                availabilities);
    }

    /**
     * 특정 면접관의 가능 시간 전체 교체
     * 
     * 해당 면접관의 모든 기존 블록을 삭제하고, 요청에 포함된 블록으로 새로 설정합니다.
     */
    @Transactional
    public InterviewerAvailabilityResponse replaceInterviewerAvailability(
            Long projectId, Long userId, Long adminId, UpsertInterviewerAvailabilityRequest request) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        Project project = ctx.project();
        project.validateAdminAccess(adminId);

        // 면접관 가능 시간 설정은 면접 단계(INTERVIEW)에서만 가능
        project.validateInterviewStage();
        InterviewSetting setting = ctx.setting();

        // 면접 정보 설정 기반으로 날짜 매핑 생성 (LocalDate -> InterviewDay)
        Map<LocalDate, InterviewDay> dateToDay = setting.getDays().stream()
                .collect(Collectors.toMap(InterviewDay::getInterviewDate, day -> day));

        // 프로젝트의 모든 InterviewDay ID 수집
        List<Long> projectDayIds = setting.getDays().stream()
                .map(InterviewDay::getId)
                .toList();

        // 해당 면접관의 프로젝트 범위 내 블록만 조회하여 삭제 (DB 레벨 필터링)
        List<InterviewerAvailabilityBlock> blocksToDelete = availabilityBlockRepository
                .findByAdminIdAndInterviewDayIdIn(adminId, projectDayIds);

        if (!blocksToDelete.isEmpty()) {
            availabilityBlockRepository.deleteAll(blocksToDelete);
            // 쓰기 지연으로 인한 unique 제약 조건 위반을 방지하기 위해 즉시 DB에 반영
            availabilityBlockRepository.flush();
        }

        // 요청에 포함된 새 블록 생성 및 저장
        List<InterviewerAvailabilityBlock> blocksToSave = new ArrayList<>();
        for (UpsertInterviewerAvailabilityRequest.DayAvailability dayAvail : request.getAvailabilities()) {
            LocalDate date = dayAvail.getDate();
            InterviewDay interviewDay = dateToDay.get(date);

            if (interviewDay == null) {
                throw new IllegalArgumentException(
                        "날짜가 유효하지 않습니다. date=" + date + ", projectId=" + projectId);
            }

            for (LocalTime startTime : dayAvail.getStartTimes()) {
                // 도메인 엔티티에서 검증 수행
                setting.validateBlockStartTime(startTime);
                setting.validateBlockWithinTimeRange(startTime);
                blocksToSave.add(InterviewerAvailabilityBlock.create(adminId, interviewDay.getId(),
                        startTime));
            }
        }

        // 새 블록 저장
        if (!blocksToSave.isEmpty()) {
            availabilityBlockRepository.saveAll(blocksToSave);
        }

        return getInterviewerAvailability(projectId, userId, adminId);
    }

    /**
     * 시간대별 가능 면접관 수 집계 ("전체" 버튼 시각화용)
     */
    public InterviewerAvailabilitySummaryResponse getAvailabilitySummary(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        // 면접 정보 설정 기반으로 날짜별 시간 블록 집계
        List<InterviewerAvailabilitySummaryResponse.DaySummary> summaries = setting.getDays().stream()
                .sorted(Comparator.comparing(InterviewDay::getInterviewDate))
                .map(day -> summarizeDay(day, setting))
                .toList();

        return InterviewerAvailabilitySummaryResponse.of(summaries);
    }

    /**
     * 날짜별 면접관 가능 시간 집계
     */
    private InterviewerAvailabilitySummaryResponse.DaySummary summarizeDay(
            InterviewDay day, InterviewSetting setting) {
        LocalDate date = day.getInterviewDate();
        Long dayId = day.getId();

        // 해당 날짜의 모든 면접관 가능 시간 블록 조회
        List<InterviewerAvailabilityBlock> dayBlocks = availabilityBlockRepository
                .findByInterviewDayId(dayId);

        // 시간대별로 그룹화하여 면접관 수 집계
        Map<LocalTime, Long> blockCounts = dayBlocks.stream()
                .collect(Collectors.groupingBy(
                        InterviewerAvailabilityBlock::getStartTime,
                        Collectors.counting()));

        // 면접 정보 설정 시간 범위 내 30분 단위 시간 블록 생성
        List<LocalTime> blocks = setting.getTimeRange().generateBlocks();

        // 블록별 면접관 수를 TimeBlockSummary로 변환
        List<InterviewerAvailabilitySummaryResponse.TimeBlockSummary> timeBlocks = blocks.stream()
                .map(blockStartTime -> {
                    int count = blockCounts.getOrDefault(blockStartTime, 0L).intValue();
                    return InterviewerAvailabilitySummaryResponse.TimeBlockSummary.of(
                            blockStartTime,
                            blockStartTime.plusMinutes(30), // 블록은 30분 고정
                            count);
                })
                .filter(timeBlock -> timeBlock.getAvailableInterviewerCount() > 0) // count가 0보다 큰 경우만
                                                                                   // 필터링
                .toList();

        return InterviewerAvailabilitySummaryResponse.DaySummary.of(date, timeBlocks);
    }
}
