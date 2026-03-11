package com.campusform.server.recruiting.application.dto.request.interview;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필수 면접관 설정 요청 DTO
 */
@Schema(description = "필수 면접관 전체 교체 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequiredInterviewersRequest {

    @Schema(description = "새로 설정할 필수 면접관들의 사용자 ID 목록", example = "[1, 2, 3]")
    @NotNull(message = "필수 면접관 ID 목록은 필수입니다.")
    private List<Long> adminIds;
}
