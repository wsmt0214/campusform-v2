package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.applicant.value.RecruitmentStage;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.repository.CommentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 도메인 CommentRepository의 JPA 구현체
 */
@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepository {

    private final CommentJpaRepository jpaRepository;

    @Override
    public Optional<Comment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Comment save(Comment comment) {
        return jpaRepository.save(comment);
    }

    @Override
    public void delete(Comment comment) {
        jpaRepository.delete(comment);
    }

    @Override
    public List<Comment> findAllByApplicantIdAndStageOrderByCreatedAtAsc(Long applicantId, RecruitmentStage stage) {
        return jpaRepository.findAllByApplicantIdAndStageOrderByCreatedAtAsc(applicantId, stage);
    }

    @Override
    public List<Comment> findAllByProjectIdAndStageOrderByCreatedAtAsc(Long projectId, RecruitmentStage stage) {
        return jpaRepository.findAllByProjectIdAndStageOrderByCreatedAtAsc(projectId, stage);
    }
}
