package com.campusform.server.project.domain.model.setting.value;

/**
 * 프로젝트 진행 단계(상태) Enum
 * 프로젝트의 모집 단계를 나타내며, 단방향으로 진행됩니다.
 */
public enum ProjectState {
    /** 프로젝트 생성 완료, 서류 단계 */
    DOCUMENT_OPEN,
    
    /** 서류 마감(수정 불가), 결과 확정 직전/직후 상태 */
    DOCUMENT_LOCKED,
    
    /** 서류 완료 및 모집 공고 종료 (면접 없이 종료) */
    DOCUMENT_DONE,
    
    /** 면접 진행/시간표/평가 가능 */
    INTERVIEW_OPEN,
    
    /** 면접 마감(수정 불가), 결과 확정 직전/직후 상태 */
    INTERVIEW_LOCKED,
    
    /** 면접 종료 및 모집 공고 종료 */
    ALL_COMPLETE
}
