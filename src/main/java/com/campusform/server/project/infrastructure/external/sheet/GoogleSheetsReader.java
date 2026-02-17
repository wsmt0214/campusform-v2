package com.campusform.server.project.infrastructure.external.sheet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;
import com.campusform.server.project.domain.service.SpreadsheetReader;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Sheets API를 사용하여 시트 데이터를 읽어오는 구현체
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetsReader implements SpreadsheetReader {

    private static final String HEADER_RANGE_TEMPLATE = "'%s'!A1:XFD1";
    private static final String DATA_RANGE_TEMPLATE = "'%s'!A2:XFD";
    private static final String VALUES_GET_BASE = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s";

    private final GoogleSheetsServiceFactory serviceFactory;

    @Override
    public List<SpreadsheetColumn> readHeader(String sheetUrl, Long ownerId) {
        return executeSheetRead(sheetUrl, ownerId, this::buildHeaderRange, this::parseHeaderResponse);
    }

    @Override
    public List<String[]> readAllLines(String sheetUrl, Long ownerId) {
        return executeSheetRead(sheetUrl, ownerId, this::buildDataRange, this::parseDataResponse);
    }

    /**
     * 시트 읽기 작업을 실행하는 공통 메서드
     */
    private <T> T executeSheetRead(
            String sheetUrl,
            Long ownerId,
            RangeBuilder rangeBuilder,
            ResponseParser<T> responseParser) {

        try {
            log.info("Google Sheets 읽기 시작 sheetUrl={}", sheetUrl);
            // 시트에 접근하기 위한 컨텍스트(서비스, 스프레드시트 ID, 시트 이름) 준비
            SheetContext context = prepareSheetContext(sheetUrl, ownerId);
            // 데이터 읽기 범위 문자열 구성 (ex: '시트명'!A1:XFD1 혹은 '시트명'!A2:XFD)
            String range = rangeBuilder.build(context.sheetName());
            ValueRange response = fetchValues(context.sheetsService(), context.spreadsheetId(), range);
            // API의 응답을 도메인 객체 또는 배열 등으로 파싱
            T result = responseParser.parse(response);

            return result;

        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Google Sheets를 읽는 중 오류가 발생했습니다: %s", e.getMessage()), e);
        }
    }

    /**
     * 헤더 응답을 파싱합니다.
     */
    private List<SpreadsheetColumn> parseHeaderResponse(ValueRange response) {
        List<Object> headerRow = extractFirstRow(response);
        List<SpreadsheetColumn> columns = parseHeaderRow(headerRow);
        log.info("컬럼 수: {}", columns.size());
        return columns;
    }

    /**
     * 데이터 응답을 파싱합니다.
     */
    private List<String[]> parseDataResponse(ValueRange response) {
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            log.info("시트에 데이터가 없습니다.");
            return Collections.emptyList();
        }
        List<String[]> parsedRows = parseDataRows(values);
        log.info("행 수: {}", parsedRows.size());
        return parsedRows;
    }

    /**
     * 시트 작업에 필요한 컨텍스트를 준비합니다.
     */
    private SheetContext prepareSheetContext(String sheetUrl, Long ownerId) throws IOException {
        Sheets sheetsService = serviceFactory.createSheetsService(ownerId);
        String spreadsheetId = GoogleSheetsUrlParser.extractSpreadsheetId(sheetUrl);
        String sheetName = getSheetName(sheetsService, spreadsheetId, sheetUrl);
        return new SheetContext(sheetsService, spreadsheetId, sheetName);
    }

    /**
     * 시트 이름을 조회합니다.
     */
    private String getSheetName(Sheets sheetsService, String spreadsheetId, String sheetUrl) throws IOException {
        Integer gid = GoogleSheetsUrlParser.extractGid(sheetUrl);
        GoogleSheetsMetadataService metadataService = new GoogleSheetsMetadataService(sheetsService);
        return metadataService.getSheetName(spreadsheetId, gid);
    }

    /**
     * Google Sheets API에서 값을 가져옵니다.
     */
    private ValueRange fetchValues(Sheets sheetsService, String spreadsheetId, String range) throws IOException {
        return sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
    }

    /**
     * 헤더 범위를 생성합니다.
     */
    private String buildHeaderRange(String sheetName) {
        return String.format(HEADER_RANGE_TEMPLATE, sheetName);
    }

    /**
     * 데이터 범위를 생성합니다.
     */
    private String buildDataRange(String sheetName) {
        return String.format(DATA_RANGE_TEMPLATE, sheetName);
    }

    /**
     * 응답에서 첫 번째 행을 추출합니다.
     */
    private List<Object> extractFirstRow(ValueRange response) {
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("시트 헤더가 비어있습니다.");
        }
        return values.get(0);
    }

    /**
     * 헤더 행을 파싱하여 SpreadsheetColumn 리스트로 변환합니다.
     */
    private List<SpreadsheetColumn> parseHeaderRow(List<Object> headerRow) {
        List<SpreadsheetColumn> columns = new ArrayList<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String columnName = extractStringValue(headerRow.get(i));
            if (!columnName.isEmpty()) {
                columns.add(new SpreadsheetColumn(columnName, i));
            }
        }
        return columns;
    }

    /**
     * 데이터 행들을 파싱하여 String 배열 리스트로 변환합니다.
     */
    private List<String[]> parseDataRows(List<List<Object>> values) {
        List<String[]> parsedRows = new ArrayList<>();
        for (List<Object> row : values) {
            String[] rowArray = new String[row.size()];
            for (int i = 0; i < row.size(); i++) {
                rowArray[i] = extractStringValue(row.get(i));
            }
            parsedRows.add(rowArray);
        }
        return parsedRows;
    }

    /**
     * 객체를 문자열로 변환합니다. null이면 빈 문자열을 반환합니다.
     */
    private String extractStringValue(Object value) {
        return value != null ? value.toString().trim() : "";
    }

    /**
     * 범위를 생성하는 함수형 인터페이스
     */
    @FunctionalInterface
    private interface RangeBuilder {
        String build(String sheetName);
    }

    /**
     * 응답을 파싱하는 함수형 인터페이스
     */
    @FunctionalInterface
    private interface ResponseParser<T> {
        T parse(ValueRange response);
    }

    /**
     * 시트 작업에 필요한 컨텍스트 정보를 담는 레코드
     */
    private record SheetContext(
            Sheets sheetsService,
            String spreadsheetId,
            String sheetName) {
    }
}
