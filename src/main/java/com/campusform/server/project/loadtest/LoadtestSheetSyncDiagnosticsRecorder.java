package com.campusform.server.project.loadtest;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * loadtest 전용 진단 로거
 *
 * Step 1 측정 조건 검증을 위한 카운트 로그 출력 목적
 */
@Slf4j
@Primary
@Component
@Profile("loadtest")
public class LoadtestSheetSyncDiagnosticsRecorder implements SheetSyncDiagnosticsRecorder {

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
        log.warn("[sheet-sync counts] projectId={} sheetRows={} preloadedApplicants={} matchedExistingRows={} newCandidateRows={} updatedCandidateRows={} unchangedRows={} duplicateSheetRows={}",
                projectId, sheetRows, preloadedApplicants, matchedExistingRows, newCandidateRows, updatedCandidateRows, unchangedRows, duplicateSheetRows);
    }
}

