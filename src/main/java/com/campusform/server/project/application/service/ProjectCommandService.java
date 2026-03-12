package com.campusform.server.project.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.application.dto.request.AddAdminRequest;
import com.campusform.server.project.application.dto.request.CreateProjectRequest;
import com.campusform.server.project.application.dto.request.UpdatePositionValueMappingsRequest;
import com.campusform.server.project.application.dto.request.UpdateProjectNameRequest;
import com.campusform.server.project.application.dto.request.UpdateProjectPeriodRequest;
import com.campusform.server.project.application.dto.response.AddAdminResponse;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.event.AdminAddedEvent;
import com.campusform.server.project.domain.exception.TokenNotFoundException;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.ProjectValueMapping;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 프로젝트 생성/수정/삭제 전용 서비스 (CQRS 패턴)
 * 기존 ProjectService에서 쓰기 책임만 분리해, 생성·수정·삭제·관리자·포지션 매핑 변경을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCommandService {

    private final SpreadsheetService spreadsheetService;
    private final GoogleOAuthTokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectRepository projectRepository;
    private final ApplicantRepository applicantRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public ProjectResponse createProject(Long ownerId, CreateProjectRequest request) {
        List<Long> adminIds = request.getAdminIds() == null ? List.of() : request.getAdminIds();
        validateCreateProjectRequest(request);

        Project project = Project.create(request.getTitle(), ownerId, request.getSheetUrl(),
                request.getStartAt(), request.getEndAt());
        adminIds.stream().distinct().forEach(project::addAdmin);
        if (request.getRequiredMappings() != null) {
            project.addMapping(request.getRequiredMappings().toDomainValue());
        }
        if (request.getValueMappings() != null) {
            request.getValueMappings().forEach(
                    item -> project.addValueMapping(item.getFromValue(), item.getToValue()));
        }

        projectRepository.save(project);

        List<Long> distinctAdminIds = adminIds.stream().distinct().toList();
        if (!distinctAdminIds.isEmpty()) {
            eventPublisher.publishEvent(new AdminAddedEvent(project.getId(), ownerId,
                    distinctAdminIds, project.getTitle()));
        }

        if (tokenService.getValidToken(ownerId).isEmpty()) {
            throw new TokenNotFoundException(
                    "Google Sheets 권한이 필요합니다. OAuth 인증을 먼저 진행해주세요. ownerId=" + ownerId);
        }
        spreadsheetService.syncSheet(project.getId());

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse updateProjectName(Long projectId, Long userId,
            UpdateProjectNameRequest request) {
        Project project = projectAccessService.getProjectWithOwnerAccess(projectId, userId);
        project.updateTitle(request.getTitle());

        long applicantCount = applicantRepository.countByProjectId(project.getId());
        return ProjectResponse.from(project, applicantCount);
    }

    @Transactional
    public ProjectResponse updateProjectPeriod(Long projectId, Long userId,
            UpdateProjectPeriodRequest request) {
        Project project = projectAccessService.getProjectWithAdminAccess(projectId, userId);
        project.updatePeriod(request.getStartAt(), request.getEndAt());

        long applicantCount = applicantRepository.countByProjectId(project.getId());
        return ProjectResponse.from(project, applicantCount);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectAccessService.getProjectWithOwnerAccess(projectId, userId);
        projectRepository.delete(project);
    }

    @Transactional
    public AddAdminResponse addAdmin(Long projectId, Long ownerId, AddAdminRequest request) {
        Project project = projectAccessService.getProjectWithOwnerAccess(projectId, ownerId);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 이메일로 가입된 사용자가 없습니다. email=" + request.getEmail()));
        Long adminId = user.getId();

        if (project.getOwnerId().equals(adminId)) {
            throw new IllegalArgumentException("프로젝트 OWNER는 관리자로 추가할 수 없습니다.");
        }
        if (project.getAdmins().stream().anyMatch(admin -> admin.getAdminId().equals(adminId))) {
            throw new IllegalArgumentException("이미 프로젝트 관리자로 등록된 사용자입니다.");
        }

        project.addAdmin(adminId);
        projectRepository.save(project);
        eventPublisher.publishEvent(new AdminAddedEvent(project.getId(), project.getOwnerId(),
                List.of(adminId), project.getTitle()));

        return new AddAdminResponse(adminId, user.getNickname(), user.getEmail(),
                user.getProfileImageUrl());
    }

    @Transactional
    public void removeAdmin(Long projectId, Long ownerId, Long adminId) {
        Project project = projectAccessService.getProjectWithOwnerAccess(projectId, ownerId);
        project.removeAdmin(adminId);
        projectRepository.save(project);
    }

    @Transactional
    public void updatePositionValueMappings(Long projectId, Long userId,
            UpdatePositionValueMappingsRequest request) {
        Project project = projectAccessService.getProjectWithAdminAccess(projectId, userId);

        if (request.getValueMappings() != null) {
            request.getValueMappings().forEach(item -> project
                    .addOrUpdateValueMapping(item.getFromValue(), item.getToValue()));
        }
        projectRepository.save(project);

        Map<String, String> positionMapping = project.getValueMappings().stream()
                .collect(Collectors.toMap(ProjectValueMapping::getFromValue,
                        ProjectValueMapping::getToValue, (existing, replacement) -> existing));
        List<Applicant> applicants = applicantRepository.findByProjectId(projectId);
        for (Applicant applicant : applicants) {
            if (applicant.getPosition() == null)
                continue;
            String mapped = positionMapping.get(applicant.getPosition().trim());
            if (mapped != null && !mapped.equals(applicant.getPosition())) {
                applicant.updatePosition(mapped);
                applicantRepository.save(applicant);
            }
        }
    }

    private void validateCreateProjectRequest(CreateProjectRequest request) {
        List<Long> adminIds = request.getAdminIds() == null ? List.of() : request.getAdminIds();
        adminIds.stream().distinct().forEach(adminId -> {
            if (!userRepository.existsById(adminId)) {
                throw new IllegalArgumentException("존재하지 않는 회원입니다. adminId=" + adminId);
            }
        });
        projectRepository.findBySheetUrl(request.getSheetUrl()).ifPresent(project -> {
            throw new IllegalArgumentException("이미 해당하는 Url로 생성된 프로젝트가 존재합니다.");
        });
    }
}
