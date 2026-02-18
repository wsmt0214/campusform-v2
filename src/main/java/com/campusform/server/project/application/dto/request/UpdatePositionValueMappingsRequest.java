package com.campusform.server.project.application.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프로젝트 포지션 값 치환 규칙 수정 요청 DTO
 */
@Schema(description = "포지션 값 치환 규칙 수정 요청")
@Getter
@Setter
@NoArgsConstructor
public class UpdatePositionValueMappingsRequest {

    @Schema(description = "포지션 값 치환 규칙 목록 (시트 원시값 → 저장용 표시값)")
    @Valid
    private List<PositionValueMappingItem> valueMappings = List.of();

    @Schema(description = "포지션 값 치환 규칙 한 건")
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PositionValueMappingItem {
        @Schema(description = "시트에서 들어오는 포지션 원시값", example = "서버 1")
        @NotBlank(message = "fromValue는 필수입니다.")
        private String fromValue;

        @Schema(description = "DB에 저장할 치환값(표시값)", example = "서버")
        @NotBlank(message = "toValue는 필수입니다.")
        private String toValue;
    }
}
