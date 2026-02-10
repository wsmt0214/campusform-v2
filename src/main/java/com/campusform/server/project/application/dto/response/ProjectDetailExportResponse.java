package com.campusform.server.project.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.value.SyncStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 상세 정보 내보내기용 응답 DTO
 */
@Schema(description = "프로젝트 상세 정보 내보내기 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailExportResponse {

    @Schema(description = "프로젝트 ID", example = "1")
    private Long id;
    @Schema(description = "프로젝트 제목", example = "2024년 1학기 신입 부원 모집")
    private String title;
    @Schema(description = "소유자(OWNER) 사용자 ID", example = "1")
    private Long ownerId;
    @Schema(description = "프로젝트 진행 상태", example = "DOCUMENT")
    private String state;
    @Schema(description = "연동된 Google Sheet URL")
    private String sheetUrl;
    @Schema(description = "시트 동기화 상태", example = "OK")
    private String sheetSyncStatus;
    @Schema(description = "마지막 동기화 시각")
    private LocalDateTime lastSyncedAt;
    @Schema(description = "모집 시작일", example = "2024-03-01")
    private LocalDate startAt;
    @Schema(description = "모집 종료일", example = "2024-03-15")
    private LocalDate endAt;
    @Schema(description = "프로젝트 생성 시각")
    private LocalDateTime createdAt;
    @Schema(description = "현재 지원자 수", example = "42")
    private Long applicantCount;

    @Schema(description = "관리자 목록 (OWNER + ADMIN)")
    private List<AdminExport> admins;

    @Schema(description = "시트 컬럼 매핑 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequiredMappingExport {
        private Integer nameIdx;
        private Integer schoolIdx;
        private Integer majorIdx;
        private Integer genderIdx;
        private Integer phoneIdx;
        private Integer emailIdx;
        private Integer positionIdx;
    }

    @Schema(description = "포지션 값 치환 규칙 한 건")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValueMappingExport {
        private String fromValue;
        private String toValue;
    }

    @Schema(description = "관리자 정보 (내보내기용)")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminExport {
        private Long adminId;
        private String adminName;
        private String email;
        @Schema(description = "OWNER 또는 ADMIN")
        private String role;
    }

    /**
     * Project 엔티티와 지원자 수, 관리자 목록으로부터 내보내기 DTO 생성
     */
    public static ProjectDetailExportResponse from(
            Project project,
            long applicantCount,
            List<AdminExport> admins) {

        String syncStatus = project.getLastSyncStatus() != null
                ? project.getLastSyncStatus().name()
                : SyncStatus.OK.name();

        return new ProjectDetailExportResponse(
                project.getId(),
                project.getTitle(),
                project.getOwnerId(),
                project.getState().name(),
                project.getSheetUrl(),
                syncStatus,
                project.getLastSyncedAt(),
                project.getStartAt(),
                project.getEndAt(),
                project.getCreatedAt(),
                applicantCount,
                admins);
    }
}
