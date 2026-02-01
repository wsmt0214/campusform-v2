package com.campusform.server.recruiting.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "지원자 목록의 개별 지원자 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantResponse {
    @Schema(description = "지원자 ID", example = "1")
    private Long id;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "전공", example = "컴퓨터공학과")
    private String major;
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;
    @Schema(description = "찜하기 여부", example = "true")
    private boolean bookmarked;
}
