package com.campusform.server.notification.application.eventhandler;

import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.campusform.server.notification.application.service.NotificationService;
import com.campusform.server.notification.domain.model.value.NotificationType;
import com.campusform.server.project.domain.event.sheet.SheetSyncChangeInfo;
import com.campusform.server.project.domain.event.sheet.SheetSyncCompletedEvent;
import com.campusform.server.project.domain.event.sheet.SheetSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 스프레드시트 동기화 완료 이벤트 알림 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SheetSyncNotificationHandler {

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
        List<SheetSyncChangeInfo> changes = event.changes();
        log.info("시트 동기화 완료 이벤트 수신 - projectId: {}, totalSynced: {}, newCount: {}, updatedCount: {}",
                event.projectId(),
                statistics.totalSyncedCount(),
                statistics.newApplicantCount(),
                statistics.updatedApplicantCount());

        if (changes == null || changes.isEmpty()) {
            return;
        }

        for (Long adminId : event.adminIds()) {
            for (SheetSyncChangeInfo change : changes) {
                try {
                    notificationService.createNotification(
                            adminId,
                            event.projectId(),
                            NotificationType.SHEET_SYNC_RESULT,
                            createSheetSyncPayload(event, change));
                } catch (Exception e) {
                    log.error("시트 동기화 알림 생성 실패 - adminId: {}, projectId: {}, applicantId: {}",
                            adminId, event.projectId(), change.applicantId(), e);
                }
            }
        }
    }

    private record SheetSyncPayload(String message, String projectTitle, int syncedCount) {
    }

    private String createSheetSyncPayload(SheetSyncCompletedEvent event, SheetSyncChangeInfo change) {
        SheetSyncPayload payload = new SheetSyncPayload(
                buildMessage(change),
                event.projectTitle(),
                event.statistics().totalSyncedCount());
        return toJson(payload);
    }

    private String buildMessage(SheetSyncChangeInfo change) {
        return switch (change.changeType()) {
            case NEW -> String.format("%s 님의 지원서가 추가되었습니다.", change.applicantName());
            case UPDATED -> String.format("%s 님의 지원서가 수정되었습니다.", change.applicantName());
        };
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload JSON", e);
            return "{}";
        }
    }
}
