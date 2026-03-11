package com.campusform.server.recruiting.domain.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.ApplicantInfo;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.DaySummary;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.InterviewerInfo;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.SlotInfo;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.Statistics;
import com.campusform.server.recruiting.application.dto.response.interview.SmartScheduleResponse.UnassignedApplicantInfo;
import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.schedule.value.UnassignmentReason;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.model.interview.setup.value.ContinuousTimeRange;

/**
 * 스마트 시간표 생성 도메인 서비스
 * 최우선적 목표: 최대한 많은 지원자를 슬롯에 배정
 */
public class SmartScheduleGenerator {

    // 도메인 서비스
    private final InterviewSlotGenerator slotGenerator = new InterviewSlotGenerator();

    /**
     * 스마트 시간표 생성
     * 
     * @param setting            면접 설정 (시간 정책, 슬롯 제한 등)
     * @param days               면접 일자 목록
     * @param applicantSlots     지원자들이 제출한 가능 슬롯 목록
     * @param interviewerBlocks  면접관들의 가능 시간 블록 목록
     * @param applicantInfoMap   지원자 ID → 상세 정보 조회용 Map
     * @param interviewerInfoMap 면접관 ID → 상세 정보 조회용 Map
     * @return 배정 결과 (배정된 슬롯, 미배정 지원자, 통계)
     */
    public SmartScheduleResponse generate(
            InterviewSetting setting,
            List<InterviewDay> days,
            List<IntervieweeAvailabilitySlot> applicantSlots,
            List<InterviewerAvailabilityBlock> interviewerBlocks,
            Map<Long, ApplicantInfo> applicantInfoMap,
            Map<Long, InterviewerInfo> interviewerInfoMap) {

        // 입력 데이터가 없으면 빈 결과 반환
        if (applicantSlots.isEmpty()) {
            return SmartScheduleResponse.empty();
        }

        // 필수 면접관 ID 목록
        Set<Long> requiredInterviewerIds = new HashSet<>(setting.getRequiredInterviewerIds());
        boolean hasRequiredInterviewers = !requiredInterviewerIds.isEmpty();

        // InterviewDay ID → 해당 날짜의 면접관 블록 목록
        Map<Long, List<InterviewerAvailabilityBlock>> blocksByDayId = interviewerBlocks.stream()
                .collect(Collectors.groupingBy(InterviewerAvailabilityBlock::getInterviewDayId));

        // ========== Phase 1: 유효 슬롯 후보 생성 ==========
        List<SlotCandidate> allCandidates = new ArrayList<>();
        for (InterviewDay day : days) {
            Long dayId = day.getId();
            List<InterviewerAvailabilityBlock> dayBlocks = blocksByDayId.getOrDefault(dayId, List.of());

            if (dayBlocks.isEmpty()) {
                continue;
            }

            // InterviewSlotGenerator를 사용하여 슬롯 생성
            List<InterviewSlotGenerator.SlotInfo> generatedSlots = slotGenerator.generateSlots(setting, dayBlocks);

            for (InterviewSlotGenerator.SlotInfo slotInfo : generatedSlots) {
                // 해당 슬롯에 배정 가능한 면접관 ID 목록 계산
                Set<Long> availableInterviewerIds = calculateAvailableInterviewers(
                        slotInfo.startTime(), setting.getSlotDurationMin(), dayBlocks);

                // Phase 2: 필수 면접관 필터링
                // 필수 면접관이 있는 경우, 해당 슬롯에 필수 면접관이 1명이라도 가능해야 유효
                if (hasRequiredInterviewers) {
                    Set<Long> availableRequiredInterviewers = availableInterviewerIds.stream()
                            .filter(requiredInterviewerIds::contains)
                            .collect(Collectors.toSet());

                    if (availableRequiredInterviewers.isEmpty()) {
                        // 필수 면접관이 없으면 이 슬롯은 사용 불가
                        continue;
                    }
                }

                // minInterviewersPerSlot 검증
                if (availableInterviewerIds.size() < setting.getMinInterviewersPerSlot()) {
                    continue;
                }

                allCandidates.add(new SlotCandidate(
                        dayId,
                        day.getInterviewDate(),
                        slotInfo.startTime(),
                        slotInfo.endTime(),
                        availableInterviewerIds));
            }
        }

        // ========== Phase 3: 지원자별 가능 슬롯 매핑 ==========
        // 지원자 ID → 제출한 슬롯 목록
        Map<Long, List<IntervieweeAvailabilitySlot>> slotsByApplicant = applicantSlots.stream()
                .collect(Collectors.groupingBy(IntervieweeAvailabilitySlot::getApplicantId));

        // 지원자 ID → 유효한 슬롯 후보 목록
        Map<Long, List<SlotCandidate>> validCandidatesByApplicant = new HashMap<>();
        List<UnassignedApplicantInfo> unassignedApplicants = new ArrayList<>();

        for (Map.Entry<Long, List<IntervieweeAvailabilitySlot>> entry : slotsByApplicant.entrySet()) {
            Long applicantId = entry.getKey();
            List<IntervieweeAvailabilitySlot> submittedSlots = entry.getValue();

            // 지원자가 제출한 슬롯 중 유효한 슬롯 후보 찾기
            List<SlotCandidate> validCandidates = findValidCandidatesForApplicant(
                    submittedSlots, allCandidates);

            if (validCandidates.isEmpty()) {
                // 가능한 슬롯이 없는 경우
                UnassignmentReason reason = determineUnassignmentReason(
                        submittedSlots, allCandidates, hasRequiredInterviewers, requiredInterviewerIds);
                ApplicantInfo applicantInfo = applicantInfoMap.get(applicantId);
                unassignedApplicants.add(UnassignedApplicantInfo.of(applicantInfo, reason));
            } else {
                validCandidatesByApplicant.put(applicantId, validCandidates);
            }
        }

        // ========== Phase 4: Greedy 배정 ==========
        // 가장 제약이 많은 지원자(가능 슬롯이 적은)부터 배정
        List<Long> sortedApplicantIds = validCandidatesByApplicant.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 슬롯별 배정된 지원자 수 추적
        Map<String, Integer> slotApplicantCount = new HashMap<>();
        // 슬롯별 배정된 지원자 ID 목록
        Map<String, List<Long>> slotAssignedApplicants = new HashMap<>();
        // 면접관별 배정 횟수 (부하 분산용)
        Map<Long, Integer> interviewerAssignmentCount = new HashMap<>();
        // 지원자 → 배정된 슬롯
        Map<Long, SlotCandidate> applicantAssignment = new HashMap<>();

        int maxApplicantsPerSlot = setting.getMaxApplicantsPerSlot();

        for (Long applicantId : sortedApplicantIds) {
            List<SlotCandidate> candidates = validCandidatesByApplicant.get(applicantId);

            // 정원이 남은 슬롯 중에서 선택
            SlotCandidate selectedSlot = selectBestSlot(
                    candidates, slotApplicantCount, maxApplicantsPerSlot, interviewerAssignmentCount);

            if (selectedSlot == null) {
                // 모든 가능 슬롯이 정원 초과
                ApplicantInfo applicantInfo = applicantInfoMap.get(applicantId);
                unassignedApplicants.add(UnassignedApplicantInfo.of(applicantInfo, UnassignmentReason.ALL_SLOTS_FULL));
                continue;
            }

            // 배정 처리
            String slotKey = selectedSlot.getSlotKey();
            slotApplicantCount.merge(slotKey, 1, Integer::sum);
            slotAssignedApplicants.computeIfAbsent(slotKey, k -> new ArrayList<>()).add(applicantId);
            applicantAssignment.put(applicantId, selectedSlot);
        }

        // ========== Phase 5: 면접관 배정 및 날짜별 그룹핑 ==========
        List<DaySummary> daySummaries = buildDaySummaries(
                slotAssignedApplicants,
                allCandidates,
                setting,
                requiredInterviewerIds,
                interviewerAssignmentCount,
                applicantInfoMap,
                interviewerInfoMap);

        // ========== 통계 생성 ==========
        int totalApplicants = slotsByApplicant.size();
        int assignedCount = applicantAssignment.size();
        int unassignedCount = unassignedApplicants.size();
        int usedSlots = (int) daySummaries.stream()
                .flatMap(day -> day.getSlots().stream())
                .filter(slot -> !slot.getApplicants().isEmpty())
                .count();

        Statistics statistics = Statistics.of(totalApplicants, assignedCount, unassignedCount, usedSlots);

        return SmartScheduleResponse.of(daySummaries, unassignedApplicants, statistics);
    }

    /**
     * 슬롯에 배정 가능한 면접관 ID 목록 계산
     * 면접관별 연속 가용 범위를 계산하여 슬롯이 완전히 포함되는 면접관만 반환
     */
    private Set<Long> calculateAvailableInterviewers(
            LocalTime slotStart, int slotDurationMin, List<InterviewerAvailabilityBlock> blocks) {
        LocalTime slotEnd = slotStart.plusMinutes(slotDurationMin);

        // 면접관별로 블록 그룹화
        Map<Long, List<InterviewerAvailabilityBlock>> blocksByAdmin = blocks.stream()
                .collect(Collectors.groupingBy(InterviewerAvailabilityBlock::getAdminId));

        Set<Long> availableInterviewers = new HashSet<>();

        for (Map.Entry<Long, List<InterviewerAvailabilityBlock>> entry : blocksByAdmin.entrySet()) {
            Long adminId = entry.getKey();
            List<InterviewerAvailabilityBlock> adminBlocks = entry.getValue();

            // 해당 면접관의 블록들을 시간순 정렬
            List<LocalTime> sortedBlockTimes = adminBlocks.stream()
                    .map(InterviewerAvailabilityBlock::getStartTime)
                    .sorted()
                    .toList();

            // 연속 범위 계산
            List<ContinuousTimeRange> continuousRanges = ContinuousTimeRange.groupFromBlocks(sortedBlockTimes);

            // 슬롯이 어떤 연속 범위에라도 완전히 포함되면 배정 가능
            boolean slotCovered = continuousRanges.stream()
                    .anyMatch(range -> range.containsSlot(slotStart, slotEnd));

            if (slotCovered) {
                availableInterviewers.add(adminId);
            }
        }

        return availableInterviewers;
    }

    /**
     * 지원자가 제출한 슬롯 중 유효한 슬롯 후보 찾기
     */
    private List<SlotCandidate> findValidCandidatesForApplicant(
            List<IntervieweeAvailabilitySlot> submittedSlots,
            List<SlotCandidate> allCandidates) {

        // 지원자가 제출한 (dayId, startTime) 쌍
        Set<String> submittedKeys = submittedSlots.stream()
                .map(slot -> slot.getInterviewDayId() + "_" + slot.getStartTime())
                .collect(Collectors.toSet());

        return allCandidates.stream()
                .filter(candidate -> submittedKeys.contains(candidate.getSlotKey()))
                .collect(Collectors.toList());
    }

    /**
     * 미배정 사유 결정
     */
    private UnassignmentReason determineUnassignmentReason(
            List<IntervieweeAvailabilitySlot> submittedSlots,
            List<SlotCandidate> allCandidates,
            boolean hasRequiredInterviewers,
            Set<Long> requiredInterviewerIds) {

        if (submittedSlots.isEmpty()) {
            return UnassignmentReason.NO_SUBMISSION;
        }

        // 제출한 슬롯 키 목록
        Set<String> submittedKeys = submittedSlots.stream()
                .map(slot -> slot.getInterviewDayId() + "_" + slot.getStartTime())
                .collect(Collectors.toSet());

        // 제출한 슬롯 중 면접관이 있는 슬롯이 있는지 확인
        boolean hasAnyInterviewerSlot = allCandidates.stream()
                .anyMatch(candidate -> submittedKeys.contains(candidate.getSlotKey()));

        if (!hasAnyInterviewerSlot) {
            return UnassignmentReason.NO_AVAILABLE_SLOTS;
        }

        // 필수 면접관이 있는 경우, 필수 면접관 가능 여부 확인
        if (hasRequiredInterviewers) {
            return UnassignmentReason.REQUIRED_INTERVIEWER_UNAVAILABLE;
        }

        return UnassignmentReason.INSUFFICIENT_INTERVIEWERS;
    }

    /**
     * 최적의 슬롯 선택 (Greedy + LCV 휴리스틱)
     * 
     * 최대 지원자 배정을 위해 LCV(Least Constraining Value) 휴리스틱 적용:
     * 1순위: 잔여 정원이 많은 슬롯 우선 (다른 지원자들을 위한 자리 확보)
     * 2순위: 면접관 부하 분산 (동일 조건일 때)
     * 
     * @param candidates                 지원자가 선택 가능한 슬롯 후보
     * @param slotApplicantCount         슬롯별 현재 배정된 지원자 수
     * @param maxApplicantsPerSlot       슬롯당 최대 지원자 수
     * @param interviewerAssignmentCount 면접관별 배정 횟수
     * @return 선택된 슬롯 (없으면 null)
     */
    private SlotCandidate selectBestSlot(
            List<SlotCandidate> candidates,
            Map<String, Integer> slotApplicantCount,
            int maxApplicantsPerSlot,
            Map<Long, Integer> interviewerAssignmentCount) {

        // 정원이 남은 슬롯만 필터링
        List<SlotCandidate> availableSlots = candidates.stream()
                .filter(candidate -> {
                    int currentCount = slotApplicantCount.getOrDefault(candidate.getSlotKey(), 0);
                    return currentCount < maxApplicantsPerSlot;
                })
                .collect(Collectors.toList());

        if (availableSlots.isEmpty()) {
            return null;
        }

        // LCV 휴리스틱: 최대 지원자 배정을 위한 슬롯 선택
        // 1순위: 잔여 정원이 많은 슬롯 (다른 지원자들에게 기회 남김)
        // 2순위: 면접관 부하 분산
        return availableSlots.stream()
                .max(Comparator
                        // 1순위: 잔여 정원이 많은 슬롯 우선 (내림차순)
                        .comparingInt((SlotCandidate candidate) -> {
                            int currentCount = slotApplicantCount.getOrDefault(candidate.getSlotKey(), 0);
                            return maxApplicantsPerSlot - currentCount; // 잔여 정원
                        })
                        // 2순위: 면접관 부하 분산 (배정 횟수가 적은 면접관이 있는 슬롯)
                        .thenComparingInt(candidate -> {
                            // 음수로 반환하여 오름차순 효과 (낮은 배정 횟수 우선)
                            return -candidate.getAvailableInterviewerIds().stream()
                                    .mapToInt(id -> interviewerAssignmentCount.getOrDefault(id, 0))
                                    .min()
                                    .orElse(Integer.MAX_VALUE);
                        }))
                .orElse(availableSlots.get(0));
    }

    /**
     * 날짜별 슬롯 요약 생성 (면접관 배정 포함)
     */
    private List<DaySummary> buildDaySummaries(
            Map<String, List<Long>> slotAssignedApplicants,
            List<SlotCandidate> allCandidates,
            InterviewSetting setting,
            Set<Long> requiredInterviewerIds,
            Map<Long, Integer> interviewerAssignmentCount,
            Map<Long, ApplicantInfo> applicantInfoMap,
            Map<Long, InterviewerInfo> interviewerInfoMap) {

        // slotKey → SlotCandidate 매핑
        Map<String, SlotCandidate> candidateMap = allCandidates.stream()
                .collect(Collectors.toMap(SlotCandidate::getSlotKey, c -> c, (a, b) -> a));

        int minInterviewers = setting.getMinInterviewersPerSlot();
        int maxInterviewers = setting.getMaxInterviewersPerSlot();
        boolean hasRequiredInterviewers = !requiredInterviewerIds.isEmpty();

        // 날짜별 슬롯 그룹핑을 위한 Map
        Map<LocalDate, List<SlotInfo>> slotsByDate = new HashMap<>();

        for (Map.Entry<String, List<Long>> entry : slotAssignedApplicants.entrySet()) {
            String slotKey = entry.getKey();
            List<Long> applicantIds = entry.getValue();

            SlotCandidate candidate = candidateMap.get(slotKey);
            if (candidate == null || applicantIds.isEmpty()) {
                continue;
            }

            // 면접관 선택
            List<Long> selectedInterviewerIds = selectInterviewers(
                    candidate.getAvailableInterviewerIds(),
                    requiredInterviewerIds,
                    hasRequiredInterviewers,
                    minInterviewers,
                    maxInterviewers,
                    interviewerAssignmentCount);

            // 면접관 배정 횟수 업데이트
            for (Long interviewerId : selectedInterviewerIds) {
                interviewerAssignmentCount.merge(interviewerId, 1, Integer::sum);
            }

            // 지원자 ID → ApplicantInfo 변환
            List<ApplicantInfo> applicants = applicantIds.stream()
                    .map(applicantInfoMap::get)
                    .filter(info -> info != null)
                    .toList();

            // 면접관 ID → InterviewerInfo 변환 (필수 면접관 여부 포함)
            List<InterviewerInfo> interviewers = selectedInterviewerIds.stream()
                    .map(id -> {
                        InterviewerInfo baseInfo = interviewerInfoMap.get(id);
                        if (baseInfo == null) {
                            return null;
                        }
                        // 필수 면접관 여부 설정
                        boolean isRequired = requiredInterviewerIds.contains(id);
                        return InterviewerInfo.of(baseInfo.getId(), baseInfo.getName(), isRequired);
                    })
                    .filter(info -> info != null)
                    .toList();

            SlotInfo slot = SlotInfo.of(
                    candidate.getStartTime(),
                    candidate.getEndTime(),
                    applicants,
                    interviewers);

            slotsByDate.computeIfAbsent(candidate.getDate(), k -> new ArrayList<>()).add(slot);
        }

        // 날짜별로 슬롯을 시간순 정렬하고 DaySummary 생성
        return slotsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<SlotInfo> sortedSlots = entry.getValue().stream()
                            .sorted(Comparator.comparing(SlotInfo::getStartTime))
                            .toList();
                    return DaySummary.of(entry.getKey(), sortedSlots);
                })
                .toList();
    }

    /**
     * 슬롯에 배정할 면접관 선택
     * - 필수 면접관이 있으면 우선 배정
     * - 배정 횟수가 적은 면접관 우선
     */
    private List<Long> selectInterviewers(
            Set<Long> availableInterviewerIds,
            Set<Long> requiredInterviewerIds,
            boolean hasRequiredInterviewers,
            int minInterviewers,
            int maxInterviewers,
            Map<Long, Integer> interviewerAssignmentCount) {

        List<Long> selected = new ArrayList<>();

        // 필수 면접관 우선 배정
        if (hasRequiredInterviewers) {
            List<Long> availableRequired = availableInterviewerIds.stream()
                    .filter(requiredInterviewerIds::contains)
                    .sorted(Comparator.comparingInt(id -> interviewerAssignmentCount.getOrDefault(id, 0)))
                    .toList();

            // 최소 1명의 필수 면접관 배정
            if (!availableRequired.isEmpty()) {
                selected.add(availableRequired.get(0));
            }
        }

        // 나머지 면접관 배정 (부하 분산)
        List<Long> remainingInterviewers = availableInterviewerIds.stream()
                .filter(id -> !selected.contains(id))
                .sorted(Comparator.comparingInt(id -> interviewerAssignmentCount.getOrDefault(id, 0)))
                .toList();

        for (Long interviewerId : remainingInterviewers) {
            if (selected.size() >= maxInterviewers) {
                break;
            }
            selected.add(interviewerId);
        }

        // minInterviewers 미달 시 경고 (실제로는 유효 슬롯 필터링에서 걸러짐)
        return selected;
    }

    /**
     * 슬롯 후보 내부 클래스
     */
    private static class SlotCandidate {
        private final Long dayId;
        private final LocalDate date;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final Set<Long> availableInterviewerIds;

        SlotCandidate(Long dayId, LocalDate date, LocalTime startTime, LocalTime endTime,
                Set<Long> availableInterviewerIds) {
            this.dayId = dayId;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.availableInterviewerIds = availableInterviewerIds;
        }

        LocalDate getDate() {
            return date;
        }

        LocalTime getStartTime() {
            return startTime;
        }

        LocalTime getEndTime() {
            return endTime;
        }

        Set<Long> getAvailableInterviewerIds() {
            return availableInterviewerIds;
        }

        /**
         * 슬롯 고유 키 (dayId_startTime)
         */
        String getSlotKey() {
            return dayId + "_" + startTime;
        }
    }
}
