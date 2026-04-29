package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduledSlot;
import com.campusform.server.recruiting.domain.repository.InterviewScheduledSlotRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InterviewScheduledSlotRepositoryImpl implements InterviewScheduledSlotRepository {

    private final InterviewScheduledSlotJpaRepository jpaRepository;

    @Override
    public InterviewScheduledSlot save(InterviewScheduledSlot slot) {
        return jpaRepository.save(slot);
    }

    @Override
    public void saveAll(List<InterviewScheduledSlot> slots) {
        jpaRepository.saveAll(slots);
    }

    @Override
    public List<InterviewScheduledSlot> findByProjectId(Long projectId) {
        return jpaRepository.findByProjectId(projectId);
    }

    @Override
    public List<InterviewScheduledSlot> findByProjectIdWithApplicants(Long projectId) {
        return jpaRepository.findByProjectIdWithApplicants(projectId);
    }

    @Override
    public List<InterviewScheduledSlot> findByProjectIdWithApplicantsFiltered(Long projectId, List<Long> applicantIds) {
        if (applicantIds == null || applicantIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByProjectIdWithApplicantsFiltered(projectId, applicantIds);
    }

    /**
     * 프로젝트의 배정 슬롯 전체 삭제.
     * 자식(applicants, interviewers)이 있어 FK 제약이 있으므로 엔티티 조회 후 deleteAll로 삭제해 cascade로 자식 먼저 삭제되게 함.
     */
    @Override
    @Transactional
    public void deleteByProjectId(Long projectId) {
        List<InterviewScheduledSlot> slots = jpaRepository.findByProjectId(projectId);
        if (!slots.isEmpty()) {
            jpaRepository.deleteAll(slots);
        }
    }
}
