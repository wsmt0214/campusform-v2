package com.campusform.server.recruiting.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.application.service.ProjectAuthorizationService;
import com.campusform.server.recruiting.application.dto.response.comment.CommentResponse;
import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * 댓글 조회 전용 서비스 (CQRS 패턴)
 * 기존 읽기 + 쓰기가 함께 구현돼 있어 코드가 비대해짐 -> 쿼리(Query)와 커맨드(Command) 책임이 분리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final ApplicantRepository applicantRepository;
    private final UserRepository userRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * 지원자별 댓글 목록 조회 (계층 구조·작성자 닉네임·프로필 이미지 포함)
     */
    public List<CommentResponse> getComments(Long projectId, Long applicantId, RecruitmentStage stage,
            Long userId) {
        projectAuthorizationService.assertAdmin(projectId, userId);

        if (!applicantRepository.existsById(applicantId)) {
            throw new EntityNotFoundException("존재하지 않는 지원자입니다.");
        }
        validateApplicantBelongsToProject(applicantId, projectId);

        List<Comment> allComments = commentRepository
                .findAllByApplicantIdAndStageOrderByCreatedAtAsc(applicantId, stage);

        List<Long> authorIds = allComments.stream().map(Comment::getAuthorId).distinct().toList();
        List<User> authors = userRepository.findByIds(authorIds);
        Map<Long, String> authorIdToNickname = authors.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : ""));
        Map<Long, String> authorIdToProfileImageUrl = authors.stream()
                .collect(Collectors.toMap(User::getId, User::getProfileImageUrl, (a, b) -> a));

        return buildCommentHierarchy(allComments, authorIdToNickname, authorIdToProfileImageUrl);
    }

    private List<CommentResponse> buildCommentHierarchy(List<Comment> allComments,
            Map<Long, String> authorIdToNickname, Map<Long, String> authorIdToProfileImageUrl) {
        List<Comment> rootComments = allComments.stream()
                .filter(c -> c.getParent() == null)
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .collect(Collectors.toList());

        Map<Long, CommentResponse> commentMap = allComments.stream().collect(Collectors.toMap(
                Comment::getId,
                c -> new CommentResponse(c.getId(), c.getAuthorId(),
                        authorIdToNickname.getOrDefault(c.getAuthorId(), ""),
                        authorIdToProfileImageUrl.get(c.getAuthorId()),
                        c.getParent() != null ? c.getParent().getId() : null,
                        c.getContent(), c.getCreatedAt(), c.getUpdatedAt())));

        for (Comment comment : allComments) {
            if (comment.getParent() != null) {
                CommentResponse parentResponse = commentMap.get(comment.getParent().getId());
                CommentResponse childResponse = commentMap.get(comment.getId());
                if (parentResponse != null && childResponse != null) {
                    parentResponse.getReplies().add(childResponse);
                }
            }
        }

        for (CommentResponse response : commentMap.values()) {
            sortRepliesRecursively(response);
        }

        return rootComments.stream().map(c -> commentMap.get(c.getId())).collect(Collectors.toList());
    }

    private void sortRepliesRecursively(CommentResponse comment) {
        if (comment.getReplies().isEmpty()) {
            return;
        }
        comment.getReplies().sort(Comparator.comparing(CommentResponse::getCreatedAt));
        for (CommentResponse reply : comment.getReplies()) {
            sortRepliesRecursively(reply);
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
}
