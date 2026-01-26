package com.campusform.server.recruiting.domain.model.interview.setup.value;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * 연속된 시간 범위를 나타내는 Value Object
 * 
 * 면접관이 선택한 30분 블록들 중 연속된 블록들을 하나의 범위로 묶어서 표현합니다.
 * 예: 10:00, 10:30 블록 → 10:00 ~ 11:00 연속 범위
 */
@Getter
public class ContinuousTimeRange {

    private final LocalTime start;
    private final LocalTime end;

    private ContinuousTimeRange(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public static ContinuousTimeRange of(LocalTime start, LocalTime end) {
        return new ContinuousTimeRange(start, end);
    }

    /**
     * 블록 목록을 연속된 시간 범위들로 그룹화
     * 
     * @param sortedBlocks 시간순 정렬된 블록 시작 시간 목록
     */
    public static List<ContinuousTimeRange> groupFromBlocks(List<LocalTime> sortedBlocks) {
        List<ContinuousTimeRange> ranges = new ArrayList<>();

        if (sortedBlocks.isEmpty()) {
            return ranges;
        }

        LocalTime rangeStart = sortedBlocks.get(0);
        LocalTime rangeEnd = rangeStart.plusMinutes(30);

        for (int i = 1; i < sortedBlocks.size(); i++) {
            LocalTime currentBlock = sortedBlocks.get(i);
            LocalTime expectedNextBlock = rangeEnd;

            // 현재 블록이 연속된 블록인지 확인
            if (currentBlock.equals(expectedNextBlock)) {
                // 연속된 블록이면 범위 확장
                rangeEnd = currentBlock.plusMinutes(30);
            } else {
                // 연속되지 않으면 현재 범위 저장하고 새 범위 시작
                ranges.add(new ContinuousTimeRange(rangeStart, rangeEnd));
                rangeStart = currentBlock;
                rangeEnd = currentBlock.plusMinutes(30);
            }
        }

        // 마지막 범위 추가
        ranges.add(new ContinuousTimeRange(rangeStart, rangeEnd));

        return ranges;
    }

    /**
     * 주어진 시간 범위와의 교집합 계산
     * 
     * @param otherStart 다른 범위의 시작 시간
     * @param otherEnd   다른 범위의 종료 시간
     * @return 교집합 범위 (없으면 null)
     */
    public ContinuousTimeRange intersect(LocalTime otherStart, LocalTime otherEnd) {
        LocalTime intersectionStart = start.isAfter(otherStart) ? start : otherStart;
        LocalTime intersectionEnd = end.isBefore(otherEnd) ? end : otherEnd;

        // 교집합이 존재하지 않음
        if (!intersectionStart.isBefore(intersectionEnd)) {
            return null;
        }

        return new ContinuousTimeRange(intersectionStart, intersectionEnd);
    }

    /**
     * 슬롯이 이 연속 범위 내에 완전히 포함되는지 확인
     */
    public boolean containsSlot(LocalTime slotStart, LocalTime slotEnd) {
        return !slotStart.isBefore(start) && !slotEnd.isAfter(end);
    }
}
