package com.campusform.server.recruiting.domain.model.interview.setup.value;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * 날짜 범위 값 객체
 */
@Getter
public class DateRange {

    private final LocalDate startDate;
    private final LocalDate endDate;

    private DateRange(LocalDate startDate, LocalDate endDate) {
        validate(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /* 날짜 범위 생성 팩토리 메서드 */
    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    /* 날짜 범위 검증 */
    private void validate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("면접 날짜 범위(startDate, endDate)가 필요합니다.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate는 startDate 이후(또는 동일)여야 합니다.");
        }
    }

    /* 날짜 범위 -> 날짜 리스트 */
    public List<LocalDate> expandToDates() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            dates.add(cur);
            cur = cur.plusDays(1);
        }
        return dates;
    }

    /* 특정 날짜가 이 범위 내에 포함되는지 확인 */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
