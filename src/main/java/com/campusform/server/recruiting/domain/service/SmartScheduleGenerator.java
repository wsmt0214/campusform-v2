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
import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan.AssignedApplicant;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan.AssignedInterviewer;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan.DayResult;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan.PlanStatistics;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan.SlotResult;
import com.campusform.server.recruiting.domain.model.interview.schedule.SchedulePlan.UnassignedApplicant;
import com.campusform.server.recruiting.domain.model.interview.schedule.value.UnassignmentReason;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.model.interview.setup.value.ContinuousTimeRange;

/**
 * 스마트 시간표 생성 도메인 서비스
 * 최우선 목표: 최대한 많은 지원자를 슬롯에 배정
 */
public class SmartScheduleGenerator {

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
    public SchedulePlan generate(
            InterviewSetting setting,
            List<InterviewDay> days,
            List<IntervieweeAvailabilitySlot> applicantSlots,
            List<InterviewerAvailabilityBlock> interviewerBlocks,
            Map<Long, AssignedApplicant> applicantInfoMap,
            Map<Long, AssignedInterviewer> interviewerInfoMap) {

        if (applicantSlots.isEmpty()) {
            return SchedulePlan.empty();
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
                Set<Long> availableInterviewerIds = calculateAvailableInterviewers(
                        slotInfo.startTime(), setting.getSlotDurationMin(), dayBlocks);

                // Phase 2: 필수 면접관 필터링
                // 필수 면접관이 있는 경우, 해당 슬롯에 필수 면접관이 1명이라도 가능해야 유효
                if (hasRequiredInterviewers) {
                    Set<Long> availableRequired = availableInterviewerIds.stream()
                            .filter(requiredInterviewerIds::contains)
                            .collect(Collectors.toSet());
                    if (availableRequired.isEmpty()) { // 필수 면접관이 없으면 이 슬롯은 사용 불가
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
        List<UnassignedApplicant> unassignedApplicants = new ArrayList<>();

        for (Map.Entry<Long, List<IntervieweeAvailabilitySlot>> entry : slotsByApplicant.entrySet()) {
            Long applicantId = entry.getKey();
            List<IntervieweeAvailabilitySlot> submittedSlots = entry.getValue();

            // 지원자가 제출한 슬롯 중 유효한 슬롯 후보 찾기
            List<SlotCandidate> validCandidates = findValidCandidatesForApplicant(submittedSlots, allCandidates);

            if (validCandidates.isEmpty()) {
                UnassignmentReason reason = determineUnassignmentReason(
                        submittedSlots, allCandidates, hasRequiredInterviewers, requiredInterviewerIds);
                AssignedApplicant info = applicantInfoMap.get(applicantId);
                unassignedApplicants.add(new UnassignedApplicant(
                        info.id(), info.name(), info.school(), info.major(), info.position(), reason));
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
                AssignedApplicant info = applicantInfoMap.get(applicantId);
                unassignedApplicants.add(new UnassignedApplicant(
                        info.id(), info.name(), info.school(), info.major(), info.position(),
                        UnassignmentReason.ALL_SLOTS_FULL));
                continue;
            }

            String slotKey = selectedSlot.getSlotKey();
            slotApplicantCount.merge(slotKey, 1, Integer::sum);
            slotAssignedApplicants.computeIfAbsent(slotKey, k -> new ArrayList<>()).add(applicantId);
            applicantAssignment.put(applicantId, selectedSlot);
        }

        // ========== Phase 5: 면접관 배정 및 날짜별 그룹핑 ==========
        List<DayResult> dayResults = buildDayResults(
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
        int usedSlots = (int) dayResults.stream()
                .flatMap(day -> day.slots().stream())
                .filter(slot -> !slot.applicants().isEmpty())
                .count();

        PlanStatistics statistics = new PlanStatistics(totalApplicants, assignedCount, unassignedCount, usedSlots);

        return new SchedulePlan(dayResults, unassignedApplicants, statistics);
    }

    /**
     * 슬롯에 배정 가능한 면접관 ID 목록 계산
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

            List<LocalTime> sortedBlockTimes = adminBlocks.stream()
                    .map(InterviewerAvailabilityBlock::getStartTime)
                    .sorted()
                    .toList();

            List<ContinuousTimeRange> continuousRanges = ContinuousTimeRange.groupFromBlocks(sortedBlockTimes);

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

        Set<String> submittedKeys = submittedSlots.stream()
                .map(slot -> slot.getInterviewDayId() + "_" + slot.getStartTime())
                .collect(Collectors.toSet());

        boolean hasAnyInterviewerSlot = allCandidates.stream()
                .anyMatch(candidate -> submittedKeys.contains(candidate.getSlotKey()));

        if (!hasAnyInterviewerSlot) {
            return UnassignmentReason.NO_AVAILABLE_SLOTS;
        }

        if (hasRequiredInterviewers) {
            return UnassignmentReason.REQUIRED_INTERVIEWER_UNAVAILABLE;
        }

        return UnassignmentReason.INSUFFICIENT_INTERVIEWERS;
    }

    /**
     * 최적의 슬롯 선택 (Greedy + LCV 휴리스틱)
     *
     * 1순위: 잔여 정원이 많은 슬롯 (다른 지원자들을 위한 자리 확보)
     * 2순위: 면접관 부하 분산 (동일 조건일 때)
     */
    private SlotCandidate selectBestSlot(
            List<SlotCandidate> candidates,
            Map<String, Integer> slotApplicantCount,
            int maxApplicantsPerSlot,
            Map<Long, Integer> interviewerAssignmentCount) {

        List<SlotCandidate> availableSlots = candidates.stream()
                .filter(candidate -> {
                    int currentCount = slotApplicantCount.getOrDefault(candidate.getSlotKey(), 0);
                    return currentCount < maxApplicantsPerSlot;
                })
                .collect(Collectors.toList());

        if (availableSlots.isEmpty()) {
            return null;
        }

        return availableSlots.stream()
                .max(Comparator
                        .comparingInt((SlotCandidate candidate) -> {
                            int currentCount = slotApplicantCount.getOrDefault(candidate.getSlotKey(), 0);
                            return maxApplicantsPerSlot - currentCount;
                        })
                        .thenComparingInt(candidate -> {
                            return -candidate.getAvailableInterviewerIds().stream()
                                    .mapToInt(id -> interviewerAssignmentCount.getOrDefault(id, 0))
                                    .min()
                                    .orElse(Integer.MAX_VALUE);
                        }))
                .orElse(availableSlots.get(0));
    }

    /**
     * 날짜별 슬롯 결과 생성 (면접관 배정 포함)
     */
    private List<DayResult> buildDayResults(
            Map<String, List<Long>> slotAssignedApplicants,
            List<SlotCandidate> allCandidates,
            InterviewSetting setting,
            Set<Long> requiredInterviewerIds,
            Map<Long, Integer> interviewerAssignmentCount,
            Map<Long, AssignedApplicant> applicantInfoMap,
            Map<Long, AssignedInterviewer> interviewerInfoMap) {

        Map<String, SlotCandidate> candidateMap = allCandidates.stream()
                .collect(Collectors.toMap(SlotCandidate::getSlotKey, c -> c, (a, b) -> a));

        int minInterviewers = setting.getMinInterviewersPerSlot();
        int maxInterviewers = setting.getMaxInterviewersPerSlot();
        boolean hasRequiredInterviewers = !requiredInterviewerIds.isEmpty();

        Map<LocalDate, List<SlotResult>> slotsByDate = new HashMap<>();

        for (Map.Entry<String, List<Long>> entry : slotAssignedApplicants.entrySet()) {
            String slotKey = entry.getKey();
            List<Long> applicantIds = entry.getValue();

            SlotCandidate candidate = candidateMap.get(slotKey);
            if (candidate == null || applicantIds.isEmpty()) {
                continue;
            }

            List<Long> selectedInterviewerIds = selectInterviewers(
                    candidate.getAvailableInterviewerIds(),
                    requiredInterviewerIds,
                    hasRequiredInterviewers,
                    minInterviewers,
                    maxInterviewers,
                    interviewerAssignmentCount);

            for (Long interviewerId : selectedInterviewerIds) {
                interviewerAssignmentCount.merge(interviewerId, 1, Integer::sum);
            }

            List<AssignedApplicant> applicants = applicantIds.stream()
                    .map(applicantInfoMap::get)
                    .filter(info -> info != null)
                    .toList();

            List<AssignedInterviewer> interviewers = selectedInterviewerIds.stream()
                    .map(id -> {
                        AssignedInterviewer base = interviewerInfoMap.get(id);
                        if (base == null) return null;
                        boolean isRequired = requiredInterviewerIds.contains(id);
                        // required 여부를 실제 값으로 재설정
                        return new AssignedInterviewer(base.id(), base.name(), isRequired);
                    })
                    .filter(info -> info != null)
                    .toList();

            SlotResult slot = new SlotResult(
                    candidate.getStartTime(),
                    candidate.getEndTime(),
                    applicants,
                    interviewers);

            slotsByDate.computeIfAbsent(candidate.getDate(), k -> new ArrayList<>()).add(slot);
        }

        return slotsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<SlotResult> sortedSlots = entry.getValue().stream()
                            .sorted(Comparator.comparing(SlotResult::startTime))
                            .toList();
                    return new DayResult(entry.getKey(), sortedSlots);
                })
                .toList();
    }

    /**
     * 슬롯에 배정할 면접관 선택
     * - 필수 면접관이 있으면 우선 배정
     * - 배정 횟수가 적은 면접관 우선 (부하 분산)
     */
    private List<Long> selectInterviewers(
            Set<Long> availableInterviewerIds,
            Set<Long> requiredInterviewerIds,
            boolean hasRequiredInterviewers,
            int minInterviewers,
            int maxInterviewers,
            Map<Long, Integer> interviewerAssignmentCount) {

        List<Long> selected = new ArrayList<>();

        if (hasRequiredInterviewers) {
            List<Long> availableRequired = availableInterviewerIds.stream()
                    .filter(requiredInterviewerIds::contains)
                    .sorted(Comparator.comparingInt(id -> interviewerAssignmentCount.getOrDefault(id, 0)))
                    .toList();

            if (!availableRequired.isEmpty()) {
                selected.add(availableRequired.get(0));
            }
        }

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

        LocalDate getDate() { return date; }
        LocalTime getStartTime() { return startTime; }
        LocalTime getEndTime() { return endTime; }
        Set<Long> getAvailableInterviewerIds() { return availableInterviewerIds; }

        /** 슬롯 고유 키 (dayId_startTime) */
        String getSlotKey() { return dayId + "_" + startTime; }
    }
}
