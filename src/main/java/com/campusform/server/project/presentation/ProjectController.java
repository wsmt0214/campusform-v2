package com.campusform.server.project.presentation;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.campusform.server.global.security.CurrentUserId;
import com.campusform.server.project.application.dto.request.AddAdminRequest;
import com.campusform.server.project.application.dto.request.CreateProjectRequest;
import com.campusform.server.project.application.dto.request.UpdatePositionValueMappingsRequest;
import com.campusform.server.project.application.dto.request.UpdateProjectNameRequest;
import com.campusform.server.project.application.dto.request.UpdateProjectPeriodRequest;
import com.campusform.server.project.application.dto.response.AddAdminResponse;
import com.campusform.server.project.application.dto.response.AdminListResponse;
import com.campusform.server.project.application.dto.response.PositionValuesResponse;
import com.campusform.server.project.application.dto.response.ProjectDetailExportResponse;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.application.service.ProjectCommandService;
import com.campusform.server.project.application.service.ProjectQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "프로젝트", description = "프로젝트 생성, 조회, 수정, 삭제 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectQueryService projectQueryService;
    private final ProjectCommandService projectCommandService;

    /**
     * 사용자가 속한 프로젝트 목록 조회
     */
    @Operation(summary = "프로젝트 목록 조회", description = "로그인한 사용자가 Owner이거나 Admin인 프로젝트 목록을 조회합니다. 각 프로젝트의 지원자 수가 포함됩니다.")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(@CurrentUserId Long userId) {
        List<ProjectResponse> projects = projectQueryService.getProjectsByUserId(userId);
        return ResponseEntity.ok(projects);
    }

    /**
     * 프로젝트 생성
     */
    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다. 로그인 사용자가 소유자가 됩니다. (Google OAuth 연동 필수)")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @CurrentUserId Long userId) {
        ProjectResponse response = projectCommandService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 프로젝트 이름(제목) 수정 (OWNER만 가능)
     */
    @Operation(summary = "프로젝트 이름 수정", description = "프로젝트의 제목(이름)을 수정합니다. OWNER만 수정 가능합니다.")
    @PatchMapping("/{projectId}/name")
    public ResponseEntity<ProjectResponse> updateProjectName(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectNameRequest request,
            @CurrentUserId Long userId) {
        ProjectResponse response = projectCommandService.updateProjectName(projectId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 프로젝트 모집 기간(시작일·종료일) 수정 (OWNER만 가능)
     */
    @Operation(summary = "프로젝트 모집 기간 수정", description = "프로젝트의 모집 시작일과 종료일을 수정합니다. OWNER만 수정 가능합니다.")
    @PatchMapping("/{projectId}/period")
    public ResponseEntity<ProjectResponse> updateProjectPeriod(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectPeriodRequest request,
            @CurrentUserId Long userId) {
        ProjectResponse response = projectCommandService.updateProjectPeriod(projectId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DB에 저장된 포지션 값(표시값) 종류 조회 (관리자만 가능)
     */
    @Operation(summary = "저장된 포지션 값 조회", description = "프로젝트에 저장된 포지션 값의 종류를 조회합니다.")
    @GetMapping("/{projectId}/position-values")
    public ResponseEntity<PositionValuesResponse> getStoredPositionValues(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        PositionValuesResponse response = projectQueryService.getStoredPositionValues(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 포지션 값 치환 규칙 설정 (관리자만 가능, 전체 교체)
     */
    @Operation(summary = "포지션 값 치환 규칙 설정", description = "프로젝트의 포지션 값 치환 규칙을 전체 교체합니다. 시트 원시값(fromValue) → 표시값(toValue) 목록을 보냅니다.")
    @PutMapping("/{projectId}/position-values")
    public ResponseEntity<Void> updatePositionValueMappings(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Valid @RequestBody UpdatePositionValueMappingsRequest request,
            @CurrentUserId Long userId) {

        projectCommandService.updatePositionValueMappings(projectId, userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 프로젝트 삭제 (OWNER만 가능)
     */
    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제합니다. OWNER만 삭제 가능하며, 관련된 Admin도 접근할 수 없게 됩니다. (로그인 사용자 기준)")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        projectCommandService.deleteProject(projectId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 프로젝트 상세 정보 내보내기 (관리자만 가능)
     */
    @Operation(summary = "프로젝트 상세 정보 내보내기", description = "프로젝트 상세 정보를를 JSON으로 반환합니다.")
    @GetMapping("/{projectId}/export")
    public ResponseEntity<ProjectDetailExportResponse> exportProjectDetail(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        ProjectDetailExportResponse response = projectQueryService.getProjectDetailForExport(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 목록 조회 (관리자만 가능)
     */
    @Operation(summary = "관리자 목록 조회", description = "프로젝트의 관리자(ADMIN) 목록을 조회합니다. OWNER와 ADMIN 모두 조회 가능하며, OWNER는 제외하고 ADMIN만 반환합니다.")
    @GetMapping("/{projectId}/admins")
    public ResponseEntity<AdminListResponse> getAdmins(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @CurrentUserId Long userId) {
        AdminListResponse response = projectQueryService.getAdmins(projectId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 추가 (OWNER만 가능)
     */
    @Operation(summary = "관리자 추가", description = "프로젝트에 새로운 관리자를 추가합니다. OWNER만 추가 가능하며, 이메일로 가입된 사용자만 추가할 수 있습니다.")
    @PostMapping("/{projectId}/admins")
    public ResponseEntity<AddAdminResponse> addAdmin(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Valid @RequestBody AddAdminRequest request,
            @CurrentUserId Long userId) {
        AddAdminResponse response = projectCommandService.addAdmin(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 관리자 제거 (OWNER만 가능)
     */
    @Operation(summary = "관리자 제거", description = "프로젝트에서 관리자를 제거합니다. OWNER만 제거 가능하며, OWNER 자신은 제거할 수 없습니다.")
    @DeleteMapping("/{projectId}/admins/{adminId}")
    public ResponseEntity<Void> removeAdmin(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Parameter(description = "제거할 관리자 ID", required = true) @PathVariable Long adminId,
            @CurrentUserId Long userId) {
        projectCommandService.removeAdmin(projectId, userId, adminId);
        return ResponseEntity.noContent().build();
    }
}
