package com.campusform.server.project.domain.model.setting.value;

/**
 * 프로젝트 관리자 역할 Enum
 */
public enum ProjectRole {
    /** 프로젝트 소유자 - 모든 권한 보유 */
    OWNER,
    
    /** 프로젝트 관리자 - 제한된 권한 */
    ADMIN
}
