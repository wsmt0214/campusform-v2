package com.campusform.server.recruiting.presentation;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.service.CommentService;
import com.campusform.server.recruiting.application.dto.request.CommentRequest;
import com.campusform.server.recruiting.application.dto.response.CommentCreateResponse;
import com.campusform.server.recruiting.application.dto.response.CommentUpdateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글", description = "지원자 프로필에 대한 댓글(코멘트) 작성, 수정, 삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/applicants/{applicantId}/comments")
public class CommentController {
    private final CommentService commentService;
    private final AuthService authService;

    @Operation(summary = "댓글 작성", description = "특정 지원자에 대해 새로운 댓글을 작성합니다.")
    @PostMapping
    public ResponseEntity<CommentCreateResponse> createComment(
        @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
        @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
        @RequestBody @Valid CommentRequest request,
        @AuthenticationPrincipal OAuth2User oauth2User
    ){
        if (oauth2User == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Long memberId = oauth2User.getAttribute("userId");
        CommentCreateResponse response = commentService.createComment(applicantId, memberId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 수정", description = "자신이 작성한 댓글을 수정합니다.")
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentUpdateResponse> updateComment(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "수정할 댓글 ID") @PathVariable Long commentId,
            @RequestBody @Valid CommentRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Long memberId = oauth2User.getAttribute("userId");
        CommentUpdateResponse response = commentService.updateComment(
                applicantId,commentId, memberId, request
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 삭제", description = "자신이 작성한 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "삭제할 댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        if (oauth2User == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Long memberId = oauth2User.getAttribute("userId");
        commentService.deleteComment(commentId, memberId);

        return ResponseEntity.ok().build();
    }
}
