package com.campusform.server.recruiting.domain.model.applicant.value;

/**
 * 서류/면접 단계별 심사 결과.
 * 지원자의 "상태"가 아니라 해당 단계에서의 평가 결과(PASS/FAIL/보류)를 나타냅니다.
 */
public enum ScreeningResult {
    /** 보류(미심사) */
    HOLD,

    /** 합격 */
    PASS,

    /** 불합격 */
    FAIL
}
