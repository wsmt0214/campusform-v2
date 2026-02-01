package com.campusform.server.project.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.campusform.server.project.domain.model.setting.value.RequiredFieldMapping;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 생성 요청 DTO
 */
@Schema(description = "프로젝트 생성 요청")
@Getter
@NoArgsConstructor
public class CreateProjectRequest {

    @Schema(description = "프로젝트 제목", example = "2024년 1학기 신입 부원 모집")
    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    private String title;

    @Schema(description = "연동할 Google Sheet URL", example = "https://docs.google.com/spreadsheets/d/...")
    @NotBlank(message = "스프레드시트 URL은 필수입니다.")
    private String sheetUrl;

    @Schema(description = "모집 시작일", example = "2024-03-01")
    @NotNull(message = "모집 시작일은 필수입니다.")
    private LocalDate startAt;

    @Schema(description = "모집 종료일", example = "2024-03-15")
    @NotNull(message = "모집 종료일은 필수입니다.")
    private LocalDate endAt;

    @Schema(description = "추가할 관리자(운영진)의 사용자 ID 목록", example = "[2, 3, 4]")
    // 추가할 관리자 ID 목록
    private List<Long> adminIds;

    @Schema(description = "지원자 정보와 Google Sheet 컬럼 매핑 정보")
    // 스프레드시트 컬럼과 필수 필드 매핑 정보
    @NotNull(message = "필수 필드 매핑 정보는 필수입니다.")
    private RequiredFieldMappingRequest requiredMappings;

    @Schema(description = "필수 필드 매핑 정보")
    @Getter
    @NoArgsConstructor
    public static class RequiredFieldMappingRequest {
        @Schema(description = "이름 컬럼 인덱스 (0부터 시작, 미매핑 시 -1)", example = "1")
        @NotNull(message = "이름 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer nameIdx;

        @Schema(description = "학교 컬럼 인덱스 (0부터 시작, 미매핑 시 -1)", example = "2")
        @NotNull(message = "학교 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer schoolIdx;

        @Schema(description = "전공 컬럼 인덱스 (0부터 시작, 미매핑 시 -1)", example = "3")
        @NotNull(message = "전공 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer majorIdx;

        @Schema(description = "성별 컬럼 인덱스 (0부터 시작, 미매핑 시 -1)", example = "4")
        @NotNull(message = "성별 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer genderIdx;

        @Schema(description = "전화번호 컬럼 인덱스 (0부터 시작, 미매핑 시 -1)", example = "5")
        @NotNull(message = "전화번호 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer phoneIdx;

        @Schema(description = "이메일 컬럼 인덱스 (0부터 시작, 미매핑 시 -1)", example = "6")
        @NotNull(message = "이메일 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer emailIdx;

        @Schema(description = "포지션 컬럼 인덱스 (0부터 시작, 미매핑 시 -1)", example = "7")
        @NotNull(message = "포지션 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer positionIdx;

        /**
         * DTO -> 필수 매핑 필드만 추출하여 묶는 값 객체로 변환
         */
        public RequiredFieldMapping toDomainValue() {
            return new RequiredFieldMapping(nameIdx, schoolIdx, majorIdx, genderIdx, phoneIdx, emailIdx, positionIdx);
        }
    }
}
