package com.campusform.server.recruiting.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Step1: 면접 정보 설정 저장/수정 요청 DTO
 *
 * - validation은 서비스 레이어에서 "정책"으로 검증합니다.
 * (컨트롤러는 입력을 받고, 정책은 한 곳에서 통제하는 게 유지보수에 유리합니다.)
 */
@Getter
@NoArgsConstructor // Jackson 역직렬화를 위해 기본 생성자 필요
@AllArgsConstructor
public class UpsertInterviewSettingRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private Integer maxApplicantsPerSlot;
    private Integer minInterviewersPerSlot;
    private Integer maxInterviewersPerSlot;

    private Integer slotDurationMin;
    private Integer slotBreakMin;
}
