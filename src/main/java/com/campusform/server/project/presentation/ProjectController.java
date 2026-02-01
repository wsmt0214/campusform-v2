package com.campusform.server.project.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}