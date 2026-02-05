package com.campusform.server.recruiting.application.service;

import com.campusform.server.recruiting.application.dto.request.CommentRequest;
import com.campusform.server.recruiting.application.dto.response.CommentCreateResponse;
import com.campusform.server.recruiting.application.dto.response.CommentResponse;
import com.campusform.server.recruiting.application.dto.response.CommentUpdateResponse;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.infrastructure.persistence.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final ApplicantRepository applicantRepository;

    // 1. 댓글 작성 (parentId가 있으면 대댓글, 없으면 루트 댓글)
    public CommentCreateResponse createComment(Long applicantId, Long authorId, CommentRequest request){
        if (!applicantRepository.existsById(applicantId)) {
            throw new EntityNotFoundException("존재하지 않는 지원자입니다.");
        }

        Comment comment;
        
        // parentId가 있으면 대댓글, 없으면 루트 댓글
        if (request.getParentId() != null) {
            // 대댓글 작성
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("부모 댓글이 없습니다. parentId: " + request.getParentId()));

            if (!parent.getApplicantId().equals(applicantId)) {
                throw new IllegalArgumentException("대댓글은 같은 지원자의 댓글에만 작성 가능합니다.");
            }
            
            // 깊이 제한 없이 무제한으로 대댓글 작성 가능
            // parent 객체를 직접 전달하여 parent_comment_id가 제대로 저장되도록 함
            comment = Comment.createReply(parent, applicantId, authorId, request.getContent());
            
            // parent가 제대로 설정되었는지 확인
            if (comment.getParent() == null || !comment.getParent().getId().equals(request.getParentId())) {
                throw new IllegalStateException("부모 댓글 설정에 실패했습니다. parentId: " + request.getParentId());
            }
        } else {
            // 루트 댓글 작성
            comment = Comment.createRoot(applicantId, authorId, request.getContent());
        }
        
        // 저장 후 반환 (parent_comment_id는 JPA가 자동으로 저장)
        Comment savedComment = commentRepository.save(comment);
        return new CommentCreateResponse(savedComment.getId(), savedComment.getParent() != null ? savedComment.getParent().getId() : null);
    }

    // 3. 댓글 수정
    public CommentUpdateResponse updateComment(Long applicantId,Long commentId, Long authorId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));

        if(!comment.getApplicantId().equals(applicantId)){
            throw new IllegalArgumentException("해당 지원자의 댓글이 아닙니다.");
        }

        validateAuthor(comment, authorId); // 작성자 검증 분리

        comment.updateContent(request.getContent());

        // 더티 체킹에 의해 트랜잭션 종료 시 자동 update 쿼리 발생
        return new CommentUpdateResponse(comment.getId(), comment.getUpdatedAt());
    }

    // 3. 댓글 삭제
    // - 루트 댓글 삭제 시: 모든 대댓글(무한 깊이)이 자동으로 삭제됨 (cascade = CascadeType.ALL)
    // - 대댓글 삭제 시: 하위 댓글들은 모두 루트 댓글의 직접 자식이므로 해당 대댓글만 삭제하면 됨
    //   (시나리오 2: 모든 대댓글이 루트의 직접 자식이므로 changeParent 불필요)
    public void deleteComment(Long commentId, Long authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));

        // 작성자 본인 확인
        validateAuthor(comment, authorId);

        // 댓글 삭제 (cascade로 하위 댓글도 자동 삭제되거나, 모든 대댓글이 루트의 직접 자식이므로 문제없음)
        commentRepository.delete(comment);
    }

    // 4. 지원자별 댓글 목록 조회 (계층 구조 포함)
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long applicantId) {
        if (!applicantRepository.existsById(applicantId)) {
            throw new EntityNotFoundException("존재하지 않는 지원자입니다.");
        }

        // 모든 댓글 조회 (루트 + 대댓글)
        List<Comment> allComments = commentRepository.findAllByApplicantIdOrderByCreatedAtAsc(applicantId);

        return buildCommentHierarchy(allComments);
    }

    // 공통: 댓글 계층 구조 구성 메서드
    private List<CommentResponse> buildCommentHierarchy(List<Comment> allComments) {
        // 루트 댓글만 필터링
        List<Comment> rootComments = allComments.stream()
                .filter(comment -> comment.getParent() == null)
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .collect(Collectors.toList());

        // 댓글 ID를 키로 하는 맵 생성 (빠른 조회를 위해)
        Map<Long, CommentResponse> commentMap = allComments.stream()
                .collect(Collectors.toMap(
                        Comment::getId,
                        comment -> new CommentResponse(
                                comment.getId(),
                                comment.getAuthorId(),
                                comment.getParent() != null ? comment.getParent().getId() : null,
                                comment.getContent(),
                                comment.getCreatedAt(),
                                comment.getUpdatedAt()
                        )
                ));

        // 계층 구조 구성
        for (Comment comment : allComments) {
            if (comment.getParent() != null) {
                CommentResponse parentResponse = commentMap.get(comment.getParent().getId());
                CommentResponse childResponse = commentMap.get(comment.getId());
                if (parentResponse != null && childResponse != null) {
                    parentResponse.getReplies().add(childResponse);
                }
            }
        }

        // 각 댓글의 대댓글들을 생성일시 순으로 정렬 (재귀적으로)
        for (CommentResponse response : commentMap.values()) {
            sortRepliesRecursively(response);
        }

        // 루트 댓글만 반환 (대댓글은 replies에 포함됨)
        return rootComments.stream()
                .map(comment -> commentMap.get(comment.getId()))
                .collect(Collectors.toList());
    }

    // 대댓글 재귀 정렬 (무한 깊이 지원)
    private void sortRepliesRecursively(CommentResponse comment) {
        if (comment.getReplies().isEmpty()) {
            return;
        }
        // 대댓글을 생성일시 순으로 정렬
        comment.getReplies().sort(Comparator.comparing(CommentResponse::getCreatedAt));
        // 각 대댓글의 대댓글도 재귀적으로 정렬
        for (CommentResponse reply : comment.getReplies()) {
            sortRepliesRecursively(reply);
        }
    }

    // 공통: 작성자 검증 로직
    private void validateAuthor(Comment comment, Long authorId) {
        if (!comment.isWrittenBy(authorId)) {
            // 예외 처리는 프로젝트 정책에 맞는 Exception으로 변경하세요 (예: AccessDeniedException)
            throw new IllegalArgumentException("작성자만 수정/삭제할 수 있습니다.");
        }
    }
}
