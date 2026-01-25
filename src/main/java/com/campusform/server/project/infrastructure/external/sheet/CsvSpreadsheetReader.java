package com.campusform.server.project.infrastructure.external.sheet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;
import com.campusform.server.project.domain.service.SpreadsheetReader;

/**
 * CSV 파일을 읽어오는 스프레드시트 리더 구현체
 * Google OAuth 구현 전까지 사용하는 임시 구현체입니다.
 */
@Component
@Primary
public class CsvSpreadsheetReader implements SpreadsheetReader {

    @Override
    public List<SpreadsheetColumn> readHeader(String sheetUrl) {
        String csvUrl = getCsvUrl(sheetUrl);

        List<SpreadsheetColumn> columnInfos = new ArrayList<>();

        try {
            URL url = new URL(csvUrl);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {

                String header = reader.readLine();
                if (header == null || header.trim().isEmpty()) {
                    throw new IllegalArgumentException("CSV 파일의 헤더가 비어있습니다.");
                }

                String[] columns = header.split(",");
                for (int i = 0; i < columns.length; i++) {
                    String columnName = columns[i].trim();
                    if (!columnName.isEmpty()) {
                        columnInfos.add(new SpreadsheetColumn(columnName, i));
                    }
                }
            }
            return columnInfos;

        } catch (IOException e) {
            throw new RuntimeException("CSV 파일을 읽는 중 오류가 발생했습니다: " + sheetUrl, e);
        }
    }

    @Override
    public List<String[]> readAllLines(String sheetUrl) {
        String csvUrl = getCsvUrl(sheetUrl);
        List<String[]> parsedRows = new ArrayList<>();

        try {
            URL url = new URL(csvUrl);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {

                // 헤더 건너뛰기
                reader.readLine();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        // CSV 파싱: 콤마로 분리하여 배열로 변환
                        // 파싱 책임을 구현체에서 담당하므로, 서비스 레이어는 파싱된 데이터를 바로 사용할 수 있습니다.
                        String[] columns = line.split(",");
                        parsedRows.add(columns);
                    }
                }
            }
            return parsedRows;

        } catch (IOException e) {
            throw new RuntimeException("CSV 파일을 읽는 중 오류가 발생했습니다: " + sheetUrl, e);
        }
    }

    public static String getCsvUrl(String sheetUrl) {
        String id = sheetUrl.split("/d/")[1].split("/")[0];
        return "https://docs.google.com/spreadsheets/d/" + id + "/export?format=csv";
    }
}
