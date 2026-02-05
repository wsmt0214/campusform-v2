package com.campusform.server.recruiting.infrastructure.persistence;

import com.campusform.server.recruiting.domain.model.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    /**
     * 특정 지원자의 루트 댓글 목록 조회 (부모가 null인 댓글만)
     * 생성일시 오름차순 정렬
     */
    @Query("SELECT c FROM Comment c WHERE c.applicantId = :applicantId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findRootCommentsByApplicantId(@Param("applicantId") Long applicantId);

    /**
     * 특정 지원자의 모든 댓글 조회 (대댓글 포함)
     * 생성일시 오름차순 정렬
     */
    List<Comment> findAllByApplicantIdOrderByCreatedAtAsc(Long applicantId);

    /**
     * 프로젝트의 모든 지원자에 대한 댓글 조회 (대댓글 포함)
     * 생성일시 오름차순 정렬
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN Applicant a ON c.applicantId = a.id " +
           "WHERE a.projectId = :projectId " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findAllByProjectIdOrderByCreatedAtAsc(@Param("projectId") Long projectId);
}
