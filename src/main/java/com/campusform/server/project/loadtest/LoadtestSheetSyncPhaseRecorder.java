package com.campusform.server.project.loadtest;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Primary
@Component
@Profile("loadtest")
public class LoadtestSheetSyncPhaseRecorder implements SheetSyncPhaseRecorder {

    @Override
    public void recordPhase(long projectId, int rows, String phase, long elapsedMs) {
        log.warn("[sheet-sync phase] projectId={} rows={} phase={} elapsedMs={}",
                projectId, rows, phase, elapsedMs);
    }
}

