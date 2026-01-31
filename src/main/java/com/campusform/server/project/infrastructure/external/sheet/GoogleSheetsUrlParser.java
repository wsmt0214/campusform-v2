package com.campusform.server.project.infrastructure.external.sheet;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Sheets URL을 파싱하는 유틸리티 클래스
 * 
 * URL에서 Spreadsheet ID와 Sheet ID(gid)를 추출합니다.
 */
@Slf4j
public final class GoogleSheetsUrlParser {

    private static final String SPREADSHEET_ID_PREFIX = "/d/";
    private static final String GID_PREFIX = "gid=";
    private static final int SPREADSHEET_ID_PREFIX_LENGTH = 3; // "/d/".length()
    private static final int GID_PREFIX_LENGTH = 4; // "gid=".length()

    // 유틸리티 클래스이므로 인스턴스화 방지
    private GoogleSheetsUrlParser() {
    }

    /**
     * Google Sheets URL에서 Spreadsheet ID를 추출합니다.
     */
    public static String extractSpreadsheetId(String sheetUrl) {
        int startIndex = sheetUrl.indexOf(SPREADSHEET_ID_PREFIX);
        if (startIndex == -1) {
            throw new IllegalArgumentException("유효하지 않은 Google Sheets URL입니다: " + sheetUrl);
        }

        startIndex += SPREADSHEET_ID_PREFIX_LENGTH;
        int endIndex = findEndIndex(sheetUrl, startIndex);

        String spreadsheetId = sheetUrl.substring(startIndex, endIndex);
        if (spreadsheetId.isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet ID를 추출할 수 없습니다: " + sheetUrl);
        }

        return spreadsheetId;
    }

    /**
     * Google Sheets URL에서 Sheet ID(gid)를 추출합니다.
     */
    public static Integer extractGid(String sheetUrl) {
        int gidIndex = findGidIndex(sheetUrl);
        if (gidIndex == -1) {
            return null;
        }

        try {
            String gidStr = sheetUrl.substring(gidIndex + GID_PREFIX_LENGTH);
            int endIndex = findEndIndex(gidStr, 0);
            return Integer.parseInt(gidStr.substring(0, endIndex));
        } catch (Exception e) {
            log.warn("gid 파싱 실패: {}", sheetUrl, e);
            return null;
        }
    }

    /**
     * URL에서 gid= 의 시작 위치를 찾습니다.
     * 우선순위: #gid= > ?gid= > gid=
     */
    private static int findGidIndex(String sheetUrl) {
        int hashGidIndex = sheetUrl.indexOf("#" + GID_PREFIX);
        if (hashGidIndex != -1) {
            return hashGidIndex + 1; // # 포함
        }

        int queryGidIndex = sheetUrl.indexOf("?" + GID_PREFIX);
        if (queryGidIndex != -1) {
            return queryGidIndex + 1; // ? 포함
        }

        int plainGidIndex = sheetUrl.indexOf(GID_PREFIX);
        return plainGidIndex;
    }

    /**
     * 문자열에서 구분자(/, ?, #, &) 중 가장 먼저 나오는 위치를 찾습니다.
     */
    private static int findEndIndex(String str, int startIndex) {
        int endIndex = str.length();
        int[] delimiters = {
                str.indexOf("/", startIndex),
                str.indexOf("?", startIndex),
                str.indexOf("#", startIndex),
                str.indexOf("&", startIndex)
        };

        for (int delimiterIndex : delimiters) {
            if (delimiterIndex != -1) {
                endIndex = Math.min(endIndex, delimiterIndex);
            }
        }

        return endIndex;
    }
}
