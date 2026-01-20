package com.campusform.server.project.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.campusform.server.project.domain.model.setting.Project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 응답 DTO
 * 클라이언트에게 반환할 프로젝트 정보를 담는 객체입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String title;
    private Long ownerId;
    private String state;
    private String sheetUrl;
    private LocalDate startAt;
    private LocalDate endAt;
    private List<Long> admins;
    private LocalDateTime createdAt;

    /**
     * Project 엔티티를 ProjectResponse로 변환
     * 엔티티를 직접 노출하지 않고 DTO로 변환하여 반환합니다.
     * 
     * @param project Project 엔티티
     * @return ProjectResponse DTO
     */
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getOwnerId(),
                project.getState().name(),
                project.getSheetUrl(),
                project.getStartAt(),
                project.getEndAt(),
                project.getAdmins().stream().map(i -> i.getAdminId()).collect(Collectors.toList()),
                project.getCreatedAt());
    }
}
