package com.campusform.server.project.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 이름(제목) 수정 요청 DTO
 */
@Schema(description = "프로젝트 이름 수정 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectNameRequest {

    @Schema(description = "변경할 프로젝트 제목(이름)", example = "2024년 2학기 신입 부원 모집")
    @NotBlank(message = "프로젝트 제목은 필수이며, 공백만으로 구성될 수 없습니다.")
    private String title;
}
