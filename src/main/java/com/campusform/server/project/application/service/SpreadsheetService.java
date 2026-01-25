package com.campusform.server.project.application.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.campusform.server.global.event.SheetSyncCompletedEvent;
import com.campusform.server.project.application.dto.SpreadsheetColumnResponse;
import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.project.domain.model.setting.ProjectAdmin;
import com.campusform.server.project.domain.model.setting.ProjectRequiredMapping;
import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;
import com.campusform.server.project.domain.repository.ProjectRepository;
import com.campusform.server.project.domain.service.SpreadsheetReader;
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

    private final SpreadsheetReader spreadsheetReader;
    private final ApplicationEventPublisher eventPublisher;

    private final ProjectRepository projectRepository;
    private final ApplicantRepository applicantRepository;

    /**
     * 스프레드시트 헤더 조회
     * 
     * 프로젝트 생성 시 컬럼 매핑을 위해 사용됩니다.
     */
    public List<SpreadsheetColumnResponse> getSheetHeaders(String sheetUrl) {
        return spreadsheetReader.readHeader(sheetUrl).stream()
                .filter(Objects::nonNull)
                .map(column -> new SpreadsheetColumnResponse(column.getName(), column.getIndex()))
                .toList();
    }

    /**
     * 스프레드시트 초기 동기화 -> 지원자의 응답 저장
     */
    public void syncInitialApplicants(String sheetUrl) {
        // 프로젝트 조회 및 검증
        Project project = projectRepository.findBySheetUrl(sheetUrl)
                .orElseThrow(() -> new IllegalArgumentException("sheetUrl에 해당하는 프로젝트가 존재하지 않습니다."));

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

        List<SpreadsheetColumn> headers = spreadsheetReader.readHeader(sheetUrl);
        List<String[]> dataRows = spreadsheetReader.readAllLines(sheetUrl);

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

            // 지원자 엔티티 생성
            Applicant applicant = Applicant.create(
                    project.getId(), name, email, phone, gender, school, major, position);

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
        List<Long> adminIds = getProjectAdminIds(project);
        eventPublisher.publishEvent(new SheetSyncCompletedEvent(
                project.getId(), adminIds, syncedCount, true
        ));
    }

    /**
     * 프로젝트의 모든 관리자 ID 목록 조회 (OWNER 포함, 중복 제거)
     */
    private List<Long> getProjectAdminIds(Project project) {
        Set<Long> adminIds = new LinkedHashSet<>();
        adminIds.add(project.getOwnerId());
        for (ProjectAdmin admin : project.getAdmins()) {
            adminIds.add(admin.getAdminId());
        }
        return List.copyOf(adminIds);
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
