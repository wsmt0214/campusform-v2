package com.campusform.server.recruiting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.comment.Comment;

/**
 * 도메인 계층의 댓글 Repository 인터페이스
 *
 * Application 계층은 이 인터페이스에만 의존하며, 구현체는 infrastructure에서 제공한다.
 */
public interface CommentRepository {

    Optional<Comment> findById(Long id);

    Comment save(Comment comment);

    void delete(Comment comment);

    List<Comment> findAllByApplicantIdAndStageOrderByCreatedAtAsc(Long applicantId, RecruitmentStage stage);

    List<Comment> findAllByProjectIdAndStageOrderByCreatedAtAsc(Long projectId, RecruitmentStage stage);
}
