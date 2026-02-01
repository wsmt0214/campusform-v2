package com.campusform.server.recruiting.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "댓글 작성 응답")
@Getter
@AllArgsConstructor
public class CommentCreateResponse {
    @Schema(description = "생성된 댓글 ID", example = "101")
    private Long commentId;
}
