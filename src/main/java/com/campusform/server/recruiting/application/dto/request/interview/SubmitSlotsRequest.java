package com.campusform.server.recruiting.application.dto.request.interview;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원자 면접 가능 슬롯 제출 요청 DTO
 */
@Schema(description = "지원자 면접 가능 시간 제출 요청 (공개 API)")
@Getter
@NoArgsConstructor // Jackson 역직렬화를 위해 기본 생성자 필요
@AllArgsConstructor
public class SubmitSlotsRequest {

    @Schema(description = "지원자 이름", example = "홍길동")
    private String name;

    @Schema(description = "지원자 전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "날짜별로 선택한 시간 목록")
    private List<DaySlotSelection> selections;

    @Schema(description = "날짜별 선택 시간 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySlotSelection {

        @Schema(description = "선택한 날짜", example = "2024-07-01")
        private LocalDate date;

        @Schema(description = "해당 날짜에 선택한 면접 시작 시간 목록", example = "[\"10:00\", \"14:30\"]")
        private List<LocalTime> startTimes;
    }
}
