package com.campusform.server.project.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.campusform.server.notification.domain.repository.NotificationRepository;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.comment.Comment;
import com.campusform.server.recruiting.domain.model.interview.availability.InterviewerAvailabilityBlock;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewDay;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import com.campusform.server.recruiting.domain.repository.CommentRepository;
import com.campusform.server.recruiting.domain.repository.InterviewScheduleUnassignedApplicantRepository;
import com.campusform.server.recruiting.domain.repository.InterviewScheduledSlotRepository;
import com.campusform.server.recruiting.domain.repository.InterviewSettingRepository;
import com.campusform.server.recruiting.domain.repository.IntervieweeAvailabilitySlotRepository;
import com.campusform.server.recruiting.domain.repository.InterviewerAvailabilityBlockRepository;
import com.campusform.server.recruiting.domain.repository.ManualInterviewAssignmentRepository;
import com.campusform.server.recruiting.domain.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 프로젝트 엔티티 삭제 전에 모집/알림 등 연관 데이터를 제거합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectRelatedDataDeletionService {

    private final InterviewScheduledSlotRepository scheduledSlotRepository;
    private final InterviewScheduleUnassignedApplicantRepository unassignedApplicantRepository;
    private final ManualInterviewAssignmentRepository manualInterviewAssignmentRepository;
    private final InterviewSettingRepository interviewSettingRepository;
    private final InterviewerAvailabilityBlockRepository interviewerAvailabilityBlockRepository;
    private final IntervieweeAvailabilitySlotRepository intervieweeAvailabilitySlotRepository;
    private final CommentRepository commentRepository;
    private final ApplicantRepository applicantRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final NotificationRepository notificationRepository;

    public void deleteAllForProject(Long projectId) {
        scheduledSlotRepository.deleteByProjectId(projectId);
        unassignedApplicantRepository.deleteByProjectId(projectId);
        manualInterviewAssignmentRepository.deleteByProjectId(projectId);

        interviewSettingRepository.findByProjectId(projectId).ifPresent(setting -> {
            List<Long> dayIds = setting.getDays().stream()
                    .map(InterviewDay::getId)
                    .toList();
            if (!dayIds.isEmpty()) {
                List<InterviewerAvailabilityBlock> blocks =
                        interviewerAvailabilityBlockRepository.findByInterviewDayIdIn(dayIds);
                if (!blocks.isEmpty()) {
                    interviewerAvailabilityBlockRepository.deleteAll(blocks);
                }
                intervieweeAvailabilitySlotRepository.deleteAllByInterviewDayIdIn(dayIds);
            }
            interviewSettingRepository.delete(setting);
        });

        List<Applicant> applicants = applicantRepository.findByProjectId(projectId);
        List<Long> applicantIds = applicants.stream().map(Applicant::getId).toList();
        if (!applicantIds.isEmpty()) {
            List<Comment> roots = commentRepository.findRootCommentsByApplicantIds(applicantIds);
            for (Comment root : roots) {
                commentRepository.delete(root);
            }
        }
        applicantRepository.deleteAllByProjectId(projectId);

        messageTemplateRepository.deleteByProjectId(projectId);
        notificationRepository.deleteByProjectId(projectId);

        log.debug("프로젝트 연관 데이터 삭제 완료 projectId={}", projectId);
    }
}
