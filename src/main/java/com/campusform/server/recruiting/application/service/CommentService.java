package com.campusform.server.recruiting.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.global.event.CommentCreatedEvent;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.application.dto.request.CommentRequest;
import com.campusform.server.recruiting.application.dto.response.CommentCreateResponse;
import com.campusform.server.recruiting.application.dto.response.CommentResponse;
import com.campusform.server.recruiting.application.dto.response.CommentUpdateResponse;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.infrastructure.persistence.CommentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final ApplicantRepository applicantRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 댓글 작성 (parentId가 있으면 대댓글, 없으면 루트 댓글)
     */
    public CommentCreateResponse createComment(Long applicantId, Long authorId, RecruitmentStage stage,
            CommentRequest request) {
        if (!applicantRepository.existsById(applicantId)) {
            throw new EntityNotFoundException("존재하지 않는 지원자입니다.");
        }

        // 종료된 프로젝트에는 댓글을 작성할 수 없음
        validateProjectNotCompleted(applicantId);

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
        publishCommentCreatedEvent(applicantId, authorId);

        return new CommentCreateResponse(
                savedComment.getId(),
                savedComment.getParent() != null ? savedComment.getParent().getId() : null,
                savedComment.getCreatedAt());
    }

    /**
     * 댓글 생성 이벤트 발행.
     * 해당 프로젝트의 모든 관리자(OWNER + ADMIN)에게 알림이 가며, 댓글 작성자 본인에게는 발송하지 않습니다.
     * NotificationEventHandler가 수신해 각 수신자별로 알림을 DB에 저장합니다.
     */
    private void publishCommentCreatedEvent(Long applicantId, Long commenterId) {
        var applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지원자입니다."));
        var project = projectRepository.findById(applicant.getProjectId())
                .orElseThrow(() -> new IllegalStateException("지원자가 속한 프로젝트를 찾을 수 없습니다."));
        var commenter = userRepository.findById(commenterId)
                .orElse(null);

        // 수신자: 해당 프로젝트의 모든 관리자(OWNER + ADMIN) 중 댓글 작성자 본인만 제외
        List<Long> recipientIds = project.getAdminIds().stream()
                .filter(id -> !id.equals(commenterId))
                .toList();

        if (recipientIds.isEmpty()) {
            return;
        }

        String commenterName = commenter != null && commenter.getNickname() != null && !commenter.getNickname().isBlank()
                ? commenter.getNickname()
                : (commenter != null && commenter.getEmail() != null ? commenter.getEmail() : "알 수 없음");

        eventPublisher.publishEvent(new CommentCreatedEvent(
                project.getId(),
                applicantId,
                applicant.getName() != null ? applicant.getName() : "지원자",
                commenterId,
                commenterName,
                recipientIds));
    }

    // 3. 댓글 수정
    public CommentUpdateResponse updateComment(Long applicantId, Long commentId, Long authorId,
            RecruitmentStage stage, CommentRequest request) {
        // 종료된 프로젝트에서는 댓글을 수정할 수 없음
        validateProjectNotCompleted(applicantId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));

        if (!comment.getApplicantId().equals(applicantId)) {
            throw new IllegalArgumentException("해당 지원자의 댓글이 아닙니다.");
        }

        // stage 일치 검증 (DOCUMENT와 INTERVIEW 댓글 구분)
        if (!comment.getStage().equals(stage)) {
            throw new IllegalArgumentException(
                    String.format("해당 모집 단계의 댓글이 아닙니다. 댓글의 stage: %s, 요청한 stage: %s",
                            comment.getStage(), stage));
        }

        validateAuthor(comment, authorId); // 작성자 검증 분리

        comment.updateContent(request.getContent());

        // 더티 체킹에 의해 트랜잭션 종료 시 자동 update 쿼리 발생
        return new CommentUpdateResponse(comment.getId(), comment.getUpdatedAt());
    }

    /**
     * 댓글 삭제 (작성자 본인만 가능).
     * 부모 댓글 삭제 시 해당 댓글의 모든 대댓글(직접·간접)은 엔티티의 cascade = ALL + orphanRemoval = true 로
     * DB에서 함께 삭제된다.
     */
    public void deleteComment(Long commentId, Long authorId, RecruitmentStage stage) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));

        // 종료된 프로젝트에서는 댓글을 삭제할 수 없음
        validateProjectNotCompleted(comment.getApplicantId());

        // stage 일치 검증 (DOCUMENT와 INTERVIEW 댓글 구분)
        if (!comment.getStage().equals(stage)) {
            throw new IllegalArgumentException(
                    String.format("해당 모집 단계의 댓글이 아닙니다. 댓글의 stage: %s, 요청한 stage: %s",
                            comment.getStage(), stage));
        }

        validateAuthor(comment, authorId);

        // 부모 삭제 시 JPA가 replies 컬렉션에 대해 cascade REMOVE + orphanRemoval 로 대댓글을 모두 삭제
        commentRepository.delete(comment);
    }

    /**
     * 특정 단계의 지원자에 달린 댓글 조회
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long applicantId, RecruitmentStage stage) {
        if (!applicantRepository.existsById(applicantId)) {
            throw new EntityNotFoundException("존재하지 않는 지원자입니다.");
        }

        // 특정 단계의 모든 댓글 조회 (루트 + 대댓글)
        List<Comment> allComments = commentRepository.findAllByApplicantIdAndStageOrderByCreatedAtAsc(applicantId,
                stage);

        // 작성자 ID 목록으로 닉네임·프로필 이미지 조회 (Identity 컨텍스트)
        List<Long> authorIds = allComments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .toList();
        List<User> authors = userRepository.findByIds(authorIds);
        Map<Long, String> authorIdToNickname = authors.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : ""));
        Map<Long, String> authorIdToProfileImageUrl = authors.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getProfileImageUrl(), (a, b) -> a));

        return buildCommentHierarchy(allComments, authorIdToNickname, authorIdToProfileImageUrl);
    }

    // 공통: 댓글 계층 구조 구성 메서드
    private List<CommentResponse> buildCommentHierarchy(List<Comment> allComments,
            Map<Long, String> authorIdToNickname, Map<Long, String> authorIdToProfileImageUrl) {
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
                                authorIdToNickname.getOrDefault(comment.getAuthorId(), ""),
                                authorIdToProfileImageUrl.get(comment.getAuthorId()),
                                comment.getParent() != null ? comment.getParent().getId() : null,
                                comment.getContent(),
                                comment.getCreatedAt(),
                                comment.getUpdatedAt())));

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

    /**
     * 지원자가 속한 프로젝트가 종료 상태가 아닌지 검증
     */
    private void validateProjectNotCompleted(Long applicantId) {
        Long projectId = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지원자입니다."))
                .getProjectId();

        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("지원자가 속한 프로젝트를 찾을 수 없습니다."))
                .validateNotCompleted();
    }
}
