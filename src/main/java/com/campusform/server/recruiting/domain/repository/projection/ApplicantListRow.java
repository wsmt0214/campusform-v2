package com.campusform.server.recruiting.domain.repository.projection;

import com.campusform.server.recruiting.domain.model.applicant.value.ScreeningResult;

/**
 * 지원자 목록 화면 전용 projection
 *
 * - 목록 화면이 요구하는 최소 컬럼만 조회해 엔티티 전체 로딩을 피하는 목적
 * - Query 전용 응답 모델로 도메인 로직(상태 변경 등)과 분리되는 목적
 */
public interface ApplicantListRow {
    Long getId();
    String getName();
    String getSchool();
    String getMajor();
    String getPosition();

    ScreeningResult getDocumentStatus();
    ScreeningResult getInterviewStatus();

    Boolean getDocumentBookmarked();
    Boolean getInterviewBookmarked();
}

