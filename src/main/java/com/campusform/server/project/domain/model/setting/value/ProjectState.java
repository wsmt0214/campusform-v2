package com.campusform.server.project.domain.model.setting.value;

/**
 * 프로젝트 진행 단계(상태) Enum
 * 
 * DOCUMENT → DOCUMENT_COMPLETE (면접 없이 종료)
 * DOCUMENT → INTERVIEW → INTERVIEW_COMPLETE (면접까지 하고 종료)
 */
public enum ProjectState {
    /** 서류 심사 진행 중 */
    DOCUMENT,

    /** 면접 진행 중 */
    INTERVIEW,

    /** 서류 심사 완료 (면접 없이 종료) */
    DOCUMENT_COMPLETE,

    /** 면접 완료 (전체 종료) */
    INTERVIEW_COMPLETE
}
