package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.recruiting.domain.model.interview.schedule.InterviewScheduleUnassignedApplicant;
import com.campusform.server.recruiting.domain.repository.InterviewScheduleUnassignedApplicantRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InterviewScheduleUnassignedApplicantRepositoryImpl
        implements InterviewScheduleUnassignedApplicantRepository {

    private final InterviewScheduleUnassignedApplicantJpaRepository jpaRepository;

    @Override
    public void save(InterviewScheduleUnassignedApplicant entity) {
        jpaRepository.save(entity);
    }

    @Override
    public void saveAll(List<InterviewScheduleUnassignedApplicant> entities) {
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<InterviewScheduleUnassignedApplicant> findByProjectId(Long projectId) {
        return jpaRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional
    public void deleteByProjectId(Long projectId) {
        jpaRepository.deleteByProjectId(projectId);
    }
}
