package com.campusform.server.project.infrastructure.external.sheet;

import java.io.IOException;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Sheets 메타데이터를 조회하는 서비스
 * 
 * 스프레드시트의 시트 목록과 시트 이름을 조회합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class GoogleSheetsMetadataService {

    private final Sheets sheetsService;

    /**
     * 스프레드시트의 메타데이터를 조회합니다.
     * 
     * @param spreadsheetId 스프레드시트 ID
     * @return Spreadsheet 메타데이터
     * @throws IOException API 호출 실패 시
     */
    private Spreadsheet getSpreadsheetMetadata(String spreadsheetId) throws IOException {
        return sheetsService.spreadsheets()
                .get(spreadsheetId)
                .execute();
    }

    /**
     * gid에 해당하는 시트 이름을 조회합니다.
     * 
     * @param spreadsheetId 스프레드시트 ID
     * @param gid           시트 ID (gid), null이면 첫 번째 시트 사용
     * @return 시트 이름
     * @throws IOException              API 호출 실패 시
     * @throws IllegalArgumentException gid에 해당하는 시트를 찾을 수 없는 경우
     */
    public String getSheetName(String spreadsheetId, Integer gid) throws IOException {
        Spreadsheet spreadsheet = getSpreadsheetMetadata(spreadsheetId);

        if (spreadsheet.getSheets() == null || spreadsheet.getSheets().isEmpty()) {
            throw new IllegalArgumentException("스프레드시트에 시트가 없습니다.");
        }

        logSheetMetadata(spreadsheet, gid);

        if (gid != null) {
            return findSheetByName(spreadsheet, gid);
        }

        return getFirstSheetName(spreadsheet);
    }

    /**
     * gid에 해당하는 시트 이름을 찾습니다.
     */
    private String findSheetByName(Spreadsheet spreadsheet, Integer gid) {
        return spreadsheet.getSheets().stream()
                .filter(sheet -> {
                    Integer sheetId = sheet.getProperties().getSheetId();
                    boolean matches = sheetId != null && sheetId.equals(gid);
                    if (matches) {
                        log.info("gid={}에 해당하는 시트를 찾았습니다: {}", gid, sheet.getProperties().getTitle());
                    }
                    return matches;
                })
                .map(sheet -> sheet.getProperties().getTitle())
                .findFirst()
                .orElseThrow(() -> createSheetNotFoundException(spreadsheet, gid));
    }

    /**
     * 첫 번째 시트 이름을 반환합니다.
     */
    private String getFirstSheetName(Spreadsheet spreadsheet) {
        String firstSheetName = spreadsheet.getSheets().get(0).getProperties().getTitle();
        log.info("gid가 없어 첫 번째 시트를 사용합니다: {}", firstSheetName);
        return firstSheetName;
    }

    /**
     * 시트를 찾을 수 없을 때 예외를 생성합니다.
     */
    private IllegalArgumentException createSheetNotFoundException(Spreadsheet spreadsheet, Integer gid) {
        String availableSheetIds = spreadsheet.getSheets().stream()
                .map(sheet -> String.valueOf(sheet.getProperties().getSheetId()))
                .collect(Collectors.joining(", "));

        return new IllegalArgumentException(
                String.format("gid=%d에 해당하는 시트를 찾을 수 없습니다. 사용 가능한 sheetId: [%s]", gid, availableSheetIds));
    }

    /**
     * 디버깅을 위한 시트 메타데이터 로그를 출력합니다.
     */
    private void logSheetMetadata(Spreadsheet spreadsheet, Integer targetGid) {
        log.info("스프레드시트의 모든 시트 정보:");
        spreadsheet.getSheets().forEach(sheet -> {
            Integer sheetId = sheet.getProperties().getSheetId();
            String title = sheet.getProperties().getTitle();
            log.info("  - sheetId: {}, title: {}", sheetId, title);
        });
        log.info("찾고 있는 gid: {}", targetGid);
    }
}
