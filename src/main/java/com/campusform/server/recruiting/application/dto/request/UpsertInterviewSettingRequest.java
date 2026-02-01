package com.campusform.server.recruiting.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Step1: 면접 정보 설정 저장/수정 요청 DTO
 *
 * - validation은 서비스 레이어에서 "정책"으로 검증합니다.
 * (컨트롤러는 입력을 받고, 정책은 한 곳에서 통제하는 게 유지보수에 유리합니다.)
 */
@Schema(description = "면접 정보 설정 저장/수정 요청")
@Getter
@NoArgsConstructor // Jackson 역직렬화를 위해 기본 생성자 필요
@AllArgsConstructor
public class UpsertInterviewSettingRequest {

    @Schema(description = "면접 시작 날짜", example = "2024-08-01")
    private LocalDate startDate;
    @Schema(description = "면접 종료 날짜", example = "2024-08-05")
    private LocalDate endDate;
    @Schema(description = "하루 면접 시작 시간", example = "10:00")
    private LocalTime startTime;
    @Schema(description = "하루 면접 종료 시간", example = "18:00")
    private LocalTime endTime;

    @Schema(description = "한 슬롯(시간)당 최대 배정 가능 지원자 수", example = "3")
    private Integer maxApplicantsPerSlot;
    @Schema(description = "한 슬롯(시간)당 배정될 최소 면접관 수", example = "2")
    private Integer minInterviewersPerSlot;
    @Schema(description = "한 슬롯(시간)당 배정될 최대 면접관 수", example = "3")
    private Integer maxInterviewersPerSlot;

    @Schema(description = "개별 면접 시간(분)", example = "20")
    private Integer slotDurationMin;
    @Schema(description = "면접과 면접 사이의 쉬는 시간(분)", example = "5")
    private Integer slotBreakMin;
}
