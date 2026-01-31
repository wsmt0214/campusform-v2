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
    @Transactional
    public void deleteByProjectId(Long projectId) {
        jpaRepository.deleteByProjectId(projectId);
    }
}
