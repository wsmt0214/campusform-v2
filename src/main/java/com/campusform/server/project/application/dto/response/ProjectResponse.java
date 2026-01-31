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
    private String sheetSyncStatus;
    private LocalDateTime lastSyncedAt;
    private LocalDate startAt;
    private LocalDate endAt;
    private List<Long> admins;
    private LocalDateTime createdAt;

    public static ProjectResponse from(Project project) {
        // lastSyncStatus가 null일 수 있으므로 안전하게 처리
        String syncStatus = project.getLastSyncStatus() != null
                ? project.getLastSyncStatus().name()
                : "NOT_SYNCED";

        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getOwnerId(),
                project.getState().name(),
                project.getSheetUrl(),
                syncStatus,
                project.getLastSyncedAt(),
                project.getStartAt(),
                project.getEndAt(),
                project.getAdmins().stream().map(i -> i.getAdminId()).collect(Collectors.toList()),
                project.getCreatedAt());
    }
}
