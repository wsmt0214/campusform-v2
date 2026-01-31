package com.campusform.server.project.domain.service;

import java.util.List;

import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;

/**
 * 스프레드시트 데이터를 읽어오는 인터페이스
 */
public interface SpreadsheetReader {

    List<SpreadsheetColumn> readHeader(String sheetUrl, Long ownerId);

    List<String[]> readAllLines(String sheetUrl, Long ownerId);
}
