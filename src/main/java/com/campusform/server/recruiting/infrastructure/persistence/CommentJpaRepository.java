package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.comment.Comment;

/**
 * JPA 기반 댓글 Repository (Infrastructure 계층)
 * 도메인 CommentRepository 구현체에서 위임용으로 사용한다.
 */
public interface CommentJpaRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByAuthorIdAndParentIsNull(Long authorId);

    List<Comment> findByAuthorId(Long authorId);

    boolean existsByAuthorId(Long authorId);

    boolean existsByParent_Id(Long parentId);

    List<Comment> findByApplicantIdInAndParentIsNull(List<Long> applicantIds);

    List<Comment> findAllByApplicantIdAndStageOrderByCreatedAtAsc(Long applicantId, RecruitmentStage stage);

    @Query("SELECT c FROM Comment c " +
            "JOIN Applicant a ON c.applicantId = a.id " +
            "WHERE a.projectId = :projectId AND c.stage = :stage " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findAllByProjectIdAndStageOrderByCreatedAtAsc(@Param("projectId") Long projectId,
            @Param("stage") RecruitmentStage stage);
}
