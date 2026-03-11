package com.campusform.server.project.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.application.dto.response.AdminListResponse;
import com.campusform.server.project.application.dto.response.PositionValuesResponse;
import com.campusform.server.project.application.dto.response.ProjectDetailExportResponse;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.ProjectAdmin;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 조회 전용 서비스 (CQRS 패턴)
 * 기존 ProjectService에서 읽기 책임만 분리해, 목록/상세/관리자/포지션값 조회를 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projectRepository;
    private final ApplicantRepository applicantRepository;
    private final UserRepository userRepository;

    /**
     * 사용자가 속한 프로젝트 목록 조회 (지원자 수 포함)
     */
    public List<ProjectResponse> getProjectsByUserId(Long userId) {
        List<Project> projects = projectRepository.findByUserId(userId);
        return projects.stream().map(project -> {
            long applicantCount = applicantRepository.countByProjectId(project.getId());
            return ProjectResponse.from(project, applicantCount);
        }).collect(Collectors.toList());
    }

    /**
     * 관리자 목록 조회 (OWNER + ADMIN, OWNER는 owner 필드로)
     */
    public AdminListResponse getAdmins(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));
        project.validateAdminAccess(userId);

        User ownerUser = userRepository.findById(project.getOwnerId())
                .orElseThrow(() -> new IllegalStateException(
                        "프로젝트 소유자 정보를 찾을 수 없습니다. ownerId=" + project.getOwnerId()));
        AdminListResponse.AdminInfo ownerInfo =
                new AdminListResponse.AdminInfo(ownerUser.getId(), ownerUser.getNickname(),
                        ownerUser.getEmail(), ownerUser.getProfileImageUrl(), "OWNER");

        List<AdminListResponse.AdminInfo> adminList = project.getAdmins().stream().map(admin -> {
            User adminUser = userRepository.findById(admin.getAdminId())
                    .orElseThrow(() -> new IllegalStateException(
                            "관리자 정보를 찾을 수 없습니다. adminId=" + admin.getAdminId()));
            return new AdminListResponse.AdminInfo(adminUser.getId(), adminUser.getNickname(),
                    adminUser.getEmail(), adminUser.getProfileImageUrl(), "ADMIN");
        }).collect(Collectors.toList());

        return new AdminListResponse(ownerInfo, adminList);
    }

    /**
     * 프로젝트 상세 정보 내보내기 (관리자만 가능)
     */
    public ProjectDetailExportResponse getProjectDetailForExport(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));
        project.validateAdminAccess(userId);

        long applicantCount = applicantRepository.countByProjectId(project.getId());

        List<ProjectDetailExportResponse.AdminExport> admins = new ArrayList<>();
        User owner = userRepository.findById(project.getOwnerId())
                .orElseThrow(() -> new IllegalStateException(
                        "소유자 정보를 찾을 수 없습니다. ownerId=" + project.getOwnerId()));
        admins.add(new ProjectDetailExportResponse.AdminExport(owner.getId(), owner.getNickname(),
                owner.getEmail(), "OWNER"));
        for (ProjectAdmin admin : project.getAdmins()) {
            User adminUser = userRepository.findById(admin.getAdminId())
                    .orElseThrow(() -> new IllegalStateException(
                            "관리자 정보를 찾을 수 없습니다. adminId=" + admin.getAdminId()));
            admins.add(new ProjectDetailExportResponse.AdminExport(adminUser.getId(),
                    adminUser.getNickname(), adminUser.getEmail(), "ADMIN"));
        }

        return ProjectDetailExportResponse.from(project, applicantCount, admins);
    }

    /**
     * 프로젝트 지원자의 position 컬럼 고유값 종류 조회
     */
    public PositionValuesResponse getStoredPositionValues(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));
        project.validateAdminAccess(userId);

        List<String> values = applicantRepository.findDistinctPositionValuesByProjectId(projectId);
        return PositionValuesResponse.from(values);
    }
}
