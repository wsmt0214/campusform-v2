package com.campusform.server.recruiting.infrastructure.persistence;

import com.campusform.server.recruiting.domain.model.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 필요한 경우 특정 지원자의 댓글 목록 조회 메서드 추가
    // List<Comment> findAllByApplicantIdOrderByCreatedAtAsc(Long applicantId);
}
