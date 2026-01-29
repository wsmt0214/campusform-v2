package com.campusform.server.recruiting.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentUpdateResponse {
    private Long commentId;
    private LocalDateTime updatedAt;
}
