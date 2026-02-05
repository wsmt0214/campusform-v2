package com.campusform.server.recruiting.application.dto.response;

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
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies;

    public CommentResponse(Long commentId, Long authorId, Long parentId, String content, 
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.commentId = commentId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.replies = new ArrayList<>();
    }
}
