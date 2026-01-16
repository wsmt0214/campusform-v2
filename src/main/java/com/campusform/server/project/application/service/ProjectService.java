package com.campusform.server.project.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.identity.domain.repository.UserRepository;
import com.campusform.server.project.application.dto.request.CreateProjectRequest;
import com.campusform.server.project.application.dto.response.ProjectResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 관련 비즈니스 로직을 처리하는 서비스
 * 
 * 프로젝트 생성, 관리자 검증 등의 핵심 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final SpreadsheetService spreadsheetService;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

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

        // 스프레드시트 초기 동기화 (지원자 데이터 가져오기)
        spreadsheetService.syncInitialApplicants(request.getSheetUrl());

        // TODO: google_oauth_tokens에 토큰 저장 또는 가입 시 토큰 저장

        return ProjectResponse.from(project);
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
