package com.campusform.server.recruiting.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusform.server.project.domain.model.setting.Project;
import com.campusform.server.recruiting.application.dto.request.interview.SetRequiredInterviewerRequest;
import com.campusform.server.recruiting.application.dto.request.interview.UpdateRequiredInterviewersRequest;
import com.campusform.server.recruiting.application.dto.response.interview.RequiredInterviewersResponse;
import com.campusform.server.recruiting.application.service.InterviewContextLoader.InterviewContext;
import com.campusform.server.recruiting.domain.model.interview.setup.InterviewSetting;

import lombok.RequiredArgsConstructor;

/**
 * 필수 면접관 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequiredInterviewerService {

    private final InterviewContextLoader contextLoader;

    /**
     * 필수 면접관 목록 조회
     */
    public RequiredInterviewersResponse getRequiredInterviewers(Long projectId, Long userId) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        ctx.project().validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        List<Long> adminIds = setting.getRequiredInterviewerIds();
        return RequiredInterviewersResponse.of(adminIds);
    }

    /**
     * 필수 면접관 전체 교체
     */
    @Transactional
    public RequiredInterviewersResponse replaceAllRequiredInterviewers(
            Long projectId, Long userId, UpdateRequiredInterviewersRequest request) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        Project project = ctx.project();
        project.validateAdminAccess(userId);
        InterviewSetting setting = ctx.setting();

        // 요청에 포함된 면접관 ID가 프로젝트 관리자인지 검증
        // @NotNull 검증으로 null은 이미 차단되지만, 방어적 프로그래밍 차원에서 명시적으로 처리
        List<Long> adminIds = request.getAdminIds();
        if (adminIds != null) {
            for (Long adminId : adminIds) {
                project.validateAdminAccess(adminId);
            }
        }

        // 변경 감지(Dirty Checking)
        // 도메인 메서드에서 null을 안전하게 처리하지만, 서비스 계층에서도 명시적으로 처리
        setting.replaceRequiredInterviewers(adminIds);

        return RequiredInterviewersResponse.of(setting.getRequiredInterviewerIds());
    }

    /**
     * 필수 면접관 개별 상태 변경 (추가/제거)
     */
    @Transactional
    public RequiredInterviewersResponse updateRequiredInterviewerStatus(
            Long projectId, Long userId, Long adminId, SetRequiredInterviewerRequest request) {
        InterviewContext ctx = contextLoader.loadContext(projectId);
        Project project = ctx.project();
        project.validateAdminAccess(adminId);
        InterviewSetting setting = ctx.setting();

        // @NotNull 검증으로 null은 이미 차단되지만, 방어적 프로그래밍 차원에서 명시적으로 처리
        Boolean required = request.getRequired();
        if (required == null) {
            throw new IllegalArgumentException("필수 면접관 여부는 필수입니다.");
        }

        // 영속화된 엔티티이므로 변경 감지(Dirty Checking)로 자동 업데이트됨
        // 원시 boolean 타입으로 언박싱 (null이면 위에서 이미 예외 발생)
        setting.setRequiredInterviewer(adminId, required);

        return RequiredInterviewersResponse.of(setting.getRequiredInterviewerIds());
    }
}
