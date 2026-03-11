package com.campusform.server.project.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Google Sheet의 컬럼(헤더) 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpreadsheetColumnResponse {
    @Schema(description = "컬럼 이름", example = "이름")
    private String name;
    @Schema(description = "컬럼 인덱스 (0부터 시작)", example = "0")
    private Integer index;
}
