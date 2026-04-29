package com.campusform.server.recruiting.domain.repository.projection;

import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;

/**
 * ScreeningResult 기준 집계 결과 projection
 *
 * - GROUP BY 결과를 엔티티 로딩 없이 받기 위한 목적
 * - 통계(count) 쿼리를 여러 번 호출하는 구조를 단일 집계 쿼리로 바꾸기 위한 목적
 */
public interface ScreeningResultCountRow {
    ScreeningResult getStatus();
    Long getCount();
}

