package com.campusform.server.project.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.campusform.server.global.event.SheetSyncCompletedEvent;
import com.campusform.server.project.application.dto.SpreadsheetColumnResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.ProjectRequiredMapping;
import com.campusform.server.project.domain.model.setting.value.SyncStatus;
import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.project.infrastructure.external.sheet.GoogleSheetsReader;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.repository.ApplicantRepository;

import lombok.RequiredArgsConstructor;

/**
 * 스프레드시트 관련 비즈니스 로직을 처리하는 서비스
 * 
 * 스프레드시트 헤더 조회 및 지원자 데이터 동기화 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class SpreadsheetService {

    private final GoogleSheetsReader googleSheetsReader;
    private final ApplicationEventPublisher eventPublisher;

    private final ProjectRepository projectRepository;
    private final ApplicantRepository applicantRepository;

    /**
     * 스프레드시트 헤더 조회 -> 칼럼 매핑 위함
     */
    public List<SpreadsheetColumnResponse> getSheetHeaders(String sheetUrl, Long ownerId) {
        return googleSheetsReader.readHeader(sheetUrl, ownerId).stream()
                .filter(Objects::nonNull)
                .map(column -> new SpreadsheetColumnResponse(column.getName(), column.getIndex()))
                .toList();
    }

    /**
     * 스프레드시트 동기화 -> 지원자의 응답 저장
     */
    public int syncSheet(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

        // 프로젝트의 sheetUrl 사용
        String sheetUrl = project.getSheetUrl();
        Long ownerId = project.getOwnerId();

        try {
            // 필수 매핑 정보를 Set에 저장하여 중복 제거 및 편의성 제공
            ProjectRequiredMapping mapping = project.getMapping();
            Set<Integer> requiredIndices = new HashSet<>();
            requiredIndices.add(mapping.getNameIdx());
            requiredIndices.add(mapping.getEmailIdx());
            requiredIndices.add(mapping.getPhoneIdx());
            requiredIndices.add(mapping.getGenderIdx());
            requiredIndices.add(mapping.getSchoolIdx());
            requiredIndices.add(mapping.getMajorIdx());
            requiredIndices.add(mapping.getPositionIdx());

            // Google Sheets API를 사용하여 헤더 및 데이터 읽기
            List<SpreadsheetColumn> headers = googleSheetsReader.readHeader(sheetUrl, ownerId);
            List<String[]> dataRows = googleSheetsReader.readAllLines(sheetUrl, ownerId);

            // 각 행을 지원자로 변환
            int syncedCount = 0;
            for (String[] columns : dataRows) {
                // 필수 필드 추출
                String name = getColumnValue(columns, mapping.getNameIdx());
                String email = getColumnValue(columns, mapping.getEmailIdx());
                String phone = getColumnValue(columns, mapping.getPhoneIdx());
                String gender = getColumnValue(columns, mapping.getGenderIdx());
                String school = getColumnValue(columns, mapping.getSchoolIdx());
                String major = getColumnValue(columns, mapping.getMajorIdx());
                String position = getColumnValue(columns, mapping.getPositionIdx());

                // 필수 필드 검증
                if (name == null || name.isEmpty() || email == null || email.isEmpty()) {
                    continue; // 이름이나 이메일이 없으면 건너뜀
                }

                // 기존 지원자 조회 (projectId, name, email로 식별)
                Applicant applicant = applicantRepository
                        .findByProjectIdAndNameAndEmail(project.getId(), name, email)
                        .orElse(null);

                if (applicant != null) {
                    // 기존 지원자 업데이트 (심사 상태와 즐겨찾기는 유지)
                    applicant.updateFromSheet(phone, gender, school, major, position);
                } else {
                    // 새 지원자 생성
                    applicant = Applicant.create(
                            project.getId(), name, email, phone, gender, school, major, position);
                }

                // 매핑되지 않은 컬럼을 추가 질문으로 저장
                for (int i = 0; i < columns.length && i < headers.size(); i++) {
                    if (requiredIndices.contains(i))
                        continue;
                    String questionText = headers.get(i).getName();
                    String answerText = getColumnValue(columns, i);
                    // 연관 관계 설정
                    applicant.addExtraAnswer(questionText, answerText);
                }

                applicantRepository.save(applicant);
                syncedCount++;
            }

            // 시트 동기화 완료 이벤트 발행
            List<Long> adminIds = project.getAdminIds();
            eventPublisher.publishEvent(new SheetSyncCompletedEvent(
                    project.getId(), adminIds, syncedCount, true));

            // 프로젝트 동기화 상태 업데이트 (성공)
            project.updateSyncStatus(SyncStatus.OK);
            projectRepository.save(project);

            return syncedCount;
        } catch (Exception e) {
            // 프로젝트 동기화 상태 업데이트 (실패)
            project.updateSyncStatus(SyncStatus.ERROR);
            projectRepository.save(project);
            throw e;
        }
    }

    /**
     * 컬럼 배열에서 특정 인덱스의 값을 안전하게 추출
     * 
     * 인덱스가 유효하지 않거나 값이 비어있으면 null을 반환합니다.
     */
    private String getColumnValue(String[] columns, Integer index) {
        if (index == null || index < 0 || index >= columns.length) {
            return null;
        }
        String value = columns[index].trim();
        return value.isEmpty() ? null : value;
    }
}
