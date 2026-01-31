package com.campusform.server.recruiting.domain.model.interview.schedule.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원자 배정 실패 사유 Enum
 */
@Getter
@RequiredArgsConstructor
public enum UnassignmentReason {

    NO_AVAILABLE_SLOTS("배정 가능한 슬롯이 없습니다."),

    ALL_SLOTS_FULL("모든 슬롯의 정원이 가득찼습니다."),

    REQUIRED_INTERVIEWER_UNAVAILABLE("필수 면접관이 해당 시간에 불가능합니다."),

    INSUFFICIENT_INTERVIEWERS("배정할 면접관이 부족합니다."),

    NO_SUBMISSION("면접 가능 시간을 제출하지 않았습니다.");

    private final String message;
}
