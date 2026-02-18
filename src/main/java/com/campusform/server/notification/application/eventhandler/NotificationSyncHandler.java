package com.campusform.server.notification.application.eventhandler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.campusform.server.global.event.ChangeType;
import com.campusform.server.global.event.SheetSyncChangeInfo;
import com.campusform.server.global.event.SheetSyncCompletedEvent;
import com.campusform.server.global.event.SheetSyncStatistics;
import com.campusform.server.notification.application.service.NotificationService;
import com.campusform.server.notification.domain.model.value.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 이벤트 핸들러
 *
 * 다른 Context에서 발행된 도메인 이벤트를 수신하여 알림을 생성합니다.
 * 비동기 처리(@Async)를 통해 이벤트 발행자에게 영향을 주지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSyncHandler {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * 시트 동기화 완료 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleSheetSyncCompleted(SheetSyncCompletedEvent event) {
        if (!event.isSuccess()) {
            return;
        }

        SheetSyncStatistics statistics = event.statistics();
        log.info("시트 동기화 완료 이벤트 수신 - projectId: {}, totalSynced: {}, newCount: {}, updatedCount: {}",
                event.projectId(),
                statistics.totalSyncedCount(),
                statistics.newApplicantCount(),
                statistics.updatedApplicantCount());

        String payload = createSheetSyncPayload(event);
        for (Long adminId : event.adminIds()) {
            try {
                notificationService.createNotification(
                        adminId, event.projectId(),
                        NotificationType.SHEET_SYNC_RESULT, payload);
            } catch (Exception e) {
                log.error("시트 동기화 알림 생성 실패 - adminId: {}, projectId: {}",
                        adminId, event.projectId(), e);
            }
        }
    }

    private record SheetSyncPayload(String message, int syncedCount) {
    }

    private String createSheetSyncPayload(SheetSyncCompletedEvent event) {
        SheetSyncStatistics statistics = event.statistics();
        String message = buildMessage(event);
        SheetSyncPayload payload = new SheetSyncPayload(message, statistics.totalSyncedCount());
        return toJson(payload);
    }

    /**
     * 통계 정보와 변경사항을 기반으로 사용자 친화적인 메시지를 생성합니다.
     * 지원자 이름을 괄호 안에 포함합니다.
     */
    private String buildMessage(SheetSyncCompletedEvent event) {
        SheetSyncStatistics statistics = event.statistics();

        if (!statistics.hasChanges()) {
            return String.format("스프레드시트 동기화가 완료되었습니다. %d명의 지원자가 동기화되었습니다.",
                    statistics.totalSyncedCount());
        }

        List<SheetSyncChangeInfo> changes = event.changes();
        if (changes == null || changes.isEmpty()) {
            return String.format("스프레드시트 동기화가 완료되었습니다. %d명의 지원자가 동기화되었습니다.",
                    statistics.totalSyncedCount());
        }

        StringBuilder message = new StringBuilder("스프레드시트 동기화가 완료되었습니다. ");

        // 새 지원자 이름 추출
        List<String> newApplicantNames = changes.stream()
                .filter(change -> change.changeType() == ChangeType.NEW)
                .map(SheetSyncChangeInfo::applicantName)
                .collect(Collectors.toList());

        if (!newApplicantNames.isEmpty()) {
            String namesText = String.join(", ", newApplicantNames);
            message.append(String.format("새 지원자 %d명(%s)이 추가되었습니다. ",
                    newApplicantNames.size(), namesText));
        }

        // 업데이트된 지원자 이름 추출
        List<String> updatedApplicantNames = changes.stream()
                .filter(change -> change.changeType() == ChangeType.UPDATED)
                .map(SheetSyncChangeInfo::applicantName)
                .collect(Collectors.toList());

        if (!updatedApplicantNames.isEmpty()) {
            String namesText = String.join(", ", updatedApplicantNames);
            message.append(String.format("%d명의 지원자 정보(%s)가 업데이트되었습니다.",
                    updatedApplicantNames.size(), namesText));
        }

        return message.toString().trim();
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Payload JSON 직렬화 실패", e);
            return "{}";
        }
    }
}
