package com.campusform.server.recruiting.domain.model.applicant.value;

/**
 * 모집 단계 (서류 전형 / 면접 전형).
 * 지원자 목록·상태 조회·템플릿 저장 등에서 "어느 단계"를 대상으로 할지 구분할 때 사용합니다.
 */
public enum RecruitmentStage {
    /** 서류 전형 */
    DOCUMENT,
    /** 면접 전형 */
    INTERVIEW
}
