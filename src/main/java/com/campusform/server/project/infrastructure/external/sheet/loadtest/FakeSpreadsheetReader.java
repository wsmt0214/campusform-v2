package com.campusform.server.project.infrastructure.external.sheet.loadtest;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.campusform.server.global.loadtest.SheetSyncLoadtestDataset;
import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;
import com.campusform.server.project.domain.service.SpreadsheetReader;

import lombok.RequiredArgsConstructor;

/**
 * loadtest 전용 SpreadsheetReader 구현체
 *
 * Google Sheets API 네트워크 영향을 제거하고 DB 동기화 로직만 측정하기 위한 목적
 */
@Primary
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class FakeSpreadsheetReader implements SpreadsheetReader {

    private final SheetSyncLoadtestDataset dataset;

    @Override
    public List<SpreadsheetColumn> readHeader(String sheetUrl, Long ownerId) {
        validateSheetUrl(sheetUrl);
        return dataset.generateHeaders();
    }

    @Override
    public List<String[]> readAllLines(String sheetUrl, Long ownerId) {
        validateSheetUrl(sheetUrl);
        return dataset.generateRows().all().stream()
                .map(row -> row.toSheetColumns(datasetPropsExtraColumns()))
                .toList();
    }

    private int datasetPropsExtraColumns() {
        return dataset.generateHeaders().size() - SheetSyncLoadtestDataset.BASE_COLUMNS;
    }

    private void validateSheetUrl(String sheetUrl) {
        if (!SheetSyncLoadtestDataset.LOADTEST_SHEET_URL.equals(sheetUrl)) {
            throw new IllegalArgumentException("loadtest sheetUrl 불일치, expected=" + SheetSyncLoadtestDataset.LOADTEST_SHEET_URL
                    + ", actual=" + sheetUrl);
        }
    }
}

