package com.campusform.server.recruiting.application.dto.response.result;

import java.util.List;

import com.campusform.server.recruiting.domain.model.applicant.Applicant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "단계별 합격/불합격자 명단 조회 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultListResponse {
    @Schema(description = "결과 통계 정보")
    private ResultStats stats;
    @Schema(description = "적용된 SMS 템플릿 정보")
    private TemplateInfo template;
    @Schema(description = "조회된 지원자 목록")
    private List<ApplicantSummary> applicants;

    @Schema(description = "결과 통계")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultStats {
        @Schema(description = "총 지원자 수", example = "100")
        private long totalApplicantCount;
        @Schema(description = "현재 단계 합격자 수", example = "20")
        private long currentStagePassCount;
        @Schema(description = "경쟁률", example = "5.0:1")
        private String competitionRate;
        @Schema(description = "성비")
        private GenderRatio genderRatio;
    }

    @Schema(description = "성비")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenderRatio {
        @Schema(description = "남성 비율 (%)", example = "60")
        private int malePercent;
        @Schema(description = "여성 비율 (%)", example = "35")
        private int femalePercent;
        @Schema(description = "기타 비율 (%)", example = "5")
        private int otherPercent;

        /**
         * 지원자 목록으로부터 성비를 계산하여 GenderRatio 인스턴스 생성
         * Applicant.isMale() / isFemale()에 성별 판단 로직이 위임되어 있음
         */
        public static GenderRatio from(List<Applicant> applicants) {
            if (applicants.isEmpty()) {
                return GenderRatio.builder()
                        .malePercent(0).femalePercent(0).otherPercent(0)
                        .build();
            }

            int total = applicants.size();
            long maleCount = applicants.stream().filter(Applicant::isMale).count();
            long femaleCount = applicants.stream().filter(Applicant::isFemale).count();

            int malePercent = (int) (maleCount * 100 / total);
            int femalePercent = (int) (femaleCount * 100 / total);

            return GenderRatio.builder()
                    .malePercent(malePercent)
                    .femalePercent(femalePercent)
                    .otherPercent(100 - malePercent - femalePercent)
                    .build();
        }
    }

    @Schema(description = "SMS 템플릿 정보")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateInfo {
        @Schema(description = "템플릿 내용", example = "안녕하세요, [NAME]님. 합격을 축하드립니다.")
        private String content;
    }

    @Schema(description = "지원자 요약 정보")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantSummary {
        @Schema(description = "지원자 ID", example = "1")
        private Long applicantId;
        @Schema(description = "이름", example = "홍길동")
        private String name;
        @Schema(description = "학교", example = "캠퍼스대학교")
        private String school;
        @Schema(description = "전공", example = "컴퓨터공학과")
        private String major;
        @Schema(description = "지원 포지션", example = "백엔드")
        private String position;
        @Schema(description = "전화번호", example = "010-1234-5678")
        private String phoneNumber;
        @Schema(description = "개인화된 메시지 내용 (@이름, @포지션이 치환된 메시지)", example = "안녕하세요, 홍길동님. 합격을 축하드립니다.")
        private String personalizedMessage;
    }
}
