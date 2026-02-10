package com.campusform.server.recruiting.application.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "댓글 작성 응답")
@Getter
@AllArgsConstructor
public class CommentCreateResponse {
    @Schema(description = "생성된 댓글 ID", example = "101")
    private Long commentId;
    @Schema(description = "부모 댓글 ID (대댓글인 경우)")
    private Long parentId;
    @Schema(description = "생성 시각 (ISO-8601)", example = "2025-02-10T14:30:00")
    private LocalDateTime createdAt;
}
