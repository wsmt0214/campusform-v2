package com.campusform.server.recruiting.application.dto.response;

/**
 * 최종 면접시간의 출처
 * 
 * - MANUAL: 관리자가 수동으로 지정한 값(최우선)
 * - AUTO: 스마트 스케줄 알고리즘이 배정한 값
 * - NONE: 배정 정보가 없음
 */
public enum InterviewTimeSource {
    MANUAL,
    AUTO,
    NONE
}
