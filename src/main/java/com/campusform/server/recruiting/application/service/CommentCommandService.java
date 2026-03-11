package com.campusform.server.recruiting.application.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.global.event.CommentCreatedEvent;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.application.service.ProjectAuthorizationService;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.application.dto.request.comment.CommentRequest;
import com.campusform.server.recruiting.application.dto.response.comment.CommentCreateResponse;
import com.campusform.server.recruiting.application.dto.response.comment.CommentUpdateResponse;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * 댓글 작성/수정/삭제 전용 서비스 (CQRS 패턴)
 * 기존 읽기 + 쓰기가 함께 구현돼 있어 코드가 비대해짐 -> 쿼리(Query)와 커맨드(Command) 책임이 분리
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentCommandService {

    private final CommentRepository commentRepository;
    private final ApplicantRepository applicantRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * 댓글 작성 (parentId가 있으면 대댓글, 없으면 루트 댓글)
     */
    public CommentCreateResponse createComment(Long projectId, Long applicantId, Long authorId,
            RecruitmentStage stage, CommentRequest request) {
        projectAuthorizationService.assertAdmin(projectId, authorId);

        if (!applicantRepository.existsById(applicantId)) {
            throw new EntityNotFoundException("존재하지 않는 지원자입니다.");
        }
        validateApplicantBelongsToProject(applicantId, projectId);
        // 종료된 프로젝트에는 댓글을 작성할 수 없음
        validateProjectNotCompleted(applicantId);

        Comment comment;
        if (request.getParentId() != null) {
            // 대댓글 작성
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "부모 댓글이 없습니다. parentId: " + request.getParentId()));
            if (!parent.getApplicantId().equals(applicantId)) {
                throw new IllegalArgumentException("대댓글은 같은 지원자의 댓글에만 작성 가능합니다.");
            }
            // 깊이 제한 없이 무제한으로 대댓글 작성 가능
            // parent 객체를 직접 전달하여 parent_comment_id가 제대로 저장되도록 함
            // createReply 내부에서 stage 일치 검증도 수행됨
            comment = Comment.createReply(parent, applicantId, authorId, stage, request.getContent());
            // parent가 제대로 설정되었는지 확인
            if (comment.getParent() == null || !comment.getParent().getId().equals(request.getParentId())) {
                throw new IllegalStateException("부모 댓글 설정에 실패했습니다. parentId: " + request.getParentId());
            }
        } else {
            // 루트 댓글 작성
            comment = Comment.createRoot(applicantId, authorId, stage, request.getContent());
        }

        // 저장 후 반환 (parent_comment_id는 JPA가 자동으로 저장)
        Comment savedComment = commentRepository.save(comment);
        // 댓글 생성 알림: 해당 프로젝트의 모든 관리자(댓글 작성자 제외)에게 발송
        publishCommentCreatedEvent(applicantId, authorId, stage);

        return new CommentCreateResponse(savedComment.getId(),
                savedComment.getParent() != null ? savedComment.getParent().getId() : null,
                savedComment.getCreatedAt());
    }

    /**
     * 댓글 수정 (작성자 본인만 가능)
     */
    public CommentUpdateResponse updateComment(Long projectId, Long applicantId, Long commentId,
            Long authorId, RecruitmentStage stage, CommentRequest request) {
        // 3. 댓글 수정 (프로젝트 관리자 권한 검증 후, 작성자 본인만 수정 가능)
        projectAuthorizationService.assertAdmin(projectId, authorId);
        // 종료된 프로젝트에서는 댓글을 수정할 수 없음
        validateProjectNotCompleted(applicantId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));
        if (!comment.getApplicantId().equals(applicantId)) {
            throw new IllegalArgumentException("해당 지원자의 댓글이 아닙니다.");
        }
        // stage 일치 검증 (DOCUMENT와 INTERVIEW 댓글 구분)
        if (!comment.getStage().equals(stage)) {
            throw new IllegalArgumentException(String.format(
                    "해당 모집 단계의 댓글이 아닙니다. 댓글의 stage: %s, 요청한 stage: %s",
                    comment.getStage(), stage));
        }
        validateAuthor(comment, authorId);
        comment.updateContent(request.getContent());
        return new CommentUpdateResponse(comment.getId(), comment.getUpdatedAt());
    }

    /**
     * 댓글 삭제 (작성자 본인만 가능). 부모 삭제 시 대댓글은 cascade로 함께 삭제됨.
     */
    public void deleteComment(Long projectId, Long commentId, Long authorId, RecruitmentStage stage) {
        projectAuthorizationService.assertAdmin(projectId, authorId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));
        validateApplicantBelongsToProject(comment.getApplicantId(), projectId);
        // 종료된 프로젝트에서는 댓글을 삭제할 수 없음
        validateProjectNotCompleted(comment.getApplicantId());
        // stage 일치 검증 (DOCUMENT와 INTERVIEW 댓글 구분)
        if (!comment.getStage().equals(stage)) {
            throw new IllegalArgumentException(String.format(
                    "해당 모집 단계의 댓글이 아닙니다. 댓글의 stage: %s, 요청한 stage: %s",
                    comment.getStage(), stage));
        }
        validateAuthor(comment, authorId);
        // 부모 삭제 시 JPA가 replies 컬렉션에 대해 cascade REMOVE + orphanRemoval 로 대댓글을 모두 삭제
        commentRepository.delete(comment);
    }

    private void publishCommentCreatedEvent(Long applicantId, Long commenterId, RecruitmentStage stage) {
        var applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지원자입니다."));
        var project = projectRepository.findById(applicant.getProjectId())
                .orElseThrow(() -> new IllegalStateException("지원자가 속한 프로젝트를 찾을 수 없습니다."));
        User commenter = userRepository.findById(commenterId).orElse(null);

        List<Long> recipientIds = project.getAdminIds().stream()
                .filter(id -> !id.equals(commenterId))
                .toList();
        if (recipientIds.isEmpty()) {
            return;
        }

        String commenterName = commenter != null && commenter.getNickname() != null
                && !commenter.getNickname().isBlank()
                ? commenter.getNickname()
                : (commenter != null && commenter.getEmail() != null ? commenter.getEmail() : "알 수 없음");
        String projectTitle = project.getTitle() != null ? project.getTitle().trim() : null;
        eventPublisher.publishEvent(new CommentCreatedEvent(project.getId(), projectTitle,
                applicantId, applicant.getName() != null ? applicant.getName() : "지원자",
                commenterId, commenterName, recipientIds, stage.name()));
    }

    private void validateAuthor(Comment comment, Long authorId) {
        if (!comment.isWrittenBy(authorId)) {
            throw new IllegalArgumentException("작성자만 수정/삭제할 수 있습니다.");
        }
    }

    private void validateApplicantBelongsToProject(Long applicantId, Long projectId) {
        Long applicantProjectId = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지원자입니다."))
                .getProjectId();
        if (!applicantProjectId.equals(projectId)) {
            throw new IllegalArgumentException("해당 지원자는 이 프로젝트에 속하지 않습니다.");
        }
    }

    private void validateProjectNotCompleted(Long applicantId) {
        Long projectId = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지원자입니다."))
                .getProjectId();
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("지원자가 속한 프로젝트를 찾을 수 없습니다."))
                .validateNotCompleted();
    }
}
