package com.campusform.server.recruiting.application.eventhandler;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.campusform.server.recruiting.application.port.SmsSender;
import com.campusform.server.recruiting.application.service.SmsMessageComposer;
import com.campusform.server.recruiting.domain.model.event.ApplicantUpdated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지원자 상태 변경 이벤트 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicantEventHandler {

    private final SmsSender smsSender;
    private final SmsMessageComposer smsMessageComposer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleApplicantUpdated(ApplicantUpdated event) {
        try {
            String message = smsMessageComposer.compose(
                    event.projectId(),
                    event.stage(),
                    event.status(),
                    event.applicantName(),
                    event.positionName());

            // 템플릿이 없거나 전화번호가 없으면 발송 스킵
            if (message != null && event.applicantPhone() != null) {
                smsSender.sendSms(event.applicantPhone(), message);
                log.info("문자 발송 완료: 지원자 ID={}", event.applicantId());
            }
        } catch (Exception e) {
            log.error("문자 발송 실패: 지원자 ID={}, 원인={}", event.applicantId(), e.getMessage(), e);
        }
    }
}
