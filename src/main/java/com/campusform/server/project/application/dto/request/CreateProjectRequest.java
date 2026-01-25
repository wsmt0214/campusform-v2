package com.campusform.server.project.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.campusform.server.project.domain.model.setting.value.RequiredFieldMapping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "스프레드시트 URL은 필수입니다.")
    private String sheetUrl;

    @NotNull(message = "모집 시작일은 필수입니다.")
    private LocalDate startAt;

    @NotNull(message = "모집 종료일은 필수입니다.")
    private LocalDate endAt;

    // 추가할 관리자 ID 목록
    private List<Long> adminIds;

    // 스프레드시트 컬럼과 필수 필드 매핑 정보
    @NotNull(message = "필수 필드 매핑 정보는 필수입니다.")
    private RequiredFieldMappingRequest requiredMappings;

    @Getter
    @NoArgsConstructor
    public static class RequiredFieldMappingRequest {
        @NotNull(message = "이름 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer nameIdx;

        @NotNull(message = "학교 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer schoolIdx;

        @NotNull(message = "전공 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer majorIdx;

        @NotNull(message = "성별 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer genderIdx;

        @NotNull(message = "전화번호 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer phoneIdx;

        @NotNull(message = "이메일 인덱스는 필수입니다. (미매핑 시 -1)")
        private Integer emailIdx;

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
