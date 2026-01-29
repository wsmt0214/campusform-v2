package com.campusform.server.recruiting.application.service;

import com.campusform.server.recruiting.application.dto.request.CommentRequest;
import com.campusform.server.recruiting.application.dto.response.CommentCreateResponse;
import com.campusform.server.recruiting.application.dto.response.CommentUpdateResponse;
import com.campusform.server.recruiting.domain.model.applicant.value.StageStatus;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.infrastructure.persistence.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final ApplicantRepository applicantRepository;

    // 1. 원본 댓글 작성
    public CommentCreateResponse createComment(Long applicantId, Long authorId, CommentRequest request){
        if (!applicantRepository.existsById(applicantId)) {
            throw new EntityNotFoundException("존재하지 않는 지원자입니다.");
        }
        Comment comment = Comment.createRoot(applicantId, authorId, request.getContent());
        commentRepository.save(comment);

        return new CommentCreateResponse(comment.getId());
    }

    // 2. 댓글(대댓글) 작성
    public CommentCreateResponse createReply(Long parentId, Long applicantId, Long authorId, CommentRequest request){
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException(" 부모 댓글이 없습니다. "));
        if (!parent.getApplicantId().equals(applicantId)) {
            throw new IllegalArgumentException("대댓글은 같은 지원자의 댓글에만 작성 가능합니다.");
        }

        // Factory Method가 대댓글 깊이 제한(depth check)을 내부적으로 수행합니다.
        Comment reply = Comment.createReply(parent, applicantId, authorId, request.getContent());
        commentRepository.save(reply);

        return new CommentCreateResponse(reply.getId());
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
    public void deleteComment(Long commentId, Long authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));

        // 작성자 본인 확인 로직 필요
        validateAuthor(comment, authorId);

        commentRepository.delete(comment);
    }

    // 공통: 작성자 검증 로직
    private void validateAuthor(Comment comment, Long authorId) {
        if (!comment.isWrittenBy(authorId)) {
            // 예외 처리는 프로젝트 정책에 맞는 Exception으로 변경하세요 (예: AccessDeniedException)
            throw new IllegalArgumentException("작성자만 수정/삭제할 수 있습니다.");
        }
    }
}
