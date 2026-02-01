package com.campusform.server.recruiting.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "댓글 수정 응답")
@Getter
@AllArgsConstructor
public class CommentUpdateResponse {
    @Schema(description = "수정된 댓글 ID", example = "101")
    private Long commentId;
    @Schema(description = "수정된 시각")
    private LocalDateTime updatedAt;
}
