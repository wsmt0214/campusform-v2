package com.campusform.server.recruiting.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 면접 가능 슬롯 제출 요청 DTO
 */
@Getter
@NoArgsConstructor // Jackson 역직렬화를 위해 기본 생성자 필요
@AllArgsConstructor
public class SubmitSlotsRequest {

    // 지원자 이름
    private String name;

    // 지원자 전화번호
    private String phone;

    // 날짜별 슬롯 선택 목록
    private List<DaySlotSelection> selections;

    // 날짜별 슬롯 선택 정보
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySlotSelection {

        // 면접 일자 (날짜)
        private LocalDate date;

        // 슬롯 시작 시간 목록
        private List<LocalTime> startTimes;
    }
}
