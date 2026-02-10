package com.campusform.server.recruiting.presentation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.recruiting.application.dto.request.CommentRequest;
import com.campusform.server.recruiting.application.dto.response.CommentCreateResponse;
import com.campusform.server.recruiting.application.dto.response.CommentResponse;
import com.campusform.server.recruiting.application.dto.response.CommentUpdateResponse;
import com.campusform.server.recruiting.application.service.CommentService;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "댓글", description = "지원자별 댓글·대댓글 조회, 작성, 수정, 삭제 API (서류/면접 단계별 구분)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}")
public class CommentController {
    private final CommentService commentService;
    private final AuthService authService;

    @Operation(summary = "지원자 댓글 조회", description = "특정 모집 단계(DOCUMENT/INTERVIEW)의 지원자에 달린 댓글을 계층 구조로 조회합니다.")
    @GetMapping("/applicants/{applicantId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "조회할 모집 단계 (DOCUMENT 또는 INTERVIEW)", required = true) @RequestParam RecruitmentStage stage) {
        List<CommentResponse> comments = commentService.getComments(applicantId, stage);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "댓글 작성", description = "루트 댓글 또는 대댓글을 작성합니다. 요청 본문의 parentId가 있으면 해당 댓글의 대댓글, 없으면 루트 댓글입니다.")
    @PostMapping("/applicants/{applicantId}/comments")
    public ResponseEntity<CommentCreateResponse> createComment(
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "모집 단계 (DOCUMENT 또는 INTERVIEW)", required = true) @RequestParam RecruitmentStage stage,
            @RequestBody @Valid CommentRequest requestCommentRequest,
            Authentication authentication) {
        Long memberId = authService.extractUserId(authentication);
        CommentCreateResponse response = commentService.createComment(applicantId, memberId, stage,
                requestCommentRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다. 작성자 본인만 가능합니다.")
    @PatchMapping("/applicants/{applicantId}/comments/{commentId}")
    public ResponseEntity<CommentUpdateResponse> updateComment(
            @Parameter(description = "지원자 ID") @PathVariable Long applicantId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(description = "모집 단계 (DOCUMENT 또는 INTERVIEW)", required = true) @RequestParam RecruitmentStage stage,
            @RequestBody @Valid CommentRequest request,
            Authentication authentication) {
        Long memberId = authService.extractUserId(authentication);
        CommentUpdateResponse response = commentService.updateComment(
                applicantId, commentId, memberId, stage, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 작성자 본인만 가능하며, 루트 댓글 삭제 시 대댓글도 함께 삭제됩니다.")
    @DeleteMapping("/applicants/{applicantId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(description = "모집 단계 (DOCUMENT 또는 INTERVIEW)", required = true) @RequestParam RecruitmentStage stage,
            Authentication authentication) {
        Long memberId = authService.extractUserId(authentication);
        commentService.deleteComment(commentId, memberId, stage);
        return ResponseEntity.ok().build();
    }
}
