package com.campusform.server.project.loadtest;

import org.springframework.stereotype.Component;

@Component
public class NoOpSheetSyncPhaseRecorder implements SheetSyncPhaseRecorder {
    @Override
    public void recordPhase(long projectId, int rows, String phase, long elapsedMs) {
        // no-op
    }
}

