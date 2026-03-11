package com.campusform.server.recruiting.application.dto.response.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class CommentResponse {
    private Long commentId;
    private Long authorId;
    private String authorNickname;
    private String authorProfileImageUrl;
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies;

    /** 대댓글 목록 없이 생성 (replies는 빈 리스트로 초기화) */
    public CommentResponse(Long commentId, Long authorId, String authorNickname, String authorProfileImageUrl,
            Long parentId, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.commentId = commentId;
        this.authorId = authorId;
        this.authorNickname = authorNickname != null ? authorNickname : "";
        this.authorProfileImageUrl = authorProfileImageUrl;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.replies = new ArrayList<>();
    }
}
