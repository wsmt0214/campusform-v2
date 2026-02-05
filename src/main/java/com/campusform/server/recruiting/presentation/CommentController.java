package com.campusform.server.recruiting.presentation;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.service.CommentService;
import com.campusform.server.recruiting.application.dto.request.CommentRequest;
import com.campusform.server.recruiting.application.dto.response.CommentCreateResponse;
import com.campusform.server.recruiting.application.dto.response.CommentResponse;
import com.campusform.server.recruiting.application.dto.response.CommentUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}")
public class CommentController {
    private final CommentService commentService;
    private final AuthService authService;

    // 프로젝트 전체 댓글 목록 조회 (계층 구조 포함)
    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByProject(
            @PathVariable Long projectId
    ) {
        List<CommentResponse> comments = commentService.getCommentsByProjectId(projectId);
        return ResponseEntity.ok(comments);
    }

    // 지원자별 댓글 목록 조회 (계층 구조 포함)
    @GetMapping("/applicants/{applicantId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long applicantId
    ) {
        List<CommentResponse> comments = commentService.getComments(applicantId);
        return ResponseEntity.ok(comments);
    }

    // 댓글 작성 (parentId가 있으면 대댓글, 없으면 루트 댓글)
    @PostMapping("/applicants/{applicantId}/comments")
    public ResponseEntity<CommentCreateResponse> createComment(
        @PathVariable Long applicantId,
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

    // 댓글 수정
    @PatchMapping("/applicants/{applicantId}/comments/{commentId}")
    public ResponseEntity<CommentUpdateResponse> updateComment(
            //@PathVariable Long projectId,
            @PathVariable Long applicantId,
            @PathVariable Long commentId,
            //@RequestParam StageStatus stage,
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

    // 댓글 삭제
    @DeleteMapping("/applicants/{applicantId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            // @PathVariable Long projectId,
            // @PathVariable Long applicantId,
            @PathVariable Long commentId,
            // @RequestParam StageStatus stage,
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
