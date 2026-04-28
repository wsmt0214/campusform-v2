package com.campusform.server.project.loadtest;

/**
 * 시트 동기화(loadtest) 진단 로그 기록용 포트
 *
 * 운영 로직과 측정 로직의 결합을 줄이기 위한 목적
 */
public interface SheetSyncDiagnosticsRecorder {

    void recordCounts(
            long projectId,
            int sheetRows,
            int preloadedApplicants,
            int matchedExistingRows,
            int newCandidateRows,
            int updatedCandidateRows,
            int unchangedRows,
            int duplicateSheetRows
    );
}

