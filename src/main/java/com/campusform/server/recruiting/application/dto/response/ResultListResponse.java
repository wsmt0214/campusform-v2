package com.campusform.server.recruiting.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ResultListResponse {
    private ResultStats stats;
    private TemplateInfo template;
    private List<ApplicantSummary> applicants;

    @Getter
    @Builder
    public static class ResultStats {
        private long totalApplicantCount;
        private long currentStagePassCount;
        private String competitionRate; // "5.0:1"
        private GenderRatio genderRatio;
    }

    @Getter
    @Builder
    public static class GenderRatio {
        private int malePercent;
        private int femalePercent;
    }

    @Getter
    @Builder
    public static class TemplateInfo {
        private String content;
    }

    @Getter
    @Builder
    public static class ApplicantSummary {
        private Long applicantId;
        private String name;
        private String school;
        private String major;
        private String position;
    }
}
