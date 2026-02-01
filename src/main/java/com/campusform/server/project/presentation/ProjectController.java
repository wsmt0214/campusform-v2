package com.campusform.server.project.presentation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusform.server.identity.application.service.AuthService;
import com.campusform.server.project.application.dto.request.CreateProjectRequest;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.application.service.GoogleOAuthTokenService;
import com.campusform.server.project.application.service.ProjectService;
import com.campusform.server.project.application.service.SpreadsheetService;
import com.campusform.server.project.domain.repository.ProjectRepository;

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

    private final ProjectService projectService;
    private final SpreadsheetService spreadsheetService;
    private final ProjectRepository projectRepository;
    private final AuthService authService;
    private final GoogleOAuthTokenService tokenService;

    /**
     * 사용자가 속한 프로젝트 목록 조회
     */
    @Operation(summary = "프로젝트 목록 조회", description = "로그인한 사용자가 Owner이거나 Admin인 프로젝트 목록을 조회합니다. 각 프로젝트의 지원자 수가 포함됩니다. (현재는 userId를 직접 받지만, 인증 로직 구현 후에는 토큰에서 추출)")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @Parameter(description = "사용자 ID (테스트용)", required = true) @RequestParam Long userId) {
        List<ProjectResponse> projects = projectService.getProjectsByUserId(userId);
        return ResponseEntity.ok(projects);
    }

    /**
     * 프로젝트 생성
     */
    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다. (현재는 ownerId를 직접 받지만, 인증 로직 구현 후에는 토큰에서 추출)")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @Parameter(description = "프로젝트 소유자 ID (테스트용)", required = true) @RequestParam Long ownerId) {
        ProjectResponse response = projectService.createProject(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 프로젝트 삭제 (OWNER만 가능)
     */
    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제합니다. OWNER만 삭제 가능하며, 관련된 Admin도 접근할 수 없게 됩니다. (현재는 userId를 직접 받지만, 인증 로직 구현 후에는 토큰에서 추출)")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Parameter(description = "사용자 ID (테스트용)", required = true) @RequestParam Long userId) {
        projectService.deleteProject(projectId, userId);
        return ResponseEntity.noContent().build();
    }
}