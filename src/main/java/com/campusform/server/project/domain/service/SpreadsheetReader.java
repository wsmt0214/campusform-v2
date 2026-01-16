package com.campusform.server.project.domain.service;

import java.util.List;

import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;

/**
 * 스프레드시트 데이터를 읽어오는 인터페이스
 * 
 * 다양한 스프레드시트 형식(CSV, Google Sheets 등)을 읽을 수 있도록 추상화합니다.
 * 파싱 로직은 각 구현체에서 처리하므로, 서비스 레이어는 파싱된 데이터를 바로 사용할 수 있습니다.
 */
public interface SpreadsheetReader {
    /**
     * 스프레드시트의 헤더(컬럼 정보)를 읽어옵니다.
     */
    List<SpreadsheetColumn> readHeader(String sheetUrl);
    
    /**
     * 스프레드시트의 모든 데이터 행을 읽어와 파싱된 배열로 반환합니다.
     * 
     * 각 행은 파싱되어 String 배열로 반환되므로, 서비스 레이어에서는 추가 파싱 없이 바로 사용할 수 있습니다.
     * 
     * @param sheetUrl 스프레드시트 URL
     * @return 파싱된 각 행의 데이터 배열 리스트 (헤더 제외)
     */
    List<String[]> readAllLines(String sheetUrl);
}
