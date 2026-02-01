package com.campusform.server.project.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.campusform.server.project.domain.model.setting.Project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 응답 DTO
 */
@Schema(description = "프로젝트 정보 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    @Schema(description = "프로젝트 ID", example = "1")
    private Long id;
    @Schema(description = "프로젝트 제목", example = "2024년 1학기 신입 부원 모집")
    private String title;
    @Schema(description = "프로젝트 소유자 ID", example = "1")
    private Long ownerId;
    @Schema(description = "프로젝트 진행 상태", example = "DOCUMENT_OPEN")
    private String state;
    @Schema(description = "연동된 Google Sheet URL", example = "https://docs.google.com/spreadsheets/d/...")
    private String sheetUrl;
    @Schema(description = "시트 동기화 상태", example = "NOT_SYNCED")
    private String sheetSyncStatus;
    @Schema(description = "마지막 동기화 시각")
    private LocalDateTime lastSyncedAt;
    @Schema(description = "모집 시작일", example = "2024-03-01")
    private LocalDate startAt;
    @Schema(description = "모집 종료일", example = "2024-03-15")
    private LocalDate endAt;
    @Schema(description = "관리자(운영진)의 사용자 ID 목록", example = "[1, 2, 3]")
    private List<Long> admins;
    @Schema(description = "프로젝트 생성 시각")
    private LocalDateTime createdAt;
    @Schema(description = "현재 지원자 수", example = "42")
    private Long applicantCount;

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
                project.getCreatedAt(),
                null);
    }

    public static ProjectResponse from(Project project, long applicantCount) {
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
                project.getCreatedAt(),
                applicantCount);
    }
}
