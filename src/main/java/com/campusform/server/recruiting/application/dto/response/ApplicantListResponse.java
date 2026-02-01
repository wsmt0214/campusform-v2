package com.campusform.server.recruiting.application.dto.response;

import com.campusform.server.recruiting.domain.model.applicant.value.ApplicantStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "지원자 목록 조회 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantListResponse {
    @Schema(description = "지원자 현황 통계")
    private ApplicantStatus status;
    @Schema(description = "지원자 목록")
    private List<ApplicantResponse> applicants;

    @Schema(description = "지원자 현황 통계")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantStatus {
        @Schema(description = "전체 지원자 수", example = "100")
        private long totalCount;
        @Schema(description = "보류중인 지원자 수", example = "10")
        private long pendingCount;
        @Schema(description = "합격한 지원자 수", example = "20")
        private long passCount;
        @Schema(description = "불합격한 지원자 수", example = "70")
        private long failCount;
    }
}
