package com.campusform.server.project.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.global.event.AdminAddedEvent;
import com.campusform.server.identity.domain.model.User;
import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.application.dto.request.AddAdminRequest;
import com.campusform.server.project.application.dto.request.CreateProjectRequest;
import com.campusform.server.project.application.dto.response.AddAdminResponse;
import com.campusform.server.project.application.dto.response.AdminListResponse;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.exception.TokenNotFoundException;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.recruiting.infrastructure.persistence.ApplicantJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 프로젝트 관련 비즈니스 로직을 처리하는 서비스
 * 
 * 프로젝트 생성, 관리자 검증 등의 핵심 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final SpreadsheetService spreadsheetService;
    private final GoogleOAuthTokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicantJpaRepository applicantJpaRepository;

    /**
     * 프로젝트 생성
     */
    @Transactional
    public ProjectResponse createProject(Long ownerId, CreateProjectRequest request) {
        // 만약 관리자가 없는 경우(null) 빈 리스트로 대체해서 NullPointerException을 방지
        List<Long> adminIds = request.getAdminIds() == null ? List.of() : request.getAdminIds();

        validateCreateProjectRequest(request);

        // 루트 애그리거트 생성
        Project project = Project.create(request.getTitle(), ownerId, request.getSheetUrl(), request.getStartAt(),
                request.getEndAt());

        // 연관관계 설정
        adminIds.stream().distinct().forEach(project::addAdmin);

        // 연관관계 설정
        if (request.getRequiredMappings() != null) {
            // DTO -> 필수 매핑 필드만 추출하여 넘김
            project.addMapping(request.getRequiredMappings().toDomainValue());
        }

        // 부모 엔티티로 한 번에 저장
        projectRepository.save(project);

        // 관리자 추가 알림 이벤트 발행 (중복 제거)
        adminIds.stream().distinct().forEach(adminId -> eventPublisher.publishEvent(new AdminAddedEvent(
                project.getId(), adminId, project.getTitle())));

        // Google OAuth 토큰 확인 (프로젝트 생성 시 필수)
        // getValidToken은 토큰이 만료되었을 때 자동으로 refresh_token으로 갱신함
        if (tokenService.getValidToken(ownerId).isEmpty()) {
            throw new TokenNotFoundException(
                    "Google Sheets 권한이 필요합니다. OAuth 인증을 먼저 진행해주세요. ownerId=" + ownerId);
        }

        // 스프레드시트 초기 동기화 (지원자 데이터 가져오기)
        // 시트 연동 실패 시 프로젝트 생성도 실패하도록 예외 전파
        spreadsheetService.syncSheet(project.getId());

        return ProjectResponse.from(project);
    }

    /**
     * 사용자가 속한 프로젝트 목록 조회 (지원자 수 포함)
     *
     * @param userId 사용자 ID (Owner이거나 Admin인 프로젝트만 조회)
     * @return 프로젝트 목록
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByUserId(Long userId) {
        List<Project> projects = projectRepository.findByUserId(userId);

        return projects.stream()
                .map(project -> {
                    long applicantCount = applicantJpaRepository.countByProjectId(project.getId());
                    return ProjectResponse.from(project, applicantCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 프로젝트 삭제 (OWNER만 가능)
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID (OWNER 권한 확인)
     */
    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // OWNER 권한 검증
        project.validateOwnerAccess(userId);

        // 프로젝트 삭제 (CASCADE로 관련된 ProjectAdmin도 자동 삭제됨)
        projectRepository.delete(project);
    }

    /**
     * 관리자 추가 (OWNER만 가능)
     */
    @Transactional
    public AddAdminResponse addAdmin(Long projectId, Long ownerId, AddAdminRequest request) {
        // 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // OWNER 권한 검증
        project.validateOwnerAccess(ownerId);

        // 이메일로 사용자 존재 여부 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 사용자가 없습니다. email=" + request.getEmail()));

        Long adminId = user.getId();

        // OWNER와 중복 체크
        if (project.getOwnerId().equals(adminId)) {
            throw new IllegalArgumentException("프로젝트 OWNER는 관리자로 추가할 수 없습니다.");
        }

        // 이미 관리자인지 확인 (Project.addAdmin에서도 체크하지만, 명확한 에러 메시지를 위해 먼저 체크)
        if (project.getAdmins().stream().anyMatch(admin -> admin.getAdminId().equals(adminId))) {
            throw new IllegalArgumentException("이미 프로젝트 관리자로 등록된 사용자입니다.");
        }

        // 관리자 추가
        project.addAdmin(adminId);
        projectRepository.save(project);

        return new AddAdminResponse(adminId, user.getNickname(), user.getEmail(), user.getProfileImageUrl());
    }

    /**
     * 관리자 제거 (OWNER만 가능)
     */
    @Transactional
    public void removeAdmin(Long projectId, Long ownerId, Long adminId) {
        // 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // OWNER 권한 검증
        project.validateOwnerAccess(ownerId);

        // 관리자 제거 (도메인 로직에서 OWNER 체크 및 존재 여부 확인)
        project.removeAdmin(adminId);
        projectRepository.save(project);
    }

    /**
     * 관리자 목록 조회 (관리자만 가능)
     * 
     * OWNER는 제외하고 ADMIN만 반환합니다.
     */
    @Transactional(readOnly = true)
    public AdminListResponse getAdmins(Long projectId, Long userId) {
        // 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // 관리자 권한 검증 (OWNER 또는 ADMIN)
        project.validateAdminAccess(userId);

        // ADMIN 목록 조회 및 정보 추가 (OWNER 제외)
        List<AdminListResponse.AdminInfo> adminList = project.getAdmins().stream()
                .map(admin -> {
                    User adminUser = userRepository.findById(admin.getAdminId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "관리자 정보를 찾을 수 없습니다. adminId=" + admin.getAdminId()));
                    return new AdminListResponse.AdminInfo(
                            adminUser.getId(),
                            adminUser.getNickname(),
                            adminUser.getEmail(),
                            adminUser.getProfileImageUrl(),
                            "ADMIN");
                })
                .collect(Collectors.toList());

        return new AdminListResponse(adminList);
    }

    private void validateCreateProjectRequest(CreateProjectRequest request) {
        List<Long> adminIds = request.getAdminIds() == null ? List.of() : request.getAdminIds();
        adminIds.stream()
                .distinct()
                .forEach(adminId -> {
                    if (!userRepository.existsById(adminId)) {
                        throw new IllegalArgumentException("존재하지 않는 회원입니다. adminId=" + adminId);
                    }
                });

        projectRepository.findBySheetUrl(request.getSheetUrl()).ifPresent(project -> {
            throw new IllegalArgumentException("이미 해당하는 Url로 생성된 프로젝트가 존재합니다.");
        });
    }
}
