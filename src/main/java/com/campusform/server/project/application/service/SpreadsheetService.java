package com.campusform.server.project.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.global.event.ChangeType;
import com.campusform.server.global.event.SheetSyncChangeInfo;
import com.campusform.server.global.event.SheetSyncCompletedEvent;
import com.campusform.server.global.event.SheetSyncStatistics;
import com.campusform.server.project.application.dto.SpreadsheetColumnResponse;
import com.campusform.server.project.application.dto.response.SheetSyncResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.ProjectRequiredMapping;
import com.campusform.server.project.domain.model.setting.ProjectValueMapping;
import com.campusform.server.project.domain.model.setting.value.SyncStatus;
import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.project.infrastructure.external.sheet.GoogleSheetsReader;
import com.campusform.server.recruiting.domain.model.applicant.Applicant;
import com.campusform.server.recruiting.domain.model.applicant.ApplicantExtraAnswer;
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
     * 0번 인덱스이면서 헤더 이름이 'timestamp' 또는 '타임스탬프'인 컬럼은 응답에서 제외 (구글 폼 기본 컬럼)
     */
    public List<SpreadsheetColumnResponse> getSheetHeaders(String sheetUrl, Long ownerId) {
        return googleSheetsReader.readHeader(sheetUrl, ownerId).stream()
                .filter(Objects::nonNull)
                .filter(column -> !isTimestampHeader(column))
                .map(column -> new SpreadsheetColumnResponse(column.getName(), column.getIndex()))
                .toList();
    }

    /**
     * 0번 인덱스이고 이름이 timestamp/타임스탬프인 헤더인지 여부 (응답 제외 대상)
     */
    private static boolean isTimestampHeader(SpreadsheetColumn column) {
        if (column.getIndex() != 0) {
            return false;
        }
        String name = column.getName() != null ? column.getName().trim() : "";
        String lower = name.toLowerCase();
        return "timestamp".equals(lower) || "타임스탬프".equals(name);
    }

    /**
     * 시트 URL 기준 포지션 컬럼 고유값 목록 조회 (프로젝트 생성 전 호출 가능)
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctPositionValues(String sheetUrl, Long ownerId, Integer positionColumnIndex) {
        // -1 또는 미지정 시 포지션 미선택으로 간주 → 빈 목록 반환
        if (positionColumnIndex == null || positionColumnIndex < 0) {
            return List.of();
        }
        List<String[]> dataRows = googleSheetsReader.readAllLines(sheetUrl, ownerId);
        return dataRows.stream()
                .map(columns -> getColumnValue(columns, positionColumnIndex))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * 스프레드시트 동기화 -> 지원자의 응답 저장
     */
    @Transactional
    public SheetSyncResponse syncSheet(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다. projectId=" + projectId));

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

            // 포지션 값 치환 규칙: 시트 원시값 → 저장용 값 (동기화 시 적용)
            Map<String, String> positionMapping = project.getValueMappings().stream()
                    .collect(Collectors.toMap(ProjectValueMapping::getFromValue, ProjectValueMapping::getToValue,
                            (existing, replacement) -> existing));

            // 변경사항 추적을 위한 리스트 (이벤트용)
            List<SheetSyncChangeInfo> eventChanges = new ArrayList<>();
            // 응답 DTO용 변경사항 리스트
            List<SheetSyncResponse.ApplicantChangeInfo> responseChanges = new ArrayList<>();

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
                String positionRaw = getColumnValue(columns, mapping.getPositionIdx());
                // 포지션 치환 규칙
                String position = applyPositionMapping(positionRaw, positionMapping);

                // 기존 지원자 조회 (projectId, name, email로 식별)
                Applicant applicant = applicantRepository
                        .findByProjectIdAndNameAndEmail(project.getId(), name, email)
                        .orElse(null);

                if (applicant != null) { // 기존 응답 존재
                    List<String> changedFields = detectChangedFields(
                            applicant, columns, headers, mapping, requiredIndices, position);

                    if (!changedFields.isEmpty()) { // UPDATE 감지됨
                        // 필수 항목 UPDATE
                        applicant.updateFromSheet(phone, gender, school, major, position);

                        // 추가 항목 UPDATE
                        for (int i = 0; i < columns.length && i < headers.size(); i++) {
                            if (requiredIndices.contains(i))
                                continue;
                            String questionText = headers.get(i).getName();
                            String answerText = getColumnValue(columns, i);
                            applicant.addExtraAnswer(questionText, answerText, i);
                        }

                        applicantRepository.save(applicant); // Upsert로 동작
                        syncedCount++;

                        // 변경사항 기록 (이벤트용)
                        eventChanges.add(new SheetSyncChangeInfo(
                                applicant.getId(),
                                applicant.getName(),
                                ChangeType.UPDATED,
                                changedFields));
                        // 변경사항 기록 (응답 DTO용)
                        responseChanges.add(new SheetSyncResponse.ApplicantChangeInfo(
                                applicant.getId(),
                                applicant.getName(),
                                ChangeType.UPDATED,
                                changedFields));
                    } else {
                        syncedCount++;
                    }
                } else { // 기존 응답 미존재 = 새로운 row 추가
                    applicant = Applicant.create(
                            project.getId(), name, email, phone, gender, school, major, position);

                    // 매핑되지 않은 컬럼을 추가 질문으로 저장 (시트 헤더 순서 기준)
                    for (int i = 0; i < columns.length && i < headers.size(); i++) {
                        if (requiredIndices.contains(i))
                            continue;
                        String questionText = headers.get(i).getName();
                        String answerText = getColumnValue(columns, i);
                        // 시트 헤더의 인덱스를 순서로 저장하여 질문-답변 매칭 보장
                        applicant.addExtraAnswer(questionText, answerText, i);
                    }

                    // 새 지원자를 DB에 저장 (영속성 컨텍스트에 등록)
                    applicant = applicantRepository.save(applicant);
                    syncedCount++;

                    // 변경사항 기록 (이벤트용)
                    eventChanges.add(new SheetSyncChangeInfo(
                            applicant.getId(),
                            applicant.getName(),
                            ChangeType.NEW,
                            List.of()));
                    // 변경사항 기록 (응답 DTO용)
                    responseChanges.add(new SheetSyncResponse.ApplicantChangeInfo(
                            applicant.getId(),
                            applicant.getName(),
                            ChangeType.NEW,
                            List.of()));
                }
            }

            // 통계 정보 계산
            int newCount = (int) eventChanges.stream()
                    .filter(change -> change.changeType() == ChangeType.NEW)
                    .count();
            int updatedCount = (int) eventChanges.stream()
                    .filter(change -> change.changeType() == ChangeType.UPDATED)
                    .count();
            SheetSyncStatistics statistics = new SheetSyncStatistics(
                    syncedCount, newCount, updatedCount);

            // 이벤트 발행
            List<Long> adminIds = project.getAdminIds();
            List<SheetSyncChangeInfo> finalChanges = eventChanges.isEmpty() ? null : eventChanges;
            String projectTitle = project.getTitle() != null ? project.getTitle().trim() : null;
            eventPublisher.publishEvent(new SheetSyncCompletedEvent(
                    project.getId(), projectTitle, adminIds, statistics, finalChanges));

            project.updateSyncStatus(SyncStatus.OK);
            projectRepository.save(project);

            return SheetSyncResponse.success(syncedCount, responseChanges);
        } catch (Exception e) {
            project.updateSyncStatus(SyncStatus.ERROR);
            projectRepository.save(project);
            throw e;
        }
    }

    /**
     * 컬럼 배열에서 특정 인덱스의 값을 안전하게 추출
     * 인덱스가 유효하지 않거나 값이 비어있으면 null을 반환합니다.
     */
    private String getColumnValue(String[] columns, Integer index) {
        if (index == null || index < 0 || index >= columns.length) {
            return null;
        }
        String value = columns[index].trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 포지션 치환 규칙 적용: 시트 원시값이 규칙에 있으면 저장용 값으로 치환
     */
    private String applyPositionMapping(String raw, Map<String, String> positionMapping) {
        if (raw == null)
            return null;
        return positionMapping.getOrDefault(raw, raw);
    }

    /**
     * 기존 지원자와 시트 데이터를 비교하여 변경된 필드를 감지합니다.
     */
    private List<String> detectChangedFields(
            Applicant applicant,
            String[] columns,
            List<SpreadsheetColumn> headers,
            ProjectRequiredMapping mapping,
            Set<Integer> requiredIndices,
            String appliedPosition) {

        List<String> changedFields = new ArrayList<>();

        // 필수 필드 비교
        String newPhone = getColumnValue(columns, mapping.getPhoneIdx());
        if (!Objects.equals(applicant.getPhone(), newPhone)) {
            String headerName = getHeaderName(headers, mapping.getPhoneIdx());
            if (headerName != null) {
                changedFields.add(headerName);
            }
        }

        String newGender = getColumnValue(columns, mapping.getGenderIdx());
        if (!Objects.equals(applicant.getGender(), newGender)) {
            String headerName = getHeaderName(headers, mapping.getGenderIdx());
            if (headerName != null) {
                changedFields.add(headerName);
            }
        }

        String newSchool = getColumnValue(columns, mapping.getSchoolIdx());
        if (!Objects.equals(applicant.getSchool(), newSchool)) {
            String headerName = getHeaderName(headers, mapping.getSchoolIdx());
            if (headerName != null) {
                changedFields.add(headerName);
            }
        }

        String newMajor = getColumnValue(columns, mapping.getMajorIdx());
        if (!Objects.equals(applicant.getMajor(), newMajor)) {
            String headerName = getHeaderName(headers, mapping.getMajorIdx());
            if (headerName != null) {
                changedFields.add(headerName);
            }
        }

        if (!Objects.equals(applicant.getPosition(), appliedPosition)) {
            String headerName = getHeaderName(headers, mapping.getPositionIdx());
            if (headerName != null) {
                changedFields.add(headerName);
            }
        }

        // 추가 질문 비교: 기존 extraAnswers를 Map으로 변환하여 효율적으로 비교
        Map<String, String> existingExtraAnswers = applicant.getExtraAnswers().stream()
                .collect(Collectors.toMap(
                        ApplicantExtraAnswer::getQuestionText,
                        answer -> answer.getAnswerText() != null ? answer.getAnswerText() : "",
                        (existing, replacement) -> existing // 중복 키가 있으면 기존 값 유지
                ));

        // 시트의 추가 질문과 비교
        for (int i = 0; i < columns.length && i < headers.size(); i++) {
            if (requiredIndices.contains(i))
                continue;

            String questionText = headers.get(i).getName();
            String newAnswer = getColumnValue(columns, i);
            String oldAnswer = existingExtraAnswers.getOrDefault(questionText, "");

            // null과 빈 문자열을 동일하게 처리
            String normalizedNewAnswer = (newAnswer == null) ? "" : newAnswer;
            String normalizedOldAnswer = (oldAnswer == null) ? "" : oldAnswer;

            if (!normalizedNewAnswer.equals(normalizedOldAnswer)) {
                changedFields.add(questionText);
            }
        }

        return changedFields;
    }

    /**
     * 헤더 리스트에서 인덱스에 해당하는 헤더 이름을 조회합니다.
     * 
     * @param headers 헤더 리스트
     * @param index   조회할 인덱스
     * @return 헤더 이름 (인덱스가 유효하지 않으면 null)
     */
    private String getHeaderName(List<SpreadsheetColumn> headers, Integer index) {
        if (index == null || index < 0 || index >= headers.size()) {
            return null;
        }
        return headers.get(index).getName();
    }
}
