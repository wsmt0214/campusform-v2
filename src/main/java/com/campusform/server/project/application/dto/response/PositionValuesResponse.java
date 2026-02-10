package com.campusform.server.project.application.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포지션 컬럼 고유값 목록 응답 DTO
 *
 * 편집하기 시 시트의 포지션 컬럼에 등장하는 값들의 종류를 반환합니다.
 */
@Schema(description = "포지션 컬럼 고유값 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PositionValuesResponse {

    @Schema(description = "시트 포지션 컬럼에 등장하는 고유값 목록 (정렬됨)")
    private List<String> values;

    public static PositionValuesResponse from(List<String> values) {
        return new PositionValuesResponse(values != null ? values : List.of());
    }
}
