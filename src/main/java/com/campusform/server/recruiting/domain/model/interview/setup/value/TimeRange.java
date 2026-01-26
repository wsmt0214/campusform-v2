package com.campusform.server.recruiting.domain.model.interview.setup.value;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * 시간 범위 값 객체
 */
@Getter
public class TimeRange {

    private final LocalTime startTime;
    private final LocalTime endTime;

    private TimeRange(LocalTime startTime, LocalTime endTime) {
        validate(startTime, endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /* 시간 범위 생성 팩토리 메서드 */
    public static TimeRange of(LocalTime startTime, LocalTime endTime) {
        return new TimeRange(startTime, endTime);
    }

    /* 시간 범위 검증 */
    private void validate(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("면접 시작/종료 시간이 필요합니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("면접 시작 시간은 종료 시간보다 빨라야 합니다.");
        }
    }

    /* 특정 시간이 이 범위 내에 포함되는지 확인 */
    public boolean contains(LocalTime time) {
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    /**
     * 특정 시간 블록(30분)이 이 범위 내에 완전히 포함되는지 확인
     */
    public boolean containsBlock(LocalTime blockStartTime) {
        LocalTime blockEndTime = blockStartTime.plusMinutes(30);
        return !blockStartTime.isBefore(startTime) && !blockEndTime.isAfter(endTime);
    }

    /**
     * 특정 시간 블록(30분)이 이 범위와 겹치는지 확인
     * 블록의 일부라도 시간 범위와 겹치면 true 반환
     */
    public boolean overlapsWithBlock(LocalTime blockStartTime) {
        LocalTime blockEndTime = blockStartTime.plusMinutes(30);
        // 블록 시작 시간이 면접 종료 시간보다 이전이고, 블록 종료 시간이 면접 시작 시간보다 이후이면 겹침
        return blockStartTime.isBefore(endTime) && blockEndTime.isAfter(startTime);
    }

    /**
     * 시간 범위 내에서 30분 단위 시간 블록 목록 생성
     */
    public List<LocalTime> generateBlocks() {
        List<LocalTime> blocks = new ArrayList<>();

        // 시작 시간을 가장 가까운 블록 시작 시간으로 조정 (xx:00 또는 xx:30)
        LocalTime current = alignToBlockStart(startTime);

        // 시간 범위 내에서 30분 단위 블록 생성
        while (current.isBefore(endTime)) {
            blocks.add(current);
            current = current.plusMinutes(30);
        }

        return blocks;
    }

    /**
     * 시간을 가장 가까운 블록 시작 시간(xx:00 또는 xx:30)으로 정렬
     *
     * 예시:
     * (1) time = 09:13 → 결과: 09:00
     * (2) time = 10:44 → 결과: 10:30
     * (3) time = 12:00 → 결과: 12:00
     * (4) time = 07:31 → 결과: 07:30
     * (5) time = 21:59 → 결과: 21:30
     */
    private LocalTime alignToBlockStart(LocalTime time) {
        int minute = time.getMinute();
        if (minute == 0 || minute == 30) {
            return time;
        }
        // 30분 미만이면 xx:00으로, 30분 이상이면 xx:30으로
        if (minute < 30) {
            return time.withMinute(0).withSecond(0).withNano(0);
        } else {
            return time.withMinute(30).withSecond(0).withNano(0);
        }
    }

    /**
     * 블록 시작 시간이 유효한지 확인 (xx:00 또는 xx:30)
     */
    public static boolean isValidBlockStartTime(LocalTime time) {
        int minute = time.getMinute();
        return minute == 0 || minute == 30;
    }
}
