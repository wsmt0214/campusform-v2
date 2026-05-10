package com.campusform.server.recruiting.infrastructure.persistence;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;

import lombok.RequiredArgsConstructor;

/**
 * InterviewerAvailabilityBlockRepository 구현체
 * 
 * Spring Data JPA에 작업을 위임합니다.
 */
@Repository
@RequiredArgsConstructor
public class InterviewerAvailabilityBlockRepositoryImpl implements InterviewerAvailabilityBlockRepository {

    private final InterviewerAvailabilityBlockJpaRepository jpaRepository;

    @Override
    public void save(InterviewerAvailabilityBlock block) {
        jpaRepository.save(block);
    }

    @Override
    public void saveAll(List<InterviewerAvailabilityBlock> blocks) {
        jpaRepository.saveAll(blocks);
    }

    @Override
    public void delete(InterviewerAvailabilityBlock block) {
        jpaRepository.delete(block);
    }

    @Override
    public void deleteAll(List<InterviewerAvailabilityBlock> blocks) {
        jpaRepository.deleteAll(blocks);
    }

    @Override
    public List<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayId(Long adminId, Long interviewDayId) {
        return jpaRepository.findByAdminIdAndInterviewDayId(adminId, interviewDayId);
    }

    @Override
    public List<InterviewerAvailabilityBlock> findByAdminId(Long adminId) {
        return jpaRepository.findByAdminId(adminId);
    }

    @Override
    public List<InterviewerAvailabilityBlock> findByInterviewDayId(Long interviewDayId) {
        return jpaRepository.findByInterviewDayId(interviewDayId);
    }

    @Override
    public Optional<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayIdAndStartTime(
            Long adminId, Long interviewDayId, LocalTime startTime) {
        return jpaRepository.findByAdminIdAndInterviewDayIdAndStartTime(adminId, interviewDayId, startTime);
    }

    @Override
    public List<InterviewerAvailabilityBlock> findByAdminIdAndInterviewDayIdIn(Long adminId,
            List<Long> interviewDayIds) {
        return jpaRepository.findByAdminIdAndInterviewDayIdIn(adminId, interviewDayIds);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public List<InterviewerAvailabilityBlock> findByInterviewDayIdIn(List<Long> interviewDayIds) {
        return jpaRepository.findByInterviewDayIdIn(interviewDayIds);
    }

    @Override
    public void deleteByInterviewDayIdIn(List<Long> interviewDayIds) {
        if (interviewDayIds == null || interviewDayIds.isEmpty()) {
            return;
        }
        jpaRepository.deleteByInterviewDayIdIn(interviewDayIds);
    }
}
