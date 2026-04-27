package com.campusform.server.recruiting.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.campusform.server.recruiting.domain.model.interview.availability.IntervieweeAvailabilitySlot;
import com.campusform.server.recruiting.domain.repository.IntervieweeAvailabilitySlotRepository;

import lombok.RequiredArgsConstructor;

/**
 * IntervieweeAvailabilitySlotRepository 구현체
 * 
 * Spring Data JPA에 작업을 위임합니다.
 */
@Repository
@RequiredArgsConstructor
public class IntervieweeAvailabilitySlotRepositoryImpl implements IntervieweeAvailabilitySlotRepository {

    private final IntervieweeAvailabilitySlotJpaRepository jpaRepository;

    @Override
    public void save(IntervieweeAvailabilitySlot slot) {
        jpaRepository.save(slot);
    }

    @Override
    public void saveAll(List<IntervieweeAvailabilitySlot> slots) {
        jpaRepository.saveAll(slots);
    }

    @Override
    public List<IntervieweeAvailabilitySlot> findByApplicantId(Long applicantId) {
        return jpaRepository.findByApplicantId(applicantId);
    }

    @Override
    public void deleteByApplicantId(Long applicantId) {
        jpaRepository.deleteByApplicantId(applicantId);
    }

    @Override
    public List<IntervieweeAvailabilitySlot> findByInterviewDayIdAndStartTime(Long interviewDayId,
            java.time.LocalTime startTime) {
        return jpaRepository.findByInterviewDayIdAndStartTime(interviewDayId, startTime);
    }

    @Override
    public List<IntervieweeAvailabilitySlot> findByInterviewDayId(Long interviewDayId) {
        return jpaRepository.findByInterviewDayId(interviewDayId);
    }

    @Override
    public List<IntervieweeAvailabilitySlot> findByInterviewDayIdIn(List<Long> interviewDayIds) {
        return jpaRepository.findByInterviewDayIdIn(interviewDayIds);
    }

    @Override
    public void deleteAllByInterviewDayIdIn(List<Long> interviewDayIds) {
        if (interviewDayIds == null || interviewDayIds.isEmpty()) {
            return;
        }
        List<IntervieweeAvailabilitySlot> slots = jpaRepository.findByInterviewDayIdIn(interviewDayIds);
        if (!slots.isEmpty()) {
            jpaRepository.deleteAll(slots);
        }
    }
}
