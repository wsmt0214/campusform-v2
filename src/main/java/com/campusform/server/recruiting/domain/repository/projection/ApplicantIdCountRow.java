package com.campusform.server.recruiting.domain.repository.projection;

/**
 * applicantId 기준 집계 결과 projection
 *
 * - 댓글 수 집계처럼 applicantId 기준 GROUP BY 결과를 엔티티 로딩 없이 받기 위한 목적
 */
public interface ApplicantIdCountRow {
    Long getApplicantId();
    Long getCount();
}

