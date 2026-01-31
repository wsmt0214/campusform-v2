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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestParam Long ownerId) {
        ProjectResponse response = projectService.createProject(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}