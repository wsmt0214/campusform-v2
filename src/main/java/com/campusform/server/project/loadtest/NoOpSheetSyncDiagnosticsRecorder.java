package com.campusform.server.project.loadtest;

import org.springframework.stereotype.Component;

/**
 * 기본 구현체
 *
 * loadtest가 아닌 환경에서 측정 로깅을 비활성화하기 위한 목적
 */
@Component
public class NoOpSheetSyncDiagnosticsRecorder implements SheetSyncDiagnosticsRecorder {
    @Override
    public void recordCounts(
            long projectId,
            int sheetRows,
            int preloadedApplicants,
            int matchedExistingRows,
            int newCandidateRows,
            int updatedCandidateRows,
            int unchangedRows,
            int duplicateSheetRows
    ) {
        // no-op
    }
}
