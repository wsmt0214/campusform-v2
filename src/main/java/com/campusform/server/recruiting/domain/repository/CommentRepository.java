package com.campusform.server.recruiting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.projection.ApplicantIdCountRow;

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

    /**
     * 프로젝트 + 단계 기준으로 지원자별 댓글 수 집계
     *
     * - 댓글 엔티티 전체 로딩 없이 GROUP BY 집계 결과만 조회하는 목적
     */
    List<ApplicantIdCountRow> countByProjectIdAndStageGroupByApplicantId(Long projectId, RecruitmentStage stage);

    /**
     * 지원자별 최상위 댓글(parent가 없는 댓글)만 조회. 하위 답글은 삭제 시 cascade로 제거됩니다.
     */
    List<Comment> findRootCommentsByApplicantIds(List<Long> applicantIds);

    /**
     * 해당 작성자가 남긴 댓글·답글을 모두 삭제합니다.
     * 루트 댓글은 기존 단건 삭제와 같이 하위 답글 전체가 함께 제거됩니다.
     */
    void deleteAllWrittenByAuthorId(Long authorId);
}
