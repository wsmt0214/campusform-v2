package com.campusform.server.recruiting.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "지원자 상태 변경 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantStatusUpdateResponse {
    @Schema(description = "상태가 변경된 지원자 ID", example = "1")
    private Long applicantId;
    @Schema(description = "변경 후 현재 상태", example = "PASS")
    private String currentStatus;
    @Schema(description = "상태 변경 시각")
    private LocalDateTime updateAt;
}
