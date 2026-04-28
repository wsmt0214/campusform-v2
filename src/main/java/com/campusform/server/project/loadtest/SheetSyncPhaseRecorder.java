package com.campusform.server.project.loadtest;

public interface SheetSyncPhaseRecorder {

    void recordPhase(long projectId, int rows, String phase, long elapsedMs);
}

