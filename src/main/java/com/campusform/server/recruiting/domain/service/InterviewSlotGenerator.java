package com.campusform.server.recruiting.domain.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;
import com.campusform.server.recruiting.domain.model.interview.setup.value.ContinuousTimeRange;

/**
 * 면접 슬롯 생성 도메인 서비스
 */
public class InterviewSlotGenerator {

    /**
     * 슬롯 정보 DTO (도메인 계층용)
     */
    public record SlotInfo(LocalTime startTime, LocalTime endTime, int availableInterviewerCount) {
    }

    /**
     * 면접 슬롯 생성
     * 
     * 연속된 블록들을 하나의 시간 범위로 묶어서 처리
     * 예: 10:00, 10:30 → 10:00 ~ 11:00 (연속 범위)
     */
    public List<SlotInfo> generateSlots(InterviewSetting setting, List<InterviewerAvailabilityBlock> blocks) {
        if (blocks.isEmpty()) {
            return List.of();
        }

        // 연속된 블록 범위 찾기
        List<ContinuousTimeRange> continuousRanges = findContinuousRanges(blocks);

        // 각 연속 범위 내에서 슬롯 생성
        return generateSlotsInRanges(continuousRanges, setting, blocks);
    }

    /**
     * 면접관이 선택한 블록들로부터 연속된 시간 범위 찾기
     */
    private List<ContinuousTimeRange> findContinuousRanges(List<InterviewerAvailabilityBlock> blocks) {
        // 블록 시작 시간별로 그룹화 (슬롯 생성에 필요한 블록 목록 추출용)
        Set<LocalTime> availableBlockTimes = blocks.stream()
                .map(InterviewerAvailabilityBlock::getStartTime)
                .collect(Collectors.toSet());

        // 면접관이 실제로 선택한 블록 목록 (시간 순으로 정렬)
        List<LocalTime> sortedBlocks = availableBlockTimes.stream()
                .sorted()
                .toList();

        // 연속된 블록들을 시간 범위로 묶기
        return ContinuousTimeRange.groupFromBlocks(sortedBlocks);
    }

    /**
     * 연속 범위들 내에서 슬롯 생성
     */
    private List<SlotInfo> generateSlotsInRanges(
            List<ContinuousTimeRange> continuousRanges,
            InterviewSetting setting,
            List<InterviewerAvailabilityBlock> blocks) {
        int slotDurationMin = setting.getSlotDurationMin();
        int slotBreakMin = setting.getSlotBreakMin();
        LocalTime interviewStartTime = setting.getStartTime();
        LocalTime interviewEndTime = setting.getEndTime();

        if (slotDurationMin <= 0) {
            throw new IllegalArgumentException("slotDurationMin은 0보다 커야 합니다.");
        }
        if (slotBreakMin < 0) {
            throw new IllegalArgumentException("slotBreakMin은 0 이상이어야 합니다.");
        }

        List<SlotInfo> slots = new ArrayList<>();

        // 각 연속 범위 내에서 슬롯 생성
        for (ContinuousTimeRange range : continuousRanges) {
            // 연속 범위와 면접 시간 범위의 교집합 계산
            ContinuousTimeRange intersection = range.intersect(interviewStartTime, interviewEndTime);

            // 교집합이 없으면 다음 범위로
            if (intersection == null) {
                continue;
            }

            // 교집합 범위 내에서 슬롯 생성
            List<SlotInfo> rangeSlots = generateSlotsInRange(
                    intersection, range, slotDurationMin, slotBreakMin, blocks);
            slots.addAll(rangeSlots);
        }

        return slots;
    }

    /**
     * 단일 연속 범위 내에서 슬롯 생성
     */
    private List<SlotInfo> generateSlotsInRange(
            ContinuousTimeRange intersection,
            ContinuousTimeRange originalRange,
            int slotDurationMin,
            int slotBreakMin,
            List<InterviewerAvailabilityBlock> blocks) {
        List<SlotInfo> slots = new ArrayList<>();

        LocalTime currentSlotStart = intersection.getStart();
        while (currentSlotStart.isBefore(intersection.getEnd())) {
            LocalTime slotEnd = currentSlotStart.plusMinutes(slotDurationMin);

            // 슬롯이 교집합 범위 내에 완전히 포함되는지 확인
            if (slotEnd.isAfter(intersection.getEnd())) {
                break;
            }

            // 슬롯이 연속 범위 내에 완전히 포함되는지 확인
            if (!originalRange.containsSlot(currentSlotStart, slotEnd)) {
                break;
            }

            // 슬롯이 완전히 포함되는 블록들의 고유 면접관 수 계산
            int uniqueInterviewerCount = calculateUniqueInterviewerCount(
                    currentSlotStart, slotDurationMin, blocks);

            // 면접관이 있는 슬롯만 추가 (availableInterviewerCount > 0)
            if (uniqueInterviewerCount > 0) {
                slots.add(new SlotInfo(currentSlotStart, slotEnd, uniqueInterviewerCount));
            }

            // 다음 슬롯 시작 시간 계산 (현재 슬롯 종료 시간 + BreakMin)
            currentSlotStart = slotEnd.plusMinutes(slotBreakMin);
        }

        return slots;
    }

    /**
     * 슬롯이 완전히 포함되는 블록들의 고유 면접관 수 계산
     * 
     * 슬롯이 면접관의 블록에 완전히 포함되어야만 카운트됩니다.
     * 예: 블록이 14:00~14:30이고 슬롯이 14:20~14:40이면 포함되지 않음 (14:30 이후 부분이 포함됨)
     * 
     * 같은 면접관이 여러 블록을 선택했을 때 중복 카운트되지 않도록 Set을 사용
     */
    private int calculateUniqueInterviewerCount(
            LocalTime slotStart, int slotDurationMin, List<InterviewerAvailabilityBlock> allBlocks) {
        LocalTime slotEnd = slotStart.plusMinutes(slotDurationMin);

        // 슬롯이 완전히 포함되는 블록들을 선택한 면접관들의 고유 집합
        Set<Long> uniqueAdminIds = allBlocks.stream()
                .filter(block -> {
                    LocalTime blockStart = block.getStartTime();
                    LocalTime blockEnd = blockStart.plusMinutes(30); // 블록은 30분 단위

                    // 슬롯이 블록에 완전히 포함되는지 확인
                    // 슬롯의 시작 시간 >= 블록의 시작 시간 && 슬롯의 종료 시간 <= 블록의 종료 시간
                    return !slotStart.isBefore(blockStart) && !slotEnd.isAfter(blockEnd);
                })
                .map(InterviewerAvailabilityBlock::getAdminId)
                .collect(Collectors.toSet());

        return uniqueAdminIds.size();
    }
}