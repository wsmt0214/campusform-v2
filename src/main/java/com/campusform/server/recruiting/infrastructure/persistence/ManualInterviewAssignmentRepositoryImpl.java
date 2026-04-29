package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.domain.model.interview.schedule.ManualInterviewAssignment;
import com.campusform.server.recruiting.domain.repository.ManualInterviewAssignmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 수동 면접 배정 Repository 구현체
 * 
 * JPA를 사용하여 데이터 영속성을 처리합니다.
 */
@Repository
@RequiredArgsConstructor
public class ManualInterviewAssignmentRepositoryImpl
        implements ManualInterviewAssignmentRepository {

    private final ManualInterviewAssignmentJpaRepository jpaRepository;

    @Override
    public ManualInterviewAssignment save(ManualInterviewAssignment assignment) {
        return jpaRepository.save(assignment);
    }

    @Override
    public void saveAll(List<ManualInterviewAssignment> assignments) {
        jpaRepository.saveAll(assignments);
    }

    @Override
    public List<ManualInterviewAssignment> findByProjectId(Long projectId) {
        return jpaRepository.findByProjectId(projectId);
    }

    @Override
    public List<ManualInterviewAssignment> findByProjectIdAndApplicantIds(Long projectId, List<Long> applicantIds) {
        if (applicantIds == null || applicantIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByProjectIdAndApplicantIdIn(projectId, applicantIds);
    }

    @Override
    public Optional<ManualInterviewAssignment> findByApplicantId(Long applicantId) {
        return jpaRepository.findByApplicantId(applicantId);
    }

    @Override
    public Optional<ManualInterviewAssignment> findByProjectIdAndApplicantId(Long projectId, Long applicantId) {
        return jpaRepository.findByProjectIdAndApplicantId(projectId, applicantId);
    }

    @Override
    @Transactional
    public void delete(ManualInterviewAssignment assignment) {
        jpaRepository.delete(assignment);
    }

    @Override
    @Transactional
    public void deleteByProjectId(Long projectId) {
        jpaRepository.deleteByProjectId(projectId);
    }
}
