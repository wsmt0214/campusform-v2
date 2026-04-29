package com.campusform.server.recruiting.domain.repository.projection;

/**
 * projectId 기준 집계 결과 projection
 *
 * - 프로젝트 목록에서 프로젝트별 지원자 수를 1회 집계 쿼리로 조회하기 위한 목적
 */
public interface ProjectIdCountRow {
    Long getProjectId();
    Long getCount();
}

